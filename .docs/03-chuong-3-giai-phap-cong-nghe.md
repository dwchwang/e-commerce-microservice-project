# Chuong 3: Giai phap cong nghe

Muc tieu cua Chuong 3 la trinh bay **bo giai phap cong nghe duoc lua chon va ly do lua chon**. Neu Chuong 2 tra loi "khai niem la gi", thi Chuong 3 tra loi "vi sao de tai nay chon cong nghe nay va chon nhu the nao".

Chuong nay la noi trinh bay cac cong nghe cu the duoc su dung trong de tai, gom Java 21, Maven multi-module, Spring Boot, Spring Cloud Gateway, Eureka, Config Server, OpenFeign, Kafka, PostgreSQL, Redis, Elasticsearch, Keycloak, VNPAY sandbox, Prometheus, Grafana, Zipkin, Mailpit va Docker Compose. Moi cong nghe nen duoc viet theo cau truc: **vai tro trong he thong -> ly do chon -> cach ap dung trong repo -> han che/rui ro**.

Dung luong goi y: **18-25 trang**.

## 3.1. Dinh huong giai phap tong the

### Noi dung can viet

- He thong backend e-commerce duoc trien khai theo microservices.
- Tat ca request tu client di qua API Gateway.
- Service discovery va config tap trung dung Spring Cloud.
- Business services giao tiep bang REST/OpenFeign va Kafka.
- Du lieu luu theo huong database per service/polyglot persistence.
- Bao mat bang Keycloak/JWT.
- Van hanh bang Docker Compose va quan sat bang Prometheus/Grafana/Zipkin.

### So do can ve

Component diagram "Cong nghe tong the":

```text
Client
  -> Spring Cloud Gateway
  -> Eureka + Config Server
  -> 13 Spring Boot services
  -> PostgreSQL / Redis / Kafka / Elasticsearch / Keycloak / Mailpit
  -> Prometheus / Grafana / Zipkin
```

### Nguon

- `README.md`
- `.guide/00-tong-quan.md`
- `docker-compose.yml`

## 3.2. Java 21, Maven multi-module va Spring Boot 3.5

### Ly do chon

| Cong nghe | Ly do |
|---|---|
| Java 21 | Ban LTS hien dai, phu hop backend enterprise, ho tro tot voi Spring Boot 3.x |
| Maven multi-module | Quan ly nhieu service trong mot repo, chia se parent dependency va module `common` |
| Spring Boot 3.5.13 | Giam boilerplate, auto-configuration, Actuator, ecosystem manh |
| Lombok | Giam code lap lai cho DTO/entity |
| JUnit 5/Testcontainers | Phu hop kiem thu service va dependency that |

### Cach ap dung trong repo

- Parent `pom.xml` khai bao Java 21, Spring Boot `3.5.13`, Spring Cloud `2025.0.0`.
- Modules gom `common`, `discovery-server`, `config-server`, `api-gateway` va 13 business services.
- `common` chua `ApiResponse`, exception va event shared contracts.

### Han che can neu

- Multi-module monorepo de quan ly trong DATN, nhung khi production lon co the tach repo theo team/service.
- Module `common` giup chia se DTO/event, nhung can tranh bien thanh "shared domain model" qua lon lam tang coupling.

## 3.3. Spring Cloud Gateway

### Ly do chon

- Lam single entry point cho client.
- Ho tro reactive Gateway, route predicate/filter chain.
- Tich hop tot voi Eureka qua `lb://service-name`.
- Ho tro security, CORS, rate limiter, Swagger aggregation.

### Cach ap dung trong repo

Gateway route:

