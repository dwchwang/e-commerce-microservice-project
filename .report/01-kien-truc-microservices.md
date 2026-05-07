# 01. Kiến Trúc Microservices

## 1. Mục Tiêu Nghiên Cứu

- Định nghĩa được Microservices và phân biệt với Monolith, SOA
- Hiểu các đặc trưng (characteristics) bắt buộc theo Sam Newman/Martin Fowler
- Trình bày được **trade-off** — lợi ích và hệ lụy khi chọn kiến trúc này
- Liên hệ được với 13 service cụ thể của đồ án

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Định nghĩa
**Microservice** (theo Martin Fowler, 2014) là một phong cách kiến trúc phần mềm trong đó ứng dụng được xây dựng bằng tập hợp các *service nhỏ, có thể triển khai độc lập (independently deployable)*, giao tiếp qua các giao thức nhẹ (HTTP/REST, message queue), mỗi service:
- Có **boundary nghiệp vụ** rõ ràng (Bounded Context — DDD)
- **Sở hữu dữ liệu riêng** (database per service)
- **Triển khai độc lập** (CI/CD pipeline riêng)
- **Có thể viết bằng ngôn ngữ/công nghệ khác nhau** (polyglot)

### 2.2. Đặc trưng cốt lõi (Sam Newman, *Building Microservices* 2nd ed.)
1. **Modeled around a business domain** — căn cứ Bounded Context, KHÔNG chia theo tầng kỹ thuật
2. **Owns its own data** — không chia sẻ DB giữa các service
3. **Independently deployable** — deploy 1 service không ảnh hưởng các service khác
4. **Decentralized** — không có DB duy nhất, không có team duy nhất
5. **Cultural/organizational alignment** (Conway's Law)

### 2.3. So sánh Monolith — SOA — Microservice

| Tiêu chí | Monolith | SOA | Microservice |
|----------|----------|-----|-------------|
| Đơn vị triển khai | 1 artifact | Vài service lớn | Nhiều service nhỏ |
| Database | 1 DB chung | Có thể chung | DB riêng mỗi service |
| Communication | In-process call | ESB (Enterprise Service Bus) | Smart endpoints, dumb pipes |
| Coupling | Cao | Trung bình | Thấp |
| Deploy | Tất-cả-hoặc-không | Coordinated | Độc lập |
| Scale | Vertical (toàn app) | Horizontal (theo service) | Fine-grained (theo service) |

### 2.4. Lợi ích (Pros)
- **Independent deployment** — release nhanh, rollback dễ
- **Scale theo nhu cầu** — chỉ scale service bị nghẽn (vd: flash-sale-service)
- **Fault isolation** — 1 service crash không sập cả hệ thống (nếu có Circuit Breaker)
- **Polyglot** — mỗi team chọn công nghệ phù hợp
- **Easier to understand** — service nhỏ, dễ onboard

### 2.5. Hệ lụy (Cons & Fallacies of Distributed Computing — Peter Deutsch)
1. The network is reliable → **không, mạng có thể fail**
2. Latency is zero → **không, mỗi RPC mất ms**
3. Bandwidth is infinite → **không**
4. The network is secure → **không**
5. Topology doesn't change → **không**
6. There is one administrator → **không**
7. Transport cost is zero → **không**
8. The network is homogeneous → **không**

→ Microservice **bắt buộc** phải xử lý: timeout, retry, circuit breaker, idempotency, distributed tracing.

### 2.6. Domain-Driven Design (DDD) — Nền tảng chia service

- **Bounded Context**: Mỗi service ứng với một bounded context của domain
- **Ubiquitous Language**: Trong context đó, mỗi term có nghĩa thống nhất
- **Aggregate**: Đơn vị consistency — cluster of entities được transaction cùng nhau

→ Trong dự án này: `Order` (order-service) và `Inventory` (inventory-service) là 2 aggregate **TÁCH BIỆT**, đồng bộ qua Saga.

---

## 3. So Sánh Với Phương Án Thay Thế

### 3.1. Vì sao KHÔNG dùng Monolith?
- 13 module nghiệp vụ (auth, product, order, payment, ...) sẽ gây code-coupling khủng khiếp
- Một bug trong notification module có thể down toàn bộ hệ thống
- Flash sale cần scale CPU lớn, monolith phải scale tất cả

### 3.2. Vì sao KHÔNG dùng SOA + ESB?
- ESB là single point of failure
- "Smart pipes" (BPMN, transformations) đặt logic ngoài service → khó maintain
- Spring Cloud + Kafka cho phép dumb pipes, smart endpoints (đúng triết lý Microservice)

### 3.3. Vì sao KHÔNG dùng Serverless (FaaS)?
- Yêu cầu stateful (order saga, flash sale Redis counter)
- Cold start latency không phù hợp với flash sale realtime
- Local development khó hơn (cần emulator AWS/GCP)
- Đồ án đại học thường phải demo offline → Docker Compose phù hợp hơn

---

## 4. Cách Áp Dụng Trong Dự Án

### 4.1. 13 Service được chia theo Bounded Context

| Service | Bounded Context | Database | Lý do tách |
|---------|----------------|----------|-----------|
| identity-service | Identity & access | (Keycloak quản lý) | Tách biệt auth khỏi business |
| user-service | User profile | user_db | Profile khác data đăng nhập |
| product-service | Catalog | product_db (+ Redis cache) | CRUD nặng, đọc nhiều |
| inventory-service | Stock | inventory_db | Cần consistency cao, transaction riêng |
| cart-service | Shopping cart | (Redis only) | Ephemeral, không cần persistence lâu dài |
| order-service | Order saga orchestrator | order_db | Là trái tim của business flow |
| payment-service | Payment | payment_db | Tách vì lý do compliance/security |
| voucher-service | Promotion | voucher_db | Logic độc lập với order |
| notification-service | Outbound communication | notification_db | Side-effect, không block main flow |
| review-service | Review/rating | review_db | Đọc nhiều, ghi ít |
| search-service | Read-side projection | (Elasticsearch) | CQRS-lite — phục vụ tìm kiếm |
| content-service | CMS/banner | content_db | Domain riêng cho marketing |
| flash-sale-service | High-concurrency campaign | flash_sale_db (+ Redis) | Atomic ops, cần isolate logic |

### 4.2. Mỗi service có:
- 1 Maven module riêng (`order-service/pom.xml`)
- 1 Spring Boot application class (`OrderServiceApplication`)
- 1 Dockerfile riêng (xem `docker-compose.yml`)
- 1 Database schema riêng (Flyway migrations dưới `src/main/resources/db/migration/`)
- Endpoint Actuator riêng cho health check & metrics

### 4.3. Conway's Law trong dự án
Vì là đồ án 1 người, ranh giới service được vẽ theo *domain* thay vì team. Tuy nhiên trong báo cáo có thể nêu: "*nếu được scale lên team thật, mỗi service ứng với 2-pizza team*".

---

## 5. Từ Khóa Nghiên Cứu

```
- microservices architecture
- bounded context DDD
- monolith vs microservices trade-off
- two-pizza team Conway's law
- fallacies of distributed computing Peter Deutsch
- decomposition pattern Chris Richardson
- microservice characteristics Sam Newman
- when not to use microservices
```

---

## 6. Câu Hỏi Phản Biện Thường Gặp

**Q1: Tại sao em chọn Microservices mà không dùng Monolith cho đồ án?**
→ Trả lời: Vì hệ thống có 13 bounded context khác biệt; flash-sale cần scale độc lập; payment cần isolate compliance; muốn chứng minh kỹ năng xử lý distributed system pattern.

**Q2: Em có biết nhược điểm của Microservices không?**
→ Trả lời: Network unreliability, eventual consistency thay vì ACID, complexity cao (cần tracing/monitoring), DevOps overhead. Em đã giải quyết qua Outbox, Idempotency, Resilience4j, Zipkin.

**Q3: 13 service có phải là quá nhiều cho 1 đồ án không?**
→ Trả lời: Em chia theo bounded context thực, không chia bừa. Mỗi service có ý nghĩa nghiệp vụ rõ ràng. Có thể demo trên 16GB RAM với Docker Compose.

**Q4: Sự khác nhau giữa SOA và Microservice là gì?**
→ Trả lời: SOA dùng ESB (smart pipes) còn Microservice dùng dumb pipes (Kafka làm broker đơn thuần) + smart endpoints. SOA chia theo functionality còn Microservice chia theo bounded context.

**Q5: Em đảm bảo consistency giữa các service như thế nào?**
→ Trả lời: Em dùng Saga Pattern (orchestration) — không dùng 2PC vì không scale. Eventual consistency được đảm bảo qua Kafka events + Outbox + Idempotency.

---

## 7. Tài Liệu Tham Khảo

### Sách (BẮT BUỘC trích trong báo cáo)
- **Sam Newman**, *Building Microservices: Designing Fine-Grained Systems*, 2nd ed., O'Reilly, 2021
- **Chris Richardson**, *Microservices Patterns*, Manning, 2018
- **Eric Evans**, *Domain-Driven Design: Tackling Complexity in the Heart of Software*, Addison-Wesley, 2003
- **Vaughn Vernon**, *Implementing Domain-Driven Design*, Addison-Wesley, 2013

### Bài viết & Web
- Martin Fowler, "Microservices", martinfowler.com (2014) — bài định nghĩa kinh điển
- James Lewis & Martin Fowler, "Microservices Resource Guide"
- Sam Newman, "Microservice Trade-offs"
- microservices.io (Chris Richardson) — kho pattern đầy đủ

### Bài báo hàn lâm
- Peter Deutsch, "The Eight Fallacies of Distributed Computing", 1994
- Lewis Newman, "Microservices: Yesterday, Today, and Tomorrow", *Present and Ulterior Software Engineering*, 2017
