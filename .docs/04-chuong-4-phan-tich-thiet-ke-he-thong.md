# Chuong 4: Phan tich va thiet ke he thong

Muc tieu cua Chuong 4 la chung minh ban **hieu bai toan nghiep vu va thiet ke duoc mot he thong microservices mach lac truoc khi cai dat**. Day la chuong nen co nhieu so do nhat.

Dung luong goi y: **30-40 trang**.

## 4.1. Phan tich yeu cau

### 4.1.1. Yeu cau chuc nang

| Nhom nguoi dung | Chuc nang |
|---|---|
| Khach hang | Dang ky, dang nhap, xem profile, quan ly dia chi |
| Khach hang | Duyet san pham, xem chi tiet, tim kiem, goi y |
| Khach hang | Them/sua/xoa gio hang, ho tro guest cart |
| Khach hang | Dat hang COD/VNPAY, xem don hang, xem trang thai |
| Khach hang | Su dung voucher, mua flash sale |
| Khach hang | Danh gia san pham sau khi co don confirmed |
| Admin | Quan ly product/category/brand |
| Admin | Quan ly inventory, voucher, content banner/blog |
| Admin | Quan ly flash-sale campaign |
| He thong | Gui email, index search, xu ly payment callback, het han reservation |

### 4.1.2. Yeu cau phi chuc nang

| Yeu cau | Cach thiet ke |
|---|---|
| Bao mat | JWT validation tai Gateway, role-based access, trusted identity headers |
| Scalability | Tach service theo bounded context, Kafka cho async processing |
| Availability | Circuit breaker/fallback, scheduler, outbox retry |
| Consistency | Saga, compensation, database constraint, idempotent consumer |
| High concurrency | Redis Lua atomic script cho flash sale |
| Observability | Metrics, logs, traces |
| Deployability | Docker Compose, config profile docker/local |

### So do can ve

- Use Case diagram cho User/Admin/External System.
- Bang traceability: requirement -> service -> API/event -> test case.

### Nguon

- `.guide/09-luong-nghiep-vu.md`
- `.guide/11-api-tham-khao.md`
- cac `*Controller.java`

## 4.2. Actor va Use Case

### Actor

| Actor | Mo ta |
|---|---|
| Guest | Nguoi chua dang nhap, co the xem san pham va thao tac gio hang guest |
| Customer | Nguoi da dang nhap, co the dat hang, thanh toan, review |
| Admin | Quan tri danh muc, san pham, voucher, content, flash sale |
| VNPAY | He thong thanh toan ben ngoai |
| Mailpit/SMTP | He thong email local/demo |
| Scheduler | Tac vu nen tu dong cua he thong |

### Use Case nen ve

- Auth: register, login, refresh, logout.
- Product browsing: list product, product detail, search, suggestion.
- Cart: add/update/remove/clear.
- Order: place order, view order, view status.
- Payment: create VNPAY URL, receive return, receive IPN.
- Voucher: list/reserve/commit/release.
- Flash sale: create campaign, activate, purchase.
- Review: create/update/delete review, product rating.
- Admin content: banner/blog.

## 4.3. Kien truc tong the

### Component view can trinh bay

```text
Client/Postman/Browser
  -> API Gateway :8080
  -> Spring Cloud layer: Eureka :8761, Config Server :8888
  -> Business services :8081-8093
  -> Infrastructure: PostgreSQL, Redis, Kafka, Elasticsearch, Keycloak, Mailpit, Prometheus, Grafana, Zipkin
```

### Bang service ownership

| Service | Port | Ownership | Persistence | Giao tiep |
|---|---:|---|---|---|
| identity-service | 8081 | Auth facade, register/login | Keycloak | Kafka pub |
| user-service | 8082 | Profile/address | PostgreSQL | Kafka sub |
| product-service | 8083 | Product/category/brand | PostgreSQL, Redis cache | Kafka pub |
| inventory-service | 8084 | Stock/reservation | PostgreSQL | Kafka pub/sub |
| cart-service | 8085 | Shopping cart | Redis | REST/Feign |
| order-service | 8086 | Order saga | PostgreSQL | REST + Kafka |
| payment-service | 8087 | Payment/VNPAY | PostgreSQL | REST + Kafka |
| voucher-service | 8088 | Voucher/reservation | PostgreSQL | REST |
| notification-service | 8089 | Email/notification | PostgreSQL | Kafka sub |
| review-service | 8090 | Review/rating | PostgreSQL | REST/Feign |
| search-service | 8091 | Full-text search | Elasticsearch | Kafka sub |
| content-service | 8092 | Banner/blog | PostgreSQL | REST |
| flash-sale-service | 8093 | Campaign/purchase slot | PostgreSQL, Redis | REST + Kafka |

