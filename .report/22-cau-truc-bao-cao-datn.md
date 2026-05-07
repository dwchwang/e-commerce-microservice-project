# 22. Đề Xuất Cấu Trúc Báo Cáo DATN & Cách Map Chủ Đề Vào Chương

## 1. Mục Đích

File này hướng dẫn bạn **map 21 chủ đề nghiên cứu** vào **cấu trúc chương báo cáo** chuẩn của trường đại học Việt Nam.

Nguyên tắc viết quan trọng:
- Chỉ viết là **đã triển khai** khi có code, screenshot hoặc kết quả test chứng minh.
- Những phần chưa có bằng chứng như k6/JMeter, JaCoCo, CI/CD, Kubernetes phải ghi là **kịch bản đánh giá**, **khuyến nghị**, hoặc **hướng phát triển**.
- Với số liệu hiệu năng, không dùng placeholder như `X ms`; chỉ điền sau khi đã chạy đo thực tế.

---

## 2. Cấu Trúc Báo Cáo Chuẩn (5 Chương)

```
LỜI CẢM ƠN
LỜI CAM ĐOAN
TÓM TẮT (tiếng Việt + tiếng Anh)
MỤC LỤC
DANH MỤC HÌNH VẼ
DANH MỤC BẢNG BIỂU
DANH MỤC TỪ VIẾT TẮT

CHƯƠNG 1: TỔNG QUAN ĐỀ TÀI                   (~10–15 trang)
CHƯƠNG 2: CƠ SỞ LÝ THUYẾT                    (~25–35 trang)
CHƯƠNG 3: PHÂN TÍCH & THIẾT KẾ HỆ THỐNG       (~25–35 trang)
CHƯƠNG 4: CÀI ĐẶT & TRIỂN KHAI                (~20–30 trang)
CHƯƠNG 5: KIỂM THỬ & ĐÁNH GIÁ                 (~15–20 trang)

KẾT LUẬN
TÀI LIỆU THAM KHẢO
PHỤ LỤC
```

Tổng ~100–150 trang.

---

## 3. Chi Tiết Mỗi Chương

### Chương 1: Tổng Quan Đề Tài

**1.1. Đặt vấn đề**
- Bùng nổ thương mại điện tử Việt Nam, traffic spike (sale 11/11, Black Friday)
- Hệ thống monolith truyền thống gặp khó khi scale
- Đặt câu hỏi: *làm thế nào xây dựng e-commerce có thể scale, resilient, support flash sale*?

**1.2. Mục tiêu đề tài**
- Xây dựng hệ thống e-commerce theo kiến trúc microservice
- Áp dụng các pattern distributed system: Saga, Outbox, Idempotency, CQRS-lite
- Tích hợp bảo mật (Keycloak/JWT), thanh toán VNPAY sandbox, monitoring/tracing (Prometheus/Grafana/Zipkin)
- Có module flash-sale để kiểm chứng bài toán high-concurrency và chống oversell

**1.3. Phạm vi đề tài**
- 13 business microservice chia theo bounded context, chạy sau API Gateway
- Hạ tầng hỗ trợ: Discovery Server, Config Server, PostgreSQL, Redis, Kafka, Elasticsearch, Keycloak, Prometheus, Grafana, Zipkin, Mailpit
- Java 21 + Spring Boot 3.5 + Spring Cloud 2025
- Triển khai Docker Compose (single-host)
- Out-of-scope: web/mobile client hoàn chỉnh, Kubernetes, third-party logistics, refund production, payment production

**1.4. Phương pháp nghiên cứu**
- Nghiên cứu tài liệu (sách, RFC, blog kỹ thuật)
- Phân tích các hệ thống tham chiếu (Tiki, Shopee, Amazon kiến trúc public)
- Implement hệ thống, chạy kiểm thử tự động/manual, thu thập screenshot và số liệu vận hành

**1.5. Khảo sát hệ thống tương tự**
- Tiki, Shopee, Lazada, Amazon — kiến trúc microservice
- Open-source: Spree, Saleor, Reactive Commerce

**1.6. Bố cục báo cáo**
- Tóm tắt 5 chương

→ Chủ yếu trích từ **file 00 (Lộ trình)** + **file 01 (Microservices)**.

---

### Chương 2: Cơ Sở Lý Thuyết

> **Chương quan trọng nhất** — chứng minh bạn nắm vững nền tảng.

**2.1. Kiến trúc Microservices**
- Định nghĩa, đặc trưng (Sam Newman)
- So sánh Monolith — SOA — Microservices
- Lợi ích, hệ lụy (8 fallacies of distributed computing)
- Bounded Context (DDD)
→ File **[01](./01-kien-truc-microservices.md)**

