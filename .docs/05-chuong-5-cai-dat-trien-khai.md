# Chuong 5: Cai dat va trien khai

Muc tieu cua Chuong 5 la chung minh thiet ke o Chuong 4 da duoc **hien thuc bang code, cau hinh va moi truong chay cu the**.

Dung luong goi y: **25-30 trang**.

## 5.1. Moi truong va cong nghe su dung

### Bang tech stack

| Nhom | Cong nghe/phien ban trong repo |
|---|---|
| Language/runtime | Java 21 |
| Build | Maven Wrapper, Maven multi-module |
| Framework | Spring Boot 3.5.13, Spring Cloud 2025.0.0 |
| Gateway/Discovery/Config | Spring Cloud Gateway, Eureka, Config Server |
| Security | Spring Security OAuth2 Resource Server, Keycloak 26.6.1 |
| Database | PostgreSQL 17-alpine |
| Cache/atomic store | Redis 8-alpine |
| Messaging | Kafka KRaft `confluentinc/cp-kafka:8.2.0` |
| Search | Elasticsearch 8.18.8 |
| Email demo | Mailpit v1.27.8 |
| Observability | Actuator, Micrometer, Prometheus v3.11.2, Grafana 13.0.1, Zipkin 3.5.0 |
| Testing | JUnit 5, Mockito, Testcontainers |
| Deployment local | Docker Compose |

### Nguon

- `pom.xml`
- `docker-compose.yml`
- `.guide/01-yeu-cau-he-thong.md`
- `.guide/02-cai-dat-moi-truong.md`

## 5.2. Cau truc du an Maven multi-module

### Noi dung can viet

- Parent `pom.xml` quan ly dependency version va plugin.
- `common` dung cho DTO response, exception, event contract.
- Cac service co cau truc Spring Boot rieng: controller, service, repository, entity, config, kafka, scheduler.

### So do thu muc nen ve

```text
e-commerce-microservice-project/
  common/
  api-gateway/
  discovery-server/
  config-server/
  identity-service/
  user-service/
  product-service/
  inventory-service/
  cart-service/
  order-service/
  payment-service/
  voucher-service/
  notification-service/
  review-service/
  search-service/
  content-service/
  flash-sale-service/
  docker-compose.yml
  prometheus/
  grafana/
  keycloak/
  init-db/
```

## 5.3. Cai dat Docker Compose infrastructure

### Noi dung can viet

- PostgreSQL chinh va Keycloak DB rieng.
- Redis cho cache/cart/rate-limit/flash-sale.
- Kafka KRaft khong dung Zookeeper.
- Elasticsearch cho search.
- Keycloak import realm.
- Mailpit de test email.
- Zipkin, Prometheus, Grafana cho observability.
- Healthcheck va `depends_on` giup dieu phoi khoi dong.

### Screenshot can chup

- `docker compose ps`.
- Prometheus Targets.
- Grafana datasource/dashboard.
- Keycloak realm `ecommerce`.

### Nguon

- `docker-compose.yml`
- `init-db/01-create-databases.sql`
- `keycloak/realm-export.json`
- `.guide/03-build-va-chay-docker.md`
- `.guide/04-kiem-tra-he-thong.md`

## 5.4. Cai dat Discovery Server va Config Server

### Discovery Server

- Module: `discovery-server`.
- Main app co `@EnableEurekaServer`.
- Port: `8761`.
- Co security/basic auth theo cau hinh.

### Config Server

- Module: `config-server`.
- Port: `8888`.
- Native config trong `config-server/src/main/resources/configs`.
- Service doc profile `docker` khi chay compose.

### Noi dung nen trich

- Doan config Eureka client cua mot service.
- Doan config server native search locations.
- Screenshot Eureka dashboard.

## 5.5. Cai dat API Gateway

### Noi dung can viet

- Route configuration.
- JWT resource server config.
- `AuthHeaderFilter`: trich claim tu JWT va forward identity headers.
- `GuestSessionFilter`: sinh/duy tri guest session cho cart.
- `RateLimiterConfig`: key resolver theo IP/user.
- Redis-backed `RequestRateLimiter`.
- CORS va Swagger UI aggregation.

### Source can trich

- `api-gateway/src/main/resources/application.yml`
- `api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java`
- `api-gateway/src/main/java/com/ecommerce/gateway/config/RateLimiterConfig.java`
- `api-gateway/src/main/java/com/ecommerce/gateway/filter/AuthHeaderFilter.java`
- `api-gateway/src/main/java/com/ecommerce/gateway/filter/GuestSessionFilter.java`

### Bang cau hinh rate limit

| Route | replenishRate | burstCapacity | Key |
|---|---:|---:|---|
| auth | 10/s | 20 | IP |
| order | 10/s | 20 | user |
| flash-sale purchase | 3/s | 5 | user |

## 5.6. Cai dat cac business service

### Cach trinh bay

Moi service nen co 1 bang ngan:

| Muc | Noi dung |
|---|---|
| Vai tro | Service lam gi |
| API chinh | Controller nao |
| Du lieu | DB/Redis/Elasticsearch nao |
| Giao tiep | Feign/Kafka nao |
| Diem code quan trong | File can trich |

### identity-service

