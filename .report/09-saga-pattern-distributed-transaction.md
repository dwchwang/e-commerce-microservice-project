# 09. Saga Pattern & Distributed Transaction

> **Đây là chủ đề ăn điểm nhất của đồ án** — Hội đồng phản biện rất quan tâm.

## 1. Mục Tiêu Nghiên Cứu

- Hiểu vì sao distributed transaction (2PC) không scale
- Hiểu Saga Pattern — Choreography vs Orchestration
- Hiểu Compensating Action
- Hiểu Eventual Consistency vs ACID
- Đặt vào context flow đặt hàng của dự án

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Bài toán Distributed Transaction

Trong monolith với 1 DB:
```sql
BEGIN;
  UPDATE inventory SET qty = qty - 1 WHERE product_id = 'p1';
  INSERT INTO orders (...) VALUES (...);
  INSERT INTO payments (...) VALUES (...);
COMMIT;
```
→ ACID transaction đảm bảo all-or-nothing.

Trong microservice với DB-per-service:
- inventory_db, order_db, payment_db **riêng biệt**
- Không thể `BEGIN; INSERT in 3 DBs; COMMIT;`

### 2.2. Two-Phase Commit (2PC) — Tại sao bị bỏ?

2PC dùng Transaction Manager (XA, JTA):
```
Phase 1 (Prepare): TM hỏi mỗi resource "ready to commit?"
  Nếu tất cả YES → Phase 2 (Commit): TM lệnh tất cả commit
  Nếu có NO → Phase 2 (Abort): TM lệnh rollback
```

Nhược điểm:
- **Blocking**: Resource giữ lock trong suốt prepare → throughput thấp
- **Not partition tolerant**: TM crash giữa phase 2 → resource stuck
- **Coupling**: Tất cả resource phải hỗ trợ XA → khó với NoSQL/Kafka
- **Scale kém**: Latency = max của tất cả participant

→ Microservice và NoSQL **không dùng 2PC**. Dùng **Saga**.

### 2.3. CAP Theorem (Eric Brewer, 2000) & PACELC (Daniel Abadi, 2010)

**CAP**: Hệ phân tán không thể đồng thời:
- **C**onsistency (linearizability)
- **A**vailability
- **P**artition tolerance

Khi Partition xảy ra → phải chọn C hoặc A.

**PACELC**: Cả khi không partition, vẫn phải chọn:
- **L**atency vs **C**onsistency

→ Hầu hết hệ thống e-commerce chọn **AP + Eventual Consistency** vì user ưu tiên hệ thống "luôn trả lời" hơn là "luôn chính xác". Saga là cách triển khai thực tế.

### 2.4. Saga Pattern (Hector Garcia-Molina, 1987)

**Saga** = chuỗi local transactions, mỗi bước có **compensating action**.

```
T1 → T2 → T3 → T4 (success)
hoặc
T1 → T2 → T3 → C2 → C1 (failure tại T4 cần rollback)
```

Compensating action **không phải undo**, mà là một transaction nghiệp vụ làm "đảo ngược tác động". Vd: refund payment thay vì rollback charge.

### 2.5. Hai biến thể Saga

**a) Choreography** — không có coordinator
```
Service A publishes Event1 → Service B consumes, does work, publishes Event2 → Service C ...
```
- Pros: Đơn giản, decoupled
- Cons: Khó hiểu flow tổng thể, dễ bị "event soup", khó debug

**b) Orchestration** — có coordinator (Saga Manager)
```
Orchestrator → command Service A → wait reply → command Service B → ...
```
- Pros: Flow rõ ràng, dễ debug, tập trung logic
- Cons: Coordinator là single point of complexity

→ **Đồ án này dùng Orchestration**: order-service đóng vai Orchestrator.

### 2.6. Saga Implementation Strategies

| Cách | Mô tả |
|------|-------|
| Event-based + Aggregator | Mỗi service xử lý event và publish kết quả; orchestrator gom |
| Command-based | Orchestrator gửi command qua Kafka, nhận reply qua Kafka |
| Workflow engine | Camunda, Temporal — quản lý state machine của saga |
| Custom state machine | Tự code (lưu state trong DB của orchestrator) |

