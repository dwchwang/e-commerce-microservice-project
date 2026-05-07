# 08. Event-Driven Architecture với Apache Kafka

## 1. Mục Tiêu Nghiên Cứu

- Hiểu Event-Driven Architecture (EDA)
- Hiểu Apache Kafka — broker, topic, partition, offset, consumer group
- Phân biệt Kafka với RabbitMQ, ActiveMQ
- Hiểu delivery guarantees: at-most-once, at-least-once, exactly-once
- Hiểu Kafka KRaft (không Zookeeper)

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Event-Driven Architecture (EDA)

Mô hình giao tiếp trong đó:
- **Producer** publish event (immutable fact đã xảy ra)
- **Consumer** subscribe và xử lý
- Producer và Consumer **decouple** — không biết nhau

3 mô hình EDA (theo Martin Fowler, "What do you mean by Event-Driven?"):
1. **Event Notification** — sự kiện chỉ báo "có chuyện xảy ra", consumer phải gọi lại để lấy chi tiết
2. **Event-Carried State Transfer (ECST)** — event mang đầy đủ data, consumer không cần gọi lại
3. **Event Sourcing** — event là nguồn sự thật duy nhất, state derive từ event log
4. **CQRS** — tách Command/Query, thường đi cùng Event Sourcing

→ Đồ án dùng **mô hình 2 (ECST)**: event `OrderCreatedEvent` chứa orderId, userId, items, total — inventory không cần gọi lại order-service.

### 2.2. Apache Kafka — Distributed Log

Kafka không phải message queue truyền thống — nó là **distributed commit log**.

Khái niệm cốt lõi:

| Term | Ý nghĩa |
|------|---------|
| **Broker** | 1 server Kafka |
| **Cluster** | Nhiều broker phối hợp |
| **Topic** | Logical channel (tên) chứa events |
| **Partition** | Topic chia thành nhiều partition để scale ngang. Mỗi partition là **append-only log** ordered |
| **Offset** | Vị trí của message trong partition (số tăng dần) |
| **Producer** | Gửi message vào topic, có thể chọn partition (key-based hashing) |
| **Consumer** | Đọc từ partition |
| **Consumer Group** | Nhiều consumer chia partition (1 partition chỉ 1 consumer trong group) |
| **Replication** | Mỗi partition có replica trên nhiều broker (fault tolerance) |
| **Leader/Follower** | Mỗi partition có 1 leader nhận write, follower replicate |
| **ISR** (In-Sync Replicas) | Replica cập nhật kịp leader |

### 2.3. Vì sao Kafka khác RabbitMQ?

| Tiêu chí | Kafka | RabbitMQ |
|----------|-------|----------|
| Mô hình | Distributed log | Message broker (AMQP) |
| Lưu message sau khi consume | **CÓ** (retention policy) — replay được | Xóa sau ack |
| Throughput | Rất cao (triệu msg/s) | Cao nhưng thấp hơn |
| Latency | ~ms | sub-ms (thấp hơn) |
| Routing logic | Đơn giản (topic + partition) | Phức tạp (exchange, routing key) |
| Use case | Event streaming, log, analytics | Task queue, RPC |

**Kafka phù hợp** với event-sourcing, audit log, replay events. **RabbitMQ phù hợp** với task queue, work distribution.

### 2.4. KRaft mode (Kafka 3.5+)

Trước đây: Kafka cần **Zookeeper** quản lý metadata (broker list, topics, ACL, ...).
**KRaft (KIP-500)**: Kafka tự lưu metadata trong `__cluster_metadata` topic, dùng Raft consensus.

Lợi ích: Đơn giản (1 binary), scale tốt hơn (millions of partitions), latency thấp hơn cho metadata operations.

→ Dự án dùng `confluent/cp-kafka:8.2.0` ở **KRaft mode** (không Zookeeper).

### 2.5. Delivery Guarantees

| Guarantee | Ý nghĩa | Cách đạt |
|-----------|---------|----------|
| **At-most-once** | Có thể mất message | Producer fire-and-forget, consumer ack trước khi xử lý |
| **At-least-once** (default) | Có thể trùng | Producer retry, consumer ack sau khi xử lý |
| **Exactly-once** | Đúng 1 lần | Idempotent producer + Transactional + idempotent consumer |

