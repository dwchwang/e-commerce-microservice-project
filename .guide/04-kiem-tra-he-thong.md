# Kiểm Tra Hệ Thống Sau Khi Khởi Động

## Checklist Kiểm Tra Nhanh

Chạy các lệnh dưới đây theo thứ tự để xác nhận hệ thống hoạt động đúng.

---

## 1. Infrastructure Services

### PostgreSQL
```bash
# Kết nối và liệt kê database
docker compose exec postgres psql -U postgres -c "\l"
# Phải thấy: user_db, product_db, inventory_db, voucher_db, order_db,
#            payment_db, notification_db, review_db, content_db, flash_sale_db
```

### Redis
```bash
docker compose exec redis redis-cli ping
# Phải trả về: PONG
```

### Kafka
```bash
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
# Phải thấy danh sách topics (sau khi services khởi động và tạo topics)
```

### Elasticsearch
```bash
curl http://localhost:9200/_cluster/health
# "status" phải là "green" hoặc "yellow"
```

---

## 2. Spring Cloud Infrastructure

### Eureka Dashboard
Truy cập: http://localhost:8761

Đăng nhập: `eureka` / `EUREKA_PASSWORD` (giá trị trong `.env`)

Phải thấy tất cả services đã đăng ký:
- IDENTITY-SERVICE
- USER-SERVICE
- PRODUCT-SERVICE
- INVENTORY-SERVICE
- CART-SERVICE
- VOUCHER-SERVICE
- ORDER-SERVICE
- PAYMENT-SERVICE
- NOTIFICATION-SERVICE
- REVIEW-SERVICE
- SEARCH-SERVICE
- CONTENT-SERVICE
- FLASH-SALE-SERVICE
- API-GATEWAY

### Config Server
```bash
# Xem cấu hình của một service (ví dụ: api-gateway)
curl http://localhost:8888/api-gateway/docker
# Phải trả về JSON cấu hình
```

---

## 3. API Gateway

### Health Check
```bash
curl http://localhost:8080/actuator/health/liveness
# {"status":"UP","components":{"livenessState":{"status":"UP"}}}

curl http://localhost:8080/actuator/health/readiness
# {"status":"UP",...}
```

### Swagger UI (tổng hợp tất cả API)
Truy cập: http://localhost:8080/swagger-ui.html

---

## 4. Keycloak (Identity Provider)

Truy cập Admin UI: http://localhost:8180

Đăng nhập: `admin` / `KEYCLOAK_ADMIN_PASSWORD`

Kiểm tra:
- Realm `ecommerce` đã được tạo
- Clients: `ecommerce-client`, `identity-service-admin` đã tồn tại

### Lấy Access Token để test API
> Chạy bước đăng ký user ở mục 5 trước nếu `test@example.com` chưa tồn tại.

```bash
# Thay YOUR_CLIENT_SECRET bằng secret thực tế
curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=ecommerce-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=test@example.com" \
  -d "password=Test@123"
```

---

## 5. Business Services (qua API Gateway)

### Identity Service
```bash
# Đăng ký user mới
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test@123","fullName":"Test User"}'
```

### Product Service
```bash
# Lấy danh sách sản phẩm (không cần token)
curl http://localhost:8080/api/products?page=0&size=10
```

### Search Service
```bash
# Tìm kiếm sản phẩm
curl "http://localhost:8080/api/search?keyword=phone"
```

---

## 6. Observability Stack

### Prometheus
Truy cập: http://localhost:9090/targets

Phải thấy tất cả targets ở trạng thái **UP** (màu xanh lá).

### Grafana
Truy cập: http://localhost:3000

Đăng nhập: `admin` / `GF_SECURITY_ADMIN_PASSWORD`

Dashboards sẵn có:
- **Spring Boot Overview** — metrics tổng quát
- **JVM Overview** — heap, GC, threads
- **E-commerce Saga Overview** — metrics nghiệp vụ

### Zipkin (Distributed Tracing)
Truy cập: http://localhost:9411

Gửi một request qua API Gateway, sau đó tìm trace trong Zipkin để xem request đi qua các service nào.

### Mailpit (Email Testing)
Truy cập: http://localhost:8025

Sau khi đăng ký user hoặc tạo đơn hàng, email thông báo sẽ xuất hiện ở đây.

---

## 7. Script Kiểm Tra Tổng Hợp

```bash
#!/bin/bash
echo "=== Kiểm tra hệ thống E-Commerce ==="

check() {
  local name=$1
  local url=$2
  if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -qE "^(200|401|302)$"; then
    echo "✓ $name - OK"
  else
    echo "✗ $name - FAILED ($url)"
  fi
}

check "API Gateway Health"    "http://localhost:8080/actuator/health/liveness"
check "Eureka Dashboard"      "http://localhost:8761"
check "Keycloak"              "http://localhost:8180"
check "Prometheus"            "http://localhost:9090"
check "Grafana"               "http://localhost:3000"
check "Zipkin"                "http://localhost:9411"
check "Mailpit"               "http://localhost:8025"
check "Elasticsearch"         "http://localhost:9200"
check "Swagger UI"            "http://localhost:8080/swagger-ui.html"
```

Lưu file trên thành `check-system.sh`, chạy: `bash check-system.sh`
