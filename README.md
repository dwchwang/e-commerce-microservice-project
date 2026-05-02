# Đồ án tốt nghiệp - E-commerce Microservice

Author: Nguyen Duc Bao Hoang

Hệ thống e-commerce được tách thành các microservice Spring Boot, giao tiếp qua API Gateway, Eureka, Config Server, Kafka, PostgreSQL, Redis và Elasticsearch. Stack observability gồm Actuator, Prometheus, Grafana và Zipkin.

## Architecture

```text
Client
  |
  v
API Gateway :8080
  |
  +-- identity-service     :8081  -> Keycloak, Kafka
  +-- user-service         :8082  -> PostgreSQL, Kafka
  +-- product-service      :8083  -> PostgreSQL, Redis, Kafka
  +-- inventory-service    :8084  -> PostgreSQL, Kafka
  +-- cart-service         :8085  -> Redis
  +-- order-service        :8086  -> PostgreSQL, Kafka, product/cart/voucher
  +-- payment-service      :8087  -> PostgreSQL, Kafka, VNPAY
  +-- voucher-service      :8088  -> PostgreSQL
  +-- notification-service :8089  -> PostgreSQL, Kafka, Mailpit
  +-- review-service       :8090  -> PostgreSQL, order-service
  +-- search-service       :8091  -> Elasticsearch, Kafka
  +-- content-service      :8092  -> PostgreSQL
  +-- flash-sale-service   :8093  -> PostgreSQL, Redis, Kafka
```

All Spring services register with Eureka and read environment-specific configuration from Config Server. API Gateway is the only host-exposed business API entrypoint.

## Tech Stack

- Java 21, Maven Wrapper, Spring Boot 3.5, Spring Cloud 2025
- Spring Cloud Gateway, Eureka, Config Server, OpenFeign
- PostgreSQL 17, Redis 8, Kafka KRaft, Elasticsearch 8
- Keycloak, JWT resource server security
- Micrometer, Prometheus, Grafana, Zipkin
- JUnit 5, Mockito, Testcontainers

## Prerequisites

- Java 21
- Docker Desktop
- Maven 3.9+ or the included `./mvnw`

## Quick Start

```bash
./mvnw clean package -DskipTests
docker compose up -d --build
docker compose ps
```

Useful checks:

```bash
curl -u eureka:eureka http://localhost:8761/eureka/apps
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:9090/targets
```

## Service URLs

| Component | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Eureka | http://localhost:8761 |
| Keycloak | http://localhost:8180 |
| Zipkin | http://localhost:9411 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Mailpit | http://localhost:8025 |
| Elasticsearch | http://localhost:9200 |

## Swagger UI

Gateway:

- http://localhost:8080/swagger-ui.html

Direct service Swagger URLs when running services with exposed ports locally:

- Identity: http://localhost:8081/swagger-ui.html
- User: http://localhost:8082/swagger-ui.html
- Product: http://localhost:8083/swagger-ui.html
- Inventory: http://localhost:8084/swagger-ui.html
- Cart: http://localhost:8085/swagger-ui.html
- Order: http://localhost:8086/swagger-ui.html
- Payment: http://localhost:8087/swagger-ui.html
- Voucher: http://localhost:8088/swagger-ui.html
- Notification: http://localhost:8089/swagger-ui.html
- Review: http://localhost:8090/swagger-ui.html
- Search: http://localhost:8091/swagger-ui.html
- Content: http://localhost:8092/swagger-ui.html
- Flash Sale: http://localhost:8093/swagger-ui.html

## Monitoring

Prometheus scrapes API Gateway and all 13 business services through `/actuator/prometheus`. Grafana provisions dashboards from files at startup:

- Spring Boot Overview
- JVM Overview
- E-commerce Saga Overview

Zipkin receives spans from API Gateway and business services. Logs include `traceId` and `spanId` in the log level pattern.

## Tests

Run all tests:

```bash
./mvnw test
```

Focused checks:

```bash
./mvnw -pl api-gateway -am test
./mvnw -pl order-service -am test
./mvnw -pl flash-sale-service -am test
docker compose config --quiet
```

The Testcontainers smoke test uses `postgres:17-alpine`, aligned with the runtime PostgreSQL image.

## Core Patterns

- Config Server centralizes docker/local service settings.
- Eureka service discovery is used by Gateway and OpenFeign clients.
- Gateway validates JWTs and forwards trusted identity headers.
- Redis-backed Gateway rate limiting protects auth, order, and flash-sale purchase routes.
- Kafka events drive order, inventory, payment, notification, search indexing, and flash-sale flows.
- Outbox and processed-event tables provide idempotency for saga-critical event handling.
- Actuator liveness/readiness groups are dependency-specific per service.
- Prometheus/Grafana/Zipkin provide metrics, dashboards, and tracing for local demo operations.
