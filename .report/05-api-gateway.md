# 05. API Gateway — Spring Cloud Gateway

## 1. Mục Tiêu Nghiên Cứu

- Hiểu vai trò API Gateway trong microservice
- Phân biệt Gateway với Reverse Proxy/Load Balancer
- Hiểu các chức năng: routing, JWT validation, CORS, rate limiting, header propagation
- Hiểu Reactive (WebFlux) và mô hình non-blocking

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Vấn đề khi không có Gateway
Client (web/mobile) phải:
- Biết IP/port của 13 service
- Tự xử lý JWT cho mỗi request
- CORS phải cấu hình cho từng service
- Không có chỗ chung để rate limit/log/audit

### 2.2. API Gateway Pattern (Chris Richardson)
> A single entry point for all clients, like a Facade in OOP — but for distributed systems.

**Trách nhiệm**:
1. **Routing** — match path/header → forward đến service backend
2. **Authentication** — verify JWT, extract claims, forward header
3. **Authorization** — kiểm tra role/scope
4. **Rate Limiting** — chống spam, DDoS
5. **CORS** — cho phép frontend domain cụ thể
6. **Request/Response transformation** — rewrite path, add header
7. **Aggregation** (BFF) — gộp nhiều API call thành một response

### 2.3. Spring Cloud Gateway

**Stack**: Spring WebFlux (reactive) + Project Reactor + Netty
- **Non-blocking I/O** → 1 thread xử lý nhiều connection (event loop)
- Phù hợp Gateway vì I/O bound (chờ backend response)

**Concepts**:
- **Route**: id, uri (target), predicates (điều kiện match), filters (transform)
- **Predicate**: Path, Method, Header, Cookie, Query, ...
- **Filter**: AddRequestHeader, RewritePath, RequestRateLimiter, CircuitBreaker, ...

```yaml
spring.cloud.gateway:
  routes:
    - id: order-route
      uri: lb://order-service
      predicates:
        - Path=/api/orders/**
      filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20
            key-resolver: '#{@userKeyResolver}'
```

### 2.4. JWT Validation Flow

Gateway = OAuth2 Resource Server:
```
Client request: Authorization: Bearer <jwt>
  → Gateway WebFlux Security Filter
  → JwtDecoder: fetch JWK Set từ Keycloak (cached)
  → Verify signature + expiry + issuer
  → Tạo JwtAuthenticationToken
  → Filter chain tiếp tục (rate limit, custom header)
```

Cấu hình:
```yaml
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: http://keycloak:8080/realms/ecommerce
  jwk-set-uri: http://keycloak:8080/realms/ecommerce/protocol/openid-connect/certs
```

### 2.5. Header Propagation (Identity forwarding)

Custom GlobalFilter trích xuất từ JWT claim → set header:
- `X-User-Id` từ claim `sub`
- `X-User-Roles` từ claim `realm_access.roles`
- `X-User-Email` từ claim `email`

Backend service **TIN TƯỞNG** các header này (KHÔNG validate JWT lại) — design choice để giảm latency. Bù lại, backend phải nằm trong network nội bộ, chỉ Gateway exposed.

→ Đây là pattern **"Trusted Subsystem"** — đáng để nhắc trong báo cáo.

### 2.6. Rate Limiting với Redis

Algorithm: **Token Bucket** (Redis Lua script atomic)
- `replenishRate`: tokens/giây nạp lại
- `burstCapacity`: max token tích lũy
- `keyResolver`: phân biệt theo IP / userId / sessionId

Trong dự án:
| Route | Rate | Burst | Key |
|-------|------|-------|-----|
| `/api/auth/**` | 10/s | 20 | IP |
| `/api/orders/**` | 10/s | 20 | userId |
| `/api/flash-sales/*/purchase` | 3/s | 5 | userId |

Vượt hạn → HTTP **429 Too Many Requests**.

### 2.7. CORS

Cấu hình `CorsWebFilter`:
```java
allowedOrigins: ["http://localhost:3000"]
allowedMethods: GET, POST, PUT, DELETE, PATCH, OPTIONS
allowedHeaders: Authorization, Content-Type, X-Session-Id
exposedHeaders: X-User-Id, X-User-Roles, X-User-Email
allowCredentials: true
```

CORS preflight: Browser gửi `OPTIONS` trước → Gateway respond Access-Control-Allow-* → mới gửi request thật.

### 2.8. Public vs Authenticated Routes

| Public | Lý do |
|--------|-------|
| `/api/auth/**` | Đăng ký/đăng nhập |
| `/api/products/**` (GET) | Xem catalog không cần login |
| `/api/search/**` | Tìm kiếm |
| `/api/payments/vnpay/return` | VNPAY redirect không có JWT |
| `/api/payments/vnpay/ipn` | VNPAY server-to-server callback |
| `/api/cart/**` | Dùng X-Session-Id thay JWT (guest cart) |

