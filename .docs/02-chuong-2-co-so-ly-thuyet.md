# Chuong 2: Co so ly thuyet

Muc tieu cua Chuong 2 la chung minh ban nam nen tang ly thuyet dung de thiet ke he thong. Chuong nay nen viet theo huong **khai niem -> van de giai quyet -> uu/nhuoc diem -> lien he voi de tai**.

Dung luong goi y: **30-40 trang**.

## Nguyen tac viet Chuong 2

- Moi khai niem lon phai co tai lieu tham khao: sach, paper, RFC, official docs.
- Khong bien Chuong 2 thanh ban dich tai lieu. Moi muc nen co doan "Lien he voi de tai".
- Khong di qua sau vao file code; code chi de minh hoa rang ly thuyet nay se duoc ap dung o Chuong 4/5.
- Moi muc lon nen co it nhat 1 so do/bang.

## 2.1. Kien truc Microservices

### Noi dung can nghien cuu

- Dinh nghia microservice theo Martin Fowler va Sam Newman.
- Dac trung: modeled around domain, independently deployable, owns its data, decentralized governance, automation.
- So sanh Monolith, SOA va Microservices.
- Uu diem: scale doc lap, deploy doc lap, technology heterogeneity, fault isolation.
- Nhuoc diem: distributed complexity, network latency, data consistency, observability, testing.
- 8 Fallacies of Distributed Computing.
- Domain-Driven Design: bounded context, aggregate, ubiquitous language.

### Lien he voi de tai

He thong tach thanh 13 business service theo mien nghiep vu, moi service so huu logic va du lieu rieng. Cac service chi giao tiep qua API/event, khong truy cap truc tiep database cua nhau.

### So do/bang can co

- Bang so sanh Monolith/SOA/Microservices.
- Context map tong quat 13 bounded context.

### Nguon

- `.report/01-kien-truc-microservices.md`
- `README.md`

## 2.2. Spring Boot va Spring Cloud

### Noi dung can nghien cuu

- Spring Boot: auto-configuration, starter dependencies, actuator, embedded server.
- Spring Cloud: Gateway, Eureka, Config Server, OpenFeign, LoadBalancer, Circuit Breaker integration.
- Java 21 va Jakarta EE trong Spring Boot 3.x.
- Maven multi-module cho project lon.

### Lien he voi de tai

Repo dung parent `pom.xml` voi Java 21, Spring Boot `3.5.13`, Spring Cloud `2025.0.0` va nhieu module Maven.

### So do/bang can co

- Bang tech stack Spring.
- So do Maven multi-module.

### Nguon

- `.report/02-spring-boot-spring-cloud.md`
- `pom.xml`

## 2.3. Service Discovery va Centralized Configuration

### Noi dung can nghien cuu

- Van de service location trong moi truong dynamic.
- Client-side discovery vs server-side discovery.
- Eureka: registry, heartbeat, self-preservation, client lookup.
- Config Server: externalized configuration, profile, native/git backend.
- Lien he Twelve-Factor App.

### Lien he voi de tai

`discovery-server` chay Eureka; `config-server` chay native profile va luu config trong `config-server/src/main/resources/configs`. Gateway va Feign dung service name nhu `lb://order-service`.

### So do/bang can co

- Sequence dang ky service vao Eureka.
- So do service lay config tu Config Server.

### Nguon

- `.report/03-service-discovery-eureka.md`
- `.report/04-config-server.md`
- `discovery-server/`
- `config-server/`

## 2.4. API Gateway Pattern

### Noi dung can nghien cuu

- API Gateway la single entry point.
- Routing, authentication, authorization, rate limiting, CORS, aggregation.
- Spring Cloud Gateway: route predicate, filter chain, WebFlux/Reactor.
- Gateway voi service discovery.
- Public routes vs authenticated routes.

### Lien he voi de tai

`api-gateway` route den 13 service, validate JWT tu Keycloak, forward identity headers va cau hinh Redis rate limiter cho auth/order/flash-sale purchase.

