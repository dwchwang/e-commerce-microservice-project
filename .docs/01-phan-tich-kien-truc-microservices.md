# PHÂN TÍCH KIẾN TRÚC MICROSERVICES ĐÃ TRIỂN KHAI
> **Mục đích:** Tài liệu tham khảo sâu cho **Chương 4: Phân tích & Thiết kế Hệ thống** — phân tích chi tiết kiến trúc microservices đã code, kèm chỉ dẫn nguồn cụ thể cho từng luận điểm.
> **Cách dùng:** Khi viết Chương 4, lấy luận điểm + sơ đồ + nguồn từ tài liệu này; kiểm chứng lại với code thật trước khi quote.

---

## 1. NHÌN TỔNG THỂ HỆ THỐNG

### 1.1. Sơ đồ kiến trúc 4 lớp (Layered View)

```
┌──────────────────────────────────────────────────────────────────────┐
│                  CLIENT (Browser / Mobile / Postman)                  │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ HTTPS, JWT
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY  (Spring Cloud Gateway)                │
│                              :8080                                    │
│  • Routing 12 service                                                 │
│  • OAuth2 Resource Server (JWT)                                       │
│  • Redis Rate Limiter (per-user / per-IP)                             │
│  • AuthHeaderFilter — forward X-User-Id, X-User-Roles, X-User-Email   │
│  • GuestSessionFilter — sinh session ẩn danh cho cart                 │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
       ┌───────────────────────┴────────────────────────┐
       │                                                 │
       ▼                                                 ▼
┌────────────────────┐                       ┌────────────────────┐
│  Discovery (Eureka)│                       │ Config Server      │
│      :8761         │                       │     :8888 (native) │
└────────┬───────────┘                       └────────┬───────────┘
         │                                            │
         ▼                                            ▼
┌────────────────────────────────────────────────────────────────────┐
│                         13 BUSINESS SERVICES                        │
│ identity:8081  user:8082  product:8083  inventory:8084  cart:8085   │
│ order:8086     payment:8087  voucher:8088  notif:8089  review:8090  │
│ search:8091    content:8092  flash-sale:8093                        │
└────────┬─────────────────────────────┬─────────────────────────────┘
         │ JDBC                        │ Kafka pub/sub
         ▼                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                          INFRASTRUCTURE                               │
│  PostgreSQL 17 (10 DB)   Redis 8   Kafka KRaft 8.2 (no ZK)           │
│  Elasticsearch 8.18      Keycloak 26   Mailpit                       │
│  Prometheus  Grafana  Zipkin                                         │
└──────────────────────────────────────────────────────────────────────┘
```

- **Nguồn `.guide`:** [00-tong-quan.md](../.guide/00-tong-quan.md) (sơ đồ ASCII gốc)
- **Nguồn source code:** [docker-compose.yml](../docker-compose.yml), [api-gateway/](../api-gateway), [discovery-server/](../discovery-server), [config-server/](../config-server)

### 1.2. Vì sao chia thành 13 service?
| # | Service | Port | DB | Kafka? | Redis? | Bounded Context — Lý do tách |
|---|---------|------|-----|--------|--------|------------------------------|
| 1 | identity-service | 8081 | (Keycloak) | ✓ pub | — | Tách auth khỏi business logic; dùng Keycloak làm IdP |
| 2 | user-service | 8082 | user_db | ✓ sub | — | Profile khác data login (chia tách kỹ thuật–nghiệp vụ) |
| 3 | product-service | 8083 | product_db | ✓ pub | ✓ cache | CRUD nặng đọc; cần CQRS-lite với search |
| 4 | inventory-service | 8084 | inventory_db | ✓ pub/sub | — | Cần consistency cao, transaction riêng |
| 5 | cart-service | 8085 | — | — | ✓ store | Ephemeral, TTL ngắn; không cần persistence |
| 6 | order-service | 8086 | order_db | ✓ pub/sub | — | **Saga orchestrator**, trái tim business flow |
| 7 | payment-service | 8087 | payment_db | ✓ pub/sub | — | Compliance/security; tích hợp VNPAY tách biệt |
| 8 | voucher-service | 8088 | voucher_db | — | — | Logic độc lập, gọi sync qua Feign |
| 9 | notification-service | 8089 | notification_db | ✓ sub | — | Side-effect, không block main flow |
| 10 | review-service | 8090 | review_db | — | — | Đọc nhiều, ghi ít |
| 11 | search-service | 8091 | (Elasticsearch) | ✓ sub | — | **CQRS-lite read model** — scale read riêng |
| 12 | content-service | 8092 | content_db | — | — | CMS/banner — domain marketing riêng |
| 13 | flash-sale-service | 8093 | flash_sale_db | ✓ pub | ✓ atomic | **Cô lập workload high-concurrency** |

