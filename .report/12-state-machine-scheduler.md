# 12. State Machine + Scheduled Reconciliation

> Cập nhật sau Phase 13: các scheduler quan trọng đã được kiểm chứng gián tiếp qua smoke test và load test. Flash-sale campaign activation từng gặp OOM Metaspace do test infra, đã sửa và suite flash-sale pass. Reconciliation/compensation được chứng minh qua flash-sale spike và inventory-failed compensation.

## 1. Mục Tiêu Nghiên Cứu

- Hiểu Finite State Machine (FSM) trong domain modeling
- Hiểu cách dùng Scheduler để bổ sung cho event-driven
- Hiểu Reconciliation Pattern — đối soát cuối kỳ
- Phân biệt event-driven, scheduled, reconciliation

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Finite State Machine
Mô hình toán học gồm:
- Tập hợp **states** hữu hạn
- Tập **events/transitions** (các sự kiện gây chuyển state)
- **Initial state**, **final states**
- Hàm chuyển trạng thái: (state, event) → state'

Trong DDD, mỗi aggregate (Order, Payment, Campaign) thường là 1 FSM.

**Lý do dùng FSM**:
- Logic transitions tập trung, dễ test
- Validate transition hợp lệ (vd: không được CANCELLED → CONFIRMED)
- Document business flow tự nhiên

### 2.2. Implementation cách

| Cách | Mô tả | Phù hợp |
|------|-------|---------|
| **Manual** (if/switch trong service) | Code thủ công | Đơn giản, đủ dùng (dự án dùng cách này) |
| **Spring State Machine** | Framework declarative | FSM phức tạp, nhiều state |
| **Workflow engine** (Camunda, Temporal) | BPMN/code workflow | Flow rất phức tạp, cần monitor visual |

### 2.3. Vì sao Scheduler bổ sung event-driven?

Event-driven phù hợp với "khi X xảy ra thì làm Y". Nhưng có tình huống:
- **Timeout**: User mở VNPAY rồi đóng tab → không có event nào → cần job kiểm tra mỗi N giây
- **Reconciliation**: Đối soát giữa 2 nguồn data, tự sửa drift
- **Cleanup**: Xóa dữ liệu hết hạn (token expired, soft-deleted records)
- **Recovery**: Khôi phục state sau crash (Redis restart, lost cache)

→ Pattern **"Event-driven core + Scheduled safety net"**.

### 2.4. Reconciliation Pattern

> "Trust but verify" — định kỳ so sánh state giữa 2 nguồn, sửa nếu lệch.

Ví dụ classic:
- Audit DB của bank vs ledger
- Inventory thực tế (warehouse) vs DB
- Trong dự án: `flash_sale.soldCount` vs số đơn thực tế trong order_db

---

## 3. State Machines Trong Dự Án

### 3.1. Order State Machine

```
                   ┌─ inventory.failed ────────────────────────┐
                   │                                            │
PENDING ─[order-created event published]─► STOCK_RESERVED       ▼
                                              │              CANCELLED
                                              ├─ COD ─► CONFIRMED
                                              │
                                              ├─ VNPAY + payment.success ─► CONFIRMED
                                              │
                                              ├─ payment.failed ──────────────► (CANCELLED)
                                              │
                                              └─ reservation expired (30m) ───► (CANCELLED)
```

**Bảng transition**:
| From | To | Trigger |
|------|-----|---------|
| PENDING | STOCK_RESERVED | event `inventory-updated` |
| PENDING | CANCELLED | event `inventory-failed` |
| STOCK_RESERVED | CONFIRMED | event `payment-success` (VNPAY) hoặc COD |
| STOCK_RESERVED | CANCELLED | event `payment-failed` hoặc reservation expired |
| Any | CANCELLED | user cancel API |

### 3.2. Payment State Machine

```
PENDING (payment URL created)
   ├── VNPAY callback success ─► COMPLETED
   ├── VNPAY callback fail ─────► FAILED
   └── PaymentTimeoutScheduler ─► TIMEOUT
       (60s scheduler, expired_at < NOW)
```

### 3.3. Flash Sale Campaign State Machine

```
SCHEDULED ─[CampaignScheduler 5s, startTime ≤ NOW < endTime]─► ACTIVE
                                                                  │
ACTIVE ─[CampaignScheduler 5s, endTime ≤ NOW]──────────────────► ENDED
   │
   └─ Admin cancel ──────────────────► CANCELLED
```

### 3.4. Inventory dual-counter (không phải FSM nhưng quan trọng)

