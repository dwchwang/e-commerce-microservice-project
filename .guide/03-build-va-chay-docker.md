# Build và Chạy Hệ Thống với Docker

## Tổng Quan

Cách chính thức và đơn giản nhất để chạy toàn bộ hệ thống là dùng **Docker Compose**. Tất cả 26 service/container sẽ được khởi động tự động.

---

## Bước 1: Build Tất Cả Service (Compile JAR)

```bash
./mvnw clean package -DskipTests
```

> **Lưu ý**: Lần đầu tiên có thể mất 5-15 phút để Maven tải dependencies. Các lần sau sẽ nhanh hơn nhờ cache.

Kiểm tra build thành công:
```bash
find . -name "*.jar" -path "*/target/*.jar" | grep -v original
# Phải thấy JAR file cho mỗi service
```

---

## Bước 2: Khởi Động Toàn Bộ Hệ Thống

```bash
docker compose up -d --build
```

- `-d`: Chạy nền (detached mode)
- `--build`: Build Docker image từ Dockerfile

> **Thời gian**: Lần đầu mất 5-10 phút vì phải pull images và build. Lần sau nhanh hơn.

---

## Bước 3: Theo Dõi Quá Trình Khởi Động

### Xem trạng thái tất cả container:
```bash
docker compose ps
```

Kết quả mong đợi (sau khoảng 3-5 phút):
```
NAME                           STATUS
ecommerce-postgres             Up (healthy)
ecommerce-keycloak-db          Up (healthy)
ecommerce-redis                Up (healthy)
ecommerce-kafka                Up (healthy)
ecommerce-elasticsearch        Up (healthy)
ecommerce-keycloak             Up (healthy)
ecommerce-mailpit              Up
ecommerce-zipkin               Up
ecommerce-prometheus           Up
ecommerce-grafana              Up
ecommerce-discovery-server     Up
ecommerce-config-server        Up
ecommerce-api-gateway          Up (healthy)
ecommerce-identity-service     Up (healthy)
ecommerce-user-service         Up (healthy)
ecommerce-product-service      Up (healthy)
... (các service còn lại)
```

### Xem log của một service cụ thể:
```bash
docker compose logs -f api-gateway
docker compose logs -f identity-service
docker compose logs -f order-service
```

### Xem log tất cả (không khuyến nghị do quá nhiều):
```bash
docker compose logs -f
```

---

## Bước 4: Kiểm Tra Hệ Thống Đã Sẵn Sàng

### Kiểm tra API Gateway:
```bash
curl http://localhost:8080/actuator/health/readiness
# Phải trả về: {"status":"UP"}
```

### Kiểm tra Eureka (services đã đăng ký chưa):
```bash
curl -u eureka:YOUR_EUREKA_PASSWORD http://localhost:8761/eureka/apps | grep '<app>'
# Phải thấy: IDENTITY-SERVICE, USER-SERVICE, PRODUCT-SERVICE, ...
```

### Kiểm tra tất cả service qua API Gateway:
```bash
curl http://localhost:8080/actuator/health
```

---

## Thứ Tự Khởi Động (Tự Động)

Docker Compose tự quản lý thứ tự khởi động theo `depends_on`:

```
1. postgres, keycloak-db, redis, kafka, elasticsearch, mailpit, zipkin, prometheus
2. keycloak (chờ keycloak-db healthy)
3. discovery-server
4. config-server (chờ discovery-server)
5. api-gateway (chờ config-server, keycloak, redis)
6. identity-service (chờ config-server, keycloak, kafka)
7. user-service, product-service, inventory-service (chờ config-server, postgres, kafka)
8. cart-service, voucher-service (chờ config-server, redis/postgres)
9. order-service (chờ product, inventory, cart, voucher)
10. payment-service, notification-service (chờ order-service)
11. review-service, search-service, content-service, flash-sale-service
```

---

## Dừng Hệ Thống

### Dừng và giữ data:
```bash
docker compose down
```

### Dừng và XÓA toàn bộ data (reset sạch):
```bash
docker compose down -v
```
> **Cảnh báo**: `-v` sẽ xóa tất cả volumes (database, kafka, elasticsearch data). Chỉ dùng khi muốn reset hoàn toàn.

---

## Rebuild Một Service Cụ Thể

Khi bạn thay đổi code của một service và muốn deploy lại:

```bash
# Build lại JAR
./mvnw clean package -pl product-service -am -DskipTests

# Rebuild và restart chỉ service đó
docker compose up -d --build product-service
```

---

## Các Lệnh Docker Compose Hữu Ích

```bash
# Xem tất cả container đang chạy
docker compose ps

# Restart một service
docker compose restart order-service

# Xem log 100 dòng cuối
docker compose logs --tail=100 payment-service

# Exec vào container
docker compose exec postgres psql -U postgres -d product_db

# Xem sử dụng tài nguyên
docker stats
```
