# 02. Hệ Sinh Thái Spring Boot & Spring Cloud

## 1. Mục Tiêu Nghiên Cứu

- Hiểu Spring Framework, Spring Boot, Spring Cloud là gì và quan hệ giữa chúng
- Nắm được auto-configuration, starter, Actuator
- Trình bày được vì sao chọn Spring Cloud thay vì tự xây
- Biết các module Spring Cloud quan trọng dùng trong đồ án

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Spring Framework (2002–nay)
Framework Java doanh nghiệp tập trung vào:
- **IoC (Inversion of Control) container**: Quản lý lifecycle bean
- **DI (Dependency Injection)**: `@Autowired`, constructor injection
- **AOP (Aspect-Oriented Programming)**: Cross-cutting concerns (logging, security)
- **Abstraction layers**: JDBC, JPA, Transaction, Messaging

### 2.2. Spring Boot (2014–nay) — Spring Boot 3.5 trong dự án
**Vấn đề Spring giải quyết**: Spring Framework cấu hình XML khổng lồ, khó khởi động dự án mới.
**Giải pháp**: 
- **Auto-configuration** — quét classpath, tự cấu hình bean phù hợp (`@EnableAutoConfiguration`)
- **Starter dependencies** — `spring-boot-starter-web`, `spring-boot-starter-data-jpa` gom các artifact thường dùng
- **Embedded server** — Tomcat/Netty embed sẵn, chạy `java -jar app.jar`
- **Production-ready actuator** — health check, metrics out-of-the-box
- **Convention over configuration** — sensible defaults

**Spring Boot 3.5 yêu cầu**:
- Java 17+ (dự án dùng Java 21)
- Jakarta EE 10 (đổi từ `javax.*` sang `jakarta.*`)
- Native compilation hỗ trợ qua GraalVM

### 2.3. Spring Cloud (2015–nay) — Spring Cloud 2025 trong dự án
**Vấn đề**: Spring Boot tạo 1 service rất nhanh, nhưng làm sao **kết nối nhiều service thành hệ phân tán**?
**Giải pháp** — bộ module:

| Module | Mục đích | Dùng trong đồ án |
|--------|---------|------------------|
| Spring Cloud Netflix Eureka | Service Discovery | discovery-server :8761 |
| Spring Cloud Config | Centralized Config | config-server :8888 |
| Spring Cloud Gateway | API Gateway routing | api-gateway :8080 |
| Spring Cloud OpenFeign | Declarative REST client | order-service → product-service |
| Spring Cloud LoadBalancer | Client-side LB | Đi kèm Eureka |
| Spring Cloud Stream | Messaging abstraction | (dự án dùng plain Spring Kafka) |
| Spring Cloud Sleuth → Micrometer Tracing | Tracing | Đã thay bằng Micrometer Tracing + Zipkin |
| Resilience4j (Spring Cloud Circuit Breaker) | CB, retry, rate limit | order-service circuit breakers |

**Lưu ý quan trọng**: Từ Spring Boot 3 / Spring Cloud 2022+, **Sleuth bị deprecated**, thay bằng **Micrometer Tracing** + **Micrometer Observation API**.

### 2.4. Auto-configuration hoạt động thế nào?

```
1. spring.factories / AutoConfiguration.imports liệt kê các *AutoConfiguration class
2. Mỗi class dùng @ConditionalOnClass, @ConditionalOnMissingBean kiểm tra
3. Nếu điều kiện đúng → tạo bean
4. Bean của user (qua @Bean) override bean auto-config
```

Ví dụ: Có `redis-starter` trên classpath + `spring.data.redis.host` được set → `RedisAutoConfiguration` tạo `RedisTemplate` bean.

### 2.5. Spring Boot Actuator

Cung cấp **production-ready endpoints** qua `/actuator/`:

| Endpoint | Mục đích |
|----------|---------|
| `/actuator/health` | Health check tổng |
| `/actuator/health/liveness` | Process còn sống không (Kubernetes liveness probe) |
| `/actuator/health/readiness` | Sẵn sàng nhận request chưa (DB/Kafka đã connect chưa) |
| `/actuator/info` | Build info, git commit |
| `/actuator/metrics` | Metric list (JVM, HTTP, DB pool, ...) |
| `/actuator/prometheus` | Format metric cho Prometheus scrape |
| `/actuator/circuitbreakers` | Trạng thái circuit (Resilience4j) |
| `/actuator/gateway/routes` | Routes hiện hành (chỉ Gateway) |

→ Mỗi service trong dự án expose `/actuator/health` cho Docker `healthcheck:` và `/actuator/prometheus` cho Prometheus.

### 2.6. Maven Multi-module

