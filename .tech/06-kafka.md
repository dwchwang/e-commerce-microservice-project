# 06. Kafka KRaft + Saga + Outbox Pattern

> Kafka là **message broker** cho mọi giao dịch async giữa các service. Trong project này, Kafka điều phối **saga đặt hàng** (order → inventory → payment → notification) với hai pattern quan trọng: **Outbox** (đảm bảo at-least-once publish) và **Processed Event** (đảm bảo idempotent consume).

## 1. Khái niệm cốt lõi

| Khái niệm | Ý nghĩa |
|-----------|---------|
| **Topic** | Hàng đợi message theo tên (vd `order-created`, `payment-success`) |
| **Partition** | Chia topic ra nhiều phần để parallel consume. Cùng key → cùng partition → giữ thứ tự |
| **Producer** | Service publish message |
| **Consumer Group** | Nhóm consumer chia phần partition cho nhau. 1 partition chỉ 1 consumer trong group đọc |
| **Offset** | Vị trí đọc trong partition. Cam kết offset (`commit`) = "tao đã xử lý đến đây" |
| **KRaft** | Kafka 3.x mode mới — bỏ Zookeeper, broker tự bầu controller |
| **Saga** | Chuỗi local transaction nối với nhau qua event, có **compensating action** khi 1 bước fail |
| **Outbox Pattern** | Ghi event vào DB cùng transaction với business → poller bơm sang Kafka. Tránh mất event nếu Kafka down |
| **Idempotent Consumer** | Consumer xử lý cùng message N lần ra cùng kết quả (nhờ bảng `processed_events`) |

## 2. Hệ thống đang dùng ra sao

### 2.1 Cấu hình thực tế

- **Container**: `confluentinc/cp-kafka:8.2.0` chạy KRaft mode (không Zookeeper)
- **Bootstrap servers**:
  - Trong Docker: `kafka:29092`
  - Từ host: `localhost:9092`
- **Replication factor = 1** (single broker, dev only)

Cấu hình Spring Kafka mẫu (xem [order-service.yml](../config-server/src/main/resources/configs/order-service.yml)):
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: order-service       # ← group-id quyết định ai đọc partition nào
      auto-offset-reset: earliest   # ← consumer mới đọc từ đầu topic
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    listener:
      ack-mode: record              # ← commit offset sau mỗi message thành công
```

### 2.2 Saga đặt hàng — luồng event

```
1. Client → POST /api/orders
2. order-service:
   - Tạo Order (status=PENDING) trong DB
   - Ghi OutboxEvent("ORDER_CREATED") cùng transaction
   - Commit DB
3. OutboxPoller (chạy mỗi 1s):
   - Đọc outbox chưa processed
   - Publish lên topic "order-created"
   - Mark processed=true
4. inventory-service consume "order-created":
   - Reserve stock (giảm available, tăng reserved)
   - Publish "inventory-reserved" hoặc "inventory-failed"
5. order-service consume "inventory-reserved":
   - Update Order status=AWAITING_PAYMENT
   - Ghi outbox "PAYMENT_REQUESTED" → topic "payment-requested"
6. payment-service consume "payment-requested":
   - Khởi tạo VNPAY transaction
   - Khi user pay xong → publish "payment-success" hoặc "payment-failed"
7. order-service consume "payment-success":
   - Update Order status=CONFIRMED
   - Ghi outbox "ORDER_CONFIRMED" → topic "order-confirmed"
8. notification-service consume "order-confirmed":
   - Gửi email qua Mailpit
9. search-service consume product events:
   - Cập nhật index Elasticsearch
