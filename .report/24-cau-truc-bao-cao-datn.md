# 24. Cau Truc Bao Cao DATN Va Cach Map Chu De Vao Chuong

## 1. Muc Dich

File nay huong dan map bo tai lieu `.report` vao bao cao DATN theo cau truc **6 chuong** hien dang dung trong `.docs`.

Nguyen tac quan trong:
- Chi viet "da trien khai" khi co source code, config, workflow, screenshot, log hoac raw test artifact.
- So lieu hieu nang/resilience chi copy tu [23-bang-chung-kiem-thu-va-so-lieu.md](./23-bang-chung-kiem-thu-va-so-lieu.md).
- Cac scenario chua du bang chung phai ghi trung thuc: Kafka outbox replay chua verify, Redis cart degradation inconclusive, Grafana/Zipkin screenshot con can capture.
- Admin FE la phan con lai can hoan thien neu chua co UI/screenshot day du.

## 2. Cau Truc Bao Cao 6 Chuong

```text
Loi cam on
Loi cam doan
Tom tat tieng Viet
Abstract tieng Anh
Muc luc
Danh muc hinh ve
Danh muc bang bieu
Danh muc tu viet tat

CHUONG 1: TONG QUAN DE TAI                         10-15 trang
CHUONG 2: CO SO LY THUYET                          30-40 trang
CHUONG 3: GIAI PHAP CONG NGHE                      18-25 trang
CHUONG 4: PHAN TICH VA THIET KE HE THONG           30-40 trang
CHUONG 5: CAI DAT VA TRIEN KHAI                    25-30 trang
CHUONG 6: KIEM THU VA DANH GIA                     15-20 trang

Ket luan va huong phat trien
Tai lieu tham khao
Phu luc
```

Tong dung luong hop ly: 130-170 trang neu tinh ca phu luc; phan chinh nen giu khoang 120-150 trang.

## 3. Diem Dong Gop Nen Lam Noi Bat

| Nhom | Dong gop |
|---|---|
| Kien truc | 13 business microservice theo bounded context, sau API Gateway, Eureka va Config Server |
| Du lieu | Database per Service, PostgreSQL logical databases, Redis cho cart/cache/rate-limit/flash-sale, Elasticsearch cho search |
| Giao tiep | REST/OpenFeign cho sync call, Kafka cho event-driven flow |
| Transaction | Saga choreography/orchestration trong order flow, Outbox va Idempotent Consumer |
| High concurrency | Flash-sale Redis Lua atomic reservation, buyer set, compensation, reconciliation |
| Security | Keycloak/JWT, RBAC, Gateway trusted headers, Redis rate limiting, VNPAY HMAC |
| Observability | Actuator, Prometheus, Grafana, Zipkin, trace/log correlation |
| Deployment | Docker Compose local va AWS EC2 production-like single-host |
| CI/CD | GitHub Actions build/push GHCR va deploy manual len EC2 |
| Frontend | Next.js storefront; admin panel la phan con lai neu chua hoan thien |
| Evaluation | Smoke test 9/9 suites pass; k6 catalog/checkout/flash-sale co so lieu that |

## 4. Map Nhanh Chuong -> Nguon

| Chuong | Noi dung | Nguon chinh |
|---|---|---|
| 1 | Bai toan, muc tieu, pham vi, dong gop | `00`, `01`, `22`, `.docs/01-chuong-1-tong-quan-de-tai.md` |
| 2 | Pattern va nguyen ly nen tang | `01`-`21`, uu tien `09`-`14`, `17`-`20` |
| 3 | Ly do chon Java/Spring/Kafka/Redis/PostgreSQL/Elasticsearch/Keycloak/Docker/Next.js/AWS/GitHub Actions | `02`-`08`, `14`-`21`, `22`, `.docs/03-chuong-3-giai-phap-cong-nghe.md` |
| 4 | Yeu cau, actor, bounded context, API, DB, event, state machine, sequence | `01`, `08`-`14`, `.docs/04-chuong-4-phan-tich-thiet-ke-he-thong.md`, `.guide/` |
| 5 | Code, Docker Compose, AWS, CI/CD, frontend, service implementation | `05`, `10`, `12`-`14`, `17`-`19`, `21`, `22`, `.docs/05-chuong-5-cai-dat-trien-khai.md` |
| 6 | Smoke test, unit/integration, k6, resilience, security, limitations | `20`, `22`, `23`, `.docs/06-chuong-6-kiem-thu-danh-gia.md`, `.test/results/` |