### Nguon

- `.docs/01-phan-tich-kien-truc-microservices.md`
- `.guide/00-tong-quan.md`
- `.guide/05-cac-dich-vu-va-cong.md`
- `README.md`

## 4.4. Thiet ke Bounded Context

### Noi dung can viet

- Giai thich tieu chi tach service: business capability, data ownership, change frequency, scale requirement.
- Khong tach theo technical layer (`controller-service-repository`) ma tach theo domain.
- Mot so quyet dinh thiet ke quan trong:
  - `flash-sale-service` tach khoi `product-service` de co workload high-concurrency rieng.
  - `search-service` tach khoi `product-service` de lam read model Elasticsearch.
  - `cart-service` dung Redis vi du lieu tam thoi.
  - `order-service` lam saga orchestrator vi order la trung tam luong mua hang.
  - `payment-service` tach rieng vi lien quan VNPAY va bao mat thanh toan.

### So do can ve

- Context map: cac bounded context va quan he upstream/downstream.
- Bang ADR rut gon: quyet dinh -> ly do -> trade-off.

## 4.5. Thiet ke du lieu

### 4.5.1. Database per Service

Moi service stateful so huu schema/database rieng. Khong service nao truy van truc tiep DB cua service khac.

### 4.5.2. ERD can ve

Ve rieng tung ERD, khong ep tat ca vao mot ERD khong lo.

| Service | File migration | Diem can chu y |
|---|---|---|
| user-service | `user-service/src/main/resources/db/migration/` | profile, delivery address, processed events |
| product-service | `product-service/src/main/resources/db/migration/` | product, category, brand, images, specifications |
| inventory-service | `inventory-service/src/main/resources/db/migration/` | inventory, stock movement, processed events |
| order-service | `order-service/src/main/resources/db/migration/` | order, order item, outbox, processed events, flash-sale uniqueness |
| payment-service | `payment-service/src/main/resources/db/migration/` | payment, payment_outbox, processed events, unique pending/COD payment |
| voucher-service | `voucher-service/src/main/resources/db/migration/` | voucher, reservation, usage |
| notification-service | `notification-service/src/main/resources/db/migration/` | notification, processed events |
| review-service | `review-service/src/main/resources/db/migration/` | review |
| content-service | `content-service/src/main/resources/db/migration/` | banner, blog post |
| flash-sale-service | `flash-sale-service/src/main/resources/db/migration/` | campaign, sold_count constraint |

### 4.5.3. Redis key model

Can ve bang key model:

- Cart: cart theo user/session.
- Product cache: cache product response.
- Gateway rate limiter: token bucket key theo IP/user.
- Flash sale: stock key va buyer set key.

### 4.5.4. Elasticsearch document

Mo ta `ProductDocument`: product id, sku, name, description, brand, category, price, status, search fields.

### Nguon

- `.report/14-database-per-service-flyway.md`
- `.report/15-redis-cache-distributed-lock.md`
- `.report/16-elasticsearch-fulltext-search.md`
- cac file migration SQL.

## 4.6. Thiet ke API

### Noi dung can viet

- API public qua Gateway co prefix `/api/...`.
- Internal API dung cho service-to-service, vi du `/internal/orders/...`, `/internal/vouchers/...`, `/internal/cart/...`.
- Response format dung `ApiResponse<T>`.
- Error handling dung `BusinessException`, `ResourceNotFoundException`, `GlobalExceptionHandler`.

### Bang endpoint nen co

Thay vi liet ke tat ca endpoint qua dai, chia bang theo service:

| Service | Endpoint nhom chinh | Public/Internal | Vai tro |
|---|---|---|---|
| identity | `/api/auth/register`, `/login`, `/refresh`, `/logout` | Public/Auth | Auth |
| product | `/api/products`, `/categories`, `/brands` | Public/Admin | Product catalog |
| order | `/api/orders`, `/internal/orders` | Auth/Internal | Order saga |
| payment | `/api/payments/vnpay/*` | Auth/Public callback | Payment |
| flash-sale | `/api/flash-sales`, `/purchase` | Public/Auth/Admin | Campaign/purchase |

### Nguon

- `.guide/11-api-tham-khao.md`
- cac `*Controller.java`
- `common/src/main/java/com/ecommerce/common/dto/ApiResponse.java`

## 4.7. Thiet ke Event va Kafka Topics

### Topic catalog can trinh bay