| Path | Service | Ghi chu |
|---|---|---|
| `/api/auth/**` | identity-service | Co rate limit 10/s, burst 20 |
| `/api/users/**` | user-service | Profile/address |
| `/api/products/**` | product-service | Product/category/brand |
| `/api/inventory/**` | inventory-service | Ton kho |
| `/api/cart/**` | cart-service | Ho tro guest session |
| `/api/orders/**` | order-service | Co rate limit 10/s, burst 20 |
| `/api/payments/**` | payment-service | VNPAY |
| `/api/vouchers/**` | voucher-service | Voucher |
| `/api/notifications/**` | notification-service | Notification |
| `/api/reviews/**` | review-service | Review |
| `/api/search/**` | search-service | Search |
| `/api/content/**` | content-service | Banner/blog |
| `/api/flash-sales/*/purchase` | flash-sale-service | Rate limit 3/s, burst 5 |
| `/api/flash-sales/**` | flash-sale-service | Campaign API |

### Source can trich

- `api-gateway/src/main/resources/application.yml`
- `api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java`
- `api-gateway/src/main/java/com/ecommerce/gateway/config/RateLimiterConfig.java`
- `api-gateway/src/main/java/com/ecommerce/gateway/filter/AuthHeaderFilter.java`
- `api-gateway/src/main/java/com/ecommerce/gateway/filter/GuestSessionFilter.java`

## 3.4. Eureka Discovery Server va Config Server

### Ly do chon Eureka

- Don gian cho moi truong Spring Cloud.
- Cac service goi nhau bang service name thay vi hard-code host/port.
- Gateway co the route `lb://...`.

### Ly do chon Config Server

- Tap trung config theo service/profile.
- Giam trung lap `application.yml` giua local/docker.
- Phu hop voi docker profile va environment variables.

### Cach ap dung

- `discovery-server` chay cong `8761`.
- `config-server` chay cong `8888`, dung profile `native,docker`.
- Config service nam trong `config-server/src/main/resources/configs`.

### So do can ve

- Service startup flow: service -> Config Server -> Eureka -> Gateway/Feign discovery.

## 3.5. REST/OpenFeign va Kafka

### Ly do ket hop hai cach giao tiep

| Kieu giao tiep | Dung khi | Vi du trong he thong |
|---|---|---|
| REST/OpenFeign | Can response ngay, truy van context nho, validate dong bo | order -> product, order -> voucher, payment -> order, review -> order |
| Kafka event | Can decouple, xu ly bat dong bo, side effect, eventual consistency | order-created, payment-success, product-updated, user-registered, flash-sale-order-requested |

### Cong nghe duoc chon

- OpenFeign cho declarative HTTP client.
- Kafka KRaft qua image `confluentinc/cp-kafka:8.2.0`.
- Event classes dat trong `common/src/main/java/com/ecommerce/common/event`.

### So do can ve

- Dependency graph REST/Feign.
- Kafka topic flow.

### Han che can neu

- REST sync co nguy co cascading failure nen can timeout/circuit breaker/fallback.
- Kafka at-least-once can idempotent consumer.
- Event schema trong `common` de tien cho DATN, nhung production co the can schema registry/versioning.

## 3.6. PostgreSQL, Redis va Elasticsearch

### PostgreSQL

Ly do chon:

- RDBMS on dinh, ACID local transaction.
- Phu hop order/payment/inventory/voucher.
- Ho tro constraint/index de bao ve integrity.
- Flyway migration de version schema.

Ap dung:

- Docker image `postgres:17-alpine`.
- Database rieng cho cac service stateful.
- Flyway migration trong `src/main/resources/db/migration`.

### Redis

Ly do chon:

- Latency thap, phu hop cache/cart/session/rate limit.
- Atomic command va Lua script phu hop flash-sale stock reservation.

Ap dung:

- `cart-service` luu gio hang bang Redis.
- `product-service` dung Redis cache.
- Gateway rate limiter dung Redis.
- `flash-sale-service` dung Redis key stock va buyer set.

### Elasticsearch

Ly do chon:

- Full-text search tot hon SQL `LIKE`.
- Inverted index, analyzer, scoring.
- Tach read model cho search-service.

Ap dung:

