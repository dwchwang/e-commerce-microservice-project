---
name: E-commerce Microservice Project Overview
description: Core facts about the Spring Boot microservice DATN project structure and tech stack
type: project
---

Spring Boot 3.5.x / Spring Cloud 2025.0.0 / Java 21, 14 microservices: api-gateway (8080, reactive/WebFlux), identity-service (8081), user-service (8082), product-service (8083), inventory-service (8084), cart-service (8085), order-service (8086), payment-service (8087), voucher-service (8088), notification-service (8089), review-service (8090), search-service (8091), content-service (8092), flash-sale-service (8093).

Config server centrally provides per-service configs. Services load via optional:configserver. Infrastructure: PostgreSQL 17, Redis 8, Kafka (KRaft), Elasticsearch 8, Keycloak 26, Zipkin, Prometheus, Grafana.

**Why:** DATN (graduation thesis) project, developed in phases (1-9+). Phases add features incrementally.
**How to apply:** When reviewing, check consistency across all 14 services. Config-server configs override service application.yml.