**2.2. Hệ sinh thái Spring Boot & Spring Cloud**
- Spring Boot, auto-configuration, Actuator
- Spring Cloud modules (Eureka, Config, Gateway, OpenFeign, Resilience4j)
→ File **[02](./02-spring-boot-spring-cloud.md)**

**2.3. Service Discovery & Centralized Configuration**
- Eureka — client-side discovery
- Spring Cloud Config Server
→ Files **[03](./03-service-discovery-eureka.md)**, **[04](./04-config-server.md)**

**2.4. API Gateway**
- Pattern, vai trò
- Spring Cloud Gateway (WebFlux, Reactor)
→ File **[05](./05-api-gateway.md)**

**2.5. Bảo mật trong hệ thống phân tán**
- OAuth 2.0, OpenID Connect, JWT
- Keycloak — IdP open source
- Trusted Subsystem Pattern
→ File **[06](./06-bao-mat-keycloak-oauth-jwt.md)**

**2.6. Giao tiếp giữa các microservice**
- Đồng bộ vs bất đồng bộ
- REST + OpenFeign
- Apache Kafka — distributed log, KRaft
- Delivery semantics (at-least-once, exactly-once)
→ Files **[07](./07-giao-tiep-rest-openfeign.md)**, **[08](./08-event-driven-kafka.md)**

**2.7. Distributed Transaction Patterns**
- Vì sao 2PC không scale
- CAP, PACELC theorem
- Saga pattern (Choreography vs Orchestration)
- Compensating action
→ File **[09](./09-saga-pattern-distributed-transaction.md)**

**2.8. Reliability Patterns**
- Transactional Outbox (Chris Richardson)
- Idempotent Consumer
- Circuit Breaker, Retry, Bulkhead, Rate Limiter (Resilience4j)
→ Files **[10](./10-outbox-idempotency.md)**, **[11](./11-resilience-circuit-breaker-rate-limit.md)**

**2.9. Persistence Patterns**
- Database per Service
- Polyglot Persistence
- Flyway migration
- Redis — cache, atomic counter, distributed lock
- Elasticsearch + CQRS-lite
→ Files **[14](./14-database-per-service-flyway.md)**, **[15](./15-redis-cache-distributed-lock.md)**, **[16](./16-elasticsearch-fulltext-search.md)**

**2.10. Observability**
- 3 trụ cột: metrics, logs, traces
- Prometheus + Grafana
- Distributed tracing (W3C, Zipkin, Micrometer Tracing)
→ Files **[17](./17-observability-prometheus-grafana.md)**, **[18](./18-distributed-tracing-zipkin.md)**

**2.11. State Machine & High Concurrency**
- Finite State Machine in domain modeling
- Atomic counter (Redis DECR/INCR)
- Distributed lock (Redlock)
- Reconciliation pattern
→ Files **[12](./12-state-machine-scheduler.md)**, **[13](./13-flash-sale-concurrency.md)**

---

### Chương 3: Phân Tích & Thiết Kế Hệ Thống

**3.1. Phân tích yêu cầu nghiệp vụ**
- Yêu cầu chức năng (use case): đăng ký, mua hàng, voucher, review, flash sale, ...
- Yêu cầu phi chức năng: bảo mật API, không oversell flash sale, eventual consistency cho order saga, khả năng quan sát qua metrics/tracing
- Mục tiêu hiệu năng để kiểm thử: ví dụ 100/500/1000 concurrent users cho một số flow chính; chỉ ghi RPS, p95, p99 sau khi có kết quả đo thực tế

**3.2. Phân tích tác nhân (Actor)**
- Khách hàng (User)
- Quản trị viên (Admin)
- Hệ thống bên thứ 3 (VNPAY, SMTP)

**3.3. Sơ đồ Use Case** (cần vẽ Draw.io / PlantUML / Mermaid)
- User: đăng ký/đăng nhập, xem sản phẩm, tìm kiếm, giỏ hàng, đặt hàng, thanh toán, review, mua flash sale
- Admin: quản lý sản phẩm, danh mục, voucher, nội dung, flash sale campaign
- External systems: VNPAY, Mailpit/SMTP

**3.4. Kiến trúc tổng thể**
- Layered diagram: Client → Gateway → Spring Cloud → Business services → Infrastructure
- (Trích sơ đồ ASCII từ `.guide/00-tong-quan.md` chuyển thành PNG)

**3.5. Phân chia Bounded Context**
- 13 service và lý do tách (xem **file 01** mục 4.1)

