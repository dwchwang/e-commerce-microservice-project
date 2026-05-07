# 07. Giao Tiếp Đồng Bộ — REST + OpenFeign

## 1. Mục Tiêu Nghiên Cứu

- Hiểu REST architectural style
- Hiểu Feign — declarative HTTP client của Netflix → Spring Cloud OpenFeign
- Phân biệt RestTemplate, WebClient, Feign
- Trade-off: synchronous vs asynchronous communication

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. REST (Roy Fielding, 2000)
6 ràng buộc của REST:
1. **Client-Server** — tách biệt UI và backend
2. **Stateless** — mỗi request tự chứa, server không lưu state
3. **Cacheable** — response có thể cache (Cache-Control header)
4. **Uniform Interface** — resource có URI, dùng HTTP method chuẩn (GET/POST/PUT/DELETE)
5. **Layered System** — client không biết có proxy/gateway giữa
6. **Code on Demand** (optional) — server gửi script (rất hiếm)

### 2.2. Khi nào dùng đồng bộ trong microservice?

**Synchronous (REST)** phù hợp khi:
- Cần response để xử lý tiếp (vd: validate sản phẩm trước khi tạo đơn)
- Yêu cầu consistency tức thời
- Operation là idempotent hoặc có thể retry

**Asynchronous (Kafka)** phù hợp khi:
- Side-effect không cần block (gửi email, index search)
- Cần decouple thời gian (producer/consumer độc lập)
- Cần broadcast cho nhiều consumer

→ Trong dự án, **order-service** dùng cả hai:
- **Sync** để check stock, validate voucher (cần biết kết quả ngay)
- **Async** để publish `order-created` cho inventory reserve, payment, notification

### 2.3. OpenFeign — Declarative HTTP Client

Feign là HTTP client kiểu "Retrofit" — bạn khai báo interface, Feign sinh proxy ngầm:

```java
@FeignClient(name = "product-service")
public interface ProductFeignClient {
  @GetMapping("/api/internal/products/{id}")
  ProductDto getProduct(@PathVariable("id") String id);
}
```

So với RestTemplate (imperative):
```java
ResponseEntity<ProductDto> resp = restTemplate.exchange(
  "http://product-service/api/internal/products/" + id,
  HttpMethod.GET, null, ProductDto.class);
```

→ Feign đẹp hơn, ít boilerplate. Tích hợp Eureka qua tên service (không cần URL cứng).

### 2.4. Kiến trúc Feign trong Spring Cloud

```
@FeignClient("product-service")
  → Spring Cloud OpenFeign tạo proxy bean
  → Mỗi method call:
    1. Build HTTP request (path, header, body)
    2. Resolve "product-service" qua LoadBalancer + Eureka
    3. HTTP call qua Apache HttpClient/OkHttp
    4. Parse response (Jackson)
    5. Có Resilience4j wrap (circuit breaker, retry)
    6. Return DTO
```

### 2.5. RestTemplate vs WebClient vs Feign

| Client | Style | Concurrency | Status |
|--------|-------|-------------|--------|
| **RestTemplate** | Imperative, blocking | Thread-per-request | Maintenance mode |
| **WebClient** | Reactive, non-blocking | Event loop | Recommended cho reactive app |
| **Feign** | Declarative | Blocking (default), có WebClient mode | Recommended cho microservice client |

→ Order-service dùng **Feign blocking** vì code đơn giản; Gateway dùng **WebClient** ngầm bên trong (Spring Cloud Gateway = WebFlux).

### 2.6. Resilience cho Feign

Wrap Feign call trong:
- **Circuit Breaker** (Resilience4j) — chặn cascading failure
- **Retry** — retry idempotent calls
- **Timeout** — không chờ vô hạn
- **Bulkhead** — giới hạn concurrent calls
- **Fallback** — return default khi failure

```java
@FeignClient(name = "product-service",
             fallback = ProductFeignClientFallback.class)
public interface ProductFeignClient { ... }
```

→ Resilience4j config trong order-service: 50% failure threshold, 10 requests window, 10s wait, 3s timeout.

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. Các Feign call hiện có