Trong dự án: Kafka gives **at-least-once** + consumer-side **idempotency** (bảng `processed_events`) → *effectively* exactly-once.

### 2.6. Producer guarantees

- `acks=0`: Không chờ ack (fastest, có thể mất)
- `acks=1`: Leader ack (default cũ)
- `acks=all` / `acks=-1`: Tất cả ISR ack (durable, chậm hơn) — **dự án dùng cho event quan trọng**
- `enable.idempotence=true`: Mỗi message có producer ID + sequence → tránh duplicate khi retry
- `transactional.id`: Atomic write to multiple topics

### 2.7. Consumer guarantees

- `auto.offset.reset=earliest`: New consumer đọc từ đầu
- `enable.auto.commit=false`: Tự commit offset → có thể skip nếu crash. Tốt hơn: commit manual sau khi xử lý xong
- **Rebalance**: Khi consumer join/leave, partitions được redistribute. Dùng `ConsumerRebalanceListener` để cleanup

### 2.8. Partition strategy

- **Key-based hashing**: Producer set key → cùng key → cùng partition → preserve order trong key
  - Vd: `orderId` làm key → mọi event của order đó vào 1 partition → consumer process tuần tự
- **Round-robin**: Không có key → phân đều partitions
- **Custom partitioner**: Tự code logic

→ Trong dự án, topic `flash-sale-order-requested` có **3 partitions** với key = `campaignId` để parallelize.

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. Kafka topics trong dự án

Dự án có 11 nhóm sự kiện nghiệp vụ; nếu đếm các topic vật lý riêng như `product-created`, `product-updated`, `product-deleted` thì có 13 topic chính.

| Topic | Producer → Consumer | Mục đích |
|-------|--------------------|----------|
| `user-registered` | identity → user | Tạo profile người dùng |
| `product-created/updated/deleted` | product → search | Index Elasticsearch |
| `order-created` | order → inventory | Reserve stock |
| `inventory-updated` | inventory → order | Confirm stock reserved |
| `inventory-failed` | inventory → order | Stock không đủ |
| `payment-requested` | order → payment | Xử lý COD tự động sau khi đã reserve stock |
| `payment-success` | payment → order | Confirm thanh toán |
| `payment-failed` | payment → order | Hủy đơn |
| `order-confirmed` | order → inventory, notification | Trừ stock thật, gửi email |
| `order-cancelled` | order → inventory, notification | Release stock, gửi email |
| `flash-sale-order-requested` | flash-sale → order | Tạo đơn flash sale (3 partitions) |

### 3.2. Spring Kafka

`spring-kafka` artifact + `@KafkaListener` + `KafkaTemplate`:

```java
// Producer
@Service
class OrderEventPublisher {
  @Autowired KafkaTemplate<String, Object> kafkaTemplate;
  public void publish(OrderCreatedEvent event) {
    kafkaTemplate.send("order-created", event.getOrderId(), event);
  }
}

// Consumer
@Component
class InventoryEventListener {
  @KafkaListener(topics = "order-created", groupId = "inventory-service")
  public void handle(OrderCreatedEvent event) {
    if (processedEventRepo.exists(event.getEventId())) return;  // idempotency
    inventoryService.reserve(event);
    processedEventRepo.save(event.getEventId());
  }
}
```

### 3.3. Cấu hình quan trọng (application.yml)

```yaml
spring.kafka:
  bootstrap-servers: kafka:29092
  producer:
    acks: all
    properties:
      enable.idempotence: true
  consumer:
    group-id: order-service
    auto-offset-reset: earliest
    enable-auto-commit: false
    properties:
      isolation.level: read_committed
  listener:
    ack-mode: manual_immediate
```

### 3.4. Event Schema (DTO trong common module)

```java
public record OrderCreatedEvent(
    String eventId,        // UUID, dùng cho idempotency
    String orderId,
    String userId,
    List<OrderItemEvent> items,
    BigDecimal totalAmount,
    Instant occurredAt
) {}
```