- **Nguồn `.report`:** [01-kien-truc-microservices.md §4.1](../.report/01-kien-truc-microservices.md)
- **Nguồn `.guide`:** [00-tong-quan.md](../.guide/00-tong-quan.md), [05-cac-dich-vu-va-cong.md](../.guide/05-cac-dich-vu-va-cong.md)

---

## 2. PHÂN TÍCH GIAO TIẾP GIỮA CÁC SERVICE

### 2.1. Đồng bộ — REST + OpenFeign (Smart Endpoints)
| Caller | Callee | Mục đích | Có Circuit Breaker? |
|--------|--------|----------|---------------------|
| cart-service | product-service | Lấy giá, tên sản phẩm | ✓ Resilience4j |
| order-service | product-service | Validate sản phẩm khi đặt đơn | ✓ ProductServiceFallback |
| order-service | voucher-service | Reserve voucher | ✓ VoucherServiceFallback |
| order-service | cart-service | Clear cart sau đặt đơn OK | ✓ CartServiceFallback |
| payment-service | order-service | Lấy context đơn (số tiền, email) | ✓ OrderServiceClientFallback |
| review-service | order-service | Kiểm tra user có quyền review (CONFIRMED order) | ✓ |
| flash-sale-service | order-service | Đếm số đơn user đã mua trong campaign | ✓ OrderCountClientFallback |

- **Nguồn `.report`:** [07-giao-tiep-rest-openfeign.md](../.report/07-giao-tiep-rest-openfeign.md)
- **Nguồn source code:**
  - [order-service/.../client/](../order-service/src/main/java/com/ecommerce/order/client) (4 Feign client + 4 Fallback)
  - [payment-service/.../client/OrderServiceClient.java](../payment-service/src/main/java/com/ecommerce/payment/client/OrderServiceClient.java)
  - [flash-sale-service/.../client/OrderCountClient.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/client/OrderCountClient.java)

### 2.2. Bất đồng bộ — Kafka (Dumb Pipes)

**Bản đồ 11 nhóm sự kiện / 13 topic vật lý:**

```
identity-service ──[user-registered]────────────────────► user-service

product-service ──[product-created]
                ──[product-updated]    ─────────────────► search-service (ES index)
                ──[product-deleted]

                  ┌─[order-created]──► inventory-service ──[inventory-updated]──► order-service
                  │                                       └─[inventory-failed]──► order-service
                  │
order-service ────┤─[payment-requested]────────────────► payment-service (COD path)
                  │
                  ├─[order-confirmed]──► inventory-service (CONFIRM)
                  │                    ► notification-service (email confirmed)
                  │
                  └─[order-cancelled]──► inventory-service (RELEASE)
                                       ► notification-service (email cancelled)

payment-service ──[payment-success]──► order-service (CONFIRMED)
                ──[payment-failed]───► order-service (CANCELLED)

flash-sale-service ──[flash-sale-order-requested *3p]──► order-service (tạo đơn flash)
```

- **Nguồn `.guide`:** [12-kafka-topics.md](../.guide/12-kafka-topics.md)
- **Nguồn `.report`:** [08-event-driven-kafka.md](../.report/08-event-driven-kafka.md)
- **Nguồn source code:** Schema event tại [common/.../event/](../common/src/main/java/com/ecommerce/common/event)

### 2.3. Lý do dùng Saga Orchestration thay vì Choreography
- **Choreography:** Các service tự subscribe event → khó tracing, khó compensate khi flow lỗi giữa chừng.
- **Orchestration (đã chọn):** order-service đóng vai trò orchestrator — tổng hợp logic state machine ở một nơi, dễ debug, dễ thêm bước mới.
- **Bằng chứng code:** [order-service/.../OrderServiceImpl.java](../order-service/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java) tổng hợp 5 consumer (Inventory*/Payment*/FlashSale*) và publish 4 loại event ra Kafka qua Outbox.
- **Nguồn `.report`:** [09-saga-pattern-distributed-transaction.md](../.report/09-saga-pattern-distributed-transaction.md)