## 5. Chuong 1 - Tong Quan De Tai

### Noi dung nen co

1. Dat van de: e-commerce co traffic spike, flash sale, thanh toan, ton kho, search, user session.
2. Gioi han cua monolith khi can scale rieng flash-sale/search/order.
3. Muc tieu: xay dung he thong e-commerce microservices bang Spring Boot/Spring Cloud.
4. Pham vi: backend, storefront FE, AWS/CI/CD, testing; admin FE ghi theo trang thai thuc te.
5. Dong gop ky thuat: Saga, Outbox, Idempotency, Redis Lua flash-sale, observability, k6 evaluation.
6. Bo cuc bao cao 6 chuong.

### Nen trich tu

- [00-LOTRINH-NGHIEN-CUU.md](./00-LOTRINH-NGHIEN-CUU.md)
- [01-kien-truc-microservices.md](./01-kien-truc-microservices.md)
- [22-phase-10-14-tong-hop-thuc-nghiem.md](./22-phase-10-14-tong-hop-thuc-nghiem.md)

## 6. Chuong 2 - Co So Ly Thuyet

Chuong nay nen viet theo **pattern/nguyen ly**, khong bien thanh catalog cong nghe.

| Muc | Noi dung | File `.report` |
|---|---|---|
| 2.1 | Microservices, bounded context, loi ich va trade-off | `01` |
| 2.2 | API Gateway, Service Discovery, Centralized Config | `03`, `04`, `05` |
| 2.3 | Security: OAuth2, OIDC, JWT, RBAC, trusted subsystem | `06` |
| 2.4 | Sync/async communication, REST, OpenFeign, Kafka | `07`, `08` |
| 2.5 | Distributed transaction, CAP/PACELC, Saga | `09` |
| 2.6 | Outbox, Idempotent Consumer, delivery semantics | `10` |
| 2.7 | Circuit Breaker, Retry, Rate Limiter, resilience | `11` |
| 2.8 | State Machine, Scheduler, Reconciliation | `12` |
| 2.9 | High concurrency, race condition, Redis atomic/Lua | `13`, `15` |
| 2.10 | Database per Service, Flyway, polyglot persistence | `14`, `16` |
| 2.11 | Observability: metrics, tracing, logging | `17`, `18` |
| 2.12 | Testing microservices va deployment/containerization | `20`, `21` |

## 7. Chuong 3 - Giai Phap Cong Nghe

Muc tieu la tra loi "vi sao chon stack nay".

| Cong nghe | Ly do chon | Ap dung trong du an |
|---|---|---|
| Java 21 + Spring Boot 3.5 | Mature ecosystem, dependency injection, Actuator, testing support | Tat ca backend service |
| Spring Cloud Gateway/Eureka/Config | Giai quyet gateway, discovery va config trong microservice | `api-gateway`, `discovery-server`, `config-server` |
| Kafka | Decouple event-driven flow, at-least-once delivery | order/payment/inventory/notification/search/flash-sale |
| PostgreSQL + Flyway | ACID, schema versioning, phu hop order/payment | database per service |
| Redis | Cache/cart/rate-limit/atomic counter | product cache, cart, gateway rate limiter, flash-sale |
| Elasticsearch | Full-text search va suggestion | `search-service` |
| Keycloak | Open-source IdP, JWT/OIDC, role management | identity/auth |
| Docker Compose | Phu hop DATN single-host, reproduce local/AWS | local va production-like EC2 |
| GitHub Actions + GHCR | CI/CD don gian, reproducible image build | Phase 11 |
| Next.js | Storefront SSR/RSC/BFF, deploy cung backend | Phase 12 |

## 8. Chuong 4 - Phan Tich Va Thiet Ke He Thong

### Cac muc nen co

