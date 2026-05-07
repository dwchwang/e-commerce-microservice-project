# API Tham Khảo Nhanh

## Xác Thực (Authentication)

Các API private cần header:
```http
Authorization: Bearer <access_token>
```

Các API giỏ hàng có thể dùng `X-Session-Id` khi chưa đăng nhập. Khi đã đăng nhập, API Gateway tự thêm `X-User-Id` từ JWT.

---

## Auth / Identity Service — `/api/auth`

### Đăng ký user
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "Pass@123",
  "fullName": "John Doe"
}
```

### Đăng nhập
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "Pass@123"
}
```

### Refresh token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<refresh_token>"
}
```

### Logout
```http
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "<refresh_token>"
}
```

---

## User Service — `/api/users`

### Lấy profile của mình
```http
GET /api/users/me
Authorization: Bearer <token>
```

### Cập nhật profile
```http
PUT /api/users/me
Authorization: Bearer <token>
Content-Type: application/json

{
  "fullName": "John Doe",
  "phoneNumber": "0901234567",
  "avatarUrl": "https://example.com/avatar.png"
}
```

### Địa chỉ giao hàng
```http
GET    /api/users/me/addresses
POST   /api/users/me/addresses
PUT    /api/users/me/addresses/{addressId}
DELETE /api/users/me/addresses/{addressId}
Authorization: Bearer <token>
```

---

## Product Service — `/api/products`

### Danh sách sản phẩm
```http
GET /api/products?page=0&size=20&keyword=iphone&minPrice=100000&maxPrice=50000000
```

### Chi tiết sản phẩm
```http
GET /api/products/{productId}
GET /api/products/sku/{sku}
```

### Tạo sản phẩm (Admin)
```http
POST /api/products
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "sku": "IPHONE-15-128",
  "name": "iPhone 15 128GB",
  "description": "Apple iPhone 15",
  "price": 25000000,
  "categoryId": "11111111-1111-1111-1111-111111111111",
  "brandId": "22222222-2222-2222-2222-222222222222",
  "imageUrls": ["https://example.com/iphone15.jpg"],
  "specs": []
}
```

### Category và Brand
```http
GET  /api/products/categories
GET  /api/products/categories/{id}
POST /api/products/categories

GET  /api/products/brands
POST /api/products/brands
```

---

## Inventory Service — `/api/inventory`

### Kiểm tra tồn kho
```http
GET /api/inventory/{sku}
GET /api/inventory/check?skus=IPHONE-15-128,SAMSUNG-S24
```

### Nhập kho / xuất kho (Admin)
```http
POST /api/inventory/stock-in
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "sku": "IPHONE-15-128",
  "productName": "iPhone 15 128GB",
  "quantity": 100,
  "note": "Initial stock"
}
```

```http
POST /api/inventory/stock-out
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "sku": "IPHONE-15-128",
  "quantity": 5,
  "note": "Manual adjustment"
}
```

---

## Cart Service — `/api/cart`

### Xem giỏ hàng
```http
GET /api/cart
X-Session-Id: browser-session-123
```

### Thêm vào giỏ
```http
POST /api/cart/items
X-Session-Id: browser-session-123
Content-Type: application/json

{
  "productId": "33333333-3333-3333-3333-333333333333",
  "quantity": 2
}
```

### Cập nhật / xóa giỏ hàng
```http
PUT    /api/cart/items/{productId}
DELETE /api/cart/items/{productId}
DELETE /api/cart
X-Session-Id: browser-session-123
```

---

## Voucher Service — `/api/vouchers`

```http
GET    /api/vouchers              # Admin
GET    /api/vouchers/active       # User/Admin
GET    /api/vouchers/{id}         # Admin
POST   /api/vouchers              # Admin
PUT    /api/vouchers/{id}         # Admin
DELETE /api/vouchers/{id}         # Admin
Authorization: Bearer <token>
```

---

## Order Service — `/api/orders`

### Tạo đơn hàng
```http
POST /api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "items": [
    {
      "productId": "33333333-3333-3333-3333-333333333333",
      "quantity": 2
    }
  ],
  "paymentMethod": "COD",
  "voucherCode": "SAVE10",
  "shippingName": "John Doe",
  "shippingPhone": "0901234567",
  "shippingAddress": "123 Main St, Ho Chi Minh City"
}
```

`paymentMethod` hiện dùng `COD` hoặc `VNPAY`.

### Xem đơn hàng
```http
GET /api/orders
GET /api/orders/{orderId}
GET /api/orders/{orderId}/status
Authorization: Bearer <token>
```

> Hiện controller chưa có endpoint user tự hủy đơn. Hủy do hệ thống xử lý qua payment failed, timeout hoặc inventory failed.

---

## Payment Service — `/api/payments`

### Tạo link thanh toán VNPAY
```http
POST /api/payments/vnpay/create?orderId={orderId}
Authorization: Bearer <token>
```

Trả về `paymentId`, `orderId`, `paymentUrl`, `expiresAt`.

### Xem payment theo order
```http
GET /api/payments/{orderId}
Authorization: Bearer <token>
```

### VNPAY Callback (gọi tự động)
```http
POST /api/payments/vnpay/ipn?...params...
GET  /api/payments/vnpay/return?...params...
```

---

## Review Service — `/api/reviews`

### Tạo review (phải đã mua hàng)
```http
POST /api/reviews
Authorization: Bearer <token>
Content-Type: application/json

{
  "productId": "33333333-3333-3333-3333-333333333333",
  "rating": 5,
  "comment": "Sản phẩm tốt!"
}
```

### Xem review
```http
GET /api/reviews/product/{productId}
GET /api/reviews/product/{productId}/rating
```

---

## Search Service — `/api/search`

```http
GET /api/search?keyword=iphone&page=0&size=10
GET /api/search/suggestions?prefix=iph
```

---

## Flash Sale Service — `/api/flash-sales`

### Danh sách flash sale đang active
```http
GET /api/flash-sales
GET /api/flash-sales/{campaignId}
```

### Tạo campaign (Admin)
```http
POST /api/flash-sales
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "productId": "33333333-3333-3333-3333-333333333333",
  "sku": "IPHONE-15-128",
  "productName": "iPhone 15 128GB",
  "originalPrice": 25000000,
  "salePrice": 19900000,
  "quantity": 100,
  "startTime": "2026-05-06T09:00:00",
  "endTime": "2026-05-06T10:00:00"
}
```

### Tham gia mua flash sale
```http
POST /api/flash-sales/{campaignId}/purchase
Authorization: Bearer <token>
Content-Type: application/json

{
  "paymentMethod": "COD",
  "shippingName": "John Doe",
  "shippingPhone": "0901234567",
  "shippingAddress": "123 Main St, Ho Chi Minh City"
}
```

---

## Content Service — `/api/content`

```http
GET /api/content/banners/active
GET /api/content/posts
GET /api/content/posts/{slug}
```

---

## Status Codes Thường Gặp

| Code | Ý nghĩa |
|------|---------|
| 200 | Thành công |
| 400 | Request không hợp lệ |
| 401 | Chưa đăng nhập hoặc token hết hạn |
| 403 | Không có quyền |
| 404 | Không tìm thấy |
| 409 | Conflict, ví dụ flash sale hết hàng |
| 429 | Rate limit exceeded |
| 500 | Lỗi server |

---

## Swagger UI

Xem đầy đủ API với giao diện trực quan:
- **Tổng hợp (Docker)**: http://localhost:8080/swagger-ui.html
- **Từng service (Local dev)**: http://localhost:PORT/swagger-ui.html