→ **Schema versioning**: Có thể thêm field optional, không xóa field cũ. Production: dùng Schema Registry (Confluent) + Avro/Protobuf.

---

## 4. Đảm Bảo Reliability — Kết hợp Outbox + Idempotency

(Xem chi tiết file [10](./10-outbox-idempotency.md))

```
Producer side:
  Lưu event vào table `outbox` cùng transaction với business data
  OutboxPoller (5s) đọc và publish lên Kafka
  → Không mất event dù service crash

Consumer side:
  Trước khi xử lý, check `processed_events` table cho event_id
  Nếu đã xử lý → skip
  Nếu chưa → xử lý + lưu vào processed_events (cùng transaction với business)
  → Idempotent: deliver nhiều lần vẫn cho kết quả đúng
```

---

## 5. Từ Khóa Nghiên Cứu

```
- event driven architecture martin fowler
- apache kafka kraft mode kip 500
- kafka partition consumer group
- at least once exactly once kafka semantics
- kafka idempotent producer transactions
- event sourcing cqrs pattern
- event carried state transfer
- spring kafka @KafkaListener
- log compaction kafka
- kafka rebalance protocol
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Vì sao chọn Kafka mà không phải RabbitMQ?**
→ Kafka phù hợp với event streaming (audit, replay), throughput cao, retention dài. Đồ án có business event quan trọng (order, payment) cần audit. Ngoài ra Spring Boot stack phổ biến với Kafka.

**Q2: Em đảm bảo exactly-once thế nào?**
→ Kafka cho at-least-once. Em thêm idempotency ở consumer (table `processed_events`) — kiểm tra event_id trước khi xử lý. Hiệu quả là *effectively-once*.

**Q3: Nếu Kafka chết thì sao?**
→ Service không publish được event → đơn hàng không được xử lý tiếp. **Outbox pattern** giúp event đã commit trong DB không bị mất — sau khi Kafka up, OutboxPoller publish lại.

**Q4: Khi consumer crash giữa chừng?**
→ Nếu offset chưa commit → Kafka deliver lại sau rebalance. Idempotency table đảm bảo không xử lý trùng.

**Q5: Tại sao topic `flash-sale-order-requested` cần 3 partition?**
→ Để parallelize. Nếu 1 partition, mọi message tuần tự — bottleneck. 3 partition + 3 consumer instance → throughput x3. Key = `campaignId` để cùng campaign vào 1 partition (preserve order).

**Q6: Schema event thay đổi thì sao?**
→ Backward compatible: thêm field optional, không xóa field cũ. Production: Schema Registry + Avro để enforce. Đồ án dùng JSON nên schema implicit qua DTO class trong `common` module.

**Q7: KRaft so với Zookeeper?**
→ KRaft đơn giản hơn (1 binary), latency metadata thấp hơn, scale partitions tốt hơn. Zookeeper là external dependency phức tạp.

**Q8: Em có dùng Kafka Streams hay ksqlDB không?**
→ Không cần — đồ án này dùng pattern publish-subscribe đơn giản. Streams phù hợp khi cần real-time processing (windowing, joins) — vd: tính dashboard analytics.

---

## 7. Tài Liệu Tham Khảo

### Sách
- Neha Narkhede, Gwen Shapira, Todd Palino, *Kafka: The Definitive Guide*, 2nd ed., O'Reilly, 2021
- Bill Bejeck, *Kafka Streams in Action*, Manning
- Adam Bellemare, *Building Event-Driven Microservices*, O'Reilly, 2020

### Bài viết
- Martin Fowler, "What do you mean by Event-Driven?" (2017)
- Martin Kleppmann, "Designing Data-Intensive Applications", Chapter 11 — Stream Processing
- Confluent Blog: "Exactly-Once Semantics in Apache Kafka"
- Apache Kafka Documentation — kafka.apache.org/documentation

### Spec
- KIP-500 — Replace Zookeeper with KRaft
- KIP-98 — Idempotent Producer & Transactions
