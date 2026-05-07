# ĐỀ CƯƠNG CHI TIẾT BÁO CÁO ĐỒ ÁN TỐT NGHIỆP

> **Đề tài:** Xây dựng hệ thống Thương mại điện tử theo kiến trúc Microservices với Spring Boot 3.5 / Spring Cloud 2025 / Java 21
> **Sinh viên:** Nguyễn Đức Bảo Hoàng
> **Ngày soạn đề cương:** 2026-05-08
> **Tổng dung lượng dự kiến:** ~120–140 trang (chính văn) + phụ lục

---

## A. CÁCH ĐỌC TÀI LIỆU NÀY

Mỗi mục trong đề cương được đánh dấu bằng 3 trường:
- **Nội dung cần viết:** Liệt kê các luận điểm bắt buộc.
- **Nguồn `.report`:** File lý thuyết tham chiếu trong `.report/` (dùng cho phần định nghĩa, lý thuyết, trích dẫn hàn lâm).
- **Nguồn `.guide`:** File hướng dẫn vận hành trong `.guide/` (dùng cho phần thiết kế, cấu hình, screenshot demo).
- **Nguồn source code:** Đường dẫn cụ thể trong repo dùng để minh chứng đã triển khai.

> **Quy ước số trang:** Số trang chỉ là mục tiêu; có thể co giãn ±20% mà vẫn đảm bảo bố cục.

---

## B. CẤU TRÚC TỔNG THỂ

```
LỜI CẢM ƠN                                                       (1 tr)
LỜI CAM ĐOAN                                                     (1 tr)
TÓM TẮT (Tiếng Việt + Tiếng Anh)                                 (2 tr)
MỤC LỤC                                                          (3 tr)
DANH MỤC HÌNH VẼ / BẢNG BIỂU / TỪ VIẾT TẮT                      (3 tr)

CHƯƠNG 1: TỔNG QUAN ĐỀ TÀI                                  (10–15 tr)
CHƯƠNG 2: CƠ SỞ LÝ THUYẾT                                  (30–40 tr)
CHƯƠNG 3: PHÂN TÍCH & THIẾT KẾ HỆ THỐNG                    (30–40 tr)
CHƯƠNG 4: CÀI ĐẶT & TRIỂN KHAI                             (25–30 tr)
CHƯƠNG 5: KIỂM THỬ & ĐÁNH GIÁ                              (15–20 tr)

KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN                                  (3 tr)
TÀI LIỆU THAM KHẢO                                            (3–5 tr)
PHỤ LỤC                                                       (10–20 tr)
```

---

# CHƯƠNG 1 — TỔNG QUAN ĐỀ TÀI (10–15 trang)

## 1.1. Đặt vấn đề
- **Nội dung cần viết:**
  - Bối cảnh thương mại điện tử Việt Nam (Tiki, Shopee, Lazada) và sự kiện cao tải (Black Friday, 11/11, sinh nhật sàn).
  - Hạn chế của kiến trúc Monolith truyền thống: scale toàn bộ, deploy chung, fault không cô lập.
  - Câu hỏi nghiên cứu: *Làm thế nào xây dựng một hệ thống e-commerce có khả năng scale, resilient, hỗ trợ flash sale chống over-sell?*
- **Nguồn `.report`:** [01-kien-truc-microservices.md §2.4–2.5](../.report/01-kien-truc-microservices.md), [00-LOTRINH-NGHIEN-CUU.md §1](../.report/00-LOTRINH-NGHIEN-CUU.md)
- **Nguồn `.guide`:** Không trực tiếp.
- **Nguồn source code:** [README.md](../README.md)

## 1.2. Mục tiêu đề tài
- **Nội dung cần viết:**
  - Mục tiêu tổng quát: triển khai một hệ thống e-commerce theo kiến trúc microservices.
  - Mục tiêu cụ thể (5 mục):
    1. Tách 13 bounded context thành 13 microservice độc lập.
    2. Áp dụng các pattern phân tán: Saga (orchestration), Transactional Outbox, Idempotent Consumer, CQRS-lite cho search.
    3. Tích hợp Keycloak (OAuth 2.0/OIDC + JWT) qua API Gateway.
    4. Tích hợp thanh toán VNPAY sandbox (HMAC-SHA512 + IPN).
    5. Tích hợp Observability stack (Prometheus + Grafana + Zipkin) và minh chứng trên trace thực tế.
- **Nguồn `.report`:** [00-LOTRINH-NGHIEN-CUU.md](../.report/00-LOTRINH-NGHIEN-CUU.md), [22-cau-truc-bao-cao-datn.md §3 Chương 1](../.report/22-cau-truc-bao-cao-datn.md)
- **Nguồn `.guide`:** [00-tong-quan.md](../.guide/00-tong-quan.md)
- **Nguồn source code:** [README.md](../README.md), [docker-compose.yml](../docker-compose.yml)

## 1.3. Phạm vi đề tài
- **Nội dung cần viết:**
  - **In-scope:** 13 business service, 3 hạ tầng Spring Cloud, 9 hệ thống hạ tầng (PostgreSQL, Redis, Kafka KRaft, Elasticsearch, Keycloak, Mailpit, Zipkin, Prometheus, Grafana), triển khai Docker Compose.
  - **Out-of-scope:** Frontend Web/Mobile hoàn chỉnh, Kubernetes, payment production, third-party logistics, refund production.
- **Nguồn `.report`:** [22-cau-truc-bao-cao-datn.md §3 Chương 1.3](../.report/22-cau-truc-bao-cao-datn.md)
- **Nguồn `.guide`:** [00-tong-quan.md](../.guide/00-tong-quan.md), [05-cac-dich-vu-va-cong.md](../.guide/05-cac-dich-vu-va-cong.md)

