# 11. Resilience4j — Circuit Breaker, Retry, Rate Limiting

> Cập nhật sau Phase 13: hệ thống đã có smoke/security pass và một số resilience test trên AWS. Kết quả cần viết trung thực: kill order-service chứng minh recovery sau downtime; inventory-failed compensation pass; Kafka replay và Redis cart degradation chưa đủ bằng chứng kết luận pass.

## 1. Mục Tiêu Nghiên Cứu

- Hiểu Resilience patterns: Circuit Breaker, Retry, Bulkhead, Timeout, Rate Limiter, Fallback
- Hiểu Resilience4j (kế thừa Hystrix)
- Hiểu Token Bucket vs Leaky Bucket
- Phân biệt Rate Limiting ở Gateway vs ở service

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Tại sao cần Resilience?

Distributed system **luôn** có thành phần fail. Không xử lý → cascading failure:
```
inventory-service chậm
  → order-service Feign call timeout 30s
  → order-service thread pool đầy
  → order-service không nhận request mới
  → Gateway timeout
  → user thấy 504
```

### 2.2. Resilience Patterns (Michael Nygard, *Release It!*)

| Pattern | Mục đích |
|---------|---------|
| **Timeout** | Không chờ vô hạn |
| **Retry** | Thử lại khi failure transient |
| **Circuit Breaker** | Stop calling khi service đang fail |
| **Bulkhead** | Cô lập resource pool |
| **Rate Limiter** | Giới hạn số request |
| **Fallback** | Trả default khi failure |
| **Time Limiter** | Cancel async sau timeout |

### 2.3. Circuit Breaker (Michael Nygard, 2007)

3 states:
- **CLOSED**: Bình thường, request pass through. Đếm failure rate
- **OPEN**: Lỗi vượt ngưỡng → block tất cả request, return fallback ngay (fail-fast)
- **HALF_OPEN**: Sau wait time, cho thử N request. Nếu OK → CLOSED, nếu fail → OPEN lại

```
CLOSED ──[failure rate > threshold]──► OPEN
  ▲                                      │
  │ [test calls succeed]                  │ [wait duration]
  │                                      ▼
  └────────────────────── HALF_OPEN ◄────┘
                              │
                              └─[test calls fail]─► OPEN
```

**Tham số quan trọng**:
- `failureRateThreshold`: % lỗi (vd: 50%)
- `slidingWindowSize`: Cửa sổ đếm (vd: 10 requests)
- `minimumNumberOfCalls`: Số call tối thiểu để evaluate
- `waitDurationInOpenState`: Bao lâu OPEN trước khi HALF_OPEN
- `permittedNumberOfCallsInHalfOpenState`: Số call thử khi HALF_OPEN

### 2.4. Retry Pattern

Phù hợp với **transient failure** (network blip, brief unavailability).

**Tham số**:
- `maxAttempts`: Số lần thử (gồm cả lần đầu)
- `waitDuration`: Khoảng cách giữa lần
- `retryExceptions` / `ignoreExceptions`: Loại exception nào trigger retry
- **Exponential backoff**: 100ms → 200ms → 400ms → ...
- **Jitter**: Cộng random vào để tránh thundering herd

**Quan trọng**: Chỉ retry **idempotent operations** (GET, PUT, DELETE). Không retry POST tạo đơn vì có thể tạo trùng → cần idempotency key.

### 2.5. Bulkhead Pattern

Cô lập resource pool để 1 call chậm không làm chết các call khác.

**Bulkhead semantic** (semaphore): Giới hạn số concurrent call.
**Bulkhead thread pool**: Mỗi loại call chạy thread pool riêng.

→ Trong dự án không dùng explicit Bulkhead nhưng ngầm có vì mỗi Feign call tạo HTTP connection riêng.

### 2.6. Rate Limiting

**Mục đích**:
- Bảo vệ service khỏi overload
- Chống abuse (bot, DDoS)
- Fair usage giữa user

**Algorithms**:

| Algorithm | Mô tả | Ưu/Nhược |
|-----------|-------|----------|
| **Token Bucket** | Bucket có dung lượng N, nạp lại R tokens/s. Mỗi request lấy 1 token | Cho phép burst, đơn giản |
| **Leaky Bucket** | Request vào queue, drain ra với rate cố định | Smooth rate, có thể block |
| **Fixed Window** | Đếm request trong cửa sổ thời gian (vd: 100 req/phút) | Đơn giản, có spike ở biên window |
| **Sliding Window** | Cửa sổ trượt | Mượt hơn, phức tạp |
| **Sliding Window Counter** | Hybrid | Best balance |

