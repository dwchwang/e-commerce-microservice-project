# Luồng Nghiệp Vụ Chính

## 1. Luồng Đăng Ký và Đăng Nhập

```
Client
  │
  ├─► POST /api/auth/register
  │     identity-service → tạo user trong Keycloak
  │     identity-service → publish Kafka topic "user-registered"
  │     user-service → consume event → tạo User profile trong user_db
  │
  └─► POST /api/auth/login hoặc gọi token endpoint của Keycloak trực tiếp
        Keycloak → trả về access_token + refresh_token (JWT)
```

**Sau khi đăng nhập**, mọi request phải kèm header:
```
Authorization: Bearer <access_token>
```

---

## 2. Luồng Tìm Kiếm Sản Phẩm

```
Client
  │
  ├─► GET /api/products          → product-service (danh sách, filter, phân trang)
  ├─► GET /api/products/{id}     → product-service (chi tiết sản phẩm)
  └─► GET /api/search?keyword=X  → search-service → Elasticsearch (full-text search)
```

**Indexing tự động:**
```
Admin tạo/cập nhật sản phẩm
  → product-service publish Kafka topic "product-created/product-updated/product-deleted"
  → search-service consume → index vào Elasticsearch
```

---

## 3. Luồng Giỏ Hàng

```
Client (đã đăng nhập hoặc có X-Session-Id)
  │
  ├─► POST /api/cart/items       → cart-service → lưu vào Redis
  │     cart-service gọi product-service (Feign) để validate sản phẩm
  │
  ├─► GET  /api/cart             → cart-service → lấy từ Redis
  │
  └─► DELETE /api/cart/items/{id} → cart-service → xóa khỏi Redis
```

> Giỏ hàng lưu trong Redis với key theo userId hoặc sessionId. Không có database.

---

## 4. Luồng Tạo Đơn Hàng (Order Saga)

Đây là luồng phức tạp nhất, dùng **Saga pattern** với Kafka:

```
Client
  │
  └─► POST /api/orders
        order-service
          ├── Gọi product-service (Feign) → kiểm tra giá + tồn tại
          ├── Gọi voucher-service (Feign) → reserve voucher (nếu có)
          ├── Tạo đơn hàng với status PENDING
          └── Lưu outbox event ORDER_CREATED
                OutboxPoller publish Kafka "order-created"
                │
                ├── inventory-service consume → RESERVE tồn kho
                │     Publish "inventory-updated" hoặc "inventory-failed"
                │
                ├── order-service consume "inventory-updated"
                │     → Update order status → STOCK_RESERVED
                │     ├── COD: publish "payment-requested"
                │     └── VNPAY: chờ client gọi REST tạo payment URL
                │
                ├── payment-service consume "payment-requested"
                │     → Tạo COD payment COMPLETED
                │     → Publish "payment-success"
                │
                └── notification-service consume "order-confirmed"/"order-cancelled"
                      → Gửi email xác nhận đơn hàng
```

### Khi thanh toán thành công (VNPAY callback):
```
VNPAY → GET /api/payments/vnpay/return?...
  payment-service → verify signature → update payment COMPLETED
  payment-service → publish "payment-success"
    order-service → update order CONFIRMED, publish "order-confirmed"
    inventory-service → consume "order-confirmed" → CONFIRM reservation (trừ hẳn tồn kho)
    notification-service → gửi email xác nhận đơn hàng
    Client → redirect đến FRONTEND_ORDER_RESULT_URL
```

### Khi thanh toán thất bại / timeout:
```
payment-service → publish "payment-failed"
  order-service → update order CANCELLED
  order-service → publish "order-cancelled"
  inventory-service → consume "order-cancelled" → RELEASE reservation (hoàn tồn kho)
  notification-service → gửi email đơn hàng bị hủy
```

---

## 5. Luồng Flash Sale

```
Admin tạo flash sale campaign với:
  - Một sản phẩm và số lượng giới hạn
  - Thời gian bắt đầu/kết thúc
  - flash-sale-service → lưu vào Redis (cho tốc độ) + PostgreSQL

Client tham gia flash sale:
  POST /api/flash-sales/{eventId}/purchase
    flash-sale-service
      ├── Check thời gian event còn hiệu lực
      ├── Atomic decrement Redis counter (đảm bảo không oversell)
      ├── Nếu còn hàng → publish Kafka "flash-sale-order-requested"
      │     order-service consume → tạo đơn hàng (bypass normal flow)
      └── Nếu hết hàng → trả về 409 Conflict
```

> **Redis atomic** đảm bảo dù có 1000 người mua cùng lúc, chỉ đúng số lượng hàng mới được bán.

---

## 6. Luồng Review Sản Phẩm

```
Client
  │
  └─► POST /api/reviews
        review-service
          ├── Gọi order-service (Feign) → kiểm tra user đã mua sản phẩm chưa
          ├── Nếu chưa mua → 403 Forbidden
          └── Nếu đã mua → tạo review trong review_db
```

> Chỉ user đã mua hàng mới được review — tránh fake review.

---

## 7. Luồng Thông Báo Email

notification-service lắng nghe các Kafka events và gửi email tương ứng:

| Kafka Event | Email được gửi |
|-------------|----------------|
| `order-confirmed` | Xác nhận đơn hàng với chi tiết |
| `order-cancelled` | Đơn hàng bị hủy |

Xem email tại: http://localhost:8025 (Mailpit)

---

## Outbox Pattern (Idempotency)

Các service quan trọng (order, payment) dùng **Outbox pattern** để đảm bảo:
- Event Kafka không bị mất khi service restart giữa transaction DB và publish Kafka
- Consumer vẫn cần idempotency vì Kafka là at-least-once delivery

Hoạt động:
1. Service lưu event vào bảng `outbox` trong cùng transaction với business data
2. Separate thread đọc outbox và publish lên Kafka
3. Sau khi publish thành công, đánh dấu event là "published"

Bảng `processed_events` lưu ID event đã xử lý — tránh xử lý trùng lặp khi consumer restart.