Routes khác → cần JWT.

---

## 3. So Sánh Với Phương Án Thay Thế

| Gateway | Stack | Pros | Cons |
|---------|-------|------|------|
| **Spring Cloud Gateway** | Java/Reactor | Tích hợp Spring ecosystem, JWT/Security tự nhiên | JVM nặng |
| Zuul 1.x | Java blocking | Đơn giản, đã được dùng nhiều | Bị deprecated, không reactive |
| Kong | Lua/OpenResty | Plugin ecosystem, fast | Cần học Lua |
| Traefik | Go | Auto-discover Docker labels | Ít plugin auth phức tạp |
| Nginx + Lua | C | Fast, mature | Khó code logic phức tạp |
| AWS API Gateway | Managed | Không cần ops | Vendor lock-in, đắt |
| Envoy | C++ | Foundation của Istio service mesh | Phức tạp |

---

## 4. Cách Áp Dụng Trong Dự Án

### 4.1. api-gateway module
- Port: `8080` (cổng vào duy nhất exposed ra ngoài)
- Spring Boot WebFlux + Spring Cloud Gateway + Spring Security OAuth2 Resource Server
- Redis cho rate limiter
- Eureka client để lookup service

### 4.2. Cấu trúc filter chain (thứ tự quan trọng)
```
1. CORS filter
2. Authentication filter (JWT validation)
3. Identity Header Filter (X-User-Id, X-User-Roles, X-User-Email)
4. Rate limiter (Redis-based)
5. Routing filter (forward đến service)
```

### 4.3. Swagger Aggregation
Gateway expose `/swagger-ui.html` aggregate Swagger từ tất cả service qua springdoc-openapi.

### 4.4. Resilience4j Circuit Breaker filter (không bắt buộc nhưng có)
```yaml
filters:
  - name: CircuitBreaker
    args:
      name: orderCB
      fallbackUri: forward:/fallback/order
```

### 4.5. Rate limit theo userId (KeyResolver bean)
```java
@Bean
KeyResolver userKeyResolver() {
  return exchange -> {
    String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
    return userId != null ? Mono.just(userId)
                          : Mono.just("anon-" + getIp(exchange));
  };
}
```

---

## 5. Từ Khóa Nghiên Cứu

```
- api gateway pattern microservices
- spring cloud gateway vs zuul
- backend for frontend BFF pattern
- token bucket rate limiting algorithm
- jwt resource server spring security
- reactive netty webflux
- gateway aggregator pattern
- trusted subsystem header propagation
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Tại sao backend tin tưởng header X-User-Id mà không validate JWT?**
→ Đây là pattern Trusted Subsystem. Gateway là cổng duy nhất exposed; backend nằm trong Docker network nội bộ. Validate JWT lại tốn ~5ms mỗi request × 13 service. Nếu cần defense-in-depth, có thể bật JWT validation ở backend trong production thật.

**Q2: Gateway có là single point of failure không?**
→ Có. Production: chạy 2+ instance Gateway sau Load Balancer (Nginx/HAProxy). Đồ án demo 1 instance.

**Q3: Tại sao chọn Spring Cloud Gateway mà không phải Zuul/Kong?**
→ Spring Cloud Gateway là next-gen của Zuul, **non-blocking** (reactive), tích hợp với Spring Security/Eureka. Kong là good cho polyglot ecosystem, nhưng chúng ta đã ở Spring stack, dùng Gateway tự nhiên hơn.

**Q4: Token Bucket khác Leaky Bucket?**
→ Token Bucket cho phép burst (tích token chưa dùng). Leaky Bucket smooth rate cố định. Spring Cloud Gateway Redis rate limiter dùng token bucket — phù hợp e-commerce (user spike khi click).

**Q5: Em có lo về CORS preflight overhead?**
→ Browser cache preflight (`Access-Control-Max-Age`). Lần đầu mất 1 OPTIONS round trip, sau đó cache.

**Q6: Khi backend service trả 5xx, Gateway có retry không?**
→ Mặc định không. Em có thể bật `Retry` filter, nhưng cẩn thận với non-idempotent (POST). Em chọn **Circuit Breaker** thay retry — fail-fast hơn.

---

## 7. Tài Liệu Tham Khảo

- Chris Richardson, *Microservices Patterns*, Chapter 8 — External API Patterns
- Spring Cloud Gateway documentation
- "API Gateway pattern" — microservices.io
- Sam Newman, *Building Microservices*, 2nd ed., Chapter 5 — Communication Styles
- "BFF Pattern" — Sam Newman, samnewman.io
- "Token Bucket vs Leaky Bucket" — RFC 2697 (srTCM)
