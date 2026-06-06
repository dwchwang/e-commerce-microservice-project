# LỘ TRÌNH NGHIÊN CỨU CHO BÁO CÁO ĐỒ ÁN TỐT NGHIỆP

> Hệ thống E-Commerce Microservice — Spring Boot 3.5 / Spring Cloud 2025 / Java 21
> Tác giả: Nguyễn Đức Bảo Hoàng

## 1. Mục Đích Của Tài Liệu Này

Tài liệu này liệt kê **toàn bộ kiến thức nền tảng** mà bạn phải nắm vững để:
- Viết được phần "Cơ sở lý thuyết" (Chương 2) đầy đủ và sâu sắc
- Giải thích được mọi quyết định kiến trúc khi bảo vệ đồ án
- Trả lời tự tin các câu hỏi phản biện của Hội đồng

Mỗi file con (`01-...`, `02-...`, ...) là **một chủ đề độc lập** — bạn có thể đọc tuần tự, hoặc theo nhu cầu khi viết từng chương.

Sau Phase 10-14, folder này có thêm 3 file điều hướng:
- [README](./README.md): cách dùng `.report` để viết báo cáo.
- [22](./22-phase-10-14-tong-hop-thuc-nghiem.md): tổng hợp AWS, CI/CD, Frontend, performance/resilience và admin panel.
- [23](./23-bang-chung-kiem-thu-va-so-lieu.md): bảng bằng chứng, chỉ số k6 và các kết luận được phép đưa vào Chương 6.
- [24](./24-cau-truc-bao-cao-datn.md): cấu trúc báo cáo DATN và cách map toàn bộ chủ đề vào chương.

## 2. Bản Đồ Tổng 24 Chủ Đề

### NHÓM A — Nền tảng kiến trúc (BẮT BUỘC nắm vững đầu tiên)

| File | Chủ đề | Trọng số trong báo cáo |
|------|--------|-----------------------|
| [01](./01-kien-truc-microservices.md) | Kiến trúc Microservices vs Monolith | ★★★★★ |
| [02](./02-spring-boot-spring-cloud.md) | Hệ sinh thái Spring Boot & Spring Cloud | ★★★★★ |
| [03](./03-service-discovery-eureka.md) | Service Discovery (Netflix Eureka) | ★★★★ |
| [04](./04-config-server.md) | Centralized Configuration (Spring Cloud Config) | ★★★ |
| [05](./05-api-gateway.md) | API Gateway (Spring Cloud Gateway) | ★★★★★ |

### NHÓM B — Bảo mật & Truyền thông

| File | Chủ đề | Trọng số |
|------|--------|----------|
| [06](./06-bao-mat-keycloak-oauth-jwt.md) | Keycloak, OAuth 2.0, OIDC, JWT | ★★★★★ |
| [07](./07-giao-tiep-rest-openfeign.md) | Giao tiếp đồng bộ — REST, OpenFeign | ★★★★ |
| [08](./08-event-driven-kafka.md) | Event-Driven Architecture với Apache Kafka | ★★★★★ |

### NHÓM C — Distributed System Patterns (ĐIỂM SÁNG CỦA ĐỒ ÁN)

| File | Chủ đề | Trọng số |
|------|--------|----------|
| [09](./09-saga-pattern-distributed-transaction.md) | Saga Pattern & Distributed Transaction | ★★★★★ |
| [10](./10-outbox-idempotency.md) | Transactional Outbox & Idempotency | ★★★★★ |
| [11](./11-resilience-circuit-breaker-rate-limit.md) | Resilience4j: Circuit Breaker, Retry, Rate Limiting | ★★★★ |
| [12](./12-state-machine-scheduler.md) | State Machine + Scheduled Reconciliation | ★★★★ |
| [13](./13-flash-sale-concurrency.md) | High Concurrency — Flash Sale, Atomic Counter, Distributed Lock | ★★★★★ |

### NHÓM D — Persistence & Search

| File | Chủ đề | Trọng số |
|------|--------|----------|
| [14](./14-database-per-service-flyway.md) | Database per Service + Flyway Migration | ★★★★ |
| [15](./15-redis-cache-distributed-lock.md) | Redis: Cache, Session, Atomic Counter, Lock | ★★★★ |
| [16](./16-elasticsearch-fulltext-search.md) | Elasticsearch — Full-text Search & CQRS-lite | ★★★ |