→ **Đồ án dùng "Custom state machine"** — order-service lưu order status (PENDING, STOCK_RESERVED, CONFIRMED, CANCELLED) trong order_db, transitions dựa trên Kafka events.

---

## 3. Áp Dụng Trong Dự Án — Order Saga

### 3.1. Các participant
- **Orchestrator**: order-service
- **Participants**: inventory-service, payment-service, voucher-service, notification-service

### 3.2. Order State Machine
```
PENDING ──[order-created]──► STOCK_RESERVED
                                  ├─ COD + payment.success ──► CONFIRMED
                                  ├─ VNPAY + payment.success ──► CONFIRMED
                                  ├─ VNPAY + payment.failed ──► CANCELLED
                                  └─ reservation expired ──► CANCELLED
PENDING ──[inventory.failed]──► CANCELLED
```

### 3.3. Happy Path (Success)

Flow dưới đây tách rõ hai nhánh thanh toán hiện có trong code: COD đi qua event `payment-requested`, còn VNPAY tạo payment URL qua REST endpoint sau khi order đã `STOCK_RESERVED`.

```
1. POST /api/orders
   order-service:
     - sync: cart-service (get items)
     - sync: product-service (validate price)
     - sync: voucher-service (validate)
     - tạo Order PENDING
     - publish order-created (qua Outbox)

2. inventory-service consume:
     - reserve stock (reserved_quantity += qty)
     - publish inventory-updated

3. order-service consume inventory-updated:
     - update Order → STOCK_RESERVED
     - nếu VNPAY: set reservation_expired_at = NOW + 30 min
     - nếu COD: publish payment-requested

4A. COD branch:
     payment-service consume payment-requested
     - tạo Payment COMPLETED
     - publish payment-success qua payment_outbox

4B. VNPAY branch:
     client gọi POST /api/payments/vnpay/create
     payment-service tạo Payment PENDING + VNPAY payment URL
     user thanh toán VNPAY → Return URL/IPN callback
     - update Payment → COMPLETED
     - publish payment-success qua payment_outbox

5. order-service consume payment-success:
     - update Order → CONFIRMED
     - publish order-confirmed

6. inventory-service consume order-confirmed:
     - quantity -= qty (trừ thật, release reserved)
   notification-service consume order-confirmed:
     - gửi email xác nhận
```

### 3.4. Compensation Paths

**Case A: Inventory không đủ**
```
order-service publish order-created
inventory-service: thiếu stock → publish inventory-failed
order-service: update Order → CANCELLED (compensation = không cần rollback gì vì chưa charge)
notification-service: gửi email "đơn hàng bị hủy"
```

**Case B: Payment thất bại**
```
COD: payment-service có thể publish payment-failed nếu xử lý payment-requested lỗi
VNPAY: user thanh toán failed hoặc IPN báo fail → payment-service publish payment-failed
order-service:
  update Order → CANCELLED
  publish order-cancelled (compensation)
inventory-service consume order-cancelled:
  release reservation (reserved_quantity -= qty)
voucher-service: release voucher (qua API)
notification-service: email
```

**Case C: User bỏ tab, không thanh toán (timeout)**
```
ReservationExpiryScheduler (60s) tìm Order STOCK_RESERVED có reservation_expired_at < NOW
  → cancel Order
  → publish order-cancelled → inventory release
  → notification email
```

### 3.5. Sơ đồ Saga (kèm compensations)

```
                     ┌──compensation──┐
                     ▼                │
T1: Create Order PENDING              │
       │                              │
T2: Reserve Inventory ─[fail]─► Cancel Order ◄─┐
       │                                       │
T3: Create Payment ──[fail]──► Release Inventory + Cancel Order
       │
T4: Confirm Payment (VNPAY callback)
       │
T5: Confirm Order CONFIRMED
       │
T6: Final inventory deduction + Email notification (no compensation needed)
```

### 3.6. Vì sao chọn Orchestration?

- Logic order saga phức tạp (4+ steps + 3 timeout) → tập trung vào order-service dễ maintain
- Cần xem trạng thái đơn hàng → chỉ cần query order_db
- Choreography sẽ làm event flow như "spaghetti" — khó debug

---

## 4. Eventual Consistency — User Experience

