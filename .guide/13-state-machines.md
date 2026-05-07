# State Machines — Trạng Thái Đơn Hàng, Thanh Toán, Flash Sale

## 1. Order Status (Trạng Thái Đơn Hàng)

### Các Trạng Thái

| Status | Ý nghĩa |
|--------|---------|
| `PENDING` | Đơn vừa được tạo, chờ kiểm tra tồn kho |
| `STOCK_RESERVED` | Tồn kho đã được giữ chỗ, chờ thanh toán (VNPAY) hoặc xác nhận (COD) |
| `CONFIRMED` | Đơn đã xác nhận — thanh toán thành công hoặc COD được chấp nhận |
| `CANCELLED` | Đơn bị hủy |

### Sơ Đồ Chuyển Trạng Thái

```
                    ┌─ inventory-failed ──────────────────────────────────────────┐
                    │                                                              │
PENDING ──[order-created]──► STOCK_RESERVED                                       ▼
                                    │                                          CANCELLED
                                    ├─── COD ──────────────────────────────────►  │
                                    │    (confirm ngay)           CONFIRMED        │
                                    │                                 │            │
                                    └─── VNPAY ──[payment-success]──►│            │
                                              │                       │            │
                                              └─[payment-failed]──────────────────┘
                                              │
                                              └─[reservation expired (30 phút)]────► CANCELLED
```

### Luồng COD (Cash On Delivery)

```
1. User đặt đơn với paymentMethod=COD
2. order-service tạo đơn: status=PENDING
3. Kafka: order-created → inventory RESERVE tồn kho
4. Kafka: inventory-updated → order: status=STOCK_RESERVED
   ├── KHÔNG set reservation_expired_at (COD không có timeout)
   └── Kafka: order-confirmed (ngay lập tức)
5. inventory-service CONFIRM tồn kho (trừ hẳn)
6. notification-service gửi email xác nhận
7. Order cuối cùng: status=CONFIRMED
```

### Luồng VNPAY

```
1. User đặt đơn với paymentMethod=VNPAY
2. order-service tạo đơn: status=PENDING
3. Kafka: order-created → inventory RESERVE tồn kho
4. Kafka: inventory-updated → order: status=STOCK_RESERVED
   └── SET reservation_expired_at = NOW() + 30 phút
5. Client gọi REST endpoint tạo VNPAY payment URL
6. payment-service tạo payment record PENDING, tạo VNPAY URL, trả về cho client
7. User thanh toán trong 30 phút...
   ├── Thành công: VNPAY callback → payment-service → Kafka: payment-success
   │                → order: status=CONFIRMED → inventory CONFIRM → email
   ├── Thất bại: VNPAY callback → payment-service → Kafka: payment-failed
   │                → order: status=CANCELLED → inventory RELEASE → email
   └── Timeout (30 phút): ReservationExpiryScheduler (chạy mỗi 60s)
                          → order: status=CANCELLED → inventory RELEASE
```

---

## 2. Payment Status (Trạng Thái Thanh Toán)

### Các Trạng Thái

| Status | Ý nghĩa |
|--------|---------|
| `PENDING` | Chờ user thanh toán, payment URL đã được tạo |
| `COMPLETED` | Thanh toán thành công (VNPAY xác nhận) |
| `FAILED` | Thanh toán thất bại (user hủy, lỗi ngân hàng) |
| `TIMEOUT` | Quá thời gian thanh toán (payment-service set khi hết 30 phút) |

### Sơ Đồ

```
                     ┌── VNPAY callback (success) ──► COMPLETED
PENDING (có URL) ────┤
                     ├── VNPAY callback (fail) ────► FAILED
                     │
                     └── PaymentTimeoutScheduler ──► TIMEOUT
                         (chạy mỗi 60 giây)
```

> **Lưu ý**: Một `order_id` chỉ có thể có **một** Payment ở trạng thái `PENDING` cùng lúc (unique constraint trong DB).

---

## 3. Campaign Status — Flash Sale (Trạng Thái Chiến Dịch)

### Các Trạng Thái

| Status | Ý nghĩa |
|--------|---------|
| `SCHEDULED` | Chiến dịch đã lên lịch, chưa đến giờ bắt đầu |
| `ACTIVE` | Đang diễn ra, user có thể mua |
| `ENDED` | Đã kết thúc (hết thời gian), không thể mua |
| `CANCELLED` | Admin hủy chiến dịch thủ công |

### Sơ Đồ

```
SCHEDULED ──[startTime đến, CampaignScheduler 5s]──► ACTIVE ──[endTime qua, CampaignScheduler 5s]──► ENDED
    │                                                    │
    │                                                    └── Admin cancel ──► CANCELLED
    │
    └── Admin cancel ──► CANCELLED
```

### Cơ Chế Redis Cho Flash Sale

Khi campaign chuyển `SCHEDULED → ACTIVE`:
1. `CampaignScheduler` gọi `seedRedisStock()` → lưu `{quantity}` vào Redis key `flash_sale:stock:{campaignId}`
2. Khi user mua: Redis `DECR` key này atomically
   - Nếu result ≥ 0: cho phép mua, publish Kafka `flash-sale-order-requested`
   - Nếu result < 0: từ chối (HTTP 409), `INCR` để hoàn lại counter
3. Khi campaign kết thúc: Redis key tự expire (TTL = thời gian đến endTime)

**Recovery**: Nếu Redis mất key (restart), `CampaignScheduler` sẽ khôi phục lại key từ DB.

---

## 4. Inventory Reservation States

Inventory-service quản lý **2 loại số lượng** riêng biệt:

| Field | Ý nghĩa |
|-------|---------|
| `quantity` | Tổng tồn kho thực tế |
| `reserved_quantity` | Số đang được giữ chỗ cho đơn PENDING/STOCK_RESERVED |

```
Khi order-created event đến:
  reserved_quantity += quantity_ordered   (RESERVE)
  
Khi order-confirmed event đến:
  quantity -= quantity_ordered             (CONFIRM: trừ hẳn)
  reserved_quantity -= quantity_ordered

Khi order-cancelled event đến:
  reserved_quantity -= quantity_ordered   (RELEASE: trả lại slot)
  (quantity KHÔNG thay đổi vì chưa bị trừ)
```

**Available for purchase** = `quantity - reserved_quantity`

---

## 5. Tóm Tắt: Ai Gây Ra Hủy Đơn?

| Nguyên nhân hủy | Cơ chế |
|----------------|--------|
| Hết tồn kho khi đặt | `inventory-failed` Kafka event |
| Thanh toán VNPAY thất bại | `payment-failed` Kafka event |
| Quá 30 phút chưa thanh toán VNPAY | `ReservationExpiryScheduler` (mỗi 60 giây) |
| Thanh toán VNPAY timeout | `PaymentTimeoutScheduler` (mỗi 60 giây) → `payment-failed` |
| User tự hủy | Chưa có public API trong controller hiện tại |