- Register/login/refresh/logout.
- Goi Keycloak Admin REST.
- Publish `user-registered`.
- Source: `AuthController`, `AuthServiceImpl`, `UserRegisteredProducer`.

### user-service

- Profile va delivery address.
- Consume `user-registered`.
- Co `processed_events` de idempotency.
- Source: `UserController`, `UserRegisteredConsumer`.

### product-service

- CRUD product/category/brand.
- Redis cache.
- Publish `product-created/updated/deleted`.
- Source: `ProductController`, `ProductEventProducer`.

### inventory-service

- Quan ly stock.
- Consume `order-created`, `order-confirmed`, `order-cancelled`.
- Publish `inventory-updated`, `inventory-failed`.
- Co dual counter `quantity/reserved_quantity`.

### cart-service

- Redis-only cart.
- Ho tro guest session qua Gateway.
- Feign den product-service de lay product context.

### order-service

Nen viet ky nhat:

- Place order.
- Validate product/voucher/cart.
- Persist order + outbox trong transaction.
- OutboxPoller publish event.
- Consume inventory/payment/flash-sale events.
- ReservationExpiryScheduler.
- Internal API cho payment/review/flash-sale.

Source quan trong:

- `OrderServiceImpl.java`
- `OutboxService.java`
- `OutboxPoller.java`
- `ReservationExpiryScheduler.java`
- `order-service/src/main/java/com/ecommerce/order/kafka/`

### payment-service

Nen viet ky:

- Consume `payment-requested`.
- Tao payment COD/VNPAY.
- Build VNPAY URL.
- Verify Return URL/IPN.
- Payment outbox va timeout scheduler.

Source:

- `PaymentServiceImpl.java`
- `VnPayServiceImpl.java`
- `VnPayUtil.java`
- `PaymentEventOutboxPoller.java`
- `PaymentTimeoutScheduler.java`

### voucher-service

- CRUD voucher.
- Internal reserve/commit/release.
- Reservation theo `orderId`.

### notification-service

- Consume `order-confirmed`, `order-cancelled`.
- Luu notification va gui email qua Mailpit.

### review-service

- CRUD review.
- Goi order-service de kiem tra user co don confirmed voi product hay khong.

### search-service

- Consume product events.
- Index/delete product document trong Elasticsearch.
- API search va suggestions.

### content-service

- Banner va blog post.
- Publish/unpublish blog.

### flash-sale-service

Nen viet ky:

- CRUD campaign.
- CampaignScheduler chuyen state.
- Redis Lua `RESERVE_SCRIPT` va `COMPENSATE_SCRIPT`.
- Publish `flash-sale-order-requested`.
- ReconciliationScheduler doi soat Redis/DB.
- Feign den order-service de dem so don flash sale.

Source:

- `FlashSaleServiceImpl.java`
- `FlashSaleEventProducer.java`
- `CampaignScheduler.java`
- `ReconciliationScheduler.java`
- `KafkaProducerConfig.java`

## 5.7. Cai dat Outbox, Idempotency va Scheduler

### Outbox

- `order-service`: bang `outbox`, `OutboxPoller` moi 1 giay.
- `payment-service`: bang `payment_outbox`, co status/attempts/last_error.

### Idempotency

- Cac service consumer stateful co `processed_events`.
- Search-service co `ProcessedSearchEventDocument`.

### Scheduler

Trinh bay bang o Chuong 4 va bo sung file code thuc te.

## 5.8. Cai dat Observability

### Metrics

- Parent dependencies: Actuator, Micrometer Prometheus registry.
- Prometheus config scrape service.
- Grafana provisioning.

### Tracing

- Micrometer Tracing Brave.
- Zipkin endpoint.
- Log level pattern co traceId/spanId.

### Source

- `prometheus/prometheus.yml`
- `grafana/provisioning/`
- `grafana/dashboards/`
- cac `application.yml`

## 5.9. Quy trinh build va chay local

### Lenh nen dua vao bao cao

```bash
./mvnw clean package -DskipTests
docker compose up -d --build
docker compose ps
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:9090/targets
```

### Thu tu khoi dong logic

1. Infrastructure: PostgreSQL, Redis, Kafka, Elasticsearch, Mailpit.
2. Keycloak va observability.
3. Discovery Server.
4. Config Server.
5. API Gateway.
6. Business services.

## 5.10. Danh sach screenshot demo

- Docker Compose containers healthy.
- Eureka Dashboard.
- Gateway health/readiness.
- Swagger UI qua Gateway.
- Keycloak realm/users/clients.
- Product API hoac Order API response.
- Kafka topic list.
- Mailpit email.
- VNPAY sandbox page/return.
- Prometheus targets.
- Grafana dashboards.
- Zipkin trace cua request tao don.

## Checklist Chuong 5

- [ ] Co bang tech stack dung phien ban trong repo.
- [ ] Co so do thu muc/multi-module.
- [ ] Co mo ta Docker Compose va healthcheck.
- [ ] Co route/rate-limit/security cua Gateway.
- [ ] Moi service co vai tro, API, data, giao tiep, source code minh chung.
- [ ] Co mo ta Outbox/Idempotency/Scheduler bang code that.
- [ ] Co screenshot chay he thong.
