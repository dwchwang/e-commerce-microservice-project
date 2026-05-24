# 04. Zipkin — Distributed Tracing

> Zipkin trả lời câu hỏi: **"1 request đi qua những service nào, bước nào chậm?"**. Khác với Prometheus (đo aggregate), Zipkin theo dõi **từng request riêng lẻ** xuyên qua các service nhờ `traceId`.

## 1. Khái niệm cốt lõi

| Khái niệm | Ý nghĩa |
|-----------|---------|
| **Trace** | Toàn bộ hành trình của 1 request, có 1 `traceId` duy nhất (16 bytes hex) |
| **Span** | Một đơn vị công việc trong trace (ví dụ: HTTP call, DB query, Kafka send). Có `spanId` và `parentSpanId` |
| **B3 propagation** | Chuẩn header để truyền traceId/spanId giữa service: `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-ParentSpanId` |
| **Sampling** | Tỉ lệ request được gửi span lên Zipkin. Project này set `1.0` = 100% (cho dev) |
| **Micrometer Tracing** | Lib trong Spring Boot 3 phụ trách auto-instrument HTTP/Kafka/DB và export sang Zipkin |

## 2. Hệ thống đang dùng Zipkin ra sao

### 2.1 Cấu hình thực tế

- **Container**: `openzipkin/zipkin:3.5.0`, port `9411`
- **Mode**: in-memory (mất data khi restart). Nâng cấp lên Elasticsearch storage nếu cần persist
- **Endpoint nhận spans**: `http://zipkin:9411/api/v2/spans`

Mọi service Spring đều cấu hình:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0      # ← 100% sample, dev mode
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://zipkin:9411/api/v2/spans}

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

Pattern log trên cực kỳ quan trọng — mỗi log line đều **kèm theo traceId/spanId** để bạn có thể grep log và đối chiếu với UI Zipkin.

### 2.2 Các thư viện auto-instrument

Spring Boot 3.5 dùng **Micrometer Tracing** (replace Sleuth). Khi có dependency:
- `io.micrometer:micrometer-tracing-bridge-brave`
- `io.zipkin.reporter2:zipkin-reporter-brave`

→ Tự động instrument:
- **Spring MVC / WebFlux** — tạo span cho mỗi HTTP request
- **OpenFeign** — propagate B3 headers khi service gọi service
- **Spring Kafka** — trace producer/consumer
- **Spring Data JPA** (qua Hibernate) — span cho query
- **WebClient / RestTemplate** — span cho outbound HTTP

## 3. Workflow vận hành

### 3.1 Truy cập UI

http://localhost:9411

Tab quan trọng:
- **Find a trace** — search trace
- **Dependencies** — đồ thị các service gọi nhau

### 3.2 Tạo trace bằng request thật

```bash
# Lấy access token (xem 01-keycloak.md)
ACCESS_TOKEN=...

# Gọi 1 endpoint phức tạp đi qua nhiều service
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8080/api/orders \
  -d '{"items":[{"productId":1,"quantity":2}]}'
```

Sau ~5 giây, mở Zipkin UI:
- **Service Name**: `api-gateway`
- **Lookback**: 15 minutes
- Click **Run Query**

Bạn sẽ thấy 1 trace với nhiều span:
```
api-gateway POST /api/orders                  120ms
└─ order-service POST /api/orders             100ms
   ├─ order-service feign productServiceClient 30ms
   │  └─ product-service GET /api/products/1   25ms
   ├─ order-service feign inventoryClient      20ms
   │  └─ inventory-service POST /api/inventory/reserve 15ms
   └─ order-service kafka send order.created   5ms
```

Click vào từng span để xem **annotation** (timing chi tiết) và **tags** (HTTP method, status, URI...).

### 3.3 Tìm trace từ traceId trong log

Khi user báo lỗi vào lúc T, bạn xem log để tìm traceId:

```bash
# Filter log ở 1 service
docker compose logs --since=10m order-service | grep "ERROR"

# Output có dạng:
# ERROR [order-service,9c1f8b2e3a4d5e6f,abcd1234] OrderServiceImpl - Stock not enough...
#                       ^^^^^^^^^^^^^^^ ← traceId
```

