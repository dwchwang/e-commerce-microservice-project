# Monitoring & Observability

## Tổng Quan Stack

```
Services → /actuator/prometheus → Prometheus (scrape 15s) → Grafana (dashboards)
Services → /actuator/health    → Docker healthcheck
Services → Zipkin              → Distributed Tracing UI
```

---

## Grafana — Dashboard Metrics

### Truy cập
- URL: http://localhost:3000
- User: `admin`
- Password: Giá trị `GF_SECURITY_ADMIN_PASSWORD` trong `.env`

### Dashboards Sẵn Có

| Dashboard | Mô tả |
|-----------|-------|
| **Spring Boot Overview** | HTTP request rate, error rate, latency của tất cả service |
| **JVM Overview** | Heap memory, GC time, thread count, CPU usage |
| **E-commerce Saga Overview** | Metrics nghiệp vụ: orders, payments, inventory |

### Cách Xem Dashboard
1. Đăng nhập Grafana
2. Click **Dashboards** (icon 4 ô vuông bên trái)
3. Chọn folder **E-Commerce**
4. Click tên dashboard

### Thêm Dashboard Tự Tạo
1. Grafana → **+** → **New dashboard**
2. **Add visualization** → chọn data source **Prometheus**
3. Viết PromQL query

---

## Prometheus — Metrics Collection

### Truy cập
- URL: http://localhost:9090
- Targets: http://localhost:9090/targets

### Kiểm Tra Targets
Tất cả 14 service phải ở trạng thái **UP** (màu xanh lá) trên trang Targets.

Nếu một service **DOWN** trên Prometheus:
1. Kiểm tra service đó có đang chạy: `docker compose ps service-name`
2. Kiểm tra `/actuator/prometheus` endpoint: `curl http://localhost:PORT/actuator/prometheus`

### PromQL Queries Hữu Ích

```promql
# HTTP request rate của tất cả service
rate(http_server_requests_seconds_count[1m])

# Error rate (5xx)
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# JVM heap usage
jvm_memory_used_bytes{area="heap"}

# CPU usage
process_cpu_usage

# Kafka messages published
kafka_producer_record_send_total

# Active connections
hikaricp_connections_active
```

---

## Zipkin — Distributed Tracing

### Truy cập
- URL: http://localhost:9411

### Cách Sử Dụng

1. Gửi một request qua API Gateway (ví dụ: tạo đơn hàng)
2. Truy cập Zipkin → **Find a trace**
3. Chọn service `api-gateway` và khoảng thời gian
4. Click **Run query** → Click vào trace để xem chi tiết

### Trace Cho Thấy Gì

Một trace tạo đơn hàng điển hình sẽ có:
```
api-gateway (10ms)
  └── order-service (150ms)
        ├── product-service (20ms)  — kiểm tra sản phẩm
        ├── inventory-service (15ms) — kiểm tra tồn kho
        ├── cart-service (10ms)     — lấy giỏ hàng
        └── voucher-service (8ms)  — kiểm tra voucher
```

### Log Có Trace ID

Mỗi log dòng có `traceId` và `spanId` để correlate với Zipkin:
```bash
docker compose logs order-service | grep "traceId"
# [traceId=abc123, spanId=def456] Processing order...
```

---

## Health Checks

### Xem Health Của Tất Cả Services

```bash
# Health tổng hợp qua Gateway
curl http://localhost:8080/actuator/health | python3 -m json.tool

# Health của từng service
for port in 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093; do
  echo -n "Port $port: "
  curl -s "http://localhost:$port/actuator/health/liveness" 2>/dev/null || echo "unreachable"
done
```

### Liveness vs Readiness

| Endpoint | Ý nghĩa |
|----------|---------|
| `/actuator/health/liveness` | Service đang chạy (process alive) |
| `/actuator/health/readiness` | Service sẵn sàng nhận request (DB connected, Kafka connected) |

---

## Actuator Endpoints Hữu Ích

```bash
# Service info
curl http://localhost:8080/actuator/info

# Metrics list
curl http://localhost:8080/actuator/metrics

# Metric cụ thể
curl http://localhost:8080/actuator/metrics/http.server.requests

# Tất cả routes của Gateway
curl http://localhost:8080/actuator/gateway/routes

# Circuit breaker status
curl http://localhost:8086/actuator/circuitbreakers
```

---

## Alerts (Cảnh Báo)

Grafana có thể cấu hình alert:
1. Dashboard → chọn panel → **Edit**
2. Tab **Alert** → **New alert rule**
3. Đặt điều kiện (ví dụ: error rate > 5%)
4. Cấu hình notification channel (email, Slack, ...)