**Spring Cloud Gateway dùng Token Bucket** (Redis Lua atomic).

### 2.7. Resilience4j

Java library kế thừa Hystrix (Netflix Hystrix bị deprecate vì khó scale ở Netflix scale). Resilience4j:
- Lightweight (~150KB)
- Functional API + Spring integration
- Module: CircuitBreaker, Retry, Bulkhead, RateLimiter, TimeLimiter, Cache

```java
@CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
@Retry(name = "productService")
public ProductDto getProduct(String id) {
  return productClient.getProduct(id);
}

private ProductDto getProductFallback(String id, Throwable t) {
  return new ProductDto(id, "(unavailable)", BigDecimal.ZERO);
}
```

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. Circuit Breaker trong order-service

| Circuit | Failure Threshold | Window | Wait OPEN | Timeout |
|---------|-------------------|--------|-----------|---------|
| productService | 50% | 10 requests | 10s | 3s |
| voucherService | 50% | 10 requests | 10s | 3s |
| cartService | 60% | 5 requests | 5s | 2s |

Cấu hình `application.yml`:
```yaml
resilience4j.circuitbreaker:
  instances:
    productService:
      failureRateThreshold: 50
      slidingWindowSize: 10
      minimumNumberOfCalls: 5
      waitDurationInOpenState: 10s
      permittedNumberOfCallsInHalfOpenState: 3
resilience4j.retry:
  instances:
    productService:
      maxAttempts: 3
      waitDuration: 500ms
resilience4j.timelimiter:
  instances:
    productService:
      timeoutDuration: 3s
```

### 3.2. Rate Limiting ở Gateway

Spring Cloud Gateway + Redis-backed rate limiter:

| Route | Rate | Burst | Key |
|-------|------|-------|-----|
| `/api/auth/**` | 10/s | 20 | IP |
| `/api/orders/**` | 10/s | 20 | userId |
| `/api/flash-sales/*/purchase` POST | 3/s | 5 | userId |

Implementation:
```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 10
      redis-rate-limiter.burstCapacity: 20
      key-resolver: '#{@userKeyResolver}'
```

KeyResolver bean:
```java
@Bean
KeyResolver userKeyResolver() {
  return exchange -> Mono.justOrEmpty(
    exchange.getRequest().getHeaders().getFirst("X-User-Id")
  ).switchIfEmpty(Mono.just("anonymous"));
}
```

### 3.3. Vì sao flash-sale rate limit thấp nhất?

Route `POST /api/flash-sales/*/purchase` là **route nhạy cảm nhất**:
- Atomic Redis decrement → mỗi call là race condition
- Bot có thể spam để chiếm slot
- Limit 3 req/giây/user vẫn cho phép user mua, nhưng bot bị chặn

### 3.4. Quan sát Circuit Breaker

Hiện cấu hình Actuator đang expose `health,info,prometheus`. Vì vậy báo cáo nên ưu tiên quan sát circuit breaker qua Prometheus/Grafana. Nếu muốn dùng endpoint trực tiếp, cần bổ sung `circuitbreakers,circuitbreakerevents` vào `management.endpoints.web.exposure.include`.

```bash
curl http://localhost:8086/actuator/circuitbreakers
# {
#   "circuitBreakers": ["productService", "voucherService", "cartService"]
# }

curl http://localhost:8086/actuator/circuitbreakers/productService
# state: CLOSED, failureRate: 0%, ...
```

Prometheus metric:
```
resilience4j_circuitbreaker_state{name="productService"} 0  # 0=CLOSED, 1=OPEN, 2=HALF_OPEN
resilience4j_circuitbreaker_calls_total{name="productService", kind="successful"} 100
```

---

## 4. Kết Hợp Patterns: Defense in Depth

```
Client → Gateway (rate limit) → Backend
                                    ↓
                         (Feign call out)
                                    ↓
                            [Time Limiter] ── timeout 3s
                                    ↓
                            [Circuit Breaker] ── fail-fast nếu OPEN
                                    ↓
                                [Retry] ── 3 lần, jitter
                                    ↓
                              [Fallback] ── default value khi fail
```

---

## 5. Kết Quả Thực Nghiệm Phase 13