## 1.4. Phương pháp nghiên cứu
- **Nội dung cần viết:**
  - Nghiên cứu tài liệu hàn lâm (Sam Newman, Chris Richardson, Eric Evans, Martin Fowler).
  - Phân tích kiến trúc tham khảo (Tiki Engineering blog, Shopee Tech Blog, Amazon Builder's Library).
  - Implement & test thực tế (Testcontainers, Postman, k6 — nếu đã chạy).
- **Nguồn `.report`:** [01-kien-truc-microservices.md §7](../.report/01-kien-truc-microservices.md), [22-cau-truc-bao-cao-datn.md §1](../.report/22-cau-truc-bao-cao-datn.md)

## 1.5. Khảo sát hệ thống tương tự
- **Nội dung cần viết:**
  - Tiki, Shopee, Lazada (kiến trúc microservice public).
  - Open source tham khảo: Spree, Saleor, Reactive Commerce, microservices.io reference architecture.
- **Nguồn `.report`:** [01-kien-truc-microservices.md §3](../.report/01-kien-truc-microservices.md)

## 1.6. Đóng góp của đề tài
- **Nội dung cần viết:** Reference architecture cho e-commerce VN nhỏ-trung; áp dụng đầy đủ Saga + Outbox + Idempotency; demo flash sale chống over-sell với Redis Lua atomic.
- **Nguồn:** Tự viết, dựa trên tổng kết Chương 4 + 5.

## 1.7. Bố cục báo cáo
- Tóm tắt 5 chương + Kết luận.

---

# CHƯƠNG 2 — CƠ SỞ LÝ THUYẾT (30–40 trang)

> **Đây là chương "ăn điểm" lý thuyết.** Mỗi mục cần ít nhất 1 sơ đồ minh họa và trích dẫn nguồn.

## 2.1. Kiến trúc Microservices
- **Nội dung cần viết:**
  - 2.1.1. Định nghĩa Microservice (Martin Fowler 2014, Sam Newman 2021).
  - 2.1.2. 5 đặc trưng cốt lõi của Sam Newman (modeled around domain, owns its data, independently deployable, decentralized, organizational alignment).
  - 2.1.3. So sánh Monolith — SOA — Microservices (bảng 6 tiêu chí).
  - 2.1.4. Lợi ích & hệ lụy. **Bắt buộc** trình bày *8 Fallacies of Distributed Computing* (Peter Deutsch).
  - 2.1.5. Domain-Driven Design: Bounded Context, Ubiquitous Language, Aggregate.
- **Nguồn `.report`:** [01-kien-truc-microservices.md](../.report/01-kien-truc-microservices.md) (toàn bộ §2)

## 2.2. Hệ sinh thái Spring Boot & Spring Cloud
- **Nội dung cần viết:**
  - 2.2.1. Spring Boot: auto-configuration, Starter, Actuator.
  - 2.2.2. Spring Cloud 2025: Eureka, Config, Gateway, OpenFeign, Resilience4j.
  - 2.2.3. So sánh với phiên bản trước (lý do dùng Spring Boot 3.5 + Java 21 + Jakarta EE).
- **Nguồn `.report`:** [02-spring-boot-spring-cloud.md](../.report/02-spring-boot-spring-cloud.md)

## 2.3. Service Discovery & Centralized Configuration
- **Nội dung cần viết:**
  - 2.3.1. Vấn đề service-to-service trong môi trường động.
  - 2.3.2. Client-side discovery (Eureka) vs server-side discovery (Kubernetes Service).
  - 2.3.3. Spring Cloud Config (native vs Git profile).
- **Nguồn `.report`:** [03-service-discovery-eureka.md](../.report/03-service-discovery-eureka.md), [04-config-server.md](../.report/04-config-server.md)

## 2.4. API Gateway Pattern
- **Nội dung cần viết:**
  - 2.4.1. Vai trò: Single entry point — Routing, Auth, Rate Limit, CORS, Aggregation.
  - 2.4.2. Spring Cloud Gateway (WebFlux, Reactor, predicate/filter chain).
  - 2.4.3. So sánh Zuul 1 (servlet) vs Gateway (reactive).
- **Nguồn `.report`:** [05-api-gateway.md](../.report/05-api-gateway.md)

## 2.5. Bảo mật trong hệ phân tán
- **Nội dung cần viết:**
  - 2.5.1. OAuth 2.0 (RFC 6749), Client Credentials, Authorization Code + PKCE.
  - 2.5.2. OpenID Connect: ID token vs Access token.
  - 2.5.3. JWT cấu trúc Header.Payload.Signature; Asymmetric (RS256) vs Symmetric (HS256).
  - 2.5.4. Keycloak: Realm, Client, Role, User Federation.
  - 2.5.5. Trusted Subsystem Pattern — Gateway xác thực, backend tin tưởng header.
- **Nguồn `.report`:** [06-bao-mat-keycloak-oauth-jwt.md](../.report/06-bao-mat-keycloak-oauth-jwt.md)

## 2.6. Giao tiếp giữa microservices
- **Nội dung cần viết:**
  - 2.6.1. Synchronous vs Asynchronous trade-off.
  - 2.6.2. REST + OpenFeign declarative client.
  - 2.6.3. Apache Kafka — Topic, Partition, Consumer Group, Offset, KRaft mode (loại bỏ Zookeeper).
  - 2.6.4. Delivery semantics: at-most-once / at-least-once / exactly-once.
- **Nguồn `.report`:** [07-giao-tiep-rest-openfeign.md](../.report/07-giao-tiep-rest-openfeign.md), [08-event-driven-kafka.md](../.report/08-event-driven-kafka.md)

## 2.7. Distributed Transaction Patterns
- **Nội dung cần viết:**
  - 2.7.1. Bài toán transaction xuyên DB.
  - 2.7.2. 2PC/XA và lý do không scale.
  - 2.7.3. CAP Theorem (Brewer 2000) + PACELC (Abadi 2010).
  - 2.7.4. Saga Pattern: Choreography vs Orchestration.
  - 2.7.5. Compensating Action (Pat Helland — *Life beyond Distributed Transactions*).
- **Nguồn `.report`:** [09-saga-pattern-distributed-transaction.md](../.report/09-saga-pattern-distributed-transaction.md)

## 2.8. Reliability Patterns
- **Nội dung cần viết:**
  - 2.8.1. Transactional Outbox (Chris Richardson).
  - 2.8.2. Idempotent Consumer (UUID dedup, processed_events table).
  - 2.8.3. Circuit Breaker (Michael Nygard — *Release It!*), state CLOSED → OPEN → HALF_OPEN.
  - 2.8.4. Retry, Bulkhead, Rate Limiter (Resilience4j).
- **Nguồn `.report`:** [10-outbox-idempotency.md](../.report/10-outbox-idempotency.md), [11-resilience-circuit-breaker-rate-limit.md](../.report/11-resilience-circuit-breaker-rate-limit.md)

## 2.9. Persistence Patterns
- **Nội dung cần viết:**
  - 2.9.1. Database per Service & Polyglot Persistence.
  - 2.9.2. Flyway migration, version-control schema.
  - 2.9.3. Redis: cache, atomic counter (DECR/INCR), distributed lock (Redlock), Lua script.
  - 2.9.4. Elasticsearch: inverted index, BM25, CQRS-lite.
- **Nguồn `.report`:** [14-database-per-service-flyway.md](../.report/14-database-per-service-flyway.md), [15-redis-cache-distributed-lock.md](../.report/15-redis-cache-distributed-lock.md), [16-elasticsearch-fulltext-search.md](../.report/16-elasticsearch-fulltext-search.md)

## 2.10. Observability — 3 trụ cột
- **Nội dung cần viết:**
  - 2.10.1. Metrics: Micrometer + Prometheus pull model + PromQL.
  - 2.10.2. Logs: structured logging, correlation ID.
  - 2.10.3. Traces: W3C Trace Context, Span/Trace, Zipkin v2 vs OpenTelemetry.
- **Nguồn `.report`:** [17-observability-prometheus-grafana.md](../.report/17-observability-prometheus-grafana.md), [18-distributed-tracing-zipkin.md](../.report/18-distributed-tracing-zipkin.md)

## 2.11. State Machine & High Concurrency
- **Nội dung cần viết:**
  - 2.11.1. Finite State Machine trong domain modeling.
  - 2.11.2. Race condition & cơ chế chống over-sell.
  - 2.11.3. Atomic counter (Redis DECR), Distributed lock (Redlock), Lua script atomicity.
  - 2.11.4. Reconciliation pattern (DB vs Redis dual source).
- **Nguồn `.report`:** [12-state-machine-scheduler.md](../.report/12-state-machine-scheduler.md), [13-flash-sale-concurrency.md](../.report/13-flash-sale-concurrency.md)

---

# CHƯƠNG 3 — PHÂN TÍCH & THIẾT KẾ HỆ THỐNG (30–40 trang)

> **Đây là chương phải phân tích sâu kiến trúc đã code.**

## 3.1. Phân tích yêu cầu

### 3.1.1. Yêu cầu chức năng
- **Nội dung cần viết:**
  - **Khách hàng (User):**
    - Đăng ký, đăng nhập (qua Keycloak), quản lý profile.
    - Duyệt sản phẩm, xem chi tiết, tìm kiếm full-text.
    - Quản lý giỏ hàng (Redis-backed), áp voucher.
    - Đặt hàng (COD hoặc VNPAY), theo dõi đơn.
    - Mua hàng flash sale.
    - Đánh giá sản phẩm sau khi đơn hàng được CONFIRMED.
  - **Quản trị viên (Admin):**
    - CRUD danh mục, sản phẩm, voucher, banner/content.
    - Tạo & quản lý chiến dịch flash sale.
  - **Hệ thống:**
    - Auto trừ tồn kho, gửi email xác nhận, sinh URL VNPAY.
- **Nguồn `.guide`:** [09-luong-nghiep-vu.md](../.guide/09-luong-nghiep-vu.md)
- **Nguồn source code:** Tổng hợp từ 13 controller — ví dụ [order-service/.../OrderController.java](../order-service/src/main/java/com/ecommerce/order/controller/OrderController.java), [flash-sale-service/.../FlashSaleController.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/controller/FlashSaleController.java)

### 3.1.2. Yêu cầu phi chức năng
- **Nội dung cần viết:** Bảo mật API (JWT + role), eventual consistency cho order saga, không over-sell flash sale, observable qua metrics & tracing, khả năng phục hồi khi service down.
- **Nguồn `.report`:** [22-cau-truc-bao-cao-datn.md §3 Chương 3.1](../.report/22-cau-truc-bao-cao-datn.md)

### 3.1.3. Sơ đồ Use Case
- **Nội dung cần vẽ:** PlantUML/Mermaid Use Case Diagram cho 3 actor (User, Admin, External).
- **Nguồn `.guide`:** [09-luong-nghiep-vu.md](../.guide/09-luong-nghiep-vu.md)

## 3.2. Kiến trúc tổng thể

### 3.2.1. Sơ đồ Layered Architecture
- **Nội dung cần vẽ:** Component Diagram 4 lớp:
  ```
  Client (Browser/Mobile/Postman)
    │
    ▼
  API Gateway (port 8080)
    │
    ▼
  Spring Cloud Layer (Eureka :8761, Config :8888)
    │
    ▼
  13 Business Services (port 8081–8093)
    │
    ▼
  Infrastructure (PostgreSQL, Redis, Kafka KRaft, Elasticsearch, Keycloak, Zipkin, Mailpit, Prometheus, Grafana)
  ```
- **Nguồn `.guide`:** [00-tong-quan.md](../.guide/00-tong-quan.md) (sơ đồ ASCII gốc)
- **Nguồn source code:** [docker-compose.yml](../docker-compose.yml)

### 3.2.2. Bảng 13 Business Services
- **Nội dung cần viết:** Bảng đầy đủ 13 service với (port, DB, dependency Kafka/Redis, Bounded Context).
- **Nguồn `.report`:** [01-kien-truc-microservices.md §4.1](../.report/01-kien-truc-microservices.md)
- **Nguồn `.guide`:** [05-cac-dich-vu-va-cong.md](../.guide/05-cac-dich-vu-va-cong.md), [00-tong-quan.md](../.guide/00-tong-quan.md)

### 3.2.3. Phân tích quyết định kiến trúc (Architecture Decision Records — tóm tắt)
- **ADR-1:** Vì sao tách `flash-sale-service` khỏi `product-service`? → Cô lập workload high-concurrency, dùng Redis Lua atomic riêng.
- **ADR-2:** Vì sao `cart-service` chỉ dùng Redis (không PostgreSQL)? → Dữ liệu ephemeral, TTL 7 ngày.
- **ADR-3:** Vì sao tách `search-service` khỏi `product-service`? → CQRS-lite, sync qua Kafka, scale read riêng.
- **ADR-4:** Vì sao chọn Saga **Orchestration** (order-service làm orchestrator) thay vì Choreography? → Giảm coupling event giữa 4 service (order, inventory, payment, voucher); dễ trace, dễ compensate.
- **Nguồn:** Tự viết, dựa trên [.report/01](../.report/01-kien-truc-microservices.md), [.report/09](../.report/09-saga-pattern-distributed-transaction.md)

## 3.3. Thiết kế Bounded Context (DDD)
- **Nội dung cần viết:** Sơ đồ context map, quan hệ Customer/Supplier giữa các service, Anti-Corruption Layer khi gọi VNPAY.
- **Nguồn `.report`:** [01-kien-truc-microservices.md §2.6, §4.1](../.report/01-kien-truc-microservices.md)

## 3.4. Thiết kế cơ sở dữ liệu

### 3.4.1. Database per Service
- **Nội dung cần viết:** 1 instance PostgreSQL chứa 10 database tách biệt, mỗi service chỉ kết nối tới DB của mình.
- **Nguồn `.guide`:** [00-tong-quan.md §"Các Database PostgreSQL"](../.guide/00-tong-quan.md)
- **Nguồn source code:** [init-db/](../init-db/)

### 3.4.2. ERD chi tiết (10 ERD)
- **Phải vẽ ERD** cho mỗi service (PlantUML/Draw.io).
- **Nguồn source code (Flyway migration):**
  - [user-service/.../V1__create_user_tables.sql](../user-service/src/main/resources/db/migration)
  - [product-service/.../V1__create_product_tables.sql](../product-service/src/main/resources/db/migration)
  - [inventory-service/.../V1__create_inventory_tables.sql](../inventory-service/src/main/resources/db/migration/V1__create_inventory_tables.sql)
  - [order-service/.../V1__create_order_tables.sql](../order-service/src/main/resources/db/migration/V1__create_order_tables.sql)
  - [payment-service/.../V1__create_payment_tables.sql](../payment-service/src/main/resources/db/migration/V1__create_payment_tables.sql)
  - [voucher-service/.../V1__create_voucher_tables.sql](../voucher-service/src/main/resources/db/migration)
  - [notification-service/.../V1__create_notification_tables.sql](../notification-service/src/main/resources/db/migration)
  - [review-service/.../V1__create_review_tables.sql](../review-service/src/main/resources/db/migration)
  - [content-service/.../V1__create_content_tables.sql](../content-service/src/main/resources/db/migration)
  - [flash-sale-service/.../V1__create_flash_sale_tables.sql](../flash-sale-service/src/main/resources/db/migration/V1__create_flash_sale_tables.sql)
- **Nội dung trọng tâm:**
  - Bảng `outbox` trong `order_db`, `payment_outbox` trong `payment_db` — phục vụ Outbox Pattern.
  - Bảng `processed_events` (UUID PK) — phục vụ Idempotent Consumer.
  - Cấu trúc `inventory` 2 cột `quantity` & `reserved_quantity` (dual-counter).
  - Unique partial index `payments(order_id) WHERE status = 'PENDING'` — chống tạo nhiều payment pending cho cùng đơn.
  - Constraint `flash_sale_campaigns.sold_count <= quantity`.

### 3.4.3. Polyglot persistence
- Cart-service dùng Redis, search-service dùng Elasticsearch, identity-service dùng dữ liệu Keycloak.
- **Nguồn `.report`:** [14-database-per-service-flyway.md](../.report/14-database-per-service-flyway.md), [15-redis-cache-distributed-lock.md](../.report/15-redis-cache-distributed-lock.md), [16-elasticsearch-fulltext-search.md](../.report/16-elasticsearch-fulltext-search.md)

## 3.5. Thiết kế API

### 3.5.1. Bảng tổng hợp API endpoint
- **Nội dung cần viết:** Bảng endpoint theo service, kèm method, path, public/internal, role bắt buộc.
- **Nguồn `.guide`:** [11-api-tham-khao.md](../.guide/11-api-tham-khao.md)
- **Nguồn source code:** Tất cả `*Controller.java` của 13 service.

### 3.5.2. Quy ước phản hồi
- `ApiResponse<T>` chuẩn (success, message, data, timestamp).
- Mã lỗi (BusinessException, ResourceNotFoundException).
- **Nguồn source code:** [common/.../ApiResponse.java](../common/src/main/java/com/ecommerce/common/dto), [common/.../exception/](../common/src/main/java/com/ecommerce/common/exception)
- **Nguồn `.guide`:** [10-xu-ly-loi.md](../.guide/10-xu-ly-loi.md)

### 3.5.3. Public vs Internal API
- Internal API (`/internal/...`) chỉ accept call từ service nội bộ (cùng cluster, không qua Gateway).
- **Nguồn source code:** [order-service/.../InternalOrderController.java](../order-service/src/main/java/com/ecommerce/order/controller/InternalOrderController.java)

## 3.6. Thiết kế bảo mật

### 3.6.1. Mô hình xác thực
- **Nội dung cần viết:**
  - Keycloak phát JWT (RS256).
  - API Gateway = OAuth 2.0 Resource Server, validate JWT bằng JWKS.
  - Sau khi validate, Gateway forward 3 header: `X-User-Id`, `X-User-Roles`, `X-User-Email`.
  - Backend services *trust* các header này (Trusted Subsystem Pattern).
  - Sơ đồ tuần tự xác thực.
- **Nguồn `.report`:** [06-bao-mat-keycloak-oauth-jwt.md](../.report/06-bao-mat-keycloak-oauth-jwt.md)
- **Nguồn `.guide`:** [06-keycloak-setup.md](../.guide/06-keycloak-setup.md), [15-gateway-security.md](../.guide/15-gateway-security.md)
- **Nguồn source code:**
  - [api-gateway/.../config/SecurityConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java)
  - [api-gateway/.../filter/AuthHeaderFilter.java](../api-gateway/src/main/java/com/ecommerce/gateway/filter/AuthHeaderFilter.java)
  - [api-gateway/.../filter/GuestSessionFilter.java](../api-gateway/src/main/java/com/ecommerce/gateway/filter/GuestSessionFilter.java)
  - [identity-service/](../identity-service)

### 3.6.2. Phân quyền (Role-Based Access Control)
- USER, ADMIN role; mapping Keycloak role → Spring Security authority.
- **Nguồn source code:** [common/.../security/](../common/src/main/java/com/ecommerce/common)

### 3.6.3. Bảo mật VNPAY (HMAC-SHA512)
- **Nội dung cần viết:** Quy trình ký request/verify response, chống tampering.
- **Nguồn `.report`:** [19-vnpay-payment-gateway.md](../.report/19-vnpay-payment-gateway.md)
- **Nguồn source code:** [payment-service/.../util/VnPayUtil.java](../payment-service/src/main/java/com/ecommerce/payment/util/VnPayUtil.java), [payment-service/.../config/VnPayConfig.java](../payment-service/src/main/java/com/ecommerce/payment/config/VnPayConfig.java), [payment-service/.../service/impl/VnPayServiceImpl.java](../payment-service/src/main/java/com/ecommerce/payment/service/impl/VnPayServiceImpl.java)

## 3.7. Thiết kế Sự kiện Kafka

### 3.7.1. Bản đồ 11 nhóm sự kiện / 13 topic vật lý
- **Nội dung cần vẽ:** Sơ đồ producer/consumer toàn cảnh.
- **Nguồn `.guide`:** [12-kafka-topics.md](../.guide/12-kafka-topics.md) (đã có bản đồ chi tiết)
- **Nguồn `.report`:** [08-event-driven-kafka.md](../.report/08-event-driven-kafka.md)
- **Nguồn source code:**
  - Producer: [order-service/.../service/OutboxService.java](../order-service/src/main/java/com/ecommerce/order/service/OutboxService.java), [flash-sale-service/.../kafka/FlashSaleEventProducer.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/kafka/FlashSaleEventProducer.java)
  - Consumer (ví dụ): [order-service/.../kafka/InventoryUpdatedConsumer.java](../order-service/src/main/java/com/ecommerce/order/kafka/InventoryUpdatedConsumer.java), [order-service/.../kafka/PaymentSuccessConsumer.java](../order-service/src/main/java/com/ecommerce/order/kafka/PaymentSuccessConsumer.java), [order-service/.../kafka/FlashSaleOrderConsumer.java](../order-service/src/main/java/com/ecommerce/order/kafka/FlashSaleOrderConsumer.java)
  - Event schema: [common/.../event/](../common/src/main/java/com/ecommerce/common/event)

### 3.7.2. Schema sự kiện chính
- `OrderCreatedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent`, `InventoryUpdatedEvent`, `InventoryFailedEvent`, `PaymentRequestedEvent`, `PaymentSuccessEvent`, `PaymentFailedEvent`, `FlashSaleOrderRequestedEvent`, `UserRegisteredEvent`, `Product*Event`.
- **Nguồn source code:** [common/src/main/java/com/ecommerce/common/event/](../common/src/main/java/com/ecommerce/common/event)

### 3.7.3. Topic đặc biệt: `flash-sale-order-requested` (3 partitions)
- Lý do: hot partition tránh bottleneck, scale consumer theo partition.
- **Nguồn `.guide`:** [12-kafka-topics.md §11](../.guide/12-kafka-topics.md)

## 3.8. Thiết kế State Machine

### 3.8.1. Order State Machine
```
PENDING ─[order-created OK]→ STOCK_RESERVED ─[COD hoặc VNPAY OK]→ CONFIRMED
   │                              │
   │                              ├─[payment-failed/expired]→ CANCELLED
   └─[inventory-failed]→ CANCELLED
```
- 4 trạng thái: PENDING, STOCK_RESERVED, CONFIRMED, CANCELLED.
- **Nguồn `.guide`:** [13-state-machines.md §1](../.guide/13-state-machines.md)
- **Nguồn source code:** [order-service/.../entity/OrderStatus.java](../order-service/src/main/java/com/ecommerce/order/entity/OrderStatus.java), [order-service/.../service/impl/OrderServiceImpl.java](../order-service/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java)

### 3.8.2. Payment State Machine
```
PENDING ─[VNPAY callback OK]→ COMPLETED
   ├─[VNPAY callback fail]→ FAILED
   └─[PaymentTimeoutScheduler 30 phút]→ TIMEOUT
```
- **Nguồn `.guide`:** [13-state-machines.md §2](../.guide/13-state-machines.md)
- **Nguồn source code:** [payment-service/.../entity/PaymentStatus.java](../payment-service/src/main/java/com/ecommerce/payment/entity/PaymentStatus.java), [payment-service/.../scheduler/PaymentTimeoutScheduler.java](../payment-service/src/main/java/com/ecommerce/payment/scheduler/PaymentTimeoutScheduler.java)

### 3.8.3. Campaign State Machine (Flash Sale)
```
SCHEDULED ─[startTime]→ ACTIVE ─[endTime]→ ENDED
        └─[admin cancel]→ CANCELLED
```
- Chuyển trạng thái do `CampaignScheduler` chạy mỗi 5s.
- **Nguồn `.guide`:** [13-state-machines.md §3](../.guide/13-state-machines.md), [14-scheduler-jobs.md](../.guide/14-scheduler-jobs.md)
- **Nguồn source code:** [flash-sale-service/.../entity/CampaignStatus.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/entity/CampaignStatus.java), [flash-sale-service/.../scheduler/CampaignScheduler.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/scheduler/CampaignScheduler.java)

### 3.8.4. Inventory Reservation Dual-Counter
- `quantity` vs `reserved_quantity`. Available = quantity − reserved.
- **Nguồn `.guide`:** [13-state-machines.md §4](../.guide/13-state-machines.md)
- **Nguồn source code:** [inventory-service/](../inventory-service)

## 3.9. Thiết kế luồng nghiệp vụ chính (Sequence Diagrams)

### 3.9.1. Đăng ký user
- identity-service → Keycloak → Kafka `user-registered` → user-service tạo profile.
- **Nguồn `.report`:** [06-bao-mat-keycloak-oauth-jwt.md](../.report/06-bao-mat-keycloak-oauth-jwt.md)

### 3.9.2. Đặt đơn COD (Saga full happy path)
```
Client → Gateway → order-service:
  - Validate, gọi product-service (Feign), voucher-service (Feign)
  - INSERT orders + INSERT outbox(order-created) trong 1 transaction
OutboxPoller (1s) → Kafka order-created
inventory-service consume → reserve → Kafka inventory-updated
order-service consume → STOCK_RESERVED → Kafka payment-requested (do COD)
payment-service consume → tạo Payment COD COMPLETED → Kafka payment-success
order-service consume → CONFIRMED → Kafka order-confirmed
inventory-service consume → CONFIRM (trừ thật) — notification-service consume → email
```
- **Nguồn `.guide`:** [09-luong-nghiep-vu.md](../.guide/09-luong-nghiep-vu.md), [13-state-machines.md](../.guide/13-state-machines.md)
- **Nguồn source code:** [order-service/.../OrderServiceImpl.java](../order-service/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java), [payment-service/.../kafka/PaymentRequestedConsumer.java](../payment-service/src/main/java/com/ecommerce/payment/kafka/PaymentRequestedConsumer.java)

### 3.9.3. Đặt đơn VNPAY + IPN (Return URL)
- Client gọi `POST /api/payments/{orderId}/vnpay` → nhận URL VNPAY.
- VNPAY callback `vnp_ResponseCode` → verify HMAC → update Payment + Outbox.
- Nếu user không thanh toán: `PaymentTimeoutScheduler` (60s tick) → TIMEOUT → `payment-failed` → order CANCELLED.
- `ReservationExpiryScheduler` (60s tick) → CANCELLED nếu quá 30 phút.
- **Nguồn `.report`:** [19-vnpay-payment-gateway.md](../.report/19-vnpay-payment-gateway.md)
- **Nguồn `.guide`:** [13-state-machines.md §1 (luồng VNPAY)](../.guide/13-state-machines.md), [14-scheduler-jobs.md](../.guide/14-scheduler-jobs.md)
- **Nguồn source code:** [payment-service/.../service/impl/VnPayServiceImpl.java](../payment-service/src/main/java/com/ecommerce/payment/service/impl/VnPayServiceImpl.java), [payment-service/.../scheduler/PaymentTimeoutScheduler.java](../payment-service/src/main/java/com/ecommerce/payment/scheduler/PaymentTimeoutScheduler.java), [order-service/.../scheduler/ReservationExpiryScheduler.java](../order-service/src/main/java/com/ecommerce/order/scheduler/ReservationExpiryScheduler.java)

### 3.9.4. Saga Compensation (đơn bị hủy)
- 3 nguồn hủy: `inventory-failed`, `payment-failed`, `reservation-expiry`.
- Compensation chuỗi: order CANCELLED → inventory RELEASE → voucher RELEASE → email.
- **Nguồn `.report`:** [09-saga-pattern-distributed-transaction.md](../.report/09-saga-pattern-distributed-transaction.md)
- **Nguồn source code:** [order-service/.../kafka/InventoryFailedConsumer.java](../order-service/src/main/java/com/ecommerce/order/kafka/InventoryFailedConsumer.java), [order-service/.../kafka/PaymentFailedConsumer.java](../order-service/src/main/java/com/ecommerce/order/kafka/PaymentFailedConsumer.java)

### 3.9.5. Flash Sale Purchase (high-concurrency)
- POST /api/flash-sales/{id}/purchase → flash-sale-service.
- **Redis Lua atomic** (xem `RESERVE_SCRIPT` trong source):
  - SISMEMBER set người-đã-mua → reject (-2).
  - GET stock; nếu ≤ 0 → reject (-1).
  - DECR stock + SADD set + EXPIRE.
- Nếu OK → publish `flash-sale-order-requested` (3 partitions, key = userId).
- order-service consume → tạo Order với cờ `is_flash_sale=true`, unique constraint `(flash_sale_id, user_id)` chống mua trùng.
- Nếu publish fail hoặc create order fail → COMPENSATE_SCRIPT INCR + SREM.
- **Nguồn `.report`:** [13-flash-sale-concurrency.md](../.report/13-flash-sale-concurrency.md)
- **Nguồn `.guide`:** [13-state-machines.md §3](../.guide/13-state-machines.md), [09-luong-nghiep-vu.md](../.guide/09-luong-nghiep-vu.md)
- **Nguồn source code:** [flash-sale-service/.../service/impl/FlashSaleServiceImpl.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/service/impl/FlashSaleServiceImpl.java) (RESERVE_SCRIPT/COMPENSATE_SCRIPT), [flash-sale-service/.../scheduler/CampaignScheduler.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/scheduler/CampaignScheduler.java), [flash-sale-service/.../scheduler/ReconciliationScheduler.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/scheduler/ReconciliationScheduler.java), [order-service/src/main/resources/db/migration/V2__add_flash_sale_order_uniqueness.sql](../order-service/src/main/resources/db/migration/V2__add_flash_sale_order_uniqueness.sql)

### 3.9.6. Search (CQRS-lite)
- product-service publish `product-created/updated/deleted` → search-service consume → index Elasticsearch.
- Client search → search-service query Elasticsearch.
- **Nguồn `.report`:** [16-elasticsearch-fulltext-search.md](../.report/16-elasticsearch-fulltext-search.md)
- **Nguồn source code:** [search-service/](../search-service), [product-service/](../product-service)

## 3.10. Thiết kế Resilience

### 3.10.1. Circuit Breaker (Resilience4j)
- Áp dụng trên Feign client.
- Threshold đề xuất: failure-rate 50%, sliding-window 10 requests, wait-duration 10s.
- Fallback class implement default response.
- **Nguồn `.report`:** [11-resilience-circuit-breaker-rate-limit.md](../.report/11-resilience-circuit-breaker-rate-limit.md)
- **Nguồn `.guide`:** [15-gateway-security.md](../.guide/15-gateway-security.md)
- **Nguồn source code:** [order-service/.../client/ProductServiceFallback.java](../order-service/src/main/java/com/ecommerce/order/client/ProductServiceFallback.java), [order-service/.../client/CartServiceFallback.java](../order-service/src/main/java/com/ecommerce/order/client/CartServiceFallback.java), [order-service/.../client/VoucherServiceFallback.java](../order-service/src/main/java/com/ecommerce/order/client/VoucherServiceFallback.java), [payment-service/.../client/OrderServiceClientFallback.java](../payment-service/src/main/java/com/ecommerce/payment/client/OrderServiceClientFallback.java), [flash-sale-service/.../client/OrderCountClientFallback.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/client/OrderCountClientFallback.java)

### 3.10.2. Rate Limiting (Redis-backed)
- Spring Cloud Gateway `RequestRateLimiter` filter, `RedisRateLimiter` (token bucket).
- Cấu hình thực tế:
  - `identity-service`: replenish=10/s, burst=20 (per IP).
  - `order-service`: replenish=10/s, burst=20 (per user).
  - `flash-sale-purchase`: replenish=3/s, burst=5 (per user) — **chặt nhất** vì high-concurrency.
- **Nguồn `.guide`:** [15-gateway-security.md](../.guide/15-gateway-security.md)
- **Nguồn source code:** [api-gateway/src/main/resources/application.yml](../api-gateway/src/main/resources/application.yml), [api-gateway/.../config/RateLimiterConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/RateLimiterConfig.java)

### 3.10.3. Outbox Pattern + Idempotent Consumer
- Outbox: bảng `outbox` / `payment_outbox`, poller mỗi 1s, status PENDING → PUBLISHED, retry với attempts.
- Idempotent: bảng `processed_events(event_id UUID PK)`. Consumer kiểm tra trước khi xử lý.
- **Nguồn `.report`:** [10-outbox-idempotency.md](../.report/10-outbox-idempotency.md)
- **Nguồn source code:**
  - [order-service/.../service/OutboxService.java](../order-service/src/main/java/com/ecommerce/order/service/OutboxService.java)
  - [order-service/.../scheduler/OutboxPoller.java](../order-service/src/main/java/com/ecommerce/order/scheduler/OutboxPoller.java)
  - [payment-service/.../kafka/PaymentEventOutboxPoller.java](../payment-service/src/main/java/com/ecommerce/payment/kafka/PaymentEventOutboxPoller.java)
  - Các `ProcessedEventRepository.java` ở order-service & payment-service.

### 3.10.4. Scheduled Reconciliation
- `ReservationExpiryScheduler` (order-service, 60s) — hủy đơn quá hạn 30 phút.
- `PaymentTimeoutScheduler` (payment-service, 60s) — set TIMEOUT cho payment quá hạn.
- `CampaignScheduler` (flash-sale-service, 5s) — chuyển SCHEDULED→ACTIVE→ENDED.
- `ReconciliationScheduler` (flash-sale-service) — đối soát Redis stock vs DB sold_count.
- **Nguồn `.report`:** [12-state-machine-scheduler.md](../.report/12-state-machine-scheduler.md)
- **Nguồn `.guide`:** [14-scheduler-jobs.md](../.guide/14-scheduler-jobs.md)

---

# CHƯƠNG 4 — CÀI ĐẶT & TRIỂN KHAI (25–30 trang)

## 4.1. Môi trường & công nghệ
- **Nội dung cần viết:** Bảng tech stack đầy đủ.
  - Java 21, Maven Wrapper 3.9, Spring Boot 3.5.x, Spring Cloud 2025.
  - PostgreSQL 17-alpine, Redis 8-alpine, Kafka 8.2 (KRaft), Elasticsearch 8.18.8.
  - Keycloak 26.6.1, Mailpit v1.27.8, Zipkin 3.5.0, Prometheus v3.11.2, Grafana 13.0.1.
  - Docker Desktop / Docker Compose v2.
- **Nguồn `.guide`:** [00-tong-quan.md](../.guide/00-tong-quan.md), [01-yeu-cau-he-thong.md](../.guide/01-yeu-cau-he-thong.md), [02-cai-dat-moi-truong.md](../.guide/02-cai-dat-moi-truong.md)
- **Nguồn source code:** [pom.xml](../pom.xml), [docker-compose.yml](../docker-compose.yml), [.mvn/](../.mvn)

## 4.2. Cấu trúc dự án Maven multi-module
- **Nội dung cần vẽ:** Sơ đồ thư mục.
- **Nguồn source code:** [README.md](../README.md), [pom.xml](../pom.xml) (parent pom với 14 module: common + 13 service)

## 4.3. Cài đặt Infrastructure (Docker Compose)
- **Nội dung cần viết:** Phân tích các service trong `docker-compose.yml`:
  - Network bridges, volumes (postgres-data, redis-data, kafka-data, es-data, keycloak-data, grafana-data).
  - `depends_on` + healthcheck.
  - 10 database tự tạo qua `init-db/init-databases.sh`.
  - Keycloak realm import.
- **Nguồn `.guide`:** [03-build-va-chay-docker.md](../.guide/03-build-va-chay-docker.md), [04-kiem-tra-he-thong.md](../.guide/04-kiem-tra-he-thong.md)
- **Nguồn `.report`:** [21-docker-container-deployment.md](../.report/21-docker-container-deployment.md)
- **Nguồn source code:** [docker-compose.yml](../docker-compose.yml), [init-db/init-databases.sh](../init-db/init-databases.sh), [keycloak/](../keycloak)

## 4.4. Cài đặt Spring Cloud nền

### 4.4.1. Discovery Server (Eureka :8761)
- `@EnableEurekaServer`, mỗi service tự đăng ký.
- **Nguồn `.report`:** [03-service-discovery-eureka.md](../.report/03-service-discovery-eureka.md)
- **Nguồn source code:** [discovery-server/](../discovery-server)

### 4.4.2. Config Server (:8888, native mode)
- Mỗi service `bootstrap.yml` import config.
- **Nguồn `.report`:** [04-config-server.md](../.report/04-config-server.md)
- **Nguồn source code:** [config-server/](../config-server)

## 4.5. Cài đặt API Gateway (chi tiết)
- **Nội dung cần viết:**
  - Routes config (12 route + flash-sale-purchase route ưu tiên).
  - JWT validation Resource Server.
  - Custom filter: `AuthHeaderFilter` (forward X-User-*), `GuestSessionFilter` (sinh session anonymous cho cart).
  - Rate Limiter Redis (`KeyResolver` per IP/per user).
  - CORS, CSRF disabled (stateless).
- **Nguồn `.report`:** [05-api-gateway.md](../.report/05-api-gateway.md)
- **Nguồn `.guide`:** [15-gateway-security.md](../.guide/15-gateway-security.md)
- **Nguồn source code:**
  - [api-gateway/src/main/resources/application.yml](../api-gateway/src/main/resources/application.yml)
  - [api-gateway/.../config/SecurityConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java)
  - [api-gateway/.../config/RateLimiterConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/RateLimiterConfig.java)
  - [api-gateway/.../filter/AuthHeaderFilter.java](../api-gateway/src/main/java/com/ecommerce/gateway/filter/AuthHeaderFilter.java)
  - [api-gateway/.../filter/GuestSessionFilter.java](../api-gateway/src/main/java/com/ecommerce/gateway/filter/GuestSessionFilter.java)

## 4.6. Cài đặt từng business service (mỗi service ~1 trang)

### 4.6.1. identity-service
- Đăng ký user qua Keycloak Admin REST, publish `user-registered`.

### 4.6.2. user-service
- Quản lý profile, consume `user-registered`.

### 4.6.3. product-service
- CRUD product, Redis cache, publish `product-created/updated/deleted`.

### 4.6.4. inventory-service
- Dual-counter (quantity, reserved_quantity), idempotent consumer.

### 4.6.5. cart-service
- Redis-only, hỗ trợ guest user (header session).

### 4.6.6. **order-service** (chi tiết — Saga orchestrator)
- **Trọng tâm:** placeOrder() chia 3 pha — validate (Feign), reserve voucher, persist order + outbox.
- OutboxPoller, ReservationExpiryScheduler, 5 Kafka consumer (Inventory*, Payment*, FlashSale*).
- **Nguồn source code:** [order-service/src/main/java/com/ecommerce/order/](../order-service/src/main/java/com/ecommerce/order)

### 4.6.7. **payment-service** (chi tiết — VNPAY)
- VnPayUtil (HMAC-SHA512), VnPayService (build URL + verify IPN).
- PaymentEventOutboxPoller, PaymentTimeoutScheduler.
- **Nguồn `.report`:** [19-vnpay-payment-gateway.md](../.report/19-vnpay-payment-gateway.md)
- **Nguồn source code:** [payment-service/src/main/java/com/ecommerce/payment/](../payment-service/src/main/java/com/ecommerce/payment)

### 4.6.8. voucher-service
- Reserve/Release, idempotency theo orderId.

### 4.6.9. notification-service
- Consume `order-confirmed`, `order-cancelled` → JavaMail → Mailpit.

### 4.6.10. review-service
- Kiểm tra quyền review qua Feign `order-service` (chỉ user có CONFIRMED order).

### 4.6.11. search-service
- Consume `product-created/updated/deleted` → ElasticsearchClient bulk index.
- **Nguồn `.report`:** [16-elasticsearch-fulltext-search.md](../.report/16-elasticsearch-fulltext-search.md)

### 4.6.12. content-service
- CMS đơn giản (banner, page).

### 4.6.13. **flash-sale-service** (chi tiết — high concurrency)
- **Trọng tâm:** RESERVE_SCRIPT & COMPENSATE_SCRIPT (Lua) trong [FlashSaleServiceImpl.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/service/impl/FlashSaleServiceImpl.java) (lines ~35–58).
- CampaignScheduler (5s), ReconciliationScheduler.
- **Nguồn `.report`:** [13-flash-sale-concurrency.md](../.report/13-flash-sale-concurrency.md)

## 4.7. Cài đặt Observability

### 4.7.1. Metrics
- Spring Boot Actuator + Micrometer Prometheus registry.
- Prometheus scrape mỗi service `/actuator/prometheus`.
- Grafana provisioned 3 dashboard (JVM, HTTP, Kafka — nếu đã có).
- **Nguồn `.report`:** [17-observability-prometheus-grafana.md](../.report/17-observability-prometheus-grafana.md)
- **Nguồn `.guide`:** [07-monitoring-observability.md](../.guide/07-monitoring-observability.md)
- **Nguồn source code:** [prometheus/](../prometheus), [grafana/](../grafana)

### 4.7.2. Distributed tracing
- Micrometer Tracing + Zipkin Brave (W3C Trace Context).
- Mỗi REST/Kafka call propagate `traceparent` header.
- **Nguồn `.report`:** [18-distributed-tracing-zipkin.md](../.report/18-distributed-tracing-zipkin.md)

## 4.8. Quy trình build/deploy local
- `./mvnw clean package -DskipTests`.
- `docker compose up -d --build`.
- Healthcheck order: postgres, redis, kafka, elasticsearch → keycloak → discovery-server → config-server → api-gateway → 13 service.
- **Nguồn `.guide`:** [03-build-va-chay-docker.md](../.guide/03-build-va-chay-docker.md), [08-chay-local-dev.md](../.guide/08-chay-local-dev.md)

## 4.9. Demo Screenshot (đính kèm hình)
- **Nội dung cần chụp:**
  - (1) Eureka Dashboard với 14 ứng dụng đăng ký.
  - (2) Keycloak Admin Console (Realm `ecommerce`).
  - (3) Swagger UI (qua `/api-gateway/api-docs`).
  - (4) Grafana dashboards.
  - (5) Zipkin trace của 1 đơn hàng đi qua order → inventory → payment → notification.
  - (6) Mailpit nhận email confirmed.
  - (7) VNPAY sandbox return page.
- **Nguồn `.guide`:** [04-kiem-tra-he-thong.md](../.guide/04-kiem-tra-he-thong.md), [02-lay-credentials.md](../.guide/02-lay-credentials.md)

---

# CHƯƠNG 5 — KIỂM THỬ & ĐÁNH GIÁ (15–20 trang)

> **Lưu ý nguyên tắc:** Chỉ ghi số liệu sau khi đã đo thực tế. Phần nào chưa đo phải ghi rõ "kịch bản đề xuất" / "hướng phát triển".

## 5.1. Chiến lược kiểm thử
- Test pyramid → Test honeycomb cho microservice.
- Unit (JUnit 5 + Mockito) + Integration (Testcontainers Postgres/Redis/Kafka) + E2E (Postman collection).
- **Nguồn `.report`:** [20-testing-microservices.md](../.report/20-testing-microservices.md)
- **Nguồn source code:** Các thư mục `src/test/` của 13 service.

## 5.2. Test case chính (bảng tổng hợp)
- Bảng test case cho mỗi luồng nghiệp vụ:
  - Đăng ký user → user-registered → user-service profile.
  - Đặt đơn COD → CONFIRMED.
  - Đặt đơn VNPAY → thanh toán thành công.
  - Đặt đơn VNPAY → user không thanh toán → TIMEOUT/CANCELLED.
  - Mua flash sale (race 100 user, slot 10) → 10 thành công, 90 reject.
  - Voucher hết → đơn bị reject.
  - Inventory không đủ → CANCELLED.
- **Nguồn `.guide`:** [09-luong-nghiep-vu.md](../.guide/09-luong-nghiep-vu.md)

## 5.3. Kết quả test tự động
- `./mvnw test` (số test pass/fail).
- JaCoCo coverage (chỉ đưa vào nếu đã thêm plugin).

## 5.4. Đánh giá hiệu năng (k6 / JMeter)

> **Chỉ ghi số liệu nếu đã chạy thật. Nếu chưa, ghi "Kịch bản đề xuất".**

- **Kịch bản đề xuất:**
  - 100/500/1000 concurrent user — flow đặt đơn (POST /api/orders).
  - 100/500/1000 concurrent user — flow flash sale (POST /api/flash-sales/{id}/purchase).
- **Chỉ số đo:** Throughput (RPS), latency p50/p95/p99, error rate.
- **Tiêu chí pass:**
  - Số request thành công flash sale = số slot.
  - Không có đơn vượt số slot (kiểm tra `sold_count <= quantity`).
- **Nguồn `.report`:** [13-flash-sale-concurrency.md](../.report/13-flash-sale-concurrency.md), [22-cau-truc-bao-cao-datn.md §3 Chương 5.4](../.report/22-cau-truc-bao-cao-datn.md)

## 5.5. Đánh giá khả năng chịu lỗi (Resilience test)
- **Kịch bản:**
  - Stop product-service → Circuit Breaker OPEN, fallback hoạt động (kiểm tra log).
  - Stop Kafka → Outbox tích lũy PENDING → restart Kafka → poller publish lại.
  - Kill payment-service giữa flow VNPAY → ReservationExpiryScheduler hủy đơn sau 30 phút.
- **Bằng chứng cần đính kèm:** Log line + screenshot trạng thái DB trước/sau.
- **Nguồn `.report`:** [11-resilience-circuit-breaker-rate-limit.md](../.report/11-resilience-circuit-breaker-rate-limit.md), [10-outbox-idempotency.md](../.report/10-outbox-idempotency.md)

## 5.6. Đánh giá bảo mật
- JWT hết hạn → Gateway trả 401.
- Vượt rate limit → 429.
- VNPAY hash sai → reject.
- Test SQL injection cơ bản (Spring Data JPA prepared statement).
- **Nguồn `.report`:** [06-bao-mat-keycloak-oauth-jwt.md](../.report/06-bao-mat-keycloak-oauth-jwt.md), [19-vnpay-payment-gateway.md](../.report/19-vnpay-payment-gateway.md)

## 5.7. So sánh với mục tiêu đề ra
- Bảng requirements (mục 1.2) vs implemented + % completion.

## 5.8. Hạn chế của hệ thống
- Single-host (chưa Kubernetes).
- Chưa có CI/CD pipeline (nếu thực tế chưa có workflow).
- Chưa có contract test (Pact) giữa các service.
- Chưa có frontend hoàn chỉnh.
- Load test ở mức demo, chưa đại diện production.

---

# KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN (3 trang)

## 1. Kết quả đạt được
- 13 business microservice triển khai đầy đủ qua Docker Compose.
- Áp dụng đúng các pattern: Saga (orchestration), Outbox, Idempotent Consumer, CQRS-lite (search), Circuit Breaker, Rate Limit.
- Flash sale với Redis Lua atomic counter — chống over-sell (kết luận cuối cần dựa kết quả test §5.4).
- Observability đầy đủ: metrics (Prometheus/Grafana), tracing (Zipkin).
- Tích hợp VNPAY sandbox với HMAC-SHA512 + IPN.

## 2. Đóng góp
- Reference architecture cho e-commerce VN cỡ vừa.
- Codebase open-source (nếu publish GitHub).

## 3. Hướng phát triển
- Migrate Kubernetes + Helm chart.
- CI/CD GitHub Actions / GitLab CI.
- Service mesh (Istio mTLS).
- Long-term metrics (Thanos/Cortex).
- Chaos engineering (Litmus, Chaos Mesh).
- Frontend Web (Next.js) / Mobile (React Native, Flutter).
- ML-based recommendation, fraud detection.
- **Nguồn:** [22-cau-truc-bao-cao-datn.md](../.report/22-cau-truc-bao-cao-datn.md)

---

# TÀI LIỆU THAM KHẢO (3–5 trang)

> Tối thiểu 30 đầu mục cho 1 báo cáo DATN nghiêm túc.

### Sách
1. Sam Newman, *Building Microservices: Designing Fine-Grained Systems*, 2nd ed., O'Reilly, 2021.
2. Chris Richardson, *Microservices Patterns*, Manning, 2018.
3. Eric Evans, *Domain-Driven Design: Tackling Complexity in the Heart of Software*, Addison-Wesley, 2003.
4. Vaughn Vernon, *Implementing Domain-Driven Design*, Addison-Wesley, 2013.
5. Michael Nygard, *Release It! Design and Deploy Production-Ready Software*, 2nd ed., Pragmatic Bookshelf, 2018.
6. Martin Kleppmann, *Designing Data-Intensive Applications*, O'Reilly, 2017.
7. Neha Narkhede et al., *Kafka: The Definitive Guide*, 2nd ed., O'Reilly, 2021.

### Bài báo / RFC
8. Eric Brewer, "Towards Robust Distributed Systems" (CAP Theorem), PODC 2000.
9. Daniel Abadi, "Consistency Tradeoffs in Modern Distributed Database System Design (PACELC)", IEEE Computer, 2012.
10. Pat Helland, "Life beyond Distributed Transactions: an Apostate's Opinion", CIDR 2007.
11. Hector Garcia-Molina, Kenneth Salem, "Sagas", ACM SIGMOD 1987.
12. Peter Deutsch, "The Eight Fallacies of Distributed Computing", Sun Microsystems, 1994.
13. RFC 6749 — *The OAuth 2.0 Authorization Framework*.
14. RFC 7519 — *JSON Web Token (JWT)*.
15. RFC 6750 — *OAuth 2.0 Bearer Token Usage*.
16. W3C, *Trace Context Specification*, 2021.

### Web / Tài liệu chính thức
17. Martin Fowler, "Microservices", martinfowler.com, 2014.
18. Chris Richardson, microservices.io.
19. Spring Boot Reference Documentation 3.5 — spring.io.
20. Spring Cloud 2025 — spring.io/projects/spring-cloud.
21. Resilience4j Documentation — resilience4j.readme.io.
22. Apache Kafka Documentation — kafka.apache.org.
23. Elasticsearch Reference 8.x — elastic.co.
24. Keycloak Documentation 26 — keycloak.org.
25. Prometheus Documentation — prometheus.io.
26. Grafana Documentation — grafana.com.
27. OpenZipkin Documentation — zipkin.io.
28. VNPAY Developer Docs — sandbox.vnpayment.vn.
29. Redis Documentation (Lua scripting, Redlock) — redis.io.
30. Flyway Documentation — flywaydb.org.

---

# PHỤ LỤC (10–20 trang)

## Phụ lục A — Cấu hình Docker Compose & Healthcheck
- Trích đoạn `docker-compose.yml`.
- **Nguồn:** [docker-compose.yml](../docker-compose.yml)

## Phụ lục B — Schema Database (Flyway DDL)
- Trích các file V1__create_*.sql.

## Phụ lục C — Postman Collection / API Reference
- **Nguồn `.guide`:** [11-api-tham-khao.md](../.guide/11-api-tham-khao.md)

## Phụ lục D — Kafka Topic Catalog
- **Nguồn `.guide`:** [12-kafka-topics.md](../.guide/12-kafka-topics.md)

## Phụ lục E — Hướng dẫn cài đặt & chạy hệ thống
- **Nguồn `.guide`:** [02-cai-dat-moi-truong.md](../.guide/02-cai-dat-moi-truong.md), [03-build-va-chay-docker.md](../.guide/03-build-va-chay-docker.md), [04-kiem-tra-he-thong.md](../.guide/04-kiem-tra-he-thong.md), [08-chay-local-dev.md](../.guide/08-chay-local-dev.md)

## Phụ lục F — Lấy credentials & Setup Keycloak
- **Nguồn `.guide`:** [02-lay-credentials.md](../.guide/02-lay-credentials.md), [06-keycloak-setup.md](../.guide/06-keycloak-setup.md)

## Phụ lục G — Xử lý lỗi thường gặp
- **Nguồn `.guide`:** [10-xu-ly-loi.md](../.guide/10-xu-ly-loi.md)

## Phụ lục H — Câu hỏi phản biện chuẩn bị bảo vệ
- Tổng hợp mục §6 của tất cả 21 file `.report/`.

---

# C. CHECKLIST HOÀN THÀNH BÁO CÁO

- [ ] Đã viết đủ 5 chương theo đề cương.
- [ ] Tất cả screenshot đã chụp với độ phân giải đủ rõ (≥1280px).
- [ ] Mọi sơ đồ vẽ bằng PlantUML/Mermaid/draw.io (không screenshot blur).
- [ ] Tối thiểu 30 hình + 15 bảng.
- [ ] Mọi định nghĩa Microservice/Saga/CAP/Outbox đều có citation `[1]`, `[2]`, ...
- [ ] Tài liệu tham khảo ≥ 30 mục, format chuẩn IEEE hoặc APA.
- [ ] Không placeholder số liệu (`X ms`, `Y RPS`); chỉ điền sau khi đo.
- [ ] Đã in & đóng quyển theo yêu cầu trường.
- [ ] Đã chuẩn bị slide defense 25–30 trang ([22-cau-truc-bao-cao-datn.md §6](../.report/22-cau-truc-bao-cao-datn.md)).
- [ ] Demo offline (Docker Compose) hoạt động ổn định.
- [ ] Backup video demo phòng trường hợp live demo lỗi.