**3.6. Thiết kế dữ liệu**
- ERD cho từng service DB (10 ERD)
- Chú ý: ERD chỉ thể hiện relationships **trong cùng service**
- Search service dùng Elasticsearch document, cart-service dùng Redis, identity-service dựa vào Keycloak

**3.7. Thiết kế API**
- Bảng API endpoint (kèm Swagger screenshot)
- Public vs Internal API
- Error response format

**3.8. Thiết kế Event (Kafka topics)**
- 11 nhóm sự kiện nghiệp vụ / 13 topic vật lý chính (do `product-created`, `product-updated`, `product-deleted` là 3 topic riêng)
- Schema event chính
- Sơ đồ luồng event tổng thể

**3.9. Thiết kế State Machine**
- Order: PENDING → STOCK_RESERVED → CONFIRMED/CANCELLED
- Payment: PENDING → COMPLETED/FAILED/TIMEOUT
- Campaign: SCHEDULED → ACTIVE → ENDED/CANCELLED
→ File **[12](./12-state-machine-scheduler.md)**

**3.10. Thiết kế luồng nghiệp vụ chính (Sequence Diagram)**
- Đăng ký user (user-registered event)
- Đặt hàng (Order Saga, kèm compensation)
- Thanh toán VNPAY (Return URL + IPN)
- Flash sale purchase
- Search product (CQRS qua Kafka)

**3.11. Thiết kế bảo mật**
- Mô hình xác thực hiện tại: Keycloak phát JWT, Gateway đóng vai trò OAuth2 Resource Server và forward trusted identity headers
- Authorization Code + PKCE: nêu là hướng production cho SPA/mobile client, không viết như flow chính nếu demo hiện tại dùng login API đơn giản
- Authorization (Role-based)
- VNPAY HMAC signature
→ Files **[06](./06-bao-mat-keycloak-oauth-jwt.md)**, **[19](./19-vnpay-payment-gateway.md)**

**3.12. Thiết kế Resilience**
- Circuit Breaker thresholds (xem `.guide/15-gateway-security.md`)
- Rate Limiting strategy
- Retry policy
- Outbox + Idempotency

---

### Chương 4: Cài Đặt & Triển Khai

**4.1. Môi trường & công nghệ sử dụng**
- Java 21, Maven 3.9, Docker Desktop
- Spring Boot 3.5, Spring Cloud 2025
- Postgres 17, Redis 8, Kafka 8.2 (KRaft), Elasticsearch 8.18
- Keycloak 26, Mailpit, Zipkin, Prometheus, Grafana

**4.2. Cấu trúc dự án (Maven multi-module)**
```
e-commerce microservice project/
  pom.xml (parent)
  common/ ...
  api-gateway/ ...
  config-server/ ...
  ...
  docker-compose.yml
  .env
```

**4.3. Cài đặt Infrastructure**
- Docker Compose: explain từng service trong `docker-compose.yml`
- init-db/ tạo 10 database
- Keycloak realm import
→ File **[21](./21-docker-container-deployment.md)**

**4.4. Cài đặt Spring Cloud nền**
- Discovery Server (Eureka)
- Config Server (native mode)
- Mỗi service tự đăng ký + lấy config
→ Files **[03](./03-service-discovery-eureka.md)**, **[04](./04-config-server.md)**

**4.5. Cài đặt API Gateway**
- Routes config
- JWT validation
- Rate limiter Redis
- CORS
- Code mẫu KeyResolver
→ File **[05](./05-api-gateway.md)**

**4.6. Cài đặt từng business service** (chia làm vài subsection)
- order-service: Saga orchestrator + Outbox + Scheduler
- payment-service: VNPAY integration + IPN handler
- flash-sale-service: Redis atomic + CampaignScheduler + ReconciliationScheduler
- inventory-service: dual-counter + idempotency
- search-service: Kafka consumer → Elasticsearch index
- (các service khác tóm tắt)

**4.7. Cài đặt Observability**
- Actuator + Prometheus
- Grafana provisioned dashboards
- Zipkin tracing
→ Files **[17](./17-observability-prometheus-grafana.md)**, **[18](./18-distributed-tracing-zipkin.md)**

**4.8. Quy trình build/deploy local**
- Build: `./mvnw clean package`
- Deploy local: `docker compose up -d --build`
- CI/CD GitHub Actions/GitLab CI: chỉ đưa vào nếu bổ sung workflow thật; nếu chưa có thì chuyển sang Hướng phát triển