| Scenario | Kỳ vọng | Kết quả thực tế | Cách đưa vào báo cáo |
|---|---|---|---|
| Kill order-service | Khi service bị dừng cưỡng bức, checkout fail có kiểm soát và phục hồi sau restart | Trong 30s downtime: `order_created` còn 47.15%, HTTP failure 25.94%; sau restart health gateway/order trả 200 | Dùng làm bằng chứng recovery, không gọi là zero-downtime |
| Inventory failure compensation | Order bị hủy khi inventory không đủ, reserved stock không bị treo | Order `4170422f-fc2d-49f8-aa5f-c7cd1d79266a` chuyển `CANCELLED`, inventory `quantity=0`, `reserved_quantity=0` | Dùng làm bằng chứng saga compensation |
| Kill Kafka | Outbox giữ event khi Kafka down và replay sau restart | Kafka stop/start xong nhưng order probe trả empty response; outbox replay chưa verify | Chỉ ghi là attempted/inconclusive |
| Kill Redis | Redis-dependent feature degrade, Postgres-backed read vẫn hoạt động | Product list trả 200 trong/after outage; cart probe 401 do token expired | Chỉ dùng phần product read, không kết luận cart |

Nguồn: `.test/results/SUMMARY.md`, `.docs/08-testing-and-evaluation.md`, `.test/results/chaos-*`.

---

## 6. Từ Khóa Nghiên Cứu

```
- circuit breaker pattern michael nygard
- resilience4j hystrix replacement
- bulkhead semaphore thread pool
- token bucket vs leaky bucket algorithm
- exponential backoff jitter retry
- thundering herd cache stampede
- backpressure flow control
- spring cloud gateway redis rate limiter
- release it stability patterns
- aws builder library prepare for failure
```

---

## 7. Câu Hỏi Phản Biện

**Q1: Vì sao chọn Resilience4j thay vì Hystrix?**
→ Hystrix đã được Netflix đưa vào maintenance mode (2018). Resilience4j lightweight, modular, functional API, tích hợp Spring tốt hơn.

**Q2: Em đặt Circuit Breaker thresholds dựa trên gì?**
→ Dựa trên SLA của downstream service. Em test trong dev với chaos test (kill service ngẫu nhiên) để tune threshold. 50% failure rate khá phổ biến cho production e-commerce.

**Q3: Retry có thể làm tệ hơn không?**
→ Có — nếu downstream đang quá tải, retry lại càng overwhelm. Cần exponential backoff + jitter. Cẩn thận với non-idempotent operation.

**Q4: Rate limit theo IP hay userId?**
→ Phụ thuộc route:
- `/api/auth/login`: theo IP (chưa đăng nhập)
- `/api/orders/`: theo userId (cần phân biệt user)
- Có thể combine: theo IP + userId

**Q5: Nếu rate limit Redis chết thì sao?**
→ Spring Cloud Gateway có tùy chọn `denyEmptyKey: false` — fall back to no rate limiting. Trong dự án em chọn allow để giữ availability — chấp nhận risk.

**Q6: Tại sao flash-sale có rate limit thấp hơn order?**
→ Flash-sale dễ bị bot abuse vì có quantity giới hạn. 3 req/giây là tốc độ user thật click, bot phải chạy ở mức đó → ít hiệu quả.

**Q7: Em có dùng Bulkhead không?**
→ Em không cấu hình Resilience4j Bulkhead riêng. Tomcat thread pool và HTTP client connection pool ngầm là bulkhead mức service. Nếu cần precise: thêm bulkhead per circuit.

**Q8: Khi Circuit OPEN, fallback trả gì?**
→ Tùy — productService fallback có thể trả product info từ cache. cartService fallback có thể trả empty cart + log alert. Voucher fallback có thể trả "no discount". Phải fail-graceful.

---

## 8. Tài Liệu Tham Khảo

### Sách
- **Michael Nygard**, *Release It! Design and Deploy Production-Ready Software*, 2nd ed., Pragmatic Bookshelf, 2018 — sách kinh điển về resilience patterns
- Brendan Gregg, *Systems Performance*, 2nd ed.

### Documentation
- https://resilience4j.readme.io/docs
- Spring Cloud Circuit Breaker
- Spring Cloud Gateway — Request Rate Limiter

### Bài viết
- Martin Fowler — "Circuit Breaker"
- AWS Builders' Library — "Avoiding insurmountable queue backlogs"
- Marc Brooker — "Exponential Backoff and Jitter" (AWS Architecture Blog)
- "Hystrix in Maintenance Mode" — Netflix Tech Blog 2018