### NHÓM E — Quan sát & Vận hành

| File | Chủ đề | Trọng số |
|------|--------|----------|
| [17](./17-observability-prometheus-grafana.md) | Metrics — Micrometer, Prometheus, Grafana | ★★★★ |
| [18](./18-distributed-tracing-zipkin.md) | Distributed Tracing — Zipkin, OpenTelemetry, W3C Trace Context | ★★★★ |

### NHÓM F — Tích hợp & Triển khai

| File | Chủ đề | Trọng số |
|------|--------|----------|
| [19](./19-vnpay-payment-gateway.md) | Tích hợp cổng thanh toán VNPAY (HMAC-SHA512) | ★★★ |
| [20](./20-testing-microservices.md) | Testing Strategies — Unit, Integration, Testcontainers | ★★★★ |
| [21](./21-docker-container-deployment.md) | Containerization với Docker & Docker Compose | ★★★★ |

### NHÓM G — Hướng dẫn viết báo cáo & bằng chứng thực nghiệm

| File | Chủ đề |
|------|--------|
| [22](./22-phase-10-14-tong-hop-thuc-nghiem.md) | Tổng hợp Phase 10-14: AWS, CI/CD, Frontend, performance/resilience, admin |
| [23](./23-bang-chung-kiem-thu-va-so-lieu.md) | Evidence matrix: số liệu k6, smoke test, resilience và phần còn thiếu |
| [24](./24-cau-truc-bao-cao-datn.md) | Đề xuất cấu trúc báo cáo DATN & cách map chủ đề vào chương |

---

## 3. Thứ Tự Đọc Đề Xuất

### Lộ trình 4 tuần (cho người đã code xong dự án)

**Tuần 1 — Nền tảng (NHÓM A)**
- Đọc 01 → 05 theo thứ tự
- Mục tiêu: Hiểu **vì sao chọn Microservices** và viết được Chương 2 phần "Tổng quan kiến trúc"

**Tuần 2 — Truyền thông & bảo mật (NHÓM B)**
- Đọc 06, 07, 08
- Mục tiêu: Vẽ được sơ đồ luồng giao tiếp giữa các service, hiểu **đồng bộ vs bất đồng bộ**

**Tuần 3 — Patterns & Persistence (NHÓM C + D)**
- Đọc 09, 10, 11, 12, 13 (đây là **trọng tâm điểm cao** của đồ án)
- Đọc 14, 15, 16
- Mục tiêu: Trình bày được Saga, Outbox, Idempotency — đây là 3 từ khóa "ăn điểm" của Hội đồng

**Tuần 4 — Vận hành & Hoàn thiện**
- Đọc 17, 18 (Observability)
- Đọc 19, 20, 21
- Đọc 22 để đưa phase 10-14 vào Chương 5-6
- Đọc 23 để lấy số liệu kiểm thử và hạn chế còn lại
- Đọc 24 cuối cùng để map toàn bộ nội dung vào cấu trúc báo cáo

---

## 4. Các "Buzzword" Bắt Buộc Phải Hiểu (Glossary nhanh)

Hội đồng phản biện thường hỏi về các thuật ngữ sau. Bạn phải định nghĩa được trong 1–2 câu:

| Thuật ngữ | Một câu giải thích |
|-----------|--------------------|
| **Microservice** | Kiến trúc chia ứng dụng thành nhiều service nhỏ, độc lập triển khai, mỗi service sở hữu DB riêng |
| **Saga Pattern** | Chuỗi local transactions kết nối qua events, mỗi bước có compensating action để rollback |
| **CAP Theorem** | Hệ phân tán chỉ đạt được tối đa 2 trong 3: Consistency, Availability, Partition Tolerance |
| **Eventual Consistency** | Dữ liệu cuối cùng sẽ nhất quán, nhưng có thể lệch tạm thời giữa các service |
| **Idempotency** | Thực hiện cùng một thao tác nhiều lần kết quả vẫn giống thực hiện một lần |
| **At-least-once delivery** | Kafka guarantee — message được gửi ÍT NHẤT 1 lần, có thể trùng |
| **Exactly-once semantics** | Đạt được bằng at-least-once + idempotency ở consumer |
| **Outbox Pattern** | Lưu event vào table cùng transaction với business data, một poller riêng publish lên Kafka |
| **Circuit Breaker** | Pattern chặn request đến service đang lỗi, tránh cascading failure |
| **Service Discovery** | Cơ chế cho phép service tìm địa chỉ network của service khác động |
| **API Gateway** | Single entry point xử lý routing, auth, rate limit cho tất cả request từ client |
| **JWT (JSON Web Token)** | Token tự chứa, được ký số, dùng cho stateless authentication |
| **OAuth 2.0** | Authorization framework chuẩn cho phép cấp quyền truy cập tài nguyên |
| **OIDC** | OpenID Connect — lớp authentication trên OAuth 2.0, thêm ID token |
| **Distributed Tracing** | Theo dõi 1 request đi qua nhiều service, mỗi span có trace_id chung |
| **CQRS** | Tách Command (ghi) và Query (đọc) — search-service trong dự án là ví dụ light-weight |
| **Evidence-based reporting** | Chỉ đưa số liệu/kết luận vào báo cáo khi có artifact raw, log hoặc screenshot đối chiếu được |

---

## 5. Mỗi File Con Có Cấu Trúc Như Sau

```
1. Mục tiêu nghiên cứu
2. Lý thuyết cốt lõi (định nghĩa, nguyên lý, lịch sử)
3. So sánh với phương án thay thế
4. Cách áp dụng trong dự án (chỉ rõ file/code)
5. Từ khóa nghiên cứu — copy thẳng vào Google Scholar
6. Câu hỏi phản biện thường gặp (kèm hướng trả lời)
7. Tài liệu tham khảo (sách, bài báo, doc chính thức)
```

→ Khi viết báo cáo, bạn có thể trích **mục 2** (lý thuyết) cho Chương 2 và **mục 4** (áp dụng) cho Chương 3.

---

## 6. Lưu Ý Khi Viết Báo Cáo

1. **Trích nguồn hàn lâm**: Mỗi định nghĩa lớn (Microservice, Saga, CAP) phải có **nguồn gốc** — cụ thể là:
   - Sam Newman — *Building Microservices* (O'Reilly, 2nd ed. 2021)
   - Chris Richardson — *Microservices Patterns* (Manning, 2018)
   - Martin Fowler — *Microservices article* (martinfowler.com, 2014)
   - Eric Brewer — CAP Theorem (PODC 2000)
   - Pat Helland — *Life beyond Distributed Transactions* (CIDR 2007)

2. **Tránh "nói chay"**: Mỗi pattern phải trả lời được "**TẠI SAO** dự án này dùng nó?" — file 22 sẽ hướng dẫn cụ thể.

3. **Sơ đồ nhiều hơn chữ**: Báo cáo IT đánh giá cao sequence diagram, component diagram, ERD, flowchart. Folder `.guide` của dự án đã có nhiều sơ đồ ASCII bạn có thể chuyển thành PlantUML/Mermaid.

4. **Đo lường thực tế**: Chương 6 đã có số liệu k6 cho catalog soak, checkout stress và flash-sale spike trong `.test/results`. Chỉ copy số liệu có trong [23](./23-bang-chung-kiem-thu-va-so-lieu.md). Grafana/Zipkin/Prometheus evidence da co the dung lam minh chung runtime.

5. **Ghi đúng phần hạn chế**: He thong da hoan thien o muc production-like single-host cho DATN, nhung khong viet thanh production multi-node/Kubernetes. Han che nen neu la autoscaling, service mesh, schema registry/contract test, mobile app rieng va payment production.

---

## 7. Lệnh Hỗ Trợ Nhanh

```bash
# Mở folder .report theo thứ tự
ls .report/

# Tìm từ khóa qua mọi file nghiên cứu
grep -r "Saga" .report/

# Build & chạy hệ thống để chụp screenshot cho báo cáo
./mvnw clean package -DskipTests && docker compose up -d
```