```
quantity = tổng tồn kho thật
reserved_quantity = đang giữ chỗ cho đơn PENDING/STOCK_RESERVED

Available = quantity - reserved_quantity
```

Transitions trên `reserved_quantity`:
- order-created → reserved += qty
- order-cancelled → reserved -= qty (release)
- order-confirmed → reserved -= qty + quantity -= qty (commit deduction)

---

## 4. Schedulers Trong Dự Án

### 4.1. Bảng tổng hợp

| Scheduler | Service | Interval | Mục đích |
|-----------|---------|----------|----------|
| ReservationExpiryScheduler | order-service | 60s | Hủy đơn VNPAY quá 30 phút |
| PaymentTimeoutScheduler | payment-service | 60s | Mark Payment TIMEOUT |
| OutboxPoller | order-service | 1s | Publish events từ outbox table |
| PaymentEventOutboxPoller | payment-service | 1s | Publish payment events |
| CampaignScheduler | flash-sale-service | 5s | Activate/end flash sale, recover Redis |
| ReconciliationScheduler | flash-sale-service | 5 min | Đồng bộ soldCount với actual orders |

### 4.2. Spring Scheduling

```java
@Component
@EnableScheduling
class ReservationExpiryScheduler {
  @Scheduled(fixedDelay = 60_000)
  public void expire() {
    List<Order> expired = orderRepo.findExpired(Instant.now());
    for (Order o : expired) {
      orderService.cancel(o, "VNPay payment not completed within 30-minute window");
    }
  }
}
```

`fixedDelay`: chờ N ms sau khi method trả về xong rồi gọi lại (an toàn — không overlap).

`fixedRate`: gọi lại mỗi N ms bất kể method có xong hay không (có thể overlap → cẩn thận).

### 4.3. Distributed Lock cho Multi-instance

Khi scale flash-sale-service lên nhiều replica, **tất cả** sẽ cố chạy CampaignScheduler → race.

Giải pháp: **Redis SET NX** (Set if not exists) với TTL:
```java
boolean locked = redisTemplate.opsForValue()
    .setIfAbsent("scheduler-lock:campaign", "1", Duration.ofSeconds(30));
if (locked) {
  try { runScheduler(); }
  finally { redisTemplate.delete("scheduler-lock:campaign"); }
}
```

Đây là **Redlock pattern** đơn giản (Salvatore Sanfilippo / antirez).

→ Production thực sự nên dùng **Redisson** library hoặc Apache ZooKeeper recipes.

### 4.4. CampaignScheduler — 3 nhiệm vụ trong 1

```
mỗi 5 giây:
  a) Activate due campaigns:
     - Query SCHEDULED campaigns có startTime ≤ NOW < endTime
     - Update status = ACTIVE
     - Seed Redis stock = quantity - soldCount
     - TTL = (endTime - NOW)

  b) Recover missing Redis keys:
     - Query ACTIVE campaigns mà Redis key không tồn tại
     - Khôi phục Redis counter từ DB

  c) End expired campaigns:
     - Query ACTIVE campaigns có endTime ≤ NOW
     - Update status = ENDED
```

### 4.5. ReconciliationScheduler — đối soát soldCount

**Bài toán**: Trong flow flash-sale:
1. Redis decrement → soldCount tăng
2. Publish Kafka `flash-sale-order-requested`
3. order-service tạo đơn

Nếu bước 3 fail (order-service down), soldCount đã tăng nhưng không có order → **orphan slot**.

**Reconciliation** (chạy mỗi 5 phút):
```
For each ACTIVE/ENDED campaign:
  actualOrders = COUNT(*) FROM orders WHERE flash_sale_id = campaignId
  
  if (soldCount > actualOrders):  // orphan slots
    if (now - lastSale > 30s):    // grace period
      fix: soldCount = actualOrders
      Redis INCR (release slots)
  
  elif (actualOrders > soldCount):  // race condition lag
    fix: soldCount = actualOrders
  
  else:
    log Redis vs expected mismatch (warn only)
```

→ Đây là pattern **"Eventual consistency có safety net"** — chấp nhận lệch tạm thời, định kỳ tự sửa.

---

## 5. Bằng Chứng Thực Nghiệm Và Cách Viết