---

## 3. CÁC PATTERN PHÂN TÁN ĐÃ TRIỂN KHAI

### 3.1. Saga Pattern — Order Saga

**Happy path COD:**
```
[1] POST /api/orders {items, COD}
    └─► order-service: tạo Order PENDING + INSERT outbox(order-created)
                       (cùng 1 DB transaction → atomic)
[2] OutboxPoller (1s tick) → publish Kafka order-created
[3] inventory-service: reserve → publish inventory-updated
[4] order-service: STOCK_RESERVED → publish payment-requested (COD)
[5] payment-service: tạo Payment COD COMPLETED → publish payment-success
[6] order-service: CONFIRMED → publish order-confirmed
[7] inventory-service: CONFIRM (trừ thật)
    notification-service: gửi email
```

**Compensation paths:**
- Inventory không đủ → `inventory-failed` → order CANCELLED → voucher RELEASE.
- VNPAY fail → `payment-failed` → order CANCELLED → inventory RELEASE.
- Quá 30 phút chưa thanh toán → `ReservationExpiryScheduler` → order CANCELLED.

- **Nguồn `.guide`:** [13-state-machines.md §1](../.guide/13-state-machines.md), [09-luong-nghiep-vu.md](../.guide/09-luong-nghiep-vu.md)
- **Nguồn source code:**
  - [order-service/.../service/impl/OrderServiceImpl.java](../order-service/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java)
  - [order-service/.../kafka/](../order-service/src/main/java/com/ecommerce/order/kafka) — 5 consumer
  - [order-service/.../scheduler/ReservationExpiryScheduler.java](../order-service/src/main/java/com/ecommerce/order/scheduler/ReservationExpiryScheduler.java)

### 3.2. Transactional Outbox

**Bảng `outbox` (order_db):**
```sql
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100),
    aggregate_id VARCHAR(255),
    event_type VARCHAR(100),
    payload TEXT,
    created_at TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP
);
CREATE INDEX idx_outbox_unprocessed ON outbox(processed, created_at) WHERE processed = FALSE;
```

**Bảng `payment_outbox` (payment_db) — phiên bản nâng cao** có thêm `status`, `attempts`, `last_error`, `topic` cho retry.

**Cơ chế:**
- INSERT business data + INSERT outbox **trong cùng 1 transaction** → atomic.
- `OutboxPoller` (1 giây/lần) đọc rows chưa publish → push lên Kafka → mark processed.
- Nếu service crash giữa chừng: restart → poller tiếp tục.

- **Nguồn `.report`:** [10-outbox-idempotency.md](../.report/10-outbox-idempotency.md)
- **Nguồn source code:**
  - [order-service/src/main/resources/db/migration/V1__create_order_tables.sql](../order-service/src/main/resources/db/migration/V1__create_order_tables.sql) (table `outbox`)
  - [payment-service/src/main/resources/db/migration/V1__create_payment_tables.sql](../payment-service/src/main/resources/db/migration/V1__create_payment_tables.sql) (table `payment_outbox`)
  - [order-service/.../service/OutboxService.java](../order-service/src/main/java/com/ecommerce/order/service/OutboxService.java)
  - [order-service/.../scheduler/OutboxPoller.java](../order-service/src/main/java/com/ecommerce/order/scheduler/OutboxPoller.java)
  - [payment-service/.../kafka/PaymentEventOutboxPoller.java](../payment-service/src/main/java/com/ecommerce/payment/kafka/PaymentEventOutboxPoller.java)

### 3.3. Idempotent Consumer

**Bảng `processed_events`:**
```sql
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP DEFAULT NOW()
);
```

**Cơ chế:** Trước khi xử lý event, consumer check `event_id` trong bảng. Nếu đã tồn tại → bỏ qua. Đảm bảo **exactly-once effect** dù Kafka deliver at-least-once.

- **Nguồn source code:** Mỗi service stateful đều có `ProcessedEvent` entity & repository, ví dụ:
  - [order-service/.../entity/ProcessedEvent.java](../order-service/src/main/java/com/ecommerce/order/entity/ProcessedEvent.java)
  - [order-service/.../repository/ProcessedEventRepository.java](../order-service/src/main/java/com/ecommerce/order/repository/ProcessedEventRepository.java)
  - [payment-service/.../entity/ProcessedEvent.java](../payment-service/src/main/java/com/ecommerce/payment/entity/ProcessedEvent.java)
  - Logic kiểm tra trong [order-service/.../kafka/SagaSupport.java](../order-service/src/main/java/com/ecommerce/order/kafka/SagaSupport.java)

