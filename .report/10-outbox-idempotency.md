# 10. Transactional Outbox & Idempotency

> Cùng với Saga, đây là pattern "ăn điểm" thứ hai. Hai pattern này cùng nhau giải bài toán "đảm bảo event không mất, không trùng".

## 1. Mục Tiêu Nghiên Cứu

- Hiểu vì sao "save DB + publish Kafka" không atomic
- Hiểu Transactional Outbox pattern
- Hiểu Idempotent Consumer pattern
- So sánh Outbox với Change Data Capture (Debezium)

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Bài toán "Dual Write"

Trong code thông thường:
```java
@Transactional
public void createOrder() {
  orderRepo.save(order);              // (1) DB write
  kafkaTemplate.send("order-created", event);  // (2) Kafka publish
}
```

Có 4 trường hợp:
| Case | (1) DB | (2) Kafka | Hậu quả |
|------|--------|-----------|---------|
| A | OK | OK | ✓ Tốt |
| B | Fail | n/a | Transaction rollback, không gì xảy ra → OK |
| C | OK | Fail | **Order tồn tại nhưng inventory không nhận event** → mất event! |
| D | OK | OK nhưng commit DB rollback | **Event đã publish mà DB rollback** → ghost event! |

→ "Dual write" KHÔNG atomic. Đây là bài toán cổ điển trong distributed systems.

### 2.2. Giải pháp 1: Transactional Outbox

**Ý tưởng**: Lưu event vào **table `outbox`** trong cùng DB transaction với business data. Một process riêng đọc outbox và publish lên Kafka.

```
┌─────────────────────────┐
│  Service Transaction     │
│  ┌────────────────────┐  │
│  │ INSERT order        │  │  cùng 1 BEGIN/COMMIT
│  │ INSERT outbox(event)│  │  → Atomic trong 1 DB
│  └────────────────────┘  │
└─────────────────────────┘
            │
            ▼
   ┌───────────────────┐
   │ Outbox Poller (5s)│ — đọc outbox WHERE published=false
   │   → publish Kafka  │
   │   → mark published │
   └───────────────────┘
```

**Ưu điểm**:
- Service crash giữa 2 thao tác → DB rollback → không có order, không có event → OK
- Service crash sau commit DB nhưng trước Kafka publish → outbox còn entry → poller publish lại → OK
- Kafka chậm/down → poller retry mãi → eventually publish → OK

**Nhược điểm**:
- Latency: event xuất hiện trên Kafka chậm hơn poll interval (5s)
- Cần thêm table và poller logic
- Có thể publish trùng (nếu poller crash sau publish trước khi mark) → cần idempotent consumer

### 2.3. Giải pháp 2: Change Data Capture (CDC)

Thay vì poller đọc outbox, dùng **Debezium** capture binlog của DB → publish lên Kafka.

- Pros: Real-time, không cần code poller
- Cons: Phức tạp setup (Kafka Connect cluster, schema registry), khó test local

→ Đồ án dùng **Outbox + Poller** vì đơn giản hơn, dễ demo.

### 2.4. Idempotent Consumer Pattern

**Vấn đề**: Kafka at-least-once → consumer có thể nhận trùng message.

**Giải pháp**: Mỗi event có `eventId` (UUID). Consumer:
```java
@KafkaListener(...)
public void handle(OrderCreatedEvent event) {
  if (processedEventRepo.existsById(event.getEventId())) {
    return;  // đã xử lý, skip
  }
  // BEGIN transaction
  doBusinessLogic(event);
  processedEventRepo.save(new ProcessedEvent(event.getEventId(), now));
  // COMMIT
}
```

**Quan trọng**: `processedEventRepo.save()` phải nằm cùng transaction với business logic. Nếu không, có thể commit business nhưng fail save → next time xử lý lại.

### 2.5. Outbox + Idempotency = Effectively Exactly-Once

```
Producer side: Outbox đảm bảo event được publish ÍT NHẤT 1 lần (tránh mất)
Consumer side: Idempotency đảm bảo event được xử lý NHIỀU NHẤT 1 lần (tránh trùng)

Hai cộng lại = ĐÚNG 1 LẦN (effectively exactly-once)
```

### 2.6. Variations & Optimizations

- **Cleanup `processed_events`**: Sau 1 tuần xóa records cũ (Kafka retention thường 7 ngày)
- **Cleanup `outbox`**: Soft delete bằng `published_at`, hoặc xóa hard sau N giờ
- **Locking trong poller**: Multi-instance service cần distributed lock để chỉ 1 instance poll
- **Partitioned outbox**: Khi có nhiều order → poll batch, publish parallel

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. Schema bảng `outbox`

```sql
CREATE TABLE outbox (
  id UUID PRIMARY KEY,
  aggregate_type VARCHAR(64),    -- 'Order', 'Payment'
  aggregate_id UUID,             -- orderId
  event_type VARCHAR(64),        -- 'order-created', 'order-cancelled'
  payload JSONB,                 -- event body
  created_at TIMESTAMP DEFAULT NOW(),
  published_at TIMESTAMP,        -- NULL = chưa publish
  retry_count INT DEFAULT 0
);
CREATE INDEX idx_outbox_pending ON outbox(created_at) WHERE published_at IS NULL;
```

### 3.2. Service code

```java
@Transactional
public Order createOrder(CreateOrderRequest req) {
  Order order = ...
  orderRepo.save(order);
  
  OutboxEntry entry = OutboxEntry.builder()
    .id(UUID.randomUUID())              // = eventId for idempotency
    .aggregateType("Order")
    .aggregateId(order.getId())
    .eventType("order-created")
    .payload(toJson(new OrderCreatedEvent(...)))
    .build();
  outboxRepo.save(entry);                // cùng transaction
  return order;
}
```

