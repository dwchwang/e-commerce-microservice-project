# 05. Spring Cloud — Eureka + Config Server + API Gateway

> Bộ ba này là **xương sống microservice** trong project: Eureka quản lý "service nào đang sống ở đâu", Config Server tập trung config, API Gateway là cửa ngõ duy nhất tiếp nhận traffic từ client.

## 1. Khái niệm cốt lõi

| Thành phần | Vai trò |
|------------|---------|
| **Eureka Server** | Service registry — mỗi service tự đăng ký, gateway và Feign tra cứu để biết IP:port |
| **Config Server** | Phục vụ file YAML cho tất cả service từ một nơi duy nhất, có thể reload không cần restart |
| **API Gateway** | Reverse proxy + JWT validation + rate limit + routing theo path |
| **OpenFeign** | HTTP client declarative, dùng `lb://service-name` để call qua Eureka |
| **Service Discovery** | Cơ chế gateway/Feign hỏi Eureka "where is order-service?" rồi load-balance |

## 2. Hệ thống đang dùng ra sao

### 2.1 Discovery Server (Eureka)

- **Container**: `discovery-server`, port `8761`
- **Auth**: basic auth `eureka:eureka` (xem `.env`)
- **URL**: http://localhost:8761

Mọi service Spring đều có:
```yaml
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_DEFAULT_ZONE:http://eureka:eureka@localhost:8761/eureka/}
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:localhost}
```

> `EUREKA_INSTANCE_HOSTNAME` được set bằng tên container trong docker-compose (ví dụ `order-service`). Nếu để mặc định `localhost`, các service trong Docker network sẽ không gọi được nhau.

### 2.2 Config Server

- **Container**: `config-server`, port `8888`
- **Profile active**: `native` — đọc file YAML từ classpath thay vì Git
- **Source**: [config-server/src/main/resources/configs/](../config-server/src/main/resources/configs/) — mỗi service một file `<service-name>.yml`

Mỗi service business có file `application.yml` ngắn gọn chỉ trỏ về Config Server:
```yaml
spring:
  application:
    name: order-service
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

Khi service start, nó tự fetch `http://config-server:8888/order-service/docker` để lấy config đầy đủ.

### 2.3 API Gateway

- **Container**: `api-gateway`, port host `8080`
- **Stack**: Spring Cloud Gateway WebFlux (reactive, non-blocking)
- **Routes**: định nghĩa trong [config-server/src/main/resources/configs/api-gateway.yml](../config-server/src/main/resources/configs/api-gateway.yml)
- **Security**: [SecurityConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java) (xem [01-keycloak.md](01-keycloak.md))

Pattern routing:
```yaml
- id: order-service
  uri: lb://order-service        # ← lb = load-balanced qua Eureka
  predicates:
    - Path=/api/orders/**
  filters:
    - name: RequestRateLimiter   # ← chống spam (xem 07-redis.md)
      args:
        redis-rate-limiter.replenishRate: 10
        redis-rate-limiter.burstCapacity: 20
        key-resolver: "#{@userKeyResolver}"
```

### 2.4 Sequence diagram khi 1 request đến

```
Client → API Gateway :8080
          │
          │  1. SecurityWebFilter validate JWT (JWKS từ Keycloak)
          │  2. CORS check
          │  3. Rate limit (Redis)
          │  4. Route match path → service name
          │  5. AuthHeaderFilter strip + inject X-User-Id, X-User-Roles
          │
          ▼
        LoadBalancerClient
          │
          │  6. Hỏi Eureka: order-service đang ở đâu?
          │  7. Eureka trả về [order-service:8086, order-service-2:8086, ...]
          │  8. Round-robin chọn 1 instance
          │
          ▼
        order-service :8086
          │
          │  9. Service nhận request đã được kèm X-User-Id
          │ 10. Business logic
          │
          ▼
        Response → Gateway → Client
```

## 3. Workflow vận hành

### 3.1 Truy cập Eureka UI

http://localhost:8761 (auth: `eureka` / `eureka`)

Trang dashboard liệt kê tất cả service đang đăng ký dưới dạng:
- **Application** = `spring.application.name` (uppercase)
- **AMIs / Availability Zones** = số instance
- **Status** = `UP` (xanh) hoặc `DOWN`

API JSON:
```bash
curl -u eureka:eureka http://localhost:8761/eureka/apps | xmllint --format -

# Lấy tất cả instance của 1 service
curl -u eureka:eureka http://localhost:8761/eureka/apps/ORDER-SERVICE
```

### 3.2 Verify Config Server đang phục vụ config

```bash
# Lấy config của order-service ở profile docker
curl -s http://localhost:8888/order-service/docker | jq

# Kết quả gồm 2 file merged:
# - configs/order-service.yml
# - configs/order-service-docker.yml (nếu có)
```

Pattern URL: `http://config-server:8888/{service-name}/{profile}[/{label}]`

### 3.3 Refresh config không restart service

Khi sửa file YAML trong Config Server → cần báo service "đọc lại config":

**Cách A — Restart service** (đơn giản nhất cho dev):
```bash
docker compose restart order-service
```

**Cách B — Spring Boot Actuator refresh** (yêu cầu service có `@RefreshScope` trên bean):
```bash
# 1. Thêm management.endpoints.web.exposure.include: refresh trong service config
# 2. Đánh @RefreshScope lên bean cần reload
# 3. Trigger:
curl -X POST http://localhost:8086/actuator/refresh
```

