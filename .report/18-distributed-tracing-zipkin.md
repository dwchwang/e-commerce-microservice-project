# 18. Distributed Tracing — Zipkin, Micrometer Tracing, W3C Trace Context

## 1. Mục Tiêu Nghiên Cứu

- Hiểu vấn đề tracing trong microservice
- Hiểu Trace, Span, Context propagation
- Hiểu W3C Trace Context spec
- Hiểu Zipkin vs Jaeger vs OpenTelemetry
- Hiểu Spring Boot 3 Micrometer Tracing (thay Sleuth)

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Vấn đề
Trong monolith: 1 stack trace là đủ debug.
Trong microservice: 1 request đi qua 5–10 service. Log riêng từng service không thấy được flow tổng thể.

→ Cần **trace ID chung** propagate qua mọi service, gom log/span lại theo trace.

### 2.2. Khái niệm

| Term | Ý nghĩa |
|------|---------|
| **Trace** | Toàn bộ hành trình của 1 request (cây spans) |
| **Span** | 1 đơn vị công việc (1 service call, 1 DB query) |
| **Trace ID** | UUID 128-bit, chung toàn trace |
| **Span ID** | UUID 64-bit, unique mỗi span |
| **Parent Span ID** | Span cha — tạo cấu trúc cây |
| **Baggage** | Key-value pairs propagate qua services (vd: userId, tenantId) |
| **Sampling** | % trace được record (1% production để giảm cost) |

### 2.3. Cấu trúc một trace

```
Trace abc123 (total 250ms)
├── Span 1 [api-gateway] HTTP POST /api/orders                250ms
│   └── Span 2 [order-service] OrderController.createOrder    240ms
│       ├── Span 3 [order-service → cart-service]             20ms
│       ├── Span 4 [order-service → product-service]          30ms
│       ├── Span 5 [order-service → inventory-service]        25ms
│       ├── Span 6 [order-service] DB INSERT order            10ms
│       └── Span 7 [order-service → Kafka]                    5ms
```

### 2.4. Context Propagation — W3C Trace Context

Header chuẩn hóa (RFC):
```
traceparent: 00-{trace_id}-{parent_span_id}-{flags}
tracestate:  vendor=value,vendor2=value2
```

Vd: `traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01`

→ Mọi outbound HTTP/Kafka request **phải inject** traceparent header. Mọi inbound **phải extract** + tiếp tục trace.

### 2.5. Sampling

Production không thể trace 100% (cost storage, latency). Chiến lược:
- **Head-based**: Decision ở entry (Gateway), tỉ lệ cố định (vd: 1%)
- **Tail-based**: Sau khi trace xong, decision dựa trên: error? slow? → keep
- **Adaptive**: Tốc độ điều chỉnh theo load

Đồ án: 100% (cho demo). Production: 1–10% + always trace errors.

### 2.6. Standards & Tools

| Tool | Ngôn ngữ | Lưu trữ | Đặc điểm |
|------|---------|---------|----------|
| **Zipkin** | Java | In-memory/MySQL/Cassandra | Đơn giản, đầu tiên (Twitter 2012, từ Dapper paper Google) |
| **Jaeger** | Go | Cassandra/Elasticsearch | CNCF graduated, scalable |
| **OpenTelemetry** | Multi | Vendor-neutral | Spec mới — thay OpenTracing + OpenCensus |
| **AWS X-Ray, GCP Trace** | Managed | Cloud | Vendor-specific |

→ Đồ án dùng **Zipkin** vì đơn giản nhất setup local.

### 2.7. Spring Boot 3 — Micrometer Tracing

**Sleuth** (Spring Cloud Sleuth) đã **deprecated** ở Spring Boot 3.

Thay bằng:
- **Micrometer Observation API** — abstraction cho cả metrics + tracing
- **Micrometer Tracing** — implementation của tracing
- Bridge tới Brave (Zipkin) hoặc OpenTelemetry

Auto-instrumentation:
- HTTP server (Tomcat, WebFlux)
- HTTP client (RestTemplate, WebClient, OpenFeign)
- Kafka producer/consumer
- JDBC (qua DataSource proxy)
- Redis (Lettuce)

### 2.8. Log correlation

Log pattern bao gồm `traceId` và `spanId`:
```
2026-05-04 12:34:56 [order-service,abc123,def456] INFO Creating order...
                                  ^trace    ^span
```