| Nội dung | Bằng chứng | Ghi chú khi viết |
|---|---|---|
| CampaignScheduler activate campaign | `.test/results/08-flash-sale-PASS-after-fix.md` | Suite flash-sale pass sau khi tăng Metaspace test infra |
| Flash-sale không oversell trong spike test | `.test/results/flash-sale-spike-20260531-114752.{json,txt}`, `.test/results/SUMMARY.md` | 500 VU, 100 stock, 100 purchases, 100 confirmed orders, duplicate buyer 0 |
| Inventory-failed compensation | `.test/results/chaos-inventory-*-20260531-120641.*` | Order chuyển `CANCELLED`, reserved stock không treo |
| Payment timeout scheduler | `payment-service/src/main/java/com/ecommerce/payment/scheduler/PaymentTimeoutScheduler.java` | Có code; chỉ đưa kết quả nếu có test/log/screenshot riêng |
| Outbox poller 1s | `order-service/src/main/java/com/ecommerce/order/scheduler/OutboxPoller.java`, `payment-service/src/main/java/com/ecommerce/payment/kafka/PaymentEventOutboxPoller.java` | Không ghi 5s trong báo cáo vì code hiện tại là 1s |

Khi viết Chương 6, nên gắn state machine với kết quả cụ thể:
- Order: inventory failed -> `CANCELLED`.
- Campaign: `SCHEDULED` -> `ACTIVE` qua scheduler -> mua thành công trong flash-sale suite.
- Flash-sale consistency: sold_count và confirmed orders đều bằng 100 sau spike.

---

## 6. Từ Khóa Nghiên Cứu

```
- finite state machine domain modeling
- spring state machine framework
- scheduled task spring fixedDelay vs fixedRate
- distributed lock redis SET NX redlock
- redisson distributed lock
- reconciliation pattern eventual consistency
- compensating transaction saga timeout
- shedlock distributed scheduler
- temporal workflow vs cron
```

---

## 7. Câu Hỏi Phản Biện

**Q1: Tại sao em không dùng Spring State Machine framework?**
→ FSM của em chỉ 4 states, code thủ công đơn giản hơn framework. Spring State Machine phù hợp khi 10+ states với transitions phức tạp, cần persistance state.

**Q2: Vì sao có scheduler bên cạnh event-driven?**
→ Event-driven không cover được "absence of event" (user không thanh toán, Redis restart). Scheduler là safety net cho các tình huống đó. Pattern "event-driven core + scheduled safety net" là chuẩn industry.

**Q3: Tại sao cả ReservationExpiryScheduler (order) và PaymentTimeoutScheduler (payment) đều xử lý timeout?**
→ Defense in depth. Nếu payment-service crash mà chưa publish payment-failed, ReservationExpiryScheduler ở order-service sẽ tự cancel order. Hai scheduler hoạt động độc lập.

**Q4: fixedDelay vs fixedRate, em dùng cái nào?**
→ Em dùng fixedDelay để tránh overlap. fixedRate có thể chạy đè nếu lần trước chưa xong → race condition.

**Q5: Distributed lock Redis có an toàn không?**
→ Single Redis: SET NX với TTL đủ dùng cho most cases. Risk nhỏ: clock skew giữa Redis và app, hoặc Redis failover. Multi-instance Redis cần **Redlock algorithm** với 5 nodes (lock trên N/2+1). Đồ án single Redis chấp nhận trade-off.

**Q6: ReconciliationScheduler có tự fix sai không?**
→ Có, nhưng chỉ trong trường hợp **safe**:
- Orphan slot (sold > actual) sau grace 30s → safe to release
- Lag (actual > sold) → safe to bump up
- Redis mismatch → CHỈ log, không tự fix (tránh interfere active sale)

**Q7: Em có monitor scheduler running?**
→ Có. Mỗi scheduler log start/end + count items processed. Prometheus có thể track:
```
scheduler_invocations_total{name="ReservationExpiry"}
scheduler_processed_items_total{name="ReservationExpiry"}
```

---

## 8. Tài Liệu Tham Khảo

### State Machine
- Erich Gamma et al, *Design Patterns* — State pattern
- Spring State Machine Reference
- Vlad Mihalcea, "Modeling order state with FSM"

### Scheduler & Lock
- Spring Framework Reference — `@Scheduled`
- Redisson documentation — distributed lock recipes
- "ShedLock — distributed task scheduler" — github.com/lukas-krecan/ShedLock
- Salvatore Sanfilippo, "Distributed locks with Redis" — Redlock spec
- Martin Kleppmann, "How to do distributed locking" (rebuttal Redlock)

### Reconciliation
- Pat Helland, "Memories, Guesses, and Apologies" — Microsoft Research 2007
- Stripe Engineering Blog — "Designing robust and predictable APIs with idempotency"
