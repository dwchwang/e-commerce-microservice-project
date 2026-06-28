# Hệ Thống E-Commerce Microservice

**Đồ án tốt nghiệp** — Nguyễn Đức Bảo Hoàng (20227116), Đại học Bách Khoa Hà Nội.

Hệ thống thương mại điện tử được xây dựng theo kiến trúc **microservice** với **13 service nghiệp vụ** trên nền Spring Boot 3.5 / Spring Cloud 2025 / Java 21, giao tiếp đồng bộ qua API Gateway + OpenFeign và bất đồng bộ qua Kafka. Đi kèm là **giao diện web** (storefront + admin panel) viết bằng Next.js 16, cùng stack observability đầy đủ (Prometheus, Grafana, Zipkin).

> 📦 **Cài đặt & chạy chi tiết:** xem [`HUONG_DAN_CAI_DAT.md`](HUONG_DAN_CAI_DAT.md) (hướng dẫn từng bước cho môi trường local).

---

## Tính Năng Chính

- **Xác thực & phân quyền** qua Keycloak (OAuth2/OIDC), JWT RS256, vai trò `ROLE_USER` / `ROLE_ADMIN`.
- **Quản lý sản phẩm & tồn kho** với cache Redis và tìm kiếm full-text Elasticsearch.
- **Giỏ hàng** lưu trên Redis, **đặt hàng** với luồng Saga điều phối qua Kafka.
- **Thanh toán** COD và **VNPAY** (sandbox), đối soát qua scheduled job.
- **Flash sale** chống oversell bằng Redis + state machine.
- **Voucher / khuyến mãi**, **đánh giá sản phẩm** (chỉ người đã mua), **nội dung** (banner/trang tĩnh).
- **Thông báo email** bất đồng bộ qua Kafka + SMTP (Mailpit khi chạy local).
- **Observability:** metrics (Micrometer/Prometheus), dashboard (Grafana), distributed tracing (Zipkin), correlation `traceId`/`spanId` trong log.

---

## Kiến Trúc

```text
                    Trình duyệt / Client
                            │
                            ▼
                  ┌──────────────────┐
                  │   API Gateway    │  :8080  — entry point, xác thực JWT, rate limiting
                  └────────┬─────────┘
                           │
          Spring Cloud:  Eureka :8761  ·  Config Server :8888
                           │
   ┌───────────────────────┴───────────────────────────────────┐
   │  identity-service     :8081  → Keycloak, Kafka             │
   │  user-service         :8082  → PostgreSQL, Kafka           │
   │  product-service      :8083  → PostgreSQL, Redis, Kafka    │
   │  inventory-service    :8084  → PostgreSQL, Kafka           │
   │  cart-service         :8085  → Redis                       │
   │  order-service        :8086  → PostgreSQL, Kafka, Feign    │
   │  payment-service      :8087  → PostgreSQL, Kafka, VNPAY    │
   │  voucher-service      :8088  → PostgreSQL                  │
   │  notification-service :8089  → PostgreSQL, Kafka, SMTP     │
   │  review-service       :8090  → PostgreSQL, order-service   │
   │  search-service       :8091  → Elasticsearch, Kafka        │
   │  content-service      :8092  → PostgreSQL                  │
   │  flash-sale-service   :8093  → PostgreSQL, Redis, Kafka    │
   └────────────────────────────────────────────────────────────┘
```

Tất cả service đăng ký với **Eureka** và đọc cấu hình theo môi trường từ **Config Server**. **API Gateway** là điểm vào duy nhất được expose ra host; Gateway xác thực JWT rồi forward các header tin cậy (`X-User-Id`, `X-User-Roles`, `X-User-Email`) xuống service backend.

**Database:** một instance PostgreSQL chứa 10 database độc lập (mỗi service một database, theo nguyên tắc database-per-service).

---

## Tech Stack

| Lớp | Công nghệ |
|---|---|
| Ngôn ngữ / Build | Java 21, Maven Wrapper |
| Framework | Spring Boot 3.5.13, Spring Cloud 2025.0.0 |
| Spring Cloud | Gateway, Eureka, Config Server, OpenFeign |
| Bảo mật | Keycloak 26, OAuth2 Resource Server, JWT (RS256) |
| Dữ liệu | PostgreSQL 17, Redis 8, Elasticsearch 8.18, Flyway (migration) |
| Messaging | Apache Kafka (KRaft) |
| Resilience | Resilience4j (Circuit Breaker), Redis rate limiting |
| Observability | Micrometer, Prometheus, Grafana, Zipkin |
| Frontend | Next.js 16, React 19, TanStack Query, Zustand, Tailwind + shadcn/ui, Zod |
| Kiểm thử | JUnit 5, Mockito, Testcontainers |
| Triển khai | Docker Compose (local) / Docker Compose prod trên AWS EC2 |

---

## Cấu Trúc Thư Mục