### 3.4. Circuit Breaker + Fallback (Resilience4j)
- Mỗi Feign client có 1 fallback class. Khi callee down → fallback trả default response thay vì cascading failure.
- **Nguồn source code:**
  - [order-service/.../client/ProductServiceFallback.java](../order-service/src/main/java/com/ecommerce/order/client/ProductServiceFallback.java)
  - [order-service/.../client/CartServiceFallback.java](../order-service/src/main/java/com/ecommerce/order/client/CartServiceFallback.java)
  - [order-service/.../client/VoucherServiceFallback.java](../order-service/src/main/java/com/ecommerce/order/client/VoucherServiceFallback.java)

### 3.5. Rate Limiting (per-user, per-IP)
**Tại Spring Cloud Gateway** — `RedisRateLimiter` (token bucket):
| Route | replenish/s | burst | Key |
|-------|-------------|-------|-----|
| identity-service | 10 | 20 | IP |
| order-service | 10 | 20 | userId |
| **flash-sale-purchase** | **3** | **5** | userId (chặt nhất) |

- **Nguồn source code:** [api-gateway/src/main/resources/application.yml](../api-gateway/src/main/resources/application.yml), [api-gateway/.../config/RateLimiterConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/RateLimiterConfig.java)

### 3.6. CQRS-lite (Search service)
- product-service = **Command side** (write).
- search-service = **Query side** (read), Elasticsearch.
- Sync qua Kafka (`product-created/updated/deleted` topics) → eventual consistency.
- **Nguồn `.report`:** [16-elasticsearch-fulltext-search.md](../.report/16-elasticsearch-fulltext-search.md)
- **Nguồn source code:** [search-service/](../search-service)

---

## 4. PHÂN TÍCH BÀI TOÁN HIGH-CONCURRENCY (FLASH SALE)

### 4.1. Vấn đề over-sell
Khi 1000 user cùng mua 100 sản phẩm trong 1 giây, nếu dùng `SELECT FOR UPDATE` trên DB → throughput thấp + nguy cơ deadlock.

### 4.2. Giải pháp đã code: Redis Lua Atomic Script

**RESERVE_SCRIPT** (trích [FlashSaleServiceImpl.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/service/impl/FlashSaleServiceImpl.java)):
```lua
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
  return -2     -- user đã mua rồi
end
local stock = redis.call('GET', KEYS[1])
if not stock then return -3 end
stock = tonumber(stock)
if stock <= 0 then return -1 end   -- hết hàng
local remaining = redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
redis.call('EXPIRE', KEYS[2], ARGV[2])
return remaining
```

**Đặc tính atomic:** Lua script chạy single-threaded trong Redis → 4 lệnh (SISMEMBER, GET, DECR, SADD) là **atomic block**, không có race condition.

**COMPENSATE_SCRIPT** (khi tạo đơn fail): INCR stock + SREM user khỏi set.

### 4.3. Đường dẫn full
```
POST /api/flash-sales/{id}/purchase
   │
   ├─► RESERVE_SCRIPT atomic ──► Redis stock−1, user vào set
   │
   ├─► publish flash-sale-order-requested (3 partitions, key=userId)
   │   │
   │   └─► order-service consume ──► tạo Order với
   │       UNIQUE constraint (flash_sale_id, user_id) [V2 migration]
   │
   ├─► CampaignScheduler (5s) chuyển trạng thái SCHEDULED↔ACTIVE↔ENDED
   └─► ReconciliationScheduler đối soát Redis stock vs DB sold_count
```

- **Nguồn `.report`:** [13-flash-sale-concurrency.md](../.report/13-flash-sale-concurrency.md)
- **Nguồn `.guide`:** [13-state-machines.md §3 + §4](../.guide/13-state-machines.md)
- **Nguồn source code:**
  - [flash-sale-service/.../service/impl/FlashSaleServiceImpl.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/service/impl/FlashSaleServiceImpl.java) (Lua scripts)
  - [flash-sale-service/.../scheduler/CampaignScheduler.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/scheduler/CampaignScheduler.java)
  - [flash-sale-service/.../scheduler/ReconciliationScheduler.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/scheduler/ReconciliationScheduler.java)
  - [order-service/.../kafka/FlashSaleOrderConsumer.java](../order-service/src/main/java/com/ecommerce/order/kafka/FlashSaleOrderConsumer.java)
  - [order-service/src/main/resources/db/migration/V2__add_flash_sale_order_uniqueness.sql](../order-service/src/main/resources/db/migration/V2__add_flash_sale_order_uniqueness.sql)
  - DB constraint: [flash-sale-service/src/main/resources/db/migration/V1__create_flash_sale_tables.sql](../flash-sale-service/src/main/resources/db/migration/V1__create_flash_sale_tables.sql) (`CHECK (sold_count <= quantity)`)