Copy traceId, paste vào Zipkin UI → ô **Trace ID** → tìm.

Hoặc dùng URL trực tiếp:
```
http://localhost:9411/zipkin/traces/9c1f8b2e3a4d5e6f
```

### 3.4 Đọc trace để debug

3 thứ cần nhìn:

| Quan sát | Ý nghĩa |
|----------|---------|
| **Span dài bất thường** | Bottleneck — DB chậm, external API chậm, lock contention |
| **Span có status > 400** | Lỗi — click xem error message trong tags |
| **Span "đứt"** giữa chừng | Service không propagate context — kiểm tra Feign/Kafka config |

### 3.5 Custom span trong code

Khi muốn trace 1 đoạn business logic riêng:

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl {
    private final Tracer tracer;   // io.micrometer.tracing.Tracer

    public Order createOrder(CreateOrderRequest req) {
        Span span = tracer.nextSpan().name("validate-stock").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("orderItemCount", String.valueOf(req.items().size()));
            // ... business logic
            return order;
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 3.6 Xem dependency graph

Tab **Dependencies** trong Zipkin → chọn time range → Run.

Đồ thị này hữu ích để:
- Tìm service "central" (nhiều mũi tên đi vào) — candidate cho rate limit
- Phát hiện coupling không mong muốn (service A gọi service B mà bạn nghĩ không gọi)

## 4. Troubleshooting

### 4.1 Không thấy trace nào trong UI

Theo thứ tự:

1. Service đã gửi span chưa?
   ```bash
   docker compose logs order-service | grep -i zipkin
   ```
2. Network — service gọi được Zipkin không?
   ```bash
   docker compose exec order-service \
     wget -qO- http://zipkin:9411/health
   ```
3. Sampling = 0 → set lại `management.tracing.sampling.probability: 1.0`
4. Zipkin chưa healthy → `docker compose ps zipkin`

### 4.2 Trace bị "đứt" — span của service B không nối với A

Nguyên nhân thường: **B3 headers không được propagate**.

| Tình huống | Cách fix |
|------------|----------|
| HTTP call dùng `RestTemplate` | Inject `RestTemplate` từ `RestTemplateBuilder` (Spring auto-config) — đừng `new RestTemplate()` |
| Feign client | Đảm bảo có `spring-cloud-starter-openfeign` + tracing bridge — đã có sẵn trong project |
| Kafka producer | `KafkaTemplate` từ Spring auto-config sẽ tự inject headers |
| Async (`@Async`, `CompletableFuture`) | Dùng `TaskDecorator` để copy MDC + tracing context sang thread mới |
| ExecutorService thuần | Wrap bằng `ContextSnapshotFactory` của Micrometer |

### 4.3 traceId trong log nhưng không có trong Zipkin

- Span đã được tạo (có MDC) nhưng chưa được report do crash trước khi flush. Tăng `spring.zipkin.message-timeout` hoặc check Zipkin reporter logs.
- Sampling drop — nếu set `0.1` thì 90% trace bị drop, log vẫn có traceId nhưng không gửi.

### 4.4 Storage in-memory bị đầy

```bash
docker compose restart zipkin
```

Để dùng Elasticsearch persistent (production):
```yaml
zipkin:
  environment:
    STORAGE_TYPE: elasticsearch
    ES_HOSTS: http://elasticsearch:9200
```

### 4.5 Performance: sampling 100% có ổn cho prod không?

Không. Production thường:
- `0.1` (10%) cho service traffic cao
- `1.0` cho service ít traffic / critical (auth, payment)
- Dùng **rate-limited sampling** thay vì probabilistic — guarantee không quá N trace/giây

Cấu hình:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1
```

## 5. Best practices đang được áp dụng

- **traceId/spanId trong log pattern** — đối chiếu log ↔ trace cực nhanh
- **B3 propagation tự động qua Feign + Kafka + WebClient** — dev không phải làm thủ công
- **Sampling 100% cho dev** — giúp khi học/debug, nhưng cần giảm khi prod
- **Zipkin in-memory cho local** — đơn giản, không cần Elasticsearch khi học
- **Dependency graph** — xác minh kiến trúc thực tế khớp với thiết kế