**4.9. Demo screenshot**
- Eureka Dashboard với các app đăng ký runtime: API Gateway, Config Server và 13 business services (xác nhận số lượng bằng screenshot thực tế)
- Keycloak Admin Console
- Swagger UI
- Grafana dashboards (3 cái)
- Zipkin trace của 1 đơn hàng
- Mailpit nhận email
- VNPAY sandbox return page

---

### Chương 5: Kiểm Thử & Đánh Giá

**5.1. Chiến lược kiểm thử**
- Test pyramid → test honeycomb cho microservice
- Unit + Integration + E2E (Postman)
→ File **[20](./20-testing-microservices.md)**

**5.2. Test case chính**
- Bảng test case cho mỗi luồng nghiệp vụ
- Kèm expected result và actual result

**5.3. Kết quả test**
- Số test pass / fail từ `./mvnw test`
- JaCoCo coverage screenshot chỉ đưa vào nếu đã thêm JaCoCo plugin và generate report
- Bug đã sửa (nếu có)

**5.4. Đánh giá hiệu năng** (mục **ăn điểm**!)
- Setup k6/JMeter test script nếu có thời gian; lưu script vào repo để hội đồng có thể kiểm tra
- Scenario đề xuất: 100/500/1000 concurrent user cho flow đặt đơn hoặc flash sale purchase
- Đo: throughput (RPS), latency p50/p95/p99, error rate
- Kết quả flash sale: số request thành công phải bằng số slot, số đơn tạo ra không vượt quá số slot, latency ghi theo kết quả đo thực tế
- Screenshot Grafana trong lúc load test

**5.5. Đánh giá khả năng chịu lỗi**
- Test scenarios:
  - Stop product-service → check Circuit Breaker OPEN, fallback hoạt động
  - Stop Kafka → check Outbox tích lũy, restart Kafka → publish lại
  - Kill payment-service giữa flow → ReservationExpiryScheduler hủy đơn, inventory release
- Ghi kết quả thực tế: trạng thái trước/sau, log hoặc screenshot chứng minh hệ thống phục hồi

**5.6. Đánh giá bảo mật**
- Test JWT expired → Gateway 401
- Test rate limit → 429
- Test VNPAY hash sai → reject

**5.7. So sánh với mục tiêu**
- Bảng requirements vs implemented
- % completion

**5.8. Hạn chế**
- Single-host (chưa K8s)
- Chưa có CI/CD nếu repo chưa bổ sung workflow
- Chưa có contract test giữa các service
- Chưa có frontend web/mobile hoàn chỉnh
- Load test ở mức demo, chưa đại diện production traffic

---

### Kết Luận

**1. Kết quả đạt được**
- 13 business microservice đã được triển khai, tích hợp qua API Gateway, Eureka, Config Server
- Áp dụng đúng các pattern: Saga, Outbox, Idempotency, CQRS-lite
- Flash sale dùng Redis Lua atomic counter để giảm rủi ro oversell; kết luận "không oversell" cần dựa trên kết quả test Chương 5
- Observability local demo với Prometheus, Grafana, Zipkin
- Tích hợp VNPAY sandbox với HMAC verification và IPN/Return URL

**2. Đóng góp**
- Reference architecture cho e-commerce VN nhỏ-trung
- Open source code base (nếu publish GitHub)

**3. Hướng phát triển**
- Migrate Kubernetes + Helm chart
- CI/CD GitHub Actions/GitLab CI
- Service mesh (Istio mTLS)
- Long-term metrics (Thanos)
- Chaos engineering (Litmus)
- Mobile app (React Native / Flutter)
- Advanced: ML-based recommendation, fraud detection

---

## 4. Bảng Map Nhanh: Chủ Đề → Chương