### 3.3. OutboxPoller (chạy mỗi 5s)

```java
@Component
@Scheduled(fixedDelay = 5_000)
public void pollAndPublish() {
  List<OutboxEntry> pending = outboxRepo
      .findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 100));
  
  for (OutboxEntry entry : pending) {
    try {
      kafkaTemplate.send(entry.getEventType(),
                         entry.getAggregateId().toString(),
                         entry.getPayload()).get();  // sync ack
      entry.setPublishedAt(now());
      outboxRepo.save(entry);
    } catch (Exception e) {
      entry.setRetryCount(entry.getRetryCount() + 1);
      outboxRepo.save(entry);
      log.warn("Retry {} for {}", entry.getId(), e.getMessage());
    }
  }
}
```

### 3.4. Schema bảng `processed_events`

```sql
CREATE TABLE processed_events (
  event_id UUID PRIMARY KEY,
  consumer_group VARCHAR(64),
  processed_at TIMESTAMP DEFAULT NOW()
);
```

### 3.5. Consumer side

```java
@KafkaListener(topics = "order-created", groupId = "inventory-service")
@Transactional
public void onOrderCreated(OrderCreatedEvent event) {
  if (processedEventRepo.existsById(event.getEventId())) {
    log.debug("Already processed event {}", event.getEventId());
    return;
  }
  
  // business logic
  inventoryService.reserveStock(event);
  
  // mark processed
  processedEventRepo.save(new ProcessedEvent(event.getEventId(), now()));
}
```

### 3.6. Service nào dùng Outbox?

| Service | Outbox | Processed Events |
|---------|--------|------------------|
| order-service | ✓ (`outbox` table) | ✓ (xử lý inventory-updated, payment-success...) |
| payment-service | ✓ (`payment_outbox`) | ✓ |
| inventory-service | (publish trực tiếp trong consumer transaction) | ✓ |
| flash-sale-service | Không có outbox riêng; publish Kafka đồng bộ + compensation Redis + reconciliation | Buyer set trong Redis chống mua trùng theo campaign |

---

## 4. Trade-offs

### Outbox Pattern
| Ưu | Nhược |
|----|-------|
| Đảm bảo không mất event | Latency thêm (poll interval) |
| Đơn giản, không cần CDC infrastructure | Tăng load DB (insert outbox + poll) |
| Test dễ (chỉ là DB) | Cần cleanup outbox cũ |

### Idempotent Consumer
| Ưu | Nhược |
|----|-------|
| Tránh duplicate side-effect | Tăng DB write (processed_events) |
| Dùng cho mọi consumer | Cần cleanup events cũ |

→ Trade-off **đáng giá** cho hệ thống quan trọng (order, payment).

---

## 5. Từ Khóa Nghiên Cứu

```
- transactional outbox pattern Chris Richardson
- dual write problem distributed systems
- change data capture debezium
- idempotent consumer pattern
- exactly once delivery semantics
- inbox pattern outbox pattern
- outbox poller batch publishing
- aggregate root event sourcing
- saga isolation
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Vì sao em không dùng `@TransactionalEventListener` của Spring?**
→ `@TransactionalEventListener` chỉ đảm bảo trong cùng JVM, không liên quan Kafka. Nếu app crash sau commit DB nhưng trước Kafka publish, event vẫn mất. Outbox là level cao hơn — đảm bảo durability.

**Q2: Tại sao không dùng Kafka Transactions trực tiếp?**
→ Kafka Transactions chỉ đảm bảo atomic giữa nhiều Kafka write, không liên quan DB. Để link DB + Kafka thật atomic, cần XA (2PC) — đã thảo luận, không scale.

**Q3: Outbox có gây latency không?**
→ Có, ~5s poll interval. Nhưng với e-commerce, độ trễ 5s giữa "tạo đơn" và "inventory bắt đầu reserve" là acceptable. Nếu cần real-time, dùng CDC (Debezium).

**Q4: Khi nào processed_events table phát triển khổng lồ?**
→ Theo retention. Em có thể job cleanup events > 7 ngày (sau Kafka retention default). Hoặc dùng partitioned table để truncate nhanh.

**Q5: Em có thể giải bằng "send Kafka trước, DB sau" không?**
→ Không an toàn — nếu Kafka publish OK rồi DB rollback → ghost event (consumer xử lý event cho order không tồn tại). Outbox đảm bảo "DB ký trước, Kafka theo sau".

**Q6: Outbox poller chạy nhiều instance thì sao?**
→ Cần distributed lock (Redis SET NX) hoặc dùng `SELECT ... FOR UPDATE SKIP LOCKED` để mỗi instance grab khác nhau. Đồ án single instance order-service nên không lo.

**Q7: Difference Inbox vs Outbox pattern?**
→ Outbox: producer side, đảm bảo publish ít nhất 1 lần. Inbox: consumer side, dedup bằng eventId. `processed_events` của em chính là Inbox pattern.

---

## 7. Tài Liệu Tham Khảo

### Bài viết & Pattern
- Chris Richardson — "Pattern: Transactional Outbox" — microservices.io
- Chris Richardson — "Pattern: Idempotent Consumer" — microservices.io
- Microsoft Learn — "Outbox, Inbox patterns and delivery guarantees explained"
- Confluent Blog — "Implementing Outbox Pattern with Spring Boot"

### Sách
- Chris Richardson, *Microservices Patterns*, Section 4.3 — Asynchronous messaging
- Vlad Mihalcea, *High-Performance Java Persistence* — Outbox section

### Tools
- Debezium — debezium.io (CDC reference)
- Eventuate Tram — eventuate.io (outbox framework Chris Richardson tạo)
