# Hướng Dẫn Hệ Thống E-Commerce Microservice

Thư mục `.guide` chứa tài liệu hướng dẫn chi tiết để chạy và sử dụng hệ thống.

## Đọc Theo Thứ Tự (Lần Đầu Cài Đặt)

| File | Nội dung |
|------|---------|
| [00-tong-quan.md](00-tong-quan.md) | Kiến trúc hệ thống, sơ đồ các service |
| [01-yeu-cau-he-thong.md](01-yeu-cau-he-thong.md) | Cài Java 21, Docker Desktop, Maven |
| [02-cai-dat-moi-truong.md](02-cai-dat-moi-truong.md) | Tạo file `.env`, cấu trúc tổng quan |
| [02-lay-credentials.md](02-lay-credentials.md) | **Chi tiết lấy từng credential** — VNPAY, Keycloak secrets, thẻ test |
| [03-build-va-chay-docker.md](03-build-va-chay-docker.md) | Build JAR + `docker compose up` |
| [04-kiem-tra-he-thong.md](04-kiem-tra-he-thong.md) | Kiểm tra health, Eureka, Swagger UI |

## Vận Hành & Tham Khảo

| File | Nội dung |
|------|---------|
| [05-cac-dich-vu-va-cong.md](05-cac-dich-vu-va-cong.md) | Danh sách port, URL truy cập |
| [06-keycloak-setup.md](06-keycloak-setup.md) | Cấu hình Keycloak, đăng ký/đăng nhập, lấy JWT |
| [07-monitoring-observability.md](07-monitoring-observability.md) | Grafana dashboards, Prometheus, Zipkin |
| [08-chay-local-dev.md](08-chay-local-dev.md) | Debug với IDE, chạy service riêng lẻ |
| [10-xu-ly-loi.md](10-xu-ly-loi.md) | Troubleshooting 10 lỗi thường gặp |
| [11-api-tham-khao.md](11-api-tham-khao.md) | API endpoints, request/response mẫu |

## Kiến Trúc Sâu (Hiểu Cơ Chế Bên Trong)

| File | Nội dung |
|------|---------|
| [09-luong-nghiep-vu.md](09-luong-nghiep-vu.md) | Luồng đăng ký, đơn hàng, flash sale, review |
| [12-kafka-topics.md](12-kafka-topics.md) | **Bản đồ đầy đủ 11 Kafka topics** — ai publish, ai consume |
| [13-state-machines.md](13-state-machines.md) | **State machine** — Order/Payment/FlashSale/Inventory |
| [14-scheduler-jobs.md](14-scheduler-jobs.md) | **6 scheduled jobs** — expiry, timeout, outbox, reconciliation |
| [15-gateway-security.md](15-gateway-security.md) | **Routes public/private, CORS, rate limiting, Circuit Breaker** |

---

## Lệnh Quan Trọng Nhất

```bash
# Cài đặt lần đầu
cp .env.example .env          # Điền secrets vào .env
./mvnw clean package -DskipTests  # Build tất cả
docker compose up -d --build  # Khởi động hệ thống

# Kiểm tra
docker compose ps             # Xem trạng thái
docker compose logs -f api-gateway  # Theo dõi log
curl http://localhost:8080/actuator/health/readiness

# Dừng
docker compose down           # Dừng (giữ data)
docker compose down -v        # Dừng + xóa data
```

## URLs Quan Trọng

| URL | Mục đích |
|-----|---------|
| http://localhost:8080 | API Gateway (entry point) |
| http://localhost:8080/swagger-ui.html | API Documentation |
| http://localhost:8761 | Eureka (service registry) |
| http://localhost:8180 | Keycloak Admin |
| http://localhost:3000 | Grafana Dashboard |
| http://localhost:9411 | Zipkin Tracing |
| http://localhost:8025 | Mailpit (email testing) |
