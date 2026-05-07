# 03. Service Discovery với Netflix Eureka

## 1. Mục Tiêu Nghiên Cứu

- Hiểu vấn đề Service Discovery giải quyết
- Phân biệt Client-side vs Server-side discovery
- So sánh Eureka, Consul, Zookeeper, Kubernetes Service
- Hiểu cơ chế heartbeat, self-preservation, AP-CP trade-off

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Vì sao cần Service Discovery?
Trong môi trường microservice, các service được scale (multiple instances), restart, đổi IP/port động. **Hardcode URL không khả thi**.

Giải pháp: Một **Service Registry** trung tâm — mỗi instance tự đăng ký khi khởi động, và các caller hỏi registry để biết "ai đang chạy?".

### 2.2. Hai mô hình Discovery

**Client-side discovery (Eureka model)**
```
Client → Registry (lookup) → list of instances
Client → load-balance (Ribbon/LoadBalancer) → 1 instance
```
- Pros: Đơn giản, ít single point of failure, client kiểm soát LB
- Cons: Client logic phức tạp, mỗi ngôn ngữ phải có client riêng

**Server-side discovery (Kubernetes model)**
```
Client → Load Balancer (Service ClusterIP) → instance
```
- Pros: Client đơn giản (chỉ cần DNS name)
- Cons: Cần infra (kube-proxy, ingress controller)

### 2.3. Netflix Eureka — Triết lý AP

CAP theorem: Eureka chọn **Availability + Partition Tolerance**, hy sinh **Consistency**.
- Khi network split, mỗi Eureka instance vẫn trả lời (AVAILABLE)
- Có thể trả về snapshot cũ (eventual consistency)

**Self-preservation mode**: Nếu mất quá 15% heartbeat, Eureka KHÔNG xóa instance khỏi registry — tránh trường hợp mass eviction do network blip.

### 2.4. Cơ chế hoạt động

```
1. Service khởi động → POST /eureka/apps/{app-name} với metadata
2. Mỗi 30s: PUT /eureka/apps/{app-name}/{instance-id} (heartbeat)
3. Sau 90s không heartbeat → instance bị mark UNAVAILABLE
4. Client gọi: GET /eureka/apps → lấy danh sách → load-balance
5. Client cache registry, refresh mỗi 30s
```

Các state của instance: `STARTING → UP → DOWN → OUT_OF_SERVICE`

### 2.5. So sánh giải pháp

| Giải pháp | Mô hình | Consistency | Health check | Phù hợp |
|-----------|---------|-------------|--------------|---------|
| **Netflix Eureka** | Client-side, AP | Eventually | HTTP heartbeat | JVM stack, Spring Cloud |
| Apache Zookeeper | CP | Strong | Session | Hadoop, Kafka coordination |
| HashiCorp Consul | Both, CP | Strong | TCP/HTTP/script | Multi-datacenter, polyglot |
| etcd | CP | Strong | TTL leases | Kubernetes internal |
| Kubernetes Service + DNS | Server-side | Strong (etcd) | liveness/readiness probes | Cloud-native |

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. discovery-server module
- Spring Boot app với annotation `@EnableEurekaServer`
- Port: `8761`
- Có Basic Auth (`eureka:password`) để bảo vệ
- Dashboard UI tại http://localhost:8761

### 3.2. Mỗi service đăng ký
- Annotation `@EnableDiscoveryClient` trong main class
- Cấu hình:
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka:password@discovery-server:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
```

### 3.3. Sử dụng qua OpenFeign
```java
@FeignClient(name = "product-service")  // dùng tên đăng ký, không phải URL
public interface ProductClient {
  @GetMapping("/api/internal/products/{id}")
  ProductDto getProduct(@PathVariable String id);
}
```
Spring Cloud LoadBalancer + Eureka tự lookup `product-service` → IP cụ thể.

### 3.4. Sử dụng qua Spring Cloud Gateway
```yaml
spring.cloud.gateway:
  routes:
    - id: product-route
      uri: lb://product-service     # lb:// = load-balanced via discovery
      predicates:
        - Path=/api/products/**
```

### 3.5. Sự cố thường gặp (đã ghi trong `.guide/10-xu-ly-loi.md`)
- `EUREKA_DEFAULT_ZONE` sai → service không đăng ký được
- Discovery server chưa healthy → service start lại → lùi lại
- Network alias trong Docker compose: service phải gọi qua hostname, không phải localhost

---

## 4. Đối Chiếu Với Lý Thuyết: Eureka và CAP

Khi network bị partition:
- Eureka chọn AP → mỗi node vẫn trả lời, có thể stale data
- Phù hợp e-commerce: Thà cho user gọi 1 instance đã chết (gặp 503 → retry) còn hơn KHÔNG gọi được gì

So với Zookeeper (CP):
- Khi partition → 1 phía mất quorum → không trả lời → **toàn hệ thống không thể discover** → mất availability

---

## 5. Từ Khóa Nghiên Cứu

```
- service discovery pattern microservices
- netflix eureka self preservation
- client side vs server side discovery
- consul vs eureka vs zookeeper comparison
- spring cloud loadbalancer
- service registry pattern Chris Richardson
- eureka heartbeat eviction timeout
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Em không dùng Kubernetes thì tại sao cần Eureka?**
→ Đồ án dùng Docker Compose, không có Service mesh. Eureka đảm nhận vai trò service registry để OpenFeign + Gateway lookup tên service.

**Q2: Khi Eureka server chết thì sao?**
→ Service đã cache registry → vẫn gọi được trong khoảng 30s–vài phút. Nếu Eureka chết lâu, có thể chạy multiple Eureka peer-aware. Trong production thực tế, dùng Eureka cluster (3+ nodes).

**Q3: Tại sao chọn Eureka thay vì Consul/Kubernetes Service?**
→ Eureka là default của Spring Cloud, tích hợp tốt với @EnableDiscoveryClient + LoadBalancer + OpenFeign. Đồ án không deploy K8s nên không cần Service object.

**Q4: Self-preservation là gì?**
→ Khi heartbeat loss > 15%, Eureka tin rằng do network problem chứ không phải instance chết, nên KHÔNG xóa instance. Tránh mass eviction.

**Q5: Eventually consistent có phải vấn đề?**
→ Worst case: Client gọi instance vừa chết → lỗi → retry → nhận snapshot mới hơn. Có circuit breaker + retry → trải nghiệm vẫn ổn.

---

## 7. Tài Liệu Tham Khảo

- Netflix Tech Blog: "Eureka, why you should use it"
- Spring Cloud Netflix Documentation
- Chris Richardson, *Microservices Patterns*, Chapter 3 — Service Discovery
- Eureka 2.x Wiki (đã archive nhưng vẫn nhiều insight)
- "Service Discovery in a Microservices Architecture" — NGINX blog