Project hiện chưa expose endpoint `refresh` — nếu cần, thêm vào `management.endpoints.web.exposure.include`.

### 3.4 Test routing của Gateway

```bash
# Direct (bypass gateway) — chỉ làm được khi service expose port host
curl http://localhost:8086/api/orders/...

# Qua gateway — đường đi production-like
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/orders/...
```

Xem actual route mapping:
```bash
curl -s http://localhost:8080/actuator/gateway/routes | jq
```

> Lưu ý: endpoint `/actuator/gateway/routes` cần expose trong `management.endpoints.web.exposure.include`. Mặc định project chỉ expose `health,info,prometheus`.

### 3.5 Thêm 1 route mới vào Gateway

Sửa [config-server/src/main/resources/configs/api-gateway.yml](../config-server/src/main/resources/configs/api-gateway.yml):
```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: my-new-service
              uri: lb://my-new-service
              predicates:
                - Path=/api/myresource/**
```

Rồi:
```bash
docker compose restart config-server api-gateway
```

### 3.6 Thêm 1 service mới vào hệ thống

Checklist:

1. **Đặt tên service** — `my-service`. Tên này dùng cho:
   - `spring.application.name` trong code
   - Tên container trong docker-compose
   - Filename trong `config-server/configs/my-service.yml`
   - URI trong gateway routes (`lb://my-service`)

2. **Tạo file config** `configs/my-service.yml` — copy mẫu từ một service khác

3. **Bổ sung Prometheus scrape target** trong [prometheus/prometheus.yml](../prometheus/prometheus.yml)

4. **Thêm route vào api-gateway.yml** nếu là service public-facing

5. **Thêm vào docker-compose.yml** với env tối thiểu:
   ```yaml
   SPRING_PROFILES_ACTIVE: docker
   CONFIG_SERVER_URL: http://config-server:8888
   EUREKA_DEFAULT_ZONE: ${EUREKA_DEFAULT_ZONE}
   EUREKA_INSTANCE_HOSTNAME: my-service
   ZIPKIN_ENDPOINT: http://zipkin:9411/api/v2/spans
   ```

### 3.7 OpenFeign — call service-to-service

Pattern trong project (xem [order-service](../order-service)):

```java
@FeignClient(name = "product-service")
public interface ProductServiceClient {
    @GetMapping("/api/internal/products/{id}")
    ProductResponse getProduct(@PathVariable Long id);
}
```

Spring Cloud LoadBalancer + Eureka tự resolve `product-service` thành instance thật. Có sẵn **circuit breaker** vì project đã enable:
```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
```

→ Khi `product-service` down, Feign trả về fallback (cần khai báo `fallback` class).

## 4. Troubleshooting

### 4.1 Service không xuất hiện trên Eureka

```bash
# Xem log service xem có lỗi đăng ký không
docker compose logs order-service | grep -i eureka

# Common errors:
# - "Cannot execute request on any known server" → Eureka chưa healthy
# - "Connection refused" → sai EUREKA_DEFAULT_ZONE
# - Service đăng ký nhưng dùng hostname `localhost` → thiếu EUREKA_INSTANCE_HOSTNAME
```

### 4.2 Gateway trả 503 "Service Unavailable"

Có nghĩa Eureka không tìm thấy instance UP cho service đó.

```bash
# Kiểm tra service có UP trên Eureka không
curl -u eureka:eureka http://localhost:8761/eureka/apps/ORDER-SERVICE | grep status
```

Nếu Eureka cache cũ (instance đã chết nhưng vẫn UP), đợi ~90s để eviction, hoặc restart Eureka.

### 4.3 Config Server trả empty

```bash
curl -s http://localhost:8888/order-service/docker
# Nếu kết quả "propertySources": [] → file config không được tìm thấy

# Check log
docker compose logs config-server | grep -i "error\|warn"
```

Common causes:
- Filename sai (`order-service.yaml` thay vì `order-service.yml`)
- Profile `native` chưa active — kiểm tra `SPRING_PROFILES_ACTIVE`
- Path mount sai

### 4.4 Service start chậm vì chờ Config Server

Spring Cloud thay đổi behavior từ Spring Boot 2.4: nếu `spring.config.import` không có `optional:` thì service sẽ **fail-fast** khi Config Server không sẵn sàng.

Project dùng `optional:configserver:...` → service vẫn start được khi Config Server chết, nhưng sẽ thiếu config động.

Trong production nên **bỏ `optional:`** để fail-fast và alert.

### 4.5 Gateway không thấy route mới

```bash
# Force reload từ config server
docker compose restart api-gateway

# Verify route loaded
curl -s http://localhost:8080/actuator/gateway/routes | jq '.[].route_id'
```

## 5. Best practices đang được áp dụng

- **Single entry point** — chỉ Gateway expose ra host, các service business chỉ `expose` (internal Docker network)
- **Centralized config** — sửa 1 file YAML thay vì SSH vào từng service
- **`lb://service-name` thay vì hardcode IP** — auto resilient khi instance scale/move
- **Circuit breaker cho Feign** — tránh cascading failure
- **Per-service health groups** — readiness chỉ pass khi DB/Kafka/Redis dependencies thực sự sẵn sàng
- **Reactive Gateway (WebFlux)** — non-blocking, throughput cao hơn MVC stack
