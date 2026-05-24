# 02. Prometheus — Metrics Scraping & PromQL

> Prometheus là **time-series database** chuyên cho metrics. Nó **scrape** (kéo) metrics từ endpoint HTTP của các service mỗi 15s, lưu trữ và cho phép truy vấn bằng PromQL.

## 1. Khái niệm cốt lõi

| Khái niệm | Ý nghĩa |
|-----------|---------|
| **Metric** | Một số đo có tên + labels, ví dụ: `http_server_requests_seconds_count{job="order-service",uri="/api/orders",status="200"}` |
| **Label** | Cặp key=value để phân loại metric (giống dimension trong DB analytic) |
| **Scrape** | Prometheus chủ động gọi HTTP đến `/actuator/prometheus` của service để lấy snapshot metrics |
| **Job** | Một nhóm targets cùng loại (config trong [prometheus.yml](../prometheus/prometheus.yml)) |
| **Counter** | Số chỉ tăng (request count, error count) — luôn dùng `rate()` để có ý nghĩa |
| **Gauge** | Số có thể tăng/giảm (CPU%, memory used, active threads) |
| **Histogram** | Phân phối giá trị, có sẵn `_bucket / _sum / _count` để tính p50/p95/p99 |
| **PromQL** | Ngôn ngữ truy vấn của Prometheus |

## 2. Hệ thống đang dùng Prometheus ra sao

### 2.1 Cấu hình thực tế

- **Container**: `prom/prometheus:v3.11.2`, port `9090`
- **Config**: [prometheus/prometheus.yml](../prometheus/prometheus.yml)
- **Scrape interval**: 15s (global)
- **Targets**: API Gateway + 13 business services, mỗi service một job

Mỗi Spring Boot service expose metrics qua **Micrometer + Prometheus Registry**:

```yaml
# Trong các config của Spring service (configs/*.yml)
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus     # ← bật endpoint /actuator/prometheus
```

Prometheus scrape:
```yaml
- job_name: 'order-service'
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: ['order-service:8086']
```

> **Lưu ý**: target dùng tên container `order-service`, không phải `localhost`. Vì Prometheus chạy trong cùng Docker network `ecommerce-network`.

### 2.2 Các metric quan trọng có sẵn

| Metric | Loại | Dùng để |
|--------|------|---------|
| `http_server_requests_seconds_count` | Counter | Đếm request HTTP |
| `http_server_requests_seconds_sum` | Counter | Tổng latency (giây) |
| `http_server_requests_seconds_bucket` | Histogram | Tính p95/p99 latency |
| `jvm_memory_used_bytes` | Gauge | RAM JVM đang dùng theo `area` (heap/nonheap) |
| `jvm_gc_pause_seconds_*` | Summary | Thời gian GC pause |
| `jvm_threads_live_threads` | Gauge | Số thread đang sống |
| `process_cpu_usage` | Gauge | CPU usage (0..1) của process |
| `hikaricp_connections_active` | Gauge | Số connection DB đang dùng |
| `kafka_consumer_records_consumed_total` | Counter | Số message đã consume từ Kafka |
| `spring_cloud_gateway_requests_seconds_*` | Histogram | Metric riêng của Gateway theo route |

## 3. Workflow vận hành

### 3.1 Truy cập UI

http://localhost:9090

3 trang quan trọng:
- **Status → Targets** (`/targets`) — xem service nào đang UP/DOWN
- **Graph** (`/graph`) — chạy PromQL ad-hoc
- **Status → Configuration** — xem prometheus.yml hiện tại

### 3.2 Verify scrape thành công

```bash
# Tất cả targets phải UP
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'
```

Nếu có target DOWN — xem cột `lastError` trên trang `/targets` để biết lý do (thường: service chưa start, port sai, hoặc network).

### 3.3 PromQL — học bằng ví dụ thực tế

Mở Prometheus UI → **Graph**, copy paste từng query bên dưới:

#### A. Throughput (request/sec) toàn hệ thống

```promql
sum(rate(http_server_requests_seconds_count[1m]))
```
> `rate()` = (delta counter / delta time) trong 1 phút. Luôn dùng `rate()` cho counter.

#### B. Throughput theo service

```promql
sum by (job) (rate(http_server_requests_seconds_count[1m]))
```

#### C. Tỉ lệ error 5xx