→ Search Zipkin theo traceId → tìm log tương ứng. Search log → click traceId → mở Zipkin.

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. Setup mỗi service

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
  <groupId>io.zipkin.reporter2</groupId>
  <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

`application.yml`:
```yaml
management:
  tracing:
    sampling.probability: 1.0    # 100% sampling for demo
    propagation:
      type: w3c                  # W3C Trace Context (default)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
logging:
  pattern:
    level: '%5p [%X{traceId:-},%X{spanId:-}]'
```

### 3.2. Zipkin server

Container `openzipkin/zipkin:3.5.0` port `9411`. UI tại http://localhost:9411.

### 3.3. Cách trace propagate

**HTTP** (RestTemplate, WebClient, Feign):
- Outgoing: thêm header `traceparent: ...`
- Incoming: extract header, tạo span con

**Kafka** (Spring Kafka):
- Outgoing: header `traceparent` set vào ProducerRecord headers
- Incoming: KafkaListener extract header, tiếp tục trace

→ Trace **vẫn tiếp tục** qua Kafka — đây là điều quan trọng để tracing async flow!

### 3.4. View trace trong Zipkin

```
http://localhost:9411
  → Search by service: api-gateway
  → Click trace
  → Cây spans hiển thị
  → Click span → xem tags, logs, error
```

---

## 4. Use Cases Của Tracing Trong Dự Án

### 4.1. Debug latency
- Trace tạo đơn 5s, span nào lâu?
- Phát hiện DB query chậm, service A gọi B chậm

### 4.2. Debug error
- Trace lỗi 500 → click span error → see stack trace tag

### 4.3. Hiểu business flow
- Trace order saga: order-service → Kafka → inventory-service → Kafka → ... 
- Visualize được luồng end-to-end

### 4.4. Performance analysis
- Span tổng = 250ms, nhưng db query mất 200ms → tối ưu DB

---

## 5. Từ Khóa Nghiên Cứu

```
- distributed tracing google dapper paper
- w3c trace context specification
- opentelemetry spec
- zipkin vs jaeger
- micrometer tracing spring boot 3
- context propagation http kafka
- baggage propagation
- head sampling vs tail sampling
- sleuth deprecation migration
- sigelman dapper original paper 2010
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Tracing vs Logging, khác gì?**
→ Logging: events (vd: "user logged in"), không có structure across services. Tracing: structure cây span across services, đo latency từng bước.

**Q2: Em chọn Zipkin hay Jaeger? Vì sao?**
→ Zipkin — đơn giản nhất setup, đủ cho đồ án. Jaeger mạnh hơn nhưng cần Cassandra/ES backend. OpenTelemetry là tương lai vendor-neutral, có thể migrate sau.

**Q3: Tại sao Spring Boot 3 bỏ Sleuth?**
→ Spring chuẩn hóa observability qua Micrometer (vendor-neutral). Sleuth chỉ tracing, còn Micrometer Observation API thống nhất cả metrics + tracing — cleaner architecture.

**Q4: 100% sampling có vấn đề không?**
→ Production: KHÔNG (cost storage, network). Đồ án demo: OK. Production thường 1–10% + always trace errors.

**Q5: Trace propagate qua Kafka thế nào?**
→ Spring Kafka tự inject `traceparent` vào ProducerRecord headers. Consumer tự extract → tiếp tục trace. Async flow vẫn trace được — đây là sức mạnh của Micrometer.

**Q6: Baggage là gì?**
→ Key-value propagate qua trace, vd: `userId=123`, `tenantId=acme`. Khác trace ID là baggage có ý nghĩa nghiệp vụ. Cẩn thận: baggage tăng size header.

**Q7: W3C Trace Context vs B3 (Zipkin format)?**
→ B3 cũ hơn (Twitter Zipkin format). W3C Trace Context là chuẩn mới (2020) — cross-vendor. Spring Boot 3 default W3C, có thể bật B3 cho compatibility cũ.

---

## 7. Tài Liệu Tham Khảo

### Bài báo gốc
- **Benjamin Sigelman et al** (Google), "Dapper, a Large-Scale Distributed Systems Tracing Infrastructure", 2010 — paper khai sinh

### Spec
- W3C Trace Context — https://www.w3.org/TR/trace-context/
- OpenTelemetry specification

### Tools
- zipkin.io
- jaegertracing.io
- opentelemetry.io
- Micrometer Tracing documentation

### Sách
- Daniel Gomez Blanco, *Practical OpenTelemetry*, Apress
- Yuri Shkuro, *Mastering Distributed Tracing*, Packt