Dự án dùng cấu trúc:
```
e-commerce microservice project/
  pom.xml                  ← parent POM (BOM Spring Boot/Cloud)
  common/                  ← shared library (DTO, exception, BaseEntity)
  api-gateway/
  config-server/
  discovery-server/
  order-service/
  ...
```

- **Parent POM** quản lý version, plugin chung
- **`common` module**: Code reuse (event payload, base entity với @CreatedDate)
- **Mỗi service POM** kế thừa parent, thêm dependency riêng

---

## 3. So Sánh Với Phương Án Thay Thế

| Framework | Ngôn ngữ | Ưu | Nhược |
|-----------|---------|-----|-------|
| **Spring Boot** | Java | Mature, ecosystem khổng lồ, tài liệu nhiều | JVM nặng RAM |
| Quarkus | Java | Native compile nhanh, low memory | Ít tài liệu hơn |
| Micronaut | Java | DI compile-time, fast startup | Cộng đồng nhỏ |
| NestJS | Node.js | Lightweight, TypeScript | Java ecosystem mạnh hơn cho enterprise |
| Go (Gin/Echo) | Go | Tốc độ, low overhead | Thiếu DI/AOP, cần code thủ công nhiều |
| .NET | C# | Mạnh trên Windows | Hạn chế trên Linux production thực tế |

→ Chọn Spring Boot vì: Java cung cấp **type safety + thread pool mature + Spring Data JPA + Spring Security tích hợp Keycloak chuẩn**.

---

## 4. Cách Áp Dụng Trong Dự Án

### 4.1. Parent POM (`/pom.xml`)
```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.5.x</version>
</parent>
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>2025.x.x</version>
      <type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 4.2. Service application class
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrderServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrderServiceApplication.class, args);
  }
}
```

### 4.3. Bootstrap & Application properties
- `bootstrap.yml`/`application.yml` — kết nối Config Server
- Config Server tải `{service-name}-{profile}.yml` từ folder `config-server/src/main/resources/configs/`
- Profile `docker` dùng khi chạy Docker Compose, profile `local` khi chạy IDE

### 4.4. Common module
`common/` chứa:
- Event DTO classes (vd: `OrderCreatedEvent`)
- `BaseEntity` với `@CreatedDate`, `@LastModifiedDate`
- Exception classes (`ServiceException`, error codes)

→ Trong báo cáo có thể nói: "*Em dùng pattern Shared Kernel của DDD để đặt event schemas chung*". (Lưu ý: Shared Kernel là tradeoff — nếu sửa common, phải build lại tất cả service.)

---

## 5. Từ Khóa Nghiên Cứu

```
- spring boot auto configuration
- spring boot starter dependency
- spring boot actuator endpoints
- spring cloud 2025 release notes
- spring cloud netflix eureka
- spring cloud config server
- spring cloud gateway reactive
- micrometer observation api
- spring boot 3 vs spring boot 2 differences
- jakarta ee migration
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Spring Boot và Spring Framework khác nhau thế nào?**
→ Spring Framework là core IoC/DI/AOP. Spring Boot là layer trên, thêm auto-configuration, starter, actuator để giảm boilerplate.

**Q2: Tại sao chọn Spring Cloud mà không tự code Service Discovery?**
→ Tránh reinvent wheel; Spring Cloud đã production-ready, tích hợp với Spring Boot, có monitoring/health sẵn.

**Q3: Auto-configuration hoạt động thế nào?**
→ Dựa trên `@Conditional` annotations + meta file `AutoConfiguration.imports`. Khi class điều kiện có trên classpath, bean tự được tạo.

**Q4: Vì sao Sleuth bị thay bằng Micrometer Tracing?**
→ Spring Boot 3 chuẩn hóa observability qua Micrometer (vendor-neutral). Sleuth chỉ cho tracing, còn Micrometer Observation cho cả metrics + tracing trong 1 API.

**Q5: Tại sao Java 21 mà không phải Java 17?**
→ Java 21 LTS (Long-Term Support) ra 2023, có Virtual Threads (Project Loom) — cải thiện đáng kể với high-concurrency I/O. Spring Boot 3.2+ hỗ trợ.

---

## 7. Tài Liệu Tham Khảo

- **Documentation chính thức**:
  - https://docs.spring.io/spring-boot/docs/current/reference/html/
  - https://spring.io/projects/spring-cloud
  - https://docs.spring.io/spring-cloud/docs/current/reference/html/
- **Sách**:
  - Craig Walls, *Spring in Action*, 6th ed., Manning, 2022
  - Felipe Gutierrez, *Spring Boot Messaging*, Apress
  - Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, 2019
- **Releases & Blog**:
  - Spring Boot 3.5 Release Notes
  - Spring Cloud 2025 Release Train
  - "From Sleuth to Micrometer Tracing" — Spring Blog
