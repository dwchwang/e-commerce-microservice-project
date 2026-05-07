# 17. Observability — Metrics với Micrometer, Prometheus, Grafana

## 1. Mục Tiêu Nghiên Cứu

- Hiểu 3 trụ cột Observability: Metrics, Logs, Traces
- Hiểu Micrometer — vendor-neutral abstraction
- Hiểu Prometheus pull model + PromQL
- Hiểu Grafana dashboard
- Hiểu RED và USE methodology

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. 3 Trụ Cột Observability

| Pillar | Câu hỏi trả lời | Tool |
|--------|-----------------|------|
| **Metrics** | "Hệ thống đang khỏe không?" (số liệu aggregate) | Prometheus, Grafana |
| **Logs** | "Chuyện gì đã xảy ra?" (event chi tiết) | ELK, Loki |
| **Traces** | "Request đã đi qua đâu?" (path 1 request) | Zipkin, Jaeger |

### 2.2. Monitoring vs Observability

- **Monitoring**: Theo dõi known issues (predefined dashboards, alerts)
- **Observability**: Khả năng *infer state* hệ thống từ output, đặc biệt là *unknown-unknowns*

### 2.3. RED Method (Tom Wilkie)

Cho mỗi service, monitor:
- **R**ate: Số request/giây
- **E**rrors: Số request lỗi/giây
- **D**uration: Latency phân bố (p50, p95, p99)

→ Phù hợp service-oriented architecture.

### 2.4. USE Method (Brendan Gregg)

Cho mỗi resource, monitor:
- **U**tilization: % thời gian busy
- **S**aturation: Queue/wait
- **E**rrors

→ Phù hợp infrastructure (CPU, RAM, disk, network).

### 2.5. Metric Types (Prometheus)

| Type | Mô tả | Ví dụ |
|------|-------|-------|
| **Counter** | Tăng đơn điệu | `http_requests_total` |
| **Gauge** | Lên-xuống | `jvm_memory_used_bytes` |
| **Histogram** | Phân bố (buckets) | `http_request_duration_seconds` |
| **Summary** | Quantiles tính sẵn client-side | (ít dùng) |

### 2.6. Prometheus

**Pull model**: Prometheus định kỳ scrape `/metrics` endpoint của target.

```
Prometheus (scrape mỗi 15s)
   ├── http_requests_total{service="order", status="200"} 1234
   ├── http_request_duration_seconds_bucket{le="0.1"} 800
   └── ...
```

**Pros pull**:
- Service không cần biết Prometheus URL
- Health check tự động (target down → alert)
- Test endpoint dễ (curl)

**Cons pull**:
- Khó với job ngắn hạn (push gateway)
- Network firewall friendly?

### 2.7. PromQL — Prometheus Query Language

```promql
# Rate of HTTP requests per second
rate(http_server_requests_seconds_count[1m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[1m])
  / rate(http_server_requests_seconds_count[1m])

# p95 latency
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket[5m]))

# JVM heap usage by service
sum by (application) (jvm_memory_used_bytes{area="heap"})
```

### 2.8. Micrometer — Spring Boot's Metric Facade

> "Micrometer is to metrics what SLF4J is to logging."

Vendor-neutral API. Backends: Prometheus, Datadog, CloudWatch, ...

Auto-instrumentation:
- HTTP server: `http.server.requests`
- DB pool: `hikaricp.connections`
- JVM: `jvm.memory.*`, `jvm.gc.*`, `jvm.threads.*`
- Tomcat: `tomcat.sessions.*`
- Kafka: `kafka.consumer.*`, `kafka.producer.*`
- Logback: `logback.events`

Custom metric:
```java
@Component
class OrderMetrics {
  private final Counter ordersCreated;
  
  OrderMetrics(MeterRegistry registry) {
    ordersCreated = Counter.builder("orders.created.total")
        .description("Total orders created")
        .tag("service", "order-service")
        .register(registry);
  }
  
  void incrementCreated() { ordersCreated.increment(); }
}
```

### 2.9. Grafana

UI to visualize Prometheus data:
- **Dashboard**: collection of panels
- **Panel**: 1 query + visualization (graph, table, gauge, ...)
- **Variable**: dropdown filter (vd: chọn service)
- **Alerting**: rule trigger qua email, Slack, PagerDuty

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. Mỗi service expose `/actuator/prometheus`

`pom.xml`:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

`application.yml`:
```yaml
management:
  endpoints.web.exposure.include: health,info,prometheus,metrics
  endpoint.prometheus.enabled: true
  metrics.tags:
    application: ${spring.application.name}
```

### 3.2. Prometheus config (`prometheus/prometheus.yml`)

```yaml
scrape_configs:
  - job_name: 'spring-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'api-gateway:8080'
          - 'order-service:8086'
          - 'payment-service:8087'
          - ...
```