### 4.4. Tại sao có 3 partitions cho topic `flash-sale-order-requested`?
- **Hot topic** — toàn bộ user mua flash sale đều push vào đây.
- 3 partitions → 3 consumer instance order-service có thể consume song song.
- Key = `userId` → đảm bảo cùng user luôn vào cùng partition (preserve ordering theo user).
- **Nguồn `.guide`:** [12-kafka-topics.md §11](../.guide/12-kafka-topics.md)

---

## 5. PHÂN TÍCH STATE MACHINES TRONG HỆ THỐNG

| Domain | Số state | Trigger transition | Compensation |
|--------|----------|---------------------|--------------|
| **Order** | 4 (PENDING, STOCK_RESERVED, CONFIRMED, CANCELLED) | Kafka events + Scheduler | inventory RELEASE, voucher RELEASE |
| **Payment** | 4 (PENDING, COMPLETED, FAILED, TIMEOUT) | VNPAY callback / PaymentTimeoutScheduler | publish `payment-failed` |
| **Campaign (Flash sale)** | 4 (SCHEDULED, ACTIVE, ENDED, CANCELLED) | CampaignScheduler (5s) / Admin API | Redis seedStock / cleanup |
| **Inventory** | Dual-counter (`quantity`, `reserved_quantity`) | Order events | reserved_quantity++/-- |

- **Nguồn `.guide`:** [13-state-machines.md](../.guide/13-state-machines.md)
- **Nguồn `.report`:** [12-state-machine-scheduler.md](../.report/12-state-machine-scheduler.md)

---

## 6. PHÂN TÍCH BẢO MẬT

### 6.1. Trusted Subsystem Pattern
```
Client ─[Authorization: Bearer JWT]─► API Gateway
                                        │
                                        │ 1. Validate JWT (JWKS từ Keycloak)
                                        │ 2. Extract claims → header
                                        │
                                        ├──► AuthHeaderFilter
                                        │     X-User-Id, X-User-Roles, X-User-Email
                                        ▼
                                  Backend service (TIN TƯỞNG header)
```

**Lý do:** Backend không gọi lại Keycloak mỗi request → giảm latency, scale tốt.
**Rủi ro:** Backend bị truy cập trực tiếp (bypass Gateway) sẽ trust header sai. → Phải đảm bảo network isolation (Docker network internal, không expose port backend ra ngoài).

- **Nguồn `.report`:** [06-bao-mat-keycloak-oauth-jwt.md](../.report/06-bao-mat-keycloak-oauth-jwt.md)
- **Nguồn `.guide`:** [15-gateway-security.md](../.guide/15-gateway-security.md)
- **Nguồn source code:**
  - [api-gateway/.../config/SecurityConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java)
  - [api-gateway/.../filter/AuthHeaderFilter.java](../api-gateway/src/main/java/com/ecommerce/gateway/filter/AuthHeaderFilter.java)

### 6.2. Bảo mật VNPAY (HMAC-SHA512)
- Payment-service ký request bằng `vnp_HashSecret` (HMAC-SHA512 toàn bộ params đã sort).
- Khi VNPAY callback (Return URL + IPN), verify lại signature → reject nếu sai.
- **Nguồn `.report`:** [19-vnpay-payment-gateway.md](../.report/19-vnpay-payment-gateway.md)
- **Nguồn source code:** [payment-service/.../util/VnPayUtil.java](../payment-service/src/main/java/com/ecommerce/payment/util/VnPayUtil.java), [payment-service/.../service/impl/VnPayServiceImpl.java](../payment-service/src/main/java/com/ecommerce/payment/service/impl/VnPayServiceImpl.java)

---

## 7. PHÂN TÍCH OBSERVABILITY (3 trụ cột)