```text
.
├── discovery-server/        # Eureka
├── config-server/           # Spring Cloud Config
├── api-gateway/             # Gateway, JWT, rate limiting
├── identity-service/        # Đăng ký/đăng nhập, tích hợp Keycloak
├── user-service/            # Hồ sơ người dùng
├── product-service/         # Sản phẩm (Redis cache)
├── inventory-service/       # Tồn kho
├── cart-service/            # Giỏ hàng (Redis)
├── order-service/           # Đơn hàng, điều phối Saga
├── payment-service/         # Thanh toán COD/VNPAY
├── voucher-service/         # Voucher/khuyến mãi
├── notification-service/    # Email (Kafka + SMTP)
├── review-service/          # Đánh giá sản phẩm
├── search-service/          # Tìm kiếm (Elasticsearch)
├── content-service/         # Banner/nội dung tĩnh
├── flash-sale-service/      # Flash sale (Redis + state machine)
├── common/                  # Thư viện dùng chung (DTO, exception, security)
├── frontend/                # Web Next.js (storefront + admin)
├── keycloak/                # realm-export.json (import tự động)
├── init-db/                 # Script khởi tạo 10 database
├── prometheus/ grafana/     # Cấu hình & dashboard observability
├── aws/                     # Script triển khai/scale trên EC2
├── docker-compose.yml       # Chạy local (backend + hạ tầng)
├── docker-compose.prod.yml  # Chạy production trên EC2 (kèm frontend)
├── .env.example             # Mẫu biến môi trường
└── HUONG_DAN_CAI_DAT.md     # Hướng dẫn cài đặt chi tiết
```

---

## Quick Start (Local)

> Yêu cầu: Java 21, Docker Desktop (Compose V2), Node.js 18+ (cho frontend).
> Chi tiết đầy đủ trong [`HUONG_DAN_CAI_DAT.md`](HUONG_DAN_CAI_DAT.md).

```bash
# 1. Cấu hình biến môi trường
cp .env.example .env                       # điền secret cần thiết

# 2. Build & chạy backend + hạ tầng
./mvnw clean package -DskipTests
docker compose up -d --build

# 3. Kiểm tra
docker compose ps
curl http://localhost:8080/actuator/health/readiness

# 4. Chạy giao diện web
cp frontend/.env.local.example frontend/.env.local   # đặt API_BASE_URL=http://localhost:8080
cd frontend && npm install && npm run dev
```

Mở **http://localhost:3000** để sử dụng. Dừng hệ thống: `docker compose down` (thêm `-v` để xóa data).

---

## URL Truy Cập

| Thành phần | URL | Đăng nhập |
|---|---|---|
| Frontend storefront | http://localhost:3000 | — |
| Admin Panel | http://localhost:3000/admin | tài khoản `ROLE_ADMIN` |
| API Gateway | http://localhost:8080 | JWT |
| Swagger UI (tổng hợp) | http://localhost:8080/swagger-ui.html | — |
| Eureka | http://localhost:8761 | `eureka` / `EUREKA_PASSWORD` |
| Keycloak Admin | http://localhost:8180 | `admin` / `KEYCLOAK_ADMIN_PASSWORD` |
| Grafana | http://localhost:3000 *(trùng cổng FE)* | `admin` / `GF_SECURITY_ADMIN_PASSWORD` |
| Prometheus | http://localhost:9090 | — |
| Zipkin | http://localhost:9411 | — |
| Mailpit | http://localhost:8025 | — |
| Elasticsearch | http://localhost:9200 | — |

Khi chạy từng service trực tiếp (local dev), mỗi service có Swagger riêng tại `http://localhost:<port>/swagger-ui.html` (port 8081–8093 theo sơ đồ trên).

---

## Các Mẫu Thiết Kế Trọng Tâm

- **Database-per-service** — mỗi service sở hữu database riêng, migration bằng Flyway.
- **API Gateway tập trung** — xác thực JWT, rate limiting (Redis) cho route auth/order/flash-sale, forward header định danh tin cậy.
- **Saga điều phối qua Kafka** — luồng đặt hàng phối hợp order ↔ inventory ↔ payment ↔ notification.
- **Outbox pattern + processed-event table** — đảm bảo idempotency và at-least-once cho event quan trọng.
- **State machine** — quản lý vòng đời Order / Payment / FlashSale / Inventory.
- **Circuit Breaker (Resilience4j)** — chịu lỗi cho các lời gọi Feign liên service.
- **Scheduled jobs** — hết hạn đơn/voucher, timeout thanh toán, đẩy outbox, đối soát.
- **Liveness/Readiness probes** — health group theo phụ thuộc thực tế của từng service.

---

## Observability

Prometheus scrape `/actuator/prometheus` của API Gateway và toàn bộ 13 service nghiệp vụ. Grafana tự nạp dashboard khi khởi động:

- **Spring Boot Overview** — metrics tổng quát
- **JVM Overview** — heap, GC, threads
- **E-commerce Saga Overview** — metrics nghiệp vụ (order/payment counters, p95/p99 latency)

Zipkin nhận span từ Gateway và các service; log gắn `traceId`/`spanId` để trace xuyên suốt.

---

## Kiểm Thử

```bash
./mvnw test                              # Chạy toàn bộ test
./mvnw -pl order-service -am test        # Test một module cụ thể
./mvnw -pl flash-sale-service -am test
docker compose config --quiet            # Validate cấu hình compose
```

Test tích hợp dùng **Testcontainers** với `postgres:17-alpine`, đồng nhất với image PostgreSQL runtime.

---

## Triển Khai

- **Local:** `docker-compose.yml` — backend + hạ tầng; frontend chạy bằng `npm run dev`.
- **Production (AWS EC2):** `docker-compose.prod.yml` + script trong thư mục `aws/` (bootstrap, start/stop, scale). Frontend chạy trong Docker, expose qua domain `nip.io`.
