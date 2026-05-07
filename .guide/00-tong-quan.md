# Tổng Quan Hệ Thống E-Commerce Microservice

## Kiến Trúc Tổng Thể

```
Client (Browser / Mobile App)
         |
         v
  ┌─────────────────┐
  │   API Gateway   │  :8080  — Cổng vào duy nhất, xác thực JWT, rate limiting
  └────────┬────────┘
           |
    ┌──────┴──────────────────────────────────────────┐
    │              Spring Cloud Ecosystem              │
    │                                                  │
    │  Discovery Server (Eureka)  :8761               │
    │  Config Server              :8888               │
    └──────┬───────────────────────────────────────────┘
           |
  ┌────────┴─────────────────────────────────────────────────────┐
  │                    Business Services                          │
  │                                                              │
  │  identity-service    :8081  → Keycloak + Kafka              │
  │  user-service        :8082  → PostgreSQL + Kafka            │
  │  product-service     :8083  → PostgreSQL + Redis + Kafka    │
  │  inventory-service   :8084  → PostgreSQL + Kafka            │
  │  cart-service        :8085  → Redis (lưu giỏ hàng)         │
  │  order-service       :8086  → PostgreSQL + Kafka            │
  │  payment-service     :8087  → PostgreSQL + Kafka + VNPAY    │
  │  voucher-service     :8088  → PostgreSQL                    │
  │  notification-service:8089  → PostgreSQL + Kafka + SMTP     │
  │  review-service      :8090  → PostgreSQL                    │
  │  search-service      :8091  → Elasticsearch + Kafka         │
  │  content-service     :8092  → PostgreSQL                    │
  │  flash-sale-service  :8093  → PostgreSQL + Redis + Kafka    │
  └──────────────────────────────────────────────────────────────┘
```

## Các Thành Phần Infrastructure

| Thành phần | Image | Port | Mục đích |
|---|---|---|---|
| PostgreSQL | postgres:17-alpine | 5432 | Database chính (10 database) |
| Keycloak DB | postgres:17-alpine | (internal) | Database riêng cho Keycloak |
| Redis | redis:8-alpine | 6379 | Cache, giỏ hàng, rate limiting |
| Kafka (KRaft) | confluent/cp-kafka:8.2.0 | 9092 | Message broker bất đồng bộ |
| Elasticsearch | elasticsearch:8.18.8 | 9200 | Full-text search |
| Keycloak | keycloak:26.6.1 | 8180 | Identity Provider, quản lý user/JWT |
| Mailpit | axllent/mailpit:v1.27.8 | 8025 | Email testing (local SMTP) |
| Zipkin | openzipkin/zipkin:3.5.0 | 9411 | Distributed tracing |
| Prometheus | prom/prometheus:v3.11.2 | 9090 | Thu thập metrics |
| Grafana | grafana/grafana:13.0.1 | 3000 | Dashboard metrics |

## Các Database PostgreSQL

Một PostgreSQL instance chứa **10 database** riêng biệt:

| Database | Service sử dụng |
|---|---|
| `user_db` | user-service |
| `product_db` | product-service |
| `inventory_db` | inventory-service |
| `voucher_db` | voucher-service |
| `order_db` | order-service |
| `payment_db` | payment-service |
| `notification_db` | notification-service |
| `review_db` | review-service |
| `content_db` | content-service |
| `flash_sale_db` | flash-sale-service |

## Luồng Giao Tiếp

### Đồng bộ (REST qua OpenFeign)
- `cart-service` → `product-service` (lấy thông tin sản phẩm)
- `order-service` → `product-service`, `inventory-service`, `cart-service`, `voucher-service`
- `payment-service` → `order-service`
- `review-service` → `order-service` (kiểm tra quyền review)
- `flash-sale-service` → `order-service`

### Bất đồng bộ (Kafka Events)
- `identity-service` → Kafka → `user-service` (tạo profile sau đăng ký)
- `order-service` → Kafka → `inventory-service` (trừ tồn kho)
- `order-service` → Kafka → `payment-service` (khởi tạo thanh toán)
- `payment-service` → Kafka → `order-service` (kết quả thanh toán)
- `order-service` → Kafka → `notification-service` (gửi email)
- `product-service` → Kafka → `search-service` (index sản phẩm)
- `flash-sale-service` → Kafka → `order-service` (tạo đơn flash sale)

## Mô Hình Bảo Mật

```
Client → API Gateway
  Gateway xác thực JWT (do Keycloak cấp)
  Gateway forward header: X-User-Id, X-User-Roles, X-User-Email
  Các service backend TIN TƯỞNG các header này (không xác thực lại)
```

## Tech Stack

- **Java 21** + Maven Wrapper
- **Spring Boot 3.5** + Spring Cloud 2025
- **Database migration**: Flyway
- **Circuit Breaker**: Resilience4j
- **Testing**: JUnit 5 + Mockito + Testcontainers
- **Observability**: Micrometer + Prometheus + Grafana + Zipkin