### So do/bang can co

- Bang route Gateway: path -> service -> auth/rate-limit.
- So do filter chain Gateway.

### Nguon

- `.report/05-api-gateway.md`
- `.guide/15-gateway-security.md`
- `api-gateway/src/main/resources/application.yml`
- `api-gateway/src/main/java/com/ecommerce/gateway/`

## 2.5. Bao mat: OAuth2, OIDC, JWT va Keycloak

### Noi dung can nghien cuu

- Authentication vs authorization.
- OAuth 2.0 roles: resource owner, client, authorization server, resource server.
- OpenID Connect va ID Token.
- JWT: header, payload, signature, claim, exp, issuer, audience.
- JWKS va signature verification.
- Keycloak: realm, client, role, user, service account.
- Trusted Subsystem Pattern: Gateway validate token, backend tin header noi bo.

### Lien he voi de tai

Keycloak lam Identity Provider; Gateway la OAuth2 Resource Server; backend services doc `X-User-Id`, `X-User-Roles`, `X-User-Email` tu Gateway.

### So do/bang can co

- Sequence dang nhap/lay token.
- Sequence request co JWT qua Gateway den backend.
- Bang public/authenticated/admin endpoints.

### Nguon

- `.report/06-bao-mat-keycloak-oauth-jwt.md`
- `.guide/06-keycloak-setup.md`
- `.guide/15-gateway-security.md`
- `keycloak/realm-export.json`
- `api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java`
- `api-gateway/src/main/java/com/ecommerce/gateway/filter/AuthHeaderFilter.java`

## 2.6. Giao tiep giua microservices

### REST/OpenFeign

- REST constraint, resource, status code.
- OpenFeign declarative client.
- Khi nao nen dung sync call: validate nhanh, query context can response ngay.
- Rui ro: latency, cascading failure, timeout.

### Kafka/Event-driven

- Kafka topic, partition, broker, consumer group, offset.
- Pub/sub va event notification.
- At-least-once delivery va yeu cau idempotency.
- KRaft mode thay cho Zookeeper.

### Lien he voi de tai

REST/OpenFeign dung cho product validation, voucher reserve, payment context, review eligibility. Kafka dung cho user-registered, product indexing, order saga, inventory, payment, notification va flash-sale order request.

### So do/bang can co

- REST dependency graph.
- Kafka topic flow.
- Bang sync vs async trong du an.

### Nguon

- `.report/07-giao-tiep-rest-openfeign.md`
- `.report/08-event-driven-kafka.md`
- `.guide/12-kafka-topics.md`
- `common/src/main/java/com/ecommerce/common/event/`

## 2.7. Distributed Transaction va Saga Pattern

### Noi dung can nghien cuu

- Vi sao transaction ACID cuc bo khong du khi moi service so huu DB rieng.
- 2PC/XA va han che trong he microservices.
- CAP theorem va PACELC.
- Saga Pattern: sequence local transactions + compensating actions.
- Choreography vs Orchestration.

### Lien he voi de tai

`order-service` dieu phoi order saga: tao don, reserve inventory, tao payment, confirm/cancel, release inventory/voucher khi loi.

### So do/bang can co

- Sequence Order Saga COD.
- Sequence Order Saga VNPAY.
- Bang choreography vs orchestration.

### Nguon

- `.report/09-saga-pattern-distributed-transaction.md`
- `.guide/09-luong-nghiep-vu.md`
- `.guide/13-state-machines.md`

## 2.8. Reliability Patterns

### Transactional Outbox

- Van de dual-write: ghi DB thanh cong nhung publish event that bai.
- Outbox: ghi business data va event vao cung transaction DB.
- Poller doc outbox va publish Kafka.

### Idempotent Consumer

- Kafka co the deliver trung trong at-least-once.
- Consumer can luu event da xu ly bang `processed_events`.
- Muc tieu la exactly-once effect o business layer.