```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  /
sum(rate(http_server_requests_seconds_count[5m]))
```

#### D. Latency p95 theo service

```promql
histogram_quantile(
  0.95,
  sum by (job, le) (rate(http_server_requests_seconds_bucket[5m]))
)
```
> `le` = "less than or equal" — bucket boundary của histogram. Phải `sum by (le)` thì `histogram_quantile` mới tính được.

#### E. Top 5 endpoint chậm nhất ở order-service

```promql
topk(5,
  histogram_quantile(0.95,
    sum by (uri, le) (
      rate(http_server_requests_seconds_bucket{job="order-service"}[5m])
    )
  )
)
```

#### F. Memory heap đang dùng (MB) theo service

```promql
jvm_memory_used_bytes{area="heap"} / 1024 / 1024
```

#### G. Connection pool DB sắp cạn

```promql
hikaricp_connections_active / hikaricp_connections_max > 0.8
```

#### H. Kafka consumer lag (cảnh báo nếu service tiêu thụ chậm hơn producer)

```promql
sum by (client_id, topic) (kafka_consumer_records_lag)
```

### 3.4 Tự thêm custom metric trong code

Khi muốn đo 1 business event (ví dụ "số đơn hàng tạo thành công"):

```java
@Component
@RequiredArgsConstructor
public class OrderMetrics {
    private final MeterRegistry registry;

    public void recordOrderCreated(String paymentMethod) {
        registry.counter("orders_created_total",
                "payment_method", paymentMethod).increment();
    }

    public Timer.Sample startOrderProcessing() {
        return Timer.start(registry);
    }
}
```

Sau đó query:
```promql
rate(orders_created_total[5m])
sum by (payment_method) (rate(orders_created_total[5m]))
```

> Convention: tên metric kết thúc bằng `_total` cho counter, đơn vị dùng base SI (`_seconds`, `_bytes`).

## 4. Troubleshooting

### 4.1 Target hiển thị DOWN trên `/targets`

```bash
# Test endpoint từ bên trong Prometheus container
docker compose exec prometheus wget -qO- http://order-service:8086/actuator/prometheus | head

# Nếu fail từ trong container nhưng bên ngoài OK → vấn đề DNS/network
docker compose exec prometheus nslookup order-service
```

Checklist:
1. Service đã expose `prometheus` trong `management.endpoints.web.exposure.include`?
2. Service có port đúng trong `prometheus.yml`?
3. Service đã JOIN network `ecommerce-network`?

### 4.2 Metric bị "missing data" sau restart

Prometheus lưu data trong volume nội bộ container (không mount). Restart Prometheus = mất history. Để giữ data dài hạn, mount volume:
```yaml
prometheus:
  volumes:
    - prometheus_data:/prometheus
    - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
```

### 4.3 PromQL trả về empty

- Thiếu `rate()` cho counter — `http_server_requests_seconds_count` raw chỉ tăng dần, không có ý nghĩa nếu không lấy rate
- Label match sai — copy chính xác từ `/targets` (case-sensitive)
- Time range chưa đủ — query `rate(...[5m])` cần ít nhất 2 data point trong 5 phút

### 4.4 Reload config mà không restart

```bash
# Bật flag --web.enable-lifecycle trong docker-compose nếu chưa có
curl -X POST http://localhost:9090/-/reload
```

### 4.5 Cardinality nổ (memory tăng nhanh)

Tránh đặt label có **cardinality cao** (ví dụ `userId`, `orderId`) vào metric. Mỗi tổ hợp label = 1 time-series. 1 triệu user = 1 triệu time-series → Prometheus sẽ OOM.

Pattern đúng:
- ✅ `orders_created_total{status="paid"}`
- ❌ `orders_created_total{order_id="ord-123"}`

## 5. Best practices đang được áp dụng

- **Per-service job** — dễ filter theo `job=...` thay vì lẫn lộn label
- **Mỗi service expose `/actuator/prometheus` qua actuator** — chuẩn Spring Boot, không phải custom
- **Scrape interval 15s** — đủ để dashboard mượt, không quá tốn resource
- **Histogram cho latency** — nhờ đó tính được p95/p99 mà không cần tự lưu raw value
- **Trace context propagation** — Micrometer kế thừa `traceId`/`spanId` vào exemplar (xem [04-zipkin.md](04-zipkin.md))