| From | To | Mục đích |
|------|----|----|
| cart-service → product-service | Validate product khi add to cart |
| order-service → cart-service | Lấy items giỏ hàng |
| order-service → product-service | Lấy giá, validate tồn tại |
| order-service → inventory-service | Check stock |
| order-service → voucher-service | Validate voucher |
| payment-service → order-service | Lấy thông tin đơn hàng |
| review-service → order-service | Verify user đã mua |
| flash-sale-service → order-service | Tạo đơn flash sale |

### 3.2. Internal API contract
Mỗi service expose 2 lớp API:
- `/api/{resource}` — public, qua Gateway
- `/api/internal/{resource}` — internal, gọi từ service khác

→ Tách biệt giúp:
- Public API có CORS, rate limit, auth
- Internal API tin tưởng caller (Docker network nội bộ)

### 3.3. Identity propagation cho Feign call

Khi service A gọi service B qua Feign, cần forward `X-User-Id`:
```java
@Component
class FeignIdentityInterceptor implements RequestInterceptor {
  @Override
  public void apply(RequestTemplate template) {
    HttpServletRequest req = currentRequest();
    template.header("X-User-Id", req.getHeader("X-User-Id"));
    template.header("X-User-Roles", req.getHeader("X-User-Roles"));
  }
}
```

### 3.4. Distributed Tracing tự động

Micrometer Tracing inject `traceparent` header (W3C Trace Context) vào Feign request — Zipkin có thể nối spans.

---

## 4. Trade-offs khi dùng Synchronous

### Pros
- Code đơn giản, đọc tự nhiên
- Có error response ngay
- Trace dễ — sequence diagram là chính cuộc gọi

### Cons
- **Coupling thời gian** — A đợi B đợi C → cascading slowdown
- **Cascading failure** — B chết → A timeout → request stack pile up
- **Lower availability** — `availability(A) = availability(A) × availability(B) × availability(C)`

→ Chính vì vậy phải có Circuit Breaker + Timeout. Và những phần "không cần ngay" (email, search index) chuyển sang async qua Kafka.

---

## 5. Từ Khóa Nghiên Cứu

```
- representational state transfer fielding dissertation
- spring cloud openfeign declarative client
- feign vs resttemplate vs webclient
- synchronous vs asynchronous microservice communication
- request interceptor feign
- contract first api design
- microservice availability cascading failure
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Em dùng REST mà không dùng gRPC?**
→ REST đơn giản, debug dễ (curl/Postman), Swagger document tự nhiên. gRPC nhanh hơn (HTTP/2, protobuf) nhưng cần `.proto` file, debug khó hơn. Đồ án demo ưu tiên dễ hiểu.

**Q2: Vì sao chọn Feign thay vì RestTemplate/WebClient?**
→ Feign declarative, ít boilerplate. Tích hợp tự nhiên với Eureka và Resilience4j thông qua Spring Cloud.

**Q3: Synchronous call gây cascading failure thế nào?**
→ A gọi B; B chậm → A's thread bị treo → thread pool A đầy → A reject mọi request mới. Em giải bằng (1) Circuit Breaker fail-fast, (2) Timeout 3s, (3) Async cho operation không cần ngay.

**Q4: Khi nào dùng sync, khi nào async?**
→ Sync khi cần response để decision (validate stock trước khi tạo đơn). Async khi side-effect không cần block (email, search index).

**Q5: Vì sao tách `/api/internal/`?**
→ Tách public và internal API: public có rate limit/CORS/auth; internal tin tưởng caller, nhanh hơn. Cũng giúp document Swagger rõ ràng hơn.

---

## 7. Tài Liệu Tham Khảo

- Roy Fielding, *Architectural Styles and the Design of Network-based Software Architectures*, PhD dissertation, 2000
- Spring Cloud OpenFeign documentation
- "Feign Client Tutorial" — Baeldung
- Sam Newman, *Building Microservices*, Chapter 5 — Communication Styles
- "RPC vs REST" — Martin Fowler
