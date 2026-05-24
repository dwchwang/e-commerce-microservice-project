# .tech — Hướng dẫn sử dụng các công nghệ trong hệ thống

Tài liệu này tập trung vào **cách dùng** (operate + observe + debug) các công nghệ đã tích hợp vào hệ thống e-commerce microservice. Không lặp lại lý thuyết chung — mỗi file đều bám vào cấu hình thực tế trong repo (file path, port, env, route, query mẫu).

## Cách đọc

- Đọc theo thứ tự đánh số nếu mới tiếp cận hệ thống.
- Mỗi file gồm 4 phần: **Khái niệm cốt lõi → Cách hệ thống đang dùng → Workflow vận hành → Troubleshooting**.
- Mọi lệnh đều giả định bạn đang ở thư mục root của project và đã chạy `docker compose up -d`.

## Mục lục

| # | File | Chủ đề | Khi nào cần |
|---|------|--------|-------------|
| 01 | [01-keycloak.md](01-keycloak.md) | Keycloak — Identity Provider, OAuth2/OIDC, JWT | Login, phân quyền, role, token |
| 02 | [02-prometheus.md](02-prometheus.md) | Prometheus — Metrics scraping & PromQL | Đo throughput, latency, error rate |
| 03 | [03-grafana.md](03-grafana.md) | Grafana — Dashboard & Visualization | Quan sát hệ thống, tạo dashboard mới |
| 04 | [04-zipkin.md](04-zipkin.md) | Zipkin — Distributed Tracing | Trace 1 request đi qua nhiều service |
| 05 | [05-spring-cloud.md](05-spring-cloud.md) | Eureka + Config Server + API Gateway | Service discovery, central config, routing |
| 06 | [06-kafka.md](06-kafka.md) | Kafka KRaft + Saga + Outbox | Event-driven, đơn hàng, inventory |
| 07 | [07-redis.md](07-redis.md) | Redis — Cache, Cart, Rate Limit, Flash Sale | Tăng tốc, chống spam, race condition |
| 08 | [08-elasticsearch.md](08-elasticsearch.md) | Elasticsearch — Full-text search | Tìm kiếm sản phẩm |

## Kiến trúc tổng quan

```
                        ┌─────────────┐
                        │   Client    │
                        └──────┬──────┘
                               │  JWT (Bearer)
                               ▼
   ┌───────────────────────────────────────────────┐
   │  API Gateway :8080  (Spring Cloud Gateway)    │
   │   - Validate JWT (Keycloak JWKS)              │
   │   - Forward X-User-Id / X-User-Roles          │
   │   - Redis Rate Limiter                        │
   └─────┬─────────────────────────────────────────┘
         │  lb://service-name  (Eureka)
         ▼
   ┌─────────────────────────────────────────────────────────┐
   │  13 Business Services (Spring Boot 3.5)                 │
   │  identity / user / product / inventory / cart / order   │
   │  payment / voucher / notification / review / search     │
   │  content / flash-sale                                   │
   └─────┬─────────────┬───────────────┬─────────────────────┘
         │             │               │
         ▼             ▼               ▼
   ┌──────────┐  ┌──────────┐   ┌──────────────┐
   │PostgreSQL│  │  Redis   │   │ Elasticsearch│
   │   :5432  │  │  :6379   │   │    :9200     │
   └──────────┘  └──────────┘   └──────────────┘
                       │
                       ▼
              ┌────────────────┐         ┌──────────────┐
              │  Kafka :9092   │◄───────►│   Keycloak   │
              │     (KRaft)    │         │    :8180     │
              └────────────────┘         └──────────────┘

   ┌───────────────  Observability  ───────────────┐
   │  Prometheus :9090 ──► scrapes /actuator/prometheus  │
   │  Grafana    :3000 ──► reads Prometheus              │
   │  Zipkin     :9411 ──► receives spans (B3 propagation) │
   └──────────────────────────────────────────────────────┘
```

## Bảng URL nhanh

| Service | URL | Credential |
|---|---|---|
| API Gateway | http://localhost:8080 | JWT trong header `Authorization` |
| Eureka | http://localhost:8761 | `eureka` / `eureka` |
| Config Server | http://localhost:8888 | — |
| Keycloak Admin | http://localhost:8180 | `admin` / `admin` (xem `.env`) |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | `admin` / `${GF_SECURITY_ADMIN_PASSWORD}` |
| Zipkin | http://localhost:9411 | — |
| Mailpit | http://localhost:8025 | — |
| Elasticsearch | http://localhost:9200 | — |
| Swagger Aggregator | http://localhost:8080/swagger-ui.html | — |

## Bộ công cụ debug khuyên dùng

```bash
# Quan sát toàn cảnh
docker compose ps
docker compose logs -f --tail=100 api-gateway order-service

# Kiểm tra trạng thái Eureka
curl -u eureka:eureka http://localhost:8761/eureka/apps | xmllint --format -

# Lấy 1 access token nhanh (dùng cho gọi API thử)
curl -s -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=ecommerce-client" \
  -d "client_secret=local-dev-ecommerce-secret" \
  -d "username=test@example.com" \
  -d "password=Password123!" | jq -r .access_token

# Quan sát metrics 1 service
curl -s http://localhost:8080/actuator/prometheus | grep http_server_requests_seconds_count
```