| Trụ cột | Stack | Endpoint | Bằng chứng đã code |
|---------|-------|----------|--------------------|
| **Metrics** | Micrometer + Prometheus + Grafana | `/actuator/prometheus` | [prometheus/](../prometheus), [grafana/](../grafana) |
| **Logs** | SLF4J + Logback (structured) + Mailpit (xem email) | container stdout | `application.yml` mỗi service |
| **Traces** | Micrometer Tracing + Zipkin Brave (W3C Trace Context) | Zipkin :9411 | Auto-instrumented qua Spring Boot 3.5 |

- **Nguồn `.report`:** [17-observability-prometheus-grafana.md](../.report/17-observability-prometheus-grafana.md), [18-distributed-tracing-zipkin.md](../.report/18-distributed-tracing-zipkin.md)
- **Nguồn `.guide`:** [07-monitoring-observability.md](../.guide/07-monitoring-observability.md)

---

## 8. BẢNG TỔNG HỢP "PATTERN ↔ NGUỒN GỐC ↔ FILE CODE"

| Pattern | Nguồn lý thuyết | Tài liệu trong repo | File code minh chứng |
|---------|-----------------|---------------------|----------------------|
| Microservices | Newman, *Building Microservices* (2021) | `.report/01` | Toàn bộ 13 module Maven |
| Bounded Context (DDD) | Evans, *DDD* (2003) | `.report/01 §2.6` | 13 service ↔ 13 bounded context |
| API Gateway | Richardson, microservices.io | `.report/05` | [api-gateway/](../api-gateway) |
| Service Discovery | Netflix Eureka docs | `.report/03` | [discovery-server/](../discovery-server) |
| Centralized Config | Spring Cloud Config docs | `.report/04` | [config-server/](../config-server) |
| OAuth 2.0 / JWT | RFC 6749, RFC 7519 | `.report/06` | [api-gateway/.../SecurityConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java) |
| Saga Pattern | Garcia-Molina & Salem, *Sagas* (1987); Richardson 2018 | `.report/09` | [order-service/.../OrderServiceImpl.java](../order-service/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java) |
| Transactional Outbox | Richardson, microservices.io | `.report/10` | bảng `outbox`, [OutboxPoller.java](../order-service/src/main/java/com/ecommerce/order/scheduler/OutboxPoller.java) |
| Idempotent Consumer | Hohpe & Woolf, *Enterprise Integration Patterns* (2003) | `.report/10` | bảng `processed_events`, [SagaSupport.java](../order-service/src/main/java/com/ecommerce/order/kafka/SagaSupport.java) |
| Circuit Breaker | Nygard, *Release It!* (2007) | `.report/11` | [*Fallback.java](../order-service/src/main/java/com/ecommerce/order/client) |
| Rate Limiting | Token Bucket algorithm | `.report/11` | [api-gateway/.../application.yml](../api-gateway/src/main/resources/application.yml) |
| Database per Service | Richardson | `.report/14` | 10 database tách biệt — [init-db/](../init-db) |
| Polyglot Persistence | Fowler 2011 | `.report/14, 15, 16` | PostgreSQL + Redis + Elasticsearch |
| Distributed Lock / Atomic Counter | Redlock paper (Salvatore Sanfilippo) | `.report/13, 15` | [FlashSaleServiceImpl.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/service/impl/FlashSaleServiceImpl.java) |
| CQRS-lite | Greg Young, "CQRS Documents" | `.report/16` | [search-service/](../search-service) |
| State Machine | Hopcroft, *Automata Theory* | `.report/12` | [OrderStatus.java](../order-service/src/main/java/com/ecommerce/order/entity/OrderStatus.java), [PaymentStatus.java](../payment-service/src/main/java/com/ecommerce/payment/entity/PaymentStatus.java), [CampaignStatus.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/entity/CampaignStatus.java) |
| W3C Trace Context | W3C Recommendation 2021 | `.report/18` | Auto, qua Micrometer Tracing |
| HMAC-SHA512 (VNPAY) | NIST FIPS 180-4 | `.report/19` | [VnPayUtil.java](../payment-service/src/main/java/com/ecommerce/payment/util/VnPayUtil.java) |
| Trusted Subsystem | Microsoft Patterns & Practices | `.report/06` | [AuthHeaderFilter.java](../api-gateway/src/main/java/com/ecommerce/gateway/filter/AuthHeaderFilter.java) |

---