1. Yeu cau chuc nang va phi chuc nang.
2. Actor: Customer, Admin, Payment Gateway, Scheduler, Email/Notification.
3. Use case: auth, catalog, cart, checkout, VNPAY, review, flash-sale, admin management.
4. Kien truc tong the: Client -> Gateway -> Spring Cloud -> Business services -> Data/Infra.
5. Bounded context va bang ownership 13 service.
6. Thiet ke database/Redis/Elasticsearch.
7. Thiet ke API public/internal/admin.
8. Kafka topics va event schema.
9. State machine: Order, Payment, Flash-sale Campaign.
10. Sequence diagram: register, COD order, VNPAY, flash-sale, search indexing, compensation.
11. Thiet ke security/resilience/observability.

### Sơ đồ bắt buộc nên có

| Nhom | Hinh |
|---|---|
| Tong quan | Component diagram, deployment diagram Docker/AWS |
| Domain | Context map 13 service, service ownership table |
| Data | ERD per service DB, Redis key model, Elasticsearch document |
| API/Event | Gateway route diagram, Kafka flow |
| Business flow | Order Saga, VNPAY, flash-sale purchase, search CQRS |
| State | Order/Payment/Campaign state machine |
| Ops | Observability flow, CI/CD pipeline |

## 9. Chuong 5 - Cai Dat Va Trien Khai

Chuong nay chung minh thiet ke da thanh code/config/runbook.

### Cau truc goi y

1. Moi truong va tech stack.
2. Maven multi-module project structure.
3. Docker Compose infrastructure: Postgres, Redis, Kafka, Elasticsearch, Keycloak, Mailpit, Prometheus, Grafana, Zipkin.
4. Discovery Server va Config Server.
5. API Gateway: route, JWT, CORS, rate limit, trusted headers, guest session.
6. Business services:
   - order-service: Saga, outbox, scheduler, consumers.
   - payment-service: COD/VNPAY, payment outbox, timeout scheduler.
   - inventory-service: stock/reserved counter, compensation events.
   - flash-sale-service: Redis Lua, campaign scheduler, reconciliation.
   - search-service: Kafka consumer -> Elasticsearch.
   - cac service con lai viet bang ngan.
7. Database/Flyway va schema validation.
8. Observability setup.
9. AWS deployment Phase 10: EC2, production compose override, Caddy/HTTPS, CORS env-driven.
10. CI/CD Phase 11: build/push/deploy workflows.
11. Frontend Phase 12: Next.js storefront, BFF/proxy, deploy chung container.
12. Admin FE Phase 14: chi dua vao day neu da co UI/screenshot; neu chua thi de vao huong phat trien.

### Bang code minh chung nen co

| Pattern/Feature | Source minh chung |
|---|---|
| Outbox order | `order-service/src/main/java/com/ecommerce/order/service/OutboxService.java`, `OutboxPoller.java` |
| Payment outbox | `payment-service/src/main/java/com/ecommerce/payment/kafka/PaymentEventOutboxPoller.java` |
| Flash-sale Lua | `flash-sale-service/src/main/java/com/ecommerce/flashsale/service/impl/FlashSaleServiceImpl.java` |
| Campaign/Reconciliation scheduler | `flash-sale-service/src/main/java/com/ecommerce/flashsale/scheduler/` |
| Gateway rate limit | `api-gateway/src/main/resources/application.yml` |
| CORS env-driven | `config-server/src/main/resources/configs/api-gateway.yml` |
| Flyway validate | `config-server/src/main/resources/configs/*-service.yml` |
| CI/CD | `.github/workflows/build-and-push.yml`, `.github/workflows/deploy.yml` |

## 10. Chuong 6 - Kiem Thu Va Danh Gia

### 10.1. Noi dung chinh

1. Chien luoc kiem thu: unit, integration, smoke/E2E, security, performance, resilience.
2. Thong ke test tu dong trong repo.
3. Backend readiness smoke test: 9 split suites pass.
4. Test case chuc nang: auth, catalog, cart, order, VNPAY, review, flash-sale.
5. Security test: 401/403/duplicate rejection/rate limit.
6. Performance test: catalog soak, checkout stress, flash-sale spike.
7. Resilience test: kill order-service, inventory compensation, Kafka/Redis inconclusive notes.
8. Danh gia muc tieu ban dau vs ket qua.
9. Han che va rui ro con lai.