| File chủ đề | Chương 2 (Lý thuyết) | Chương 3 (Thiết kế) | Chương 4 (Cài đặt) |
|-------------|---------------------|---------------------|---------------------|
| 01 — Microservices | §2.1 ★ | §3.4–3.5 | - |
| 02 — Spring Boot/Cloud | §2.2 ★ | - | §4.2 |
| 03 — Eureka | §2.3 | §3.4 | §4.4 |
| 04 — Config Server | §2.3 | §3.4 | §4.4 |
| 05 — API Gateway | §2.4 ★ | §3.7, §3.11 | §4.5 ★ |
| 06 — Keycloak/JWT | §2.5 ★ | §3.11 ★ | §4.5 (auth filter) |
| 07 — REST/Feign | §2.6 | §3.7 | §4.6 (per service) |
| 08 — Kafka | §2.6 ★ | §3.8 ★ | §4.6 |
| 09 — Saga | §2.7 ★ | §3.10 ★ | §4.6 (order-service) |
| 10 — Outbox/Idempotency | §2.8 ★ | §3.12 | §4.6 |
| 11 — Resilience4j | §2.8 | §3.12 | §4.5, §4.6 |
| 12 — State Machine/Scheduler | §2.11 | §3.9 ★ | §4.6 |
| 13 — Flash Sale Concurrency | §2.11 ★ | §3.10 | §4.6 ★ |
| 14 — Database/Flyway | §2.9 | §3.6 | §4.6 |
| 15 — Redis | §2.9 | §3.10 (flash sale) | §4.6 |
| 16 — Elasticsearch | §2.9 | §3.10 (search) | §4.6 |
| 17 — Prometheus/Grafana | §2.10 ★ | - | §4.7 |
| 18 — Zipkin Tracing | §2.10 | - | §4.7 |
| 19 — VNPAY | §2.5 (security side) | §3.10 (payment seq) | §4.6 |
| 20 — Testing | - | - | §4.7 + §5.1 ★ |
| 21 — Docker Compose | - | - | §4.3 ★ |

★ = trọng tâm phải viết kỹ.

---

## 5. Tips Viết Báo Cáo

### 5.1. Hình ảnh & sơ đồ
- **Tối thiểu 30–40 hình** cho 1 báo cáo DATN ổn
- Sequence diagram cho mỗi luồng chính
- Component diagram (architecture overview)
- ERD per service
- State machine diagram (order, payment, campaign)
- Screenshot UI/dashboard
- Tools: PlantUML, Mermaid, draw.io, Excalidraw

### 5.2. Số liệu & bảng
- **Tránh viết chay** — mọi pattern phải gắn với số cụ thể trong dự án
- Vd: "Circuit Breaker với failure rate 50%, sliding window 10 requests, mở 10 giây" → cụ thể hơn "có dùng Circuit Breaker"

### 5.3. Trích nguồn
- Mỗi định nghĩa lớn (Microservice, Saga, CAP) → trích sách/RFC/paper
- Format: `[1]`, `[2]` trong text + bibliography ở cuối
- Tối thiểu 30+ tài liệu tham khảo cho 1 báo cáo nghiêm túc

### 5.4. Tránh sai lầm phổ biến
- **Tránh** copy-paste từ blog không trích nguồn → đạo văn
- **Tránh** dùng định nghĩa Wikipedia làm nguồn duy nhất
- **Tránh** viết quá nhiều code trong báo cáo (chỉ snippet quan trọng)
- **Tránh** "em đã làm X" mà không giải thích "vì sao chọn X"

### 5.5. Phòng phản biện
- Đọc kỹ các mục **"Câu hỏi phản biện"** ở cuối mỗi file
- Tự practice trả lời (record video chính mình)
- Hỏi bạn bè giả vờ làm Hội đồng
- Chuẩn bị slide riêng cho từng pattern lớn (Saga, Outbox, Flash sale)

---

## 6. Slide Defense (gợi ý 25–30 slide)

```
1. Title slide
2. Đặt vấn đề & mục tiêu
3. Tổng quan kiến trúc (component diagram)
4-5. Tech stack tổng thể
6. Bounded context — 13 service breakdown
7-8. Saga pattern (sơ đồ + giải thích)
9. Outbox + Idempotency
10. Flash sale concurrency (Redis atomic)
11. State machines (order, payment, campaign)
12. API Gateway: routing + auth + rate limit
13. Keycloak + JWT flow
14. Distributed tracing demo (Zipkin screenshot)
15. Observability dashboards
16. Testing strategy + coverage
17. VNPAY integration flow
18-19. Demo video (3–5 min): place order, flash sale, monitoring
20. Performance results (k6 chart)
21. Resilience test (circuit breaker, kill service)
22. Hạn chế
23. Hướng phát triển
24. Q&A
25. Cảm ơn
```

---

## 7. Checklist Trước Bảo Vệ

- [ ] In báo cáo, đóng quyển (theo yêu cầu trường)
- [ ] Slide đầy đủ, đã practice
- [ ] Demo hệ thống chạy được offline (Docker Compose ready)
- [ ] Backup video demo nếu live demo lỗi
- [ ] Đã đọc kỹ 21 file `.report/` và trả lời được câu hỏi phản biện
- [ ] Chuẩn bị các bằng chứng số liệu (Grafana screenshot, k6 report)
- [ ] Không để placeholder hoặc số liệu chưa đo trong báo cáo chính thức
- [ ] Mặc trang phục formal
- [ ] Đến sớm 30 phút để setup máy

Chúc bạn bảo vệ thành công! 🎓
