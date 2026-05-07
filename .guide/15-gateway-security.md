# API Gateway — Security, Routing & Resilience

## 1. Routes & Bảo Mật (Public vs Authenticated)

### Public Routes (không cần token)

| Route | Method | Lý do public |
|-------|--------|-------------|
| `/api/auth/**` | ALL | Đăng ký, đăng nhập |
| `/api/products/**` | GET | Xem sản phẩm không cần đăng nhập |
| `/api/search/**` | GET | Tìm kiếm sản phẩm |
| `/api/content/**` | GET | Xem banner, nội dung trang |
| `/api/reviews/product/**` | GET | Xem review của sản phẩm |
| `/api/inventory/**` | GET | Kiểm tra tồn kho |
| `/api/flash-sales/**` | GET | Xem danh sách flash sale |
| `/api/cart`, `/api/cart/**` | ALL | Giỏ hàng (dùng X-Session-Id thay JWT) |
| `/api/payments/vnpay/ipn` | POST | VNPAY server gọi callback (không có token) |
| `/api/payments/vnpay/return` | GET | VNPAY redirect sau thanh toán |
| `/eureka/**` | ALL | Health check của Eureka |
| `/actuator/**` | ALL | Health endpoints |

### Authenticated Routes (cần JWT token)

Tất cả các routes khác đều yêu cầu `Authorization: Bearer <token>`:
- `POST /api/orders/**` — Tạo đơn hàng
- `POST /api/reviews/**` — Viết review
- `POST /api/flash-sales/*/purchase` — Mua flash sale
- `PUT /api/users/**` — Cập nhật profile
- `POST/PUT/DELETE /api/products/**` — Admin quản lý sản phẩm
- `POST/PUT/DELETE /api/vouchers/**` — Admin quản lý voucher
- v.v.

---

## 2. JWT Validation

Gateway tự động validate JWT token với Keycloak:

```yaml
# Cấu hình trong api-gateway.yml
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: http://keycloak:8080/realms/ecommerce
  jwk-set-uri: http://keycloak:8080/realms/ecommerce/protocol/openid-connect/certs
```

**Luồng validate:**
```
Client → Gateway với Bearer token
  Gateway → fetch JWK từ Keycloak (cached)
  Gateway → verify token signature + expiry
  Token valid → forward request + thêm identity headers
  Token invalid/expired → 401 Unauthorized (KHÔNG forward)
```

### Identity Headers Được Forward

Khi token hợp lệ, Gateway trích xuất thông tin từ JWT và forward qua headers:

| Header | Nội dung | Ví dụ |
|--------|---------|-------|
| `X-User-Id` | Keycloak user UUID | `550e8400-e29b-41d4-a716-446655440000` |
| `X-User-Roles` | Danh sách roles | `ROLE_USER` hoặc `ROLE_ADMIN` |
| `X-User-Email` | Email của user | `user@example.com` |

> **Các backend service TIN TƯỞNG hoàn toàn** các headers này — không validate JWT lại.
> Chỉ Gateway mới kiểm tra JWT. Đây là thiết kế intentional để giảm latency.

---

## 3. CORS Configuration

```java
// Được cấu hình trong SecurityConfig.java
allowedOrigins:  ["http://localhost:3000"]   // Frontend React (local dev)
allowedMethods:  GET, POST, PUT, DELETE, PATCH, OPTIONS
allowedHeaders:  Authorization, Content-Type, X-Session-Id
exposedHeaders:  X-User-Id, X-User-Roles, X-User-Email
allowCredentials: true
```

> **Lưu ý cho production**: Phải đổi `allowedOrigins` sang domain thực của frontend.

### Nếu Frontend Gặp CORS Error:
```
Access to fetch at 'http://localhost:8080/api/...' from origin 'http://localhost:3000' 
has been blocked by CORS policy
```
Nguyên nhân phổ biến:
1. Frontend chạy ở port khác (không phải 3000) → sửa `allowedOrigins` trong SecurityConfig
2. Header không được phép → thêm vào `allowedHeaders`

---

## 4. Rate Limiting (Chống Spam)

Gateway dùng Redis-backed rate limiter theo **IP** (cho auth) và **User ID** (cho order/flash-sale).

| Route | Rate Limit | Burst | Key |
|-------|-----------|-------|-----|
| `/api/auth/**` | 10 req/giây | 20 | Theo IP |
| `/api/orders/**` | 10 req/giây | 20 | Theo User ID |
| `/api/flash-sales/*/purchase` (POST) | **3 req/giây** | 5 | Theo User ID |
| Các routes khác | Không giới hạn | - | - |

Khi vượt giới hạn → HTTP `429 Too Many Requests`

### Flash Sale Rate Limit đặc biệt:
Route `POST /api/flash-sales/{id}/purchase` có rate limit **thấp nhất** (3/s) vì:
- Đây là route nhạy cảm nhất (atomic Redis decrement)
- Chống bot spam để mua hàng

---

## 5. Circuit Breaker (Resilience4j) trong Order-Service

Khi order-service gọi sang các service khác qua Feign, Circuit Breaker bảo vệ chống cascading failure:

### Cấu hình:

| Circuit | Failure Threshold | Window Size | Wait (Open State) | Timeout |
|---------|-------------------|-------------|-------------------|---------|
| productService | 50% | 10 requests | 10s | 3s |
| voucherService | 50% | 10 requests | 10s | 3s |
| cartService | 60% | 5 requests | 5s | 2s |

### Retry Policy:

| Circuit | Max Attempts | Wait Between |
|---------|-------------|-------------|
| productService | 3 | 500ms |
| voucherService | 3 | 500ms |
| cartService | 2 | 300ms |

### Các Trạng Thái Circuit Breaker:

```
CLOSED (bình thường) 
  → nhiều lỗi → OPEN (chặn request 10s, return fallback ngay)
  → sau 10s → HALF-OPEN (thử 3 request)
  → nếu OK → CLOSED lại
  → nếu vẫn lỗi → OPEN lại
```

### Xem Trạng Thái Circuit Breaker:
```bash
# Khi chạy local dev (port 8086 exposed)
curl http://localhost:8086/actuator/circuitbreakers
```

---

## 6. Cart Service — Session-Based (Không Dùng JWT)

Cart service khác biệt: dùng `X-Session-Id` header thay vì JWT để nhận diện giỏ hàng.

```bash
# Ví dụ: Thêm sản phẩm vào giỏ không cần đăng nhập
curl -X POST http://localhost:8080/api/cart/items \
  -H "X-Session-Id: my-browser-session-id-123" \
  -H "Content-Type: application/json" \
  -d '{"productId":"33333333-3333-3333-3333-333333333333","quantity":2}'
```

Giỏ hàng được lưu trong Redis với key: `cart:guest:{sessionId}` hoặc `cart:{userId}` sau đăng nhập.

---

## 7. VNPAY Callback Security

VNPAY server gọi 2 endpoints mà không có JWT:

| Endpoint | Loại | Dùng để |
|----------|------|---------|
| `GET /api/payments/vnpay/return` | User redirect | Chuyển hướng user sau thanh toán |
| `POST /api/payments/vnpay/ipn` | Server-to-server | VNPAY thông báo kết quả cho server |

**Bảo mật bằng VNPAY Secure Hash**: Cả 2 endpoints đều verify chữ ký HMAC-SHA512 bằng `VNPAY_HASH_SECRET` → không thể giả mạo callback.