```

Khi bước nào fail → publish event compensating (`order-cancelled`, `inventory-failed`...) để rollback các bước trước.

### 2.3 Outbox Pattern — tại sao cần?

**Vấn đề**: Nếu service làm 2 bước rời rạc:
```java
orderRepository.save(order);              // (a) DB commit
kafkaTemplate.send("order-created", ...); // (b) Kafka send
```
- (a) thành công, (b) fail → đơn hàng có trong DB nhưng inventory không biết → đơn treo mãi
- (b) thành công, (a) fail (rollback) → inventory reserve cho đơn không tồn tại

**Giải pháp Outbox**: gộp ghi event vào DB cùng business transaction:

```sql
-- Cùng 1 transaction
INSERT INTO orders (...) VALUES (...);
INSERT INTO outbox_events (event_type, aggregate_id, payload, processed)
VALUES ('ORDER_CREATED', :orderId, :json, false);
COMMIT;
```

Sau đó **OutboxPoller** ([OutboxPoller.java](../order-service/src/main/java/com/ecommerce/order/scheduler/OutboxPoller.java)) chạy nền:
```java
@Scheduled(fixedDelay = 1_000)
@Transactional
public void pollOutbox() {
    List<OutboxEvent> events = outboxRepository.findUnprocessedBatch();
    for (OutboxEvent event : events) {
        kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get();
        event.setProcessed(true);
        event.setProcessedAt(LocalDateTime.now());
        outboxRepository.save(event);
    }
}
```

Đảm bảo **at-least-once**: event sẽ được publish ít nhất 1 lần, kể cả khi Kafka tạm down.

### 2.4 Idempotent Consumer — chống xử lý trùng

Vì Outbox = at-least-once → consumer có thể nhận message 2 lần. Pattern dùng bảng `processed_events`:

```java
public void handle(InventoryReservedEvent event) {
    if (processedEventRepository.existsById(event.getEventId())) {
        return;  // đã xử lý rồi, skip
    }
    // ... business logic ...
    processedEventRepository.save(new ProcessedEvent(event.getEventId(), ...));
}
```

Đảm bảo **effectively-once**: cùng `eventId` → cùng kết quả, không double-update.

## 3. Workflow vận hành

### 3.1 List và monitor topic

```bash
# Vào shell của Kafka container
docker compose exec kafka bash

# Liệt kê topic
kafka-topics --bootstrap-server localhost:9092 --list

# Mô tả topic
kafka-topics --bootstrap-server localhost:9092 --describe --topic order-created

# Xem messages real-time
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-created --from-beginning --max-messages 10

# Xem dưới dạng key + value
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-created --from-beginning \
  --property print.key=true --property print.value=true \
  --property key.separator=" :: "
```

### 3.2 Publish message thủ công (test consumer)

```bash
docker compose exec kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic order-created \
  --property "parse.key=true" \
  --property "key.separator=:"

# Gõ:
order-123:{"eventId":"...","orderId":"order-123","items":[...]}
```

### 3.3 Xem consumer lag

```bash
# Liệt kê consumer group
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 --list

# Xem chi tiết: lag = số message chưa consume
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group order-service --describe
```

Output:
```
TOPIC          PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
order-created  0          15              15              0      ← good
payment-success 0         8               42              34     ← lag, consumer chậm
```

### 3.4 Reset consumer offset (replay events)

Hữu ích khi muốn re-process toàn bộ event từ đầu (ví dụ vừa reset DB):

```bash
# Stop consumer trước (consumer phải offline)
docker compose stop order-service

# Reset về earliest
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group order-service \
  --topic payment-success \
  --reset-offsets --to-earliest --execute

# Start lại
docker compose start order-service
```

### 3.5 UI tools (optional)

Cài thêm AKHQ hoặc Kafka UI:
```yaml
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  ports:
    - "8090:8080"   # Note: trùng review-service, đổi cổng nếu cần
  environment:
    KAFKA_CLUSTERS_0_NAME: local
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
  networks:
    - ecommerce-network
```

### 3.6 Debug saga đang stuck

Khi đơn hàng "treo" ở 1 status:

```bash
# 1. Tìm orderId trong DB
docker compose exec postgres psql -U postgres -d order_db \
  -c "SELECT id, status, created_at FROM orders ORDER BY created_at DESC LIMIT 5;"

# 2. Xem outbox còn pending không
docker compose exec postgres psql -U postgres -d order_db \
  -c "SELECT id, event_type, processed, created_at FROM outbox_events WHERE processed=false;"