## 9. CÁC ĐIỂM ĐẶC BIỆT CỦA HỆ THỐNG NÀY (so với reference architecture sách giáo khoa)

1. **Saga Orchestration thuần** — không dùng Camunda/Saga framework, tự implement bằng Kafka + state field trong DB (đơn giản hóa cho đồ án).
2. **2 phiên bản Outbox** — order-service phiên bản đơn giản (chỉ `processed boolean`), payment-service phiên bản có retry (`status`, `attempts`, `last_error`) → cho thấy quá trình tiến hóa kiến trúc.
3. **Reservation Pattern dual** — đơn VNPAY có timeout 30 phút (có `reservation_expired_at`), đơn COD không timeout. Logic này được thực thi qua `ReservationExpiryScheduler` filter theo `payment_method` & `reservation_expired_at`.
4. **Topic riêng cho flash sale** — duy nhất `flash-sale-order-requested` được khai báo rõ ràng với 3 partitions; các topic khác auto-create với 1 partition.
5. **Compensation cuộn ngược** — khi tạo flash sale order fail, COMPENSATE_SCRIPT vừa INCR stock vừa SREM khỏi set duplicate-buyer.
6. **Unique partial index** — `payments(order_id) WHERE status='PENDING'` chống tạo nhiều payment pending cho cùng đơn. Đây là cách dùng PostgreSQL feature thay vì application-level lock.

---

## 10. KHUYẾN NGHỊ KHI TRÌNH BÀY CHƯƠNG 4 BÁO CÁO

1. **Mở đầu:** Bắt đầu bằng sơ đồ tổng thể (mục 1.1) — gây ấn tượng trực quan.
2. **Trình bày từ Domain → Architecture:** Giải thích bounded context trước (mục 1.2), sau đó mới đến giao tiếp (mục 2).
3. **Đi sâu vào 3 pattern "ăn điểm":** Saga (mục 3.1), Outbox (mục 3.2), Flash Sale concurrency (mục 4) — mỗi pattern 2–3 trang với sequence diagram.
4. **Kết thúc:** Bảng tổng hợp Pattern ↔ Nguồn ↔ Code (mục 8) — cho hội đồng thấy nền tảng học thuật.

---

## 11. CÁC SƠ ĐỒ BẮT BUỘC PHẢI VẼ (chuyển ASCII → PlantUML/Mermaid/draw.io)

| # | Sơ đồ | Loại | Nguồn ASCII gốc |
|---|------|------|-----------------|
| 1 | Component Diagram tổng thể (4 lớp) | Component | mục 1.1 + [.guide/00-tong-quan.md](../.guide/00-tong-quan.md) |
| 2 | Use Case Diagram (3 actor) | Use Case | tự vẽ, dựa [.guide/09-luong-nghiep-vu.md](../.guide/09-luong-nghiep-vu.md) |
| 3 | Sơ đồ Kafka topics | Component/Flow | mục 2.2 + [.guide/12-kafka-topics.md](../.guide/12-kafka-topics.md) |
| 4 | Sequence — Đăng ký user | Sequence | tự vẽ |
| 5 | Sequence — Đặt đơn COD (Saga) | Sequence | mục 3.1 |
| 6 | Sequence — Đặt đơn VNPAY + IPN | Sequence | [.guide/13-state-machines.md §1](../.guide/13-state-machines.md) |
| 7 | Sequence — Hủy đơn (3 nguyên nhân) | Sequence | tự vẽ |
| 8 | Sequence — Flash Sale Purchase | Sequence | mục 4.3 |
| 9 | Sequence — CQRS Search | Sequence | tự vẽ |
| 10 | State Machine — Order | State | [.guide/13-state-machines.md §1](../.guide/13-state-machines.md) |
| 11 | State Machine — Payment | State | [.guide/13-state-machines.md §2](../.guide/13-state-machines.md) |
| 12 | State Machine — Campaign | State | [.guide/13-state-machines.md §3](../.guide/13-state-machines.md) |
| 13 | ERD × 10 service | ERD | từ Flyway migration files |
| 14 | Class Diagram order-service (Saga orchestrator) | Class | tự vẽ từ [order-service/](../order-service) |
| 15 | Deployment Diagram (Docker Compose network) | Deployment | từ [docker-compose.yml](../docker-compose.yml) |

> **Tools đề xuất:** PlantUML (text-based, version control), Mermaid (markdown native), draw.io (drag-drop, đẹp cho deployment diagram).