→ Truy cập http://localhost:9090/targets → kiểm tra các target xanh: API Gateway + 13 business services. Prometheus itself cũng có một target riêng.

### 3.3. Grafana dashboards (3 cái pre-provisioned)

1. **Spring Boot Overview** — request rate, error rate, latency cho mỗi service
2. **JVM Overview** — heap, GC, threads, CPU
3. **E-commerce Saga Overview** — business metrics: orders/min, payments success/fail rate, inventory reservations

### 3.4. Custom business metrics

| Metric | Type | Tag |
|--------|------|-----|
| `orders.created.total` | Counter | service, paymentMethod |
| `orders.cancelled.total` | Counter | service, reason |
| `payments.success.total` | Counter | provider |
| `payments.failed.total` | Counter | provider, reason |
| `inventory.reserved.total` | Counter | productId |
| `flash_sale.purchases.total` | Counter | campaignId, result |
| `circuit_breaker.state` | Gauge | name |

### 3.5. Healthcheck integration

Docker Compose:
```yaml
order-service:
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8086/actuator/health/liveness"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 60s
```

---

## 4. Alerting (Bổ sung)

Có thể add Prometheus Alertmanager:
```yaml
groups:
- name: critical
  rules:
  - alert: HighErrorRate
    expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
    for: 5m
    annotations:
      summary: "Error rate > 5%"
```

→ Đồ án không bắt buộc, nhưng có thể nêu là future work.

---

## 5. SLI / SLO / SLA (Google SRE)

| Term | Ý nghĩa |
|------|---------|
| **SLI** (Service Level Indicator) | Metric đo được (vd: latency p99) |
| **SLO** (Service Level Objective) | Mục tiêu nội bộ (vd: p99 < 500ms 99% time/30 days) |
| **SLA** (Service Level Agreement) | Hợp đồng với khách hàng |
| **Error budget** | 1 - SLO = bao nhiêu được phép fail |

---

## 6. Từ Khóa Nghiên Cứu

```
- observability three pillars metrics logs traces
- micrometer registry prometheus
- prometheus pull model architecture
- promql histogram quantile
- grafana dashboard provisioning
- RED method tom wilkie
- USE method brendan gregg
- google sre slo error budget
- spring boot actuator metrics
```

---

## 7. Câu Hỏi Phản Biện

**Q1: Pull vs Push monitoring, khác gì?**
→ Push (Graphite, StatsD): app gửi metric đến server. Đơn giản nhưng app phải biết server URL, hard to detect "im lặng" (app crash). Pull (Prometheus): server scrape app. Health check tự nhiên — target down = alert.

**Q2: Micrometer làm gì?**
→ Abstraction giống SLF4J cho metrics. Cho phép switch backend (Prometheus, Datadog, ...) không sửa code. Tích hợp Spring Boot, auto-instrument HTTP/DB/JVM.

**Q3: Histogram vs Summary?**
→ Histogram: bucket counters, có thể aggregate cross-instance (rate + histogram_quantile). Summary: quantile tính client-side, không aggregate được. Histogram phổ biến hơn.

**Q4: Em đặt p95 latency target bao nhiêu?**
→ Tùy endpoint. `/api/products` (GET): p95 < 200ms. `/api/orders` (POST): p95 < 1s (do saga). Flash sale purchase: p95 < 300ms.

**Q5: Custom metric vs auto-instrumented, khi nào dùng?**
→ Auto-instrumented (HTTP, JVM) là technical metrics. Custom là business metrics — cần khi muốn track domain event (orders/min, GMV) không thể infer từ HTTP metric.

**Q6: Prometheus retention bao lâu?**
→ Default 15 ngày local. Production: long-term storage (Thanos, Cortex, Mimir). Đồ án 15 ngày đủ demo.

**Q7: Cardinality của tag/label cao có vấn đề không?**
→ Có — mỗi unique combination = 1 series. Tránh tag với userId, orderId (high cardinality). Tag chỉ cho dimension finite (service, status, method, ...).

---

## 8. Tài Liệu Tham Khảo

### Sách
- Cindy Sridharan, *Distributed Systems Observability*, O'Reilly, 2018
- Betsy Beyer et al, *Site Reliability Engineering*, Google/O'Reilly, 2016
- Brendan Gregg, *Systems Performance*, 2nd ed.
- James Turnbull, *The Art of Monitoring*

### Documentation
- prometheus.io/docs
- micrometer.io
- grafana.com/docs
- Spring Boot Actuator reference

### Bài viết
- Tom Wilkie — "RED Method" (Weaveworks blog)
- Brendan Gregg — "USE Method" (brendangregg.com)
- Google SRE Book — Chapter 4 — "Service Level Objectives"