### Circuit Breaker, Retry, Rate Limiter

- Circuit states: CLOSED, OPEN, HALF_OPEN.
- Retry can gioi han de tranh lam he loi nang hon.
- Rate limiter token bucket cho endpoint nhay cam.

### Nguon

- `.report/10-outbox-idempotency.md`
- `.report/11-resilience-circuit-breaker-rate-limit.md`
- `.guide/15-gateway-security.md`

## 2.9. Persistence Patterns

### Noi dung can nghien cuu

- Database per Service.
- Polyglot Persistence.
- Flyway migration.
- Redis: cache, session/cart, rate limiter, atomic counter, Lua script.
- Elasticsearch: inverted index, analyzer, BM25, document model.
- CQRS-lite: product-service lam command side, search-service lam query side.

### Lien he voi de tai

PostgreSQL luu du lieu nghiep vu, Redis luu cart/cache/rate-limit/flash-sale stock, Elasticsearch luu product document phuc vu search.

### So do/bang can co

- Bang service -> persistence.
- ERD per service.
- Redis key model cho cart va flash-sale.
- Elasticsearch product document model.

### Nguon

- `.report/14-database-per-service-flyway.md`
- `.report/15-redis-cache-distributed-lock.md`
- `.report/16-elasticsearch-fulltext-search.md`
- cac file `V1__create_*_tables.sql`

## 2.10. Observability

### Noi dung can nghien cuu

- Monitoring vs observability.
- 3 tru cot: metrics, logs, traces.
- RED method va USE method.
- Micrometer, Actuator, Prometheus pull model, PromQL.
- Grafana dashboard.
- Distributed tracing: trace, span, context propagation, W3C Trace Context, Zipkin.
- Log correlation bang traceId/spanId.

### Lien he voi de tai

Tat ca service co Actuator/Prometheus registry; Prometheus scrape `/actuator/prometheus`; Grafana provision dashboard; Zipkin nhan span tu Gateway va service.

### Nguon

- `.report/17-observability-prometheus-grafana.md`
- `.report/18-distributed-tracing-zipkin.md`
- `prometheus/prometheus.yml`
- `grafana/`

## 2.11. State Machine, Scheduler va High Concurrency

### Noi dung can nghien cuu

- Finite State Machine trong domain.
- Trang thai don hang, thanh toan, campaign.
- Scheduler cho timeout, campaign activation, reconciliation, outbox polling.
- Race condition va oversell.
- Redis Lua atomicity.
- Reconciliation giua Redis va DB.

### Lien he voi de tai

Order, Payment va Flash-sale Campaign deu co state machine. Flash sale dung Redis Lua de reserve slot atomic va scheduler de chuyen trang thai/doi soat.

### Nguon

- `.report/12-state-machine-scheduler.md`
- `.report/13-flash-sale-concurrency.md`
- `.guide/13-state-machines.md`
- `.guide/14-scheduler-jobs.md`

## 2.12. Docker va Container Deployment

### Noi dung can nghien cuu

- Container vs VM.
- Docker image, layer, Dockerfile.
- Docker Compose service, network, volume, healthcheck, depends_on.
- Single-host deployment va gioi han so voi Kubernetes.

### Lien he voi de tai

He thong chay local bang `docker-compose.yml`, co PostgreSQL, Keycloak DB, Redis, Kafka, Elasticsearch, Keycloak, Zipkin, Prometheus, Grafana, Mailpit va cac Spring service.

### Nguon

- `.report/21-docker-container-deployment.md`
- `.guide/03-build-va-chay-docker.md`
- `docker-compose.yml`

## Checklist Chuong 2

- [ ] Co citation cho cac khai niem lon.
- [ ] Co bang/so do cho moi nhom ly thuyet.
- [ ] Co doan lien he voi de tai sau moi muc.
- [ ] Khong copy nguyen van qua dai tu `.report`.
- [ ] Khong bien Chuong 2 thanh Chuong 5 cai dat.
