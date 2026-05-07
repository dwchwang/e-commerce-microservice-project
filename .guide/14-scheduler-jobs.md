# Scheduled Jobs — Các Tác Vụ Nền Tự Động

Hệ thống có **6 scheduled jobs** chạy nền tự động, xử lý các tình huống mà event-driven không cover được (timeout, expiry, outbox, reconciliation).

---

## 1. ReservationExpiryScheduler (order-service)

| | |
|---|---|
| **Chạy mỗi** | 60 giây (fixedDelay = 60,000ms) |
| **Service** | order-service |

### Nhiệm vụ:
Tìm tất cả đơn hàng VNPAY đã quá hạn thanh toán và tự động hủy chúng.

### Điều kiện hủy:
- `status = STOCK_RESERVED`
- `payment_method = VNPAY`
- `reservation_expired_at < NOW()` (quá 30 phút kể từ khi đặt)

### Khi hủy:
1. Update order → `CANCELLED`, lý do: "VNPay payment not completed within 30-minute window"
2. Release voucher (nếu đơn có dùng voucher)
3. Publish Kafka `order-cancelled` → inventory-service release tồn kho
4. notification-service gửi email thông báo đơn bị hủy

### Tại sao cần scheduler này?
Nếu user chỉ mở trang VNPAY rồi đóng tab mà không thanh toán, hệ thống sẽ giữ tồn kho mãi mãi. Scheduler này đảm bảo tồn kho được giải phóng sau 30 phút.

---

## 2. PaymentTimeoutScheduler (payment-service)

| | |
|---|---|
| **Chạy mỗi** | 60 giây (fixedDelay = 60,000ms) |
| **Service** | payment-service |

### Nhiệm vụ:
Tìm tất cả Payment `PENDING` đã quá hạn (`expires_at < NOW()`) và chuyển sang `TIMEOUT`.

### Khi timeout:
1. Update payment → `TIMEOUT`
2. Publish Kafka `payment-failed` → order-service nhận → update order `CANCELLED`
3. order-service publish `order-cancelled`
4. Dây chuyền tiếp tục: inventory release, notification email

### Mối quan hệ với ReservationExpiryScheduler:
Hai scheduler này xử lý cùng tình huống "VNPAY timeout" từ **hai phía**:
- `PaymentTimeoutScheduler`: xử lý phía payment record
- `ReservationExpiryScheduler`: xử lý phía order record (backup nếu payment event bị mất)

---

## 3. OutboxPoller (order-service)

| | |
|---|---|
| **Chạy mỗi** | 1 giây (fixedDelay = 1,000ms) |
| **Service** | order-service |

### Nhiệm vụ:
Đọc bảng `outbox` trong `order_db` và publish các event chưa được gửi lên Kafka.

### Cơ chế:
```
Luồng bình thường:
  order-service tạo đơn → save outbox event (cùng transaction)
  OutboxPoller (1s) → publish lên Kafka → mark PUBLISHED

Khi service crash giữa chừng:
  Crash xảy ra giữa "save order" và "publish Kafka"
  → Kafka chưa nhận event
  → Sau khi restart, OutboxPoller đọc lại outbox → publish lại
  → Không mất event!
```

### Tương đương trong payment-service:
`PaymentEventOutboxPoller` hoạt động y hệt nhưng cho bảng `payment_outbox` trong `payment_db` và cũng chạy mỗi 1 giây.

---

## 4. CampaignScheduler (flash-sale-service)

| | |
|---|---|
| **Chạy mỗi** | 5 giây (fixedDelay = 5,000ms) |
| **Service** | flash-sale-service |
| **Distributed Lock** | Redis (chỉ 1 instance chạy scheduler dù scale nhiều replica) |

### Nhiệm vụ 3-trong-1:

**a) Activate due campaigns:**
- Tìm campaigns có `status=SCHEDULED` và `startTime <= NOW() < endTime`
- Update status → `ACTIVE`
- Seed Redis stock: lưu `quantity - soldCount` vào `flash_sale:stock:{id}`
- TTL của Redis key = thời gian còn lại đến endTime

**b) Recover missing Redis keys:**
- Tìm campaigns `ACTIVE` mà Redis key bị mất (sau Redis restart)
- Khôi phục Redis counter từ `quantity - soldCount` trong DB

**c) End expired campaigns:**
- Tìm campaigns `ACTIVE` có `endTime <= NOW()`
- Update status → `ENDED`

### Distributed Lock:
Dùng Redis `SET NX` (SetIfAbsent) với TTL 30 giây để đảm bảo chỉ 1 instance chạy scheduler.
Tránh tình trạng race condition khi scale flash-sale-service lên nhiều replica.

---

## 5. ReconciliationScheduler (flash-sale-service)

| | |
|---|---|
| **Chạy mỗi** | 5 phút (fixedDelay = 300,000ms) |
| **Service** | flash-sale-service |

### Nhiệm vụ:
So sánh và đồng bộ `soldCount` trong DB với số đơn hàng thực tế trong order-service.

### Khi nào cần?
Trong luồng flash sale:
1. Flash-sale-service deduct Redis counter → tăng `soldCount` trong DB
2. Kafka publish `flash-sale-order-requested`
3. order-service tạo đơn

Nếu bước 3 thất bại (order-service down), soldCount trong DB tăng nhưng không có đơn thực tế → **"orphan slot"**.

### Xử lý:

**Orphan slot** (soldCount > actualOrders):
- Sau grace period 30 giây (chờ orphan orders có thể đang processing)
- Restore `soldCount` về `actualOrders`
- Tăng Redis counter lại → giải phóng slot cho user khác

**Lag** (actualOrders > soldCount):
- Có thể do race condition
- Update `soldCount` = `actualOrders` để sync lại

**Redis/DB mismatch** (Redis remaining ≠ expected remaining):
- Chỉ log warning, không tự sửa (để tránh can thiệp vào active sale)

---

## Bảng Tóm Tắt

| Scheduler | Service | Interval | Mục đích chính |
|-----------|---------|----------|---------------|
| ReservationExpiryScheduler | order-service | 60s | Hủy đơn VNPAY quá 30 phút |
| PaymentTimeoutScheduler | payment-service | 60s | Mark payment TIMEOUT |
| OutboxPoller | order-service | 1s | Publish events từ outbox table |
| PaymentEventOutboxPoller | payment-service | 1s | Publish payment events từ outbox |
| CampaignScheduler | flash-sale-service | 5s | Activate/end flash sale, recover Redis |
| ReconciliationScheduler | flash-sale-service | 5 phút | Sync soldCount vs actual orders |

---

## Kiểm Tra Scheduler Đang Chạy

```bash
# Xem log của scheduler trong order-service
docker compose logs order-service | grep -i "scheduler\|expired\|cancelled"

# Xem log flash sale scheduler
docker compose logs flash-sale-service | grep -i "scheduler\|campaign\|activated\|ended\|reconcil"

# Xem log payment timeout
docker compose logs payment-service | grep -i "timeout\|expired"
```