# 3. Xem processed_events đã ghi nhận event nào
docker compose exec postgres psql -U postgres -d order_db \
  -c "SELECT * FROM processed_events ORDER BY created_at DESC LIMIT 10;"

# 4. Xem consumer lag — service consume chậm hay đang chết?
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group order-service --describe

# 5. Xem trace bằng Zipkin (xem 04-zipkin.md)
```

### 3.7 Test 1 saga end-to-end

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@ecommerce.local","password":"Password123!"}' \
  | jq -r .accessToken)

# Tạo đơn
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"productId": 1, "quantity": 2}],
    "paymentMethod": "VNPAY",
    "shippingName": "Test",
    "shippingPhone": "0900000000",
    "shippingAddress": "Hanoi"
  }'

# Theo dõi log của các service liên quan
docker compose logs -f order-service inventory-service payment-service notification-service
```

## 4. Troubleshooting

### 4.1 Producer error: "TimeoutException: Topic not present"

Topic chưa tồn tại. Project dùng `auto.create.topics.enable=true` mặc định của Kafka, topic sẽ được tạo lần publish đầu. Nếu vẫn fail:

```bash
# Tạo topic thủ công
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic order-created \
  --partitions 3 --replication-factor 1
```

### 4.2 Consumer không nhận được message

Theo thứ tự kiểm tra:
1. Consumer có subscribe đúng topic? — xem `@KafkaListener(topics = ...)`
2. Consumer group đúng? — group-id trùng = chia partition; group-id khác = mỗi group đọc copy riêng
3. `auto-offset-reset: earliest` — nếu là `latest`, consumer mới chỉ đọc message từ thời điểm join group
4. Lag = 0 nhưng vẫn không xử lý → consumer chết, restart service

### 4.3 Outbox poller không publish

```bash
# Xem có event pending không
docker compose exec postgres psql -U postgres -d order_db \
  -c "SELECT count(*) FROM outbox_events WHERE processed=false;"

# Xem log poller
docker compose logs order-service | grep -i outbox
```

Common causes:
- `OutboxRepository.findUnprocessedBatch()` query sai
- Schedule chưa active — kiểm tra `@EnableScheduling`
- Transaction rollback do exception — log sẽ show

### 4.4 Message bị xử lý trùng

Pattern đúng đã có sẵn trong project (`processed_events`). Nếu vẫn trùng:
- `eventId` không unique (UUID phải sinh 1 lần ở producer, không sinh lại ở consumer)
- Không kiểm tra trước khi xử lý — đảm bảo `if (processedEventRepository.existsById(eventId)) return;` ở đầu method

### 4.5 Saga stuck — bước giữa fail không compensate

Project hiện handle compensation manual qua event flow (publish `order-cancelled` khi inventory fail). Nếu thấy đơn treo:

1. Xem `payment-failed` topic có message không
2. order-service có consume `payment-failed` không
3. Có ghi outbox `ORDER_CANCELLED` không

Dài hạn nên dùng **Saga Orchestrator** (Camunda, Temporal) thay vì choreography để dễ trace và rollback.

### 4.6 Reset toàn bộ Kafka (mất data)

```bash
docker compose down
docker volume rm ecommerce-microservice-project_kafka_data
docker compose up -d kafka
```

## 5. Best practices đang được áp dụng

- **KRaft mode** — không cần Zookeeper, đơn giản hơn cho dev
- **Outbox pattern** — atomic giữa DB write và event publish
- **Idempotent consumer (`processed_events`)** — chống double-process
- **String key (= aggregateId)** — cùng order → cùng partition → giữ thứ tự event
- **`ack-mode: record`** — commit offset sau mỗi message, đánh đổi throughput lấy reliability
- **Tên topic theo event past-tense** (`order-created`, `payment-success`) — phản ánh fact đã xảy ra, không phải command
- **EventId UUID** — định danh duy nhất cho idempotency, không dùng id business