Saga **KHÔNG đảm bảo ACID** giữa services. Có khoảnh khắc:
- Order = PENDING nhưng inventory chưa biết
- Order = CONFIRMED nhưng email chưa gửi

Để UI tốt:
- Hiển thị order status realtime cho user
- Email gửi async, không block confirm đơn
- Hiển thị "đơn hàng đang xử lý..." cho status PENDING/STOCK_RESERVED

---

## 5. Từ Khóa Nghiên Cứu

```
- saga pattern microservices
- two phase commit 2pc XA distributed transaction
- compensating transaction
- choreography vs orchestration saga
- BASE properties eventual consistency
- CAP theorem brewer
- PACELC theorem abadi
- Hector Garcia-Molina sagas 1987 paper
- distributed sagas Caitie McCaffrey
- temporal workflow camunda saga orchestrator
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Vì sao không dùng 2PC?**
→ 2PC blocking, không scale, không partition tolerant. Microservice + NoSQL không hỗ trợ XA tự nhiên. Saga là cách thực tế cho hệ phân tán hiện đại.

**Q2: Saga và 2PC khác nhau thế nào?**
→ 2PC: ACID, blocking, atomic. Saga: BASE (Basically Available, Soft state, Eventual consistency), non-blocking, mỗi bước commit local, rollback bằng compensation.

**Q3: Em chọn Choreography hay Orchestration? Vì sao?**
→ Orchestration với order-service làm orchestrator. Lý do: flow phức tạp 4+ bước, cần tracking state, dễ debug. Choreography phù hợp flow đơn giản hơn.

**Q4: Compensating action có rollback hoàn toàn không?**
→ Không hoàn toàn — đó là transaction nghiệp vụ "đảo ngược tác động". Vd: nếu đã gửi email thành công mà order cancel, em không "unsend" email mà gửi email mới "đơn đã hủy".

**Q5: Khi nào saga không thể compensation?**
→ Khi action không thể đảo ngược (vd: shipped hàng vật lý). Cần thiết kế **pivot transaction** — bước cuối, sau đó không thể fail. Đơn hàng confirm thành công là pivot.

**Q6: Saga có vấn đề "isolation" không?**
→ Có — đây gọi là *lack of isolation*. Trong khoảng saga đang chạy, các transaction khác có thể đọc state intermediate. Giải pháp: **semantic locks** (vd: stock đặt status "RESERVED" thay vì commit ngay), **commutative updates** (cộng/trừ thay vì ghi đè), **versioning**.

**Q7: Em làm sao biết saga "chạy đến đâu"?**
→ Order status trong order_db (PENDING, STOCK_RESERVED, CONFIRMED, CANCELLED) chính là saga state. Production có thể thêm `saga_log` table riêng.

**Q8: Nếu inventory release fail giữa compensation thì sao?**
→ Em có 2 retry layer:
1. Kafka at-least-once redeliver event order-cancelled
2. Idempotency: inventory consumer kiểm tra `processed_events`, đảm bảo release không trùng

Nếu vẫn fail liên tục → DLQ (dead letter queue) + alert manual.

---

## 7. Tài Liệu Tham Khảo

### Bài báo gốc
- **Hector Garcia-Molina, Kenneth Salem**, "Sagas", *ACM SIGMOD 1987* — paper khai sinh
- **Eric Brewer**, "Towards Robust Distributed Systems", *PODC 2000* — CAP theorem
- **Pat Helland**, "Life beyond Distributed Transactions", *CIDR 2007*
- **Daniel J. Abadi**, "Consistency Tradeoffs in Modern Distributed Database System Design", *Computer Magazine 2012* — PACELC

### Sách
- Chris Richardson, *Microservices Patterns*, **Chapter 4 — Sagas** (BẮT BUỘC ĐỌC)
- Sam Newman, *Building Microservices* 2nd ed., Chapter 6 — Workflow
- Bill Bejeck, *Building Event-Driven Microservices*

### Web
- microservices.io — "Saga pattern" (Chris Richardson)
- "Distributed Sagas: A Protocol for Coordinating Microservices" — Caitie McCaffrey (talk YouTube)
- Confluent Blog — "Patterns for Microservices: Sagas"