### 10.2. So lieu co the copy

Chi copy tu [23](./23-bang-chung-kiem-thu-va-so-lieu.md). Cac metric chinh:

| Scenario | Ket qua nen dua vao |
|---|---|
| Backend readiness | 9/9 split suites pass sau khi fix bug/test infra |
| Catalog soak | 34 phut, max 200 VU, p95 61.68 ms, p99 85.48 ms, error 0.00% |
| Checkout stress | 7 phut, max 50 VU, order success 100.00%, p95 160.92 ms, error 0.00% |
| Flash-sale spike | 500 VU, 100 stock, 100 purchase success, 100 confirmed orders, duplicate buyer 0 |
| Kill order-service | Downtime co chu dich gay fail, sau restart health 200 |
| Inventory compensation | Order inventory-failed chuyen `CANCELLED`, stock khong treo |

### 10.3. Cac ket luan chua du bang chung

| Muc | Trang thai |
|---|---|
| Kafka outbox replay khi Kafka down | Attempted, chua verify replay |
| Redis cart degradation khi Redis down | Inconclusive vi token expired 401 |
| Grafana/Zipkin screenshots | Pending capture |
| Admin panel | Chi ghi la hoan thien neu co UI/screenshot/demo |

## 11. Ket Luan Va Huong Phat Trien

### Ket qua dat duoc

- Xay dung duoc he thong e-commerce microservices voi Spring Boot/Spring Cloud.
- Trien khai cac pattern quan trong: Saga, Outbox, Idempotency, Database per Service, Redis atomic flash-sale, scheduler/reconciliation.
- Co AWS deployment, CI/CD va storefront FE theo phase 10-12.
- Co smoke test va performance/resilience artifact that cho Chuong 6.

### Han che nen viet trung thuc

- Single-host Docker Compose, chua co Kubernetes/autoscaling.
- Chua co schema registry/contract test day du cho Kafka event.
- Kafka outbox replay resilience scenario can retest.
- Grafana/Zipkin screenshot can capture truoc khi nop.
- Admin FE con can hoan thien neu chua co UI day du.

### Huong phat trien

- Hoan thien Admin Panel.
- Kubernetes/Helm, autoscaling va rolling deployment.
- Contract testing/Pact, schema registry cho Kafka.
- Dedicated analytics/reporting service.
- S3/object storage cho product image.
- Chaos test co he thong va long-running soak test.

## 12. Slide Defense Goi Y

| Slide | Noi dung |
|---|---|
| 1 | Ten de tai, tac gia, GVHD |
| 2 | Bai toan va muc tieu |
| 3 | Kien truc tong the |
| 4 | Tech stack |
| 5 | 13 bounded contexts |
| 6 | API Gateway + security |
| 7 | Order Saga |
| 8 | Outbox + Idempotency |
| 9 | Flash-sale Redis Lua |
| 10 | State machine + scheduler |
| 11 | Database per Service |
| 12 | Observability |
| 13 | AWS deployment |
| 14 | CI/CD |
| 15 | Storefront demo |
| 16 | Backend readiness test |
| 17 | k6 performance results |
| 18 | Flash-sale result: 100/100, duplicate 0 |
| 19 | Resilience results va han che |
| 20 | Ket luan va huong phat trien |

## 13. Checklist Truoc Khi Nop

- [ ] Bao cao dung 6 chuong.
- [ ] Khong con placeholder `X ms`, `TODO`, `...`.
- [ ] Moi so lieu co artifact trong `.test/results`.
- [ ] Co screenshot Docker/Eureka/Gateway/Keycloak/Grafana/Zipkin/VNPAY/FE.
- [ ] Co sequence diagram cho Order Saga, VNPAY va Flash-sale.
- [ ] Co ERD/Redis key/Elasticsearch document model.
- [ ] Co bang service ownership va Kafka topic catalog.
- [ ] Co phan han che trung thuc ve single-host, Kafka replay, admin FE.
- [ ] Tai lieu tham khao uu tien sach, paper, RFC, official docs.