- `search-service` consume `product-created`, `product-updated`, `product-deleted` de index document.

### Bang nen co

| Service | Persistence chinh | Ly do |
|---|---|---|
| user/product/inventory/order/payment/voucher/notification/review/content/flash-sale | PostgreSQL | Du lieu nghiep vu can durable |
| cart | Redis | Du lieu tam thoi, thao tac nhanh |
| search | Elasticsearch | Search document/read model |
| identity | Keycloak DB | Auth/user credential do Keycloak quan ly |

## 3.7. Keycloak va JWT

### Ly do chon

- Tranh tu xay auth server.
- Ho tro realm/client/role/user/token.
- Phu hop OAuth2/OIDC va JWT.
- Co Admin REST API de `identity-service` tao user/assign role.

### Cach ap dung

- Keycloak chay port host `8180`, trong Docker la `keycloak:8080`.
- Realm `ecommerce`.
- Gateway validate issuer/JWKS.
- `identity-service` goi Keycloak Admin API.

### Diem can canh bao trong bao cao

- Trusted header chi an toan neu backend services khong bi expose truc tiep ra public.
- Production can HTTPS, secret management, token rotation, network policy.

## 3.8. VNPAY sandbox

### Ly do chon

- Gan voi boi canh Viet Nam.
- Co sandbox de demo khong dung tien that.
- Minh hoa payment gateway integration va signature verification.

### Cach ap dung

- `payment-service` tao payment URL.
- `VnPayUtil` ky/verify HMAC-SHA512.
- Payment callback gom Return URL va IPN.
- Payment timeout xu ly bang scheduler.

### Source can trich

- `payment-service/src/main/java/com/ecommerce/payment/util/VnPayUtil.java`
- `payment-service/src/main/java/com/ecommerce/payment/service/impl/VnPayServiceImpl.java`
- `payment-service/src/main/java/com/ecommerce/payment/controller/PaymentController.java`

## 3.9. Observability stack

### Ly do chon

| Cong nghe | Vai tro |
|---|---|
| Spring Boot Actuator | Health/readiness/metrics endpoints |
| Micrometer | Metric facade va tracing integration |
| Prometheus | Scrape va luu metrics |
| Grafana | Dashboard truc quan |
| Zipkin | Distributed tracing |
| Mailpit | Demo email local |

### Cach ap dung

- Prometheus scrape Gateway va cac service qua `/actuator/prometheus`.
- Grafana provision 3 dashboard: Spring Boot Overview, JVM Overview, E-commerce Saga Overview.
- Zipkin nhan span tu API Gateway va business services.
- Log pattern co `traceId` va `spanId`.

### Screenshot can chup

- Prometheus Targets UP.
- Grafana dashboard.
- Zipkin trace cua luong dat hang.
- Mailpit email order confirmed/cancelled.

## 3.10. Docker Compose

### Ly do chon

- Phu hop DATN va demo local.
- Co the dong goi toan bo dependency.
- De khoi dong lai moi truong nhat quan.

### Cach ap dung

`docker-compose.yml` khai bao:

- PostgreSQL, Keycloak DB.
- Redis.
- Kafka KRaft.
- Elasticsearch.
- Keycloak.
- Mailpit.
- Zipkin.
- Prometheus.
- Grafana.
- Discovery Server, Config Server, API Gateway.
- 13 business services.

### Han che

- Single-host, chua co autoscaling.
- Chua co service mesh/network policy nhu Kubernetes.
- Secret trong `.env` phu hop local, production can secret manager.

## Checklist Chuong 3

- [ ] Moi cong nghe deu co "ly do chon".
- [ ] Co bang stack tong the.
- [ ] Co so do cong nghe tong the.
- [ ] Co muc "han che" cho cac lua chon quan trong.
- [ ] Khong lap lai qua nhieu ly thuyet cua Chuong 2.
- [ ] Khong di qua sau vao code nhu Chuong 5.
