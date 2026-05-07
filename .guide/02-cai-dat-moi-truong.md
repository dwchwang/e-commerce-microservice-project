# Cài Đặt Môi Trường (.env và Keycloak)

## Bước 1: Tạo File .env

File `.env` chứa tất cả mật khẩu và cấu hình bí mật. Tạo từ template:

```bash
cp .env.example .env
```

Sau đó mở file `.env` và điền giá trị:

```bash
# ============================================================
# PostgreSQL — Database chính
# ============================================================
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123          # Đặt mật khẩu mạnh hơn khi deploy

# ============================================================
# Keycloak DB — Database riêng cho Keycloak
# ============================================================
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=keycloak123

# ============================================================
# Keycloak — Identity Provider
# ============================================================
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123

# Client để các service gọi Keycloak API
KEYCLOAK_CLIENT_ID=ecommerce-client
KEYCLOAK_CLIENT_SECRET=<lấy từ realm-export hoặc Keycloak Admin UI>

# Service account cho identity-service quản lý users
KEYCLOAK_ADMIN_CLIENT_ID=identity-service-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=<lấy từ realm-export hoặc Keycloak Admin UI>

# ============================================================
# Eureka — Service Discovery
# ============================================================
EUREKA_USER=eureka
EUREKA_PASSWORD=eureka123
EUREKA_DEFAULT_ZONE=http://eureka:eureka123@discovery-server:8761/eureka/

# ============================================================
# Grafana — Dashboard
# ============================================================
GF_SECURITY_ADMIN_PASSWORD=grafana123

# ============================================================
# VNPAY — Thanh toán (dùng sandbox để test)
# ============================================================
VNPAY_TMN_CODE=<mã TMN từ VNPAY sandbox>
VNPAY_HASH_SECRET=<secret key từ VNPAY sandbox>
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/api/payments/vnpay/return
FRONTEND_ORDER_RESULT_URL=http://localhost:3000/order/result

# ============================================================
# Email — Mailpit (chạy local, không cần thay đổi)
# ============================================================
SMTP_HOST=mailpit
SMTP_PORT=1025
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_AUTH=false
SMTP_STARTTLS=false
```

> **Quan trọng**: `EUREKA_DEFAULT_ZONE` phải khớp với `EUREKA_USER` và `EUREKA_PASSWORD`.
> Ví dụ: nếu `EUREKA_PASSWORD=mypass` thì `EUREKA_DEFAULT_ZONE=http://eureka:mypass@discovery-server:8761/eureka/`

## Bước 2: Lấy Keycloak Client Secret

Keycloak được import tự động từ file `keycloak/realm-export.json` khi khởi động. Các secret đã được định nghĩa sẵn trong file này.

### Cách lấy secret từ realm-export.json:

```bash
# Tìm client secret của ecommerce-client
cat keycloak/realm-export.json | grep -A 20 '"clientId": "ecommerce-client"' | grep "secret"

# Tìm client secret của identity-service-admin
cat keycloak/realm-export.json | grep -A 20 '"clientId": "identity-service-admin"' | grep "secret"
```

Hoặc sau khi hệ thống khởi động:
1. Truy cập http://localhost:8180
2. Đăng nhập với `admin` / `KEYCLOAK_ADMIN_PASSWORD`
3. Vào realm `ecommerce` → Clients → chọn client → Credentials tab → Copy Secret

## Bước 3: Kiểm Tra File .env

```bash
# Xem nội dung đã điền đúng chưa
cat .env

# Kiểm tra EUREKA_DEFAULT_ZONE khớp với EUREKA_USER và EUREKA_PASSWORD
grep EUREKA .env
```

## Cấu Trúc File .env Mẫu Hoàn Chỉnh (Để Test Nhanh)

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=keycloak123
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
KEYCLOAK_CLIENT_ID=ecommerce-client
KEYCLOAK_CLIENT_SECRET=local-dev-ecommerce-secret
KEYCLOAK_ADMIN_CLIENT_ID=identity-service-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=local-dev-identity-admin-secret
EUREKA_USER=eureka
EUREKA_PASSWORD=eureka123
EUREKA_DEFAULT_ZONE=http://eureka:eureka123@discovery-server:8761/eureka/
GF_SECURITY_ADMIN_PASSWORD=grafana123
VNPAY_TMN_CODE=DEMO
VNPAY_HASH_SECRET=DEMOSECRET
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/api/payments/vnpay/return
FRONTEND_ORDER_RESULT_URL=http://localhost:3000/order/result
SMTP_HOST=mailpit
SMTP_PORT=1025
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_AUTH=false
SMTP_STARTTLS=false
```