| Topic | Publisher | Consumer | Muc dich |
|---|---|---|---|
| `user-registered` | identity | user | Tao profile sau dang ky |
| `product-created` | product | search | Index product moi |
| `product-updated` | product | search | Cap nhat index |
| `product-deleted` | product | search | Xoa index |
| `order-created` | order | inventory | Reserve stock |
| `inventory-updated` | inventory | order | Bao reserve thanh cong |
| `inventory-failed` | inventory | order | Bao reserve that bai |
| `payment-requested` | order | payment | Tao payment |
| `payment-success` | payment | order | Confirm order |
| `payment-failed` | payment | order | Cancel order |
| `order-confirmed` | order | inventory, notification | Tru ton/gui email |
| `order-cancelled` | order | inventory, notification | Release/gui email |
| `flash-sale-order-requested` | flash-sale | order | Tao don flash sale |

### So do can ve

- Kafka event flow tong the.
- Sequence luong product -> search indexing.
- Sequence luong order saga qua Kafka.

### Nguon

- `.guide/12-kafka-topics.md`
- `common/src/main/java/com/ecommerce/common/event/`
- cac class `*Consumer.java`, `*Producer.java`.

## 4.8. Thiet ke State Machine va Scheduler

### State machine can ve

| Domain | State | Trigger |
|---|---|---|
| Order | `PENDING`, `STOCK_RESERVED`, `CONFIRMED`, `CANCELLED` | inventory/payment events, scheduler |
| Payment | `PENDING`, `COMPLETED`, `FAILED`, `TIMEOUT` | VNPAY callback, timeout scheduler |
| Campaign | `SCHEDULED`, `ACTIVE`, `ENDED`, `CANCELLED` | CampaignScheduler, Admin API |
| Inventory reservation | quantity/reserved_quantity | order-created/order-confirmed/order-cancelled |

### Scheduler can mo ta

| Scheduler | Service | Chu ky | Vai tro |
|---|---|---:|---|
| `OutboxPoller` | order | 1s | Publish outbox event |
| `PaymentEventOutboxPoller` | payment | 1s | Publish payment outbox |
| `ReservationExpiryScheduler` | order | 60s | Huy don qua han reservation |
| `PaymentTimeoutScheduler` | payment | 60s | Timeout payment pending |
| `CampaignScheduler` | flash-sale | 5s | Chuyen trang thai campaign |
| `ReconciliationScheduler` | flash-sale | 300s | Doi soat Redis/DB |

### Nguon

- `.guide/13-state-machines.md`
- `.guide/14-scheduler-jobs.md`
- cac class scheduler.

## 4.9. Thiet ke luong nghiep vu chinh

Moi luong nen co sequence diagram va bang mo ta buoc.

### Luong 1: Dang ky user

Client -> Gateway -> identity-service -> Keycloak -> Kafka `user-registered` -> user-service tao profile.

### Luong 2: Dat hang COD

Client -> Gateway -> order-service -> product/voucher/cart -> DB order + outbox -> Kafka `order-created` -> inventory reserve -> `inventory-updated` -> order -> `payment-requested` -> payment COD completed -> `payment-success` -> order confirmed -> inventory confirm + notification email.

### Luong 3: Dat hang VNPAY

Giong order saga nhung payment-service tao VNPAY URL, cho Return URL/IPN; neu success thi `payment-success`, neu fail/timeout thi `payment-failed` va compensation.

### Luong 4: Flash sale purchase

Client -> Gateway rate limit -> flash-sale-service -> Redis Lua reserve -> Kafka `flash-sale-order-requested` -> order-service tao don -> neu fail thi compensate Redis.

### Luong 5: Search CQRS-lite

Admin cap nhat product -> product-service publish event -> search-service consume -> Elasticsearch index -> client search qua `/api/search`.

## 4.10. Thiet ke bao mat, resilience va observability

### Bao mat

- Gateway validate JWT.
- Forward `X-User-Id`, `X-User-Roles`, `X-User-Email`.
- Backend filter doc header.
- VNPAY verify HMAC.

### Resilience

- Feign fallback.
- Circuit breaker/retry.
- Outbox.
- Idempotent consumer.
- Rate limit flash-sale purchase.

### Observability

- Metrics: Prometheus.
- Logs: traceId/spanId.
- Traces: Zipkin.
- Health/readiness: Actuator.

## Checklist Chuong 4

- [ ] Co Use Case diagram.
- [ ] Co component diagram.
- [ ] Co context map 13 service.
- [ ] Co ERD rieng cho cac service stateful.
- [ ] Co API table.
- [ ] Co Kafka topic flow.
- [ ] Co sequence cho COD, VNPAY, Flash Sale, Search.
- [ ] Co state machine Order/Payment/Campaign.
- [ ] Co security/resilience/observability design.
