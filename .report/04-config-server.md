# 04. Centralized Configuration — Spring Cloud Config Server

## 1. Mục Tiêu Nghiên Cứu

- Hiểu vì sao cần Centralized Configuration trong microservice
- Phân biệt static config, environment variables, config server
- Hiểu cơ chế refresh, encryption, profile

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Vấn đề
Mỗi service có hàng chục cấu hình: DB connection string, Kafka broker, Keycloak issuer, secret key, ... Nếu hardcode trong từng service:
- Đổi 1 cấu hình → phải build & deploy 13 service
- Secret bị commit vào git
- Khác nhau giữa môi trường (dev/staging/prod) khó quản lý

### 2.2. The Twelve-Factor App (heroku.com/12factor)

Factor III: **Config** — Store config in the environment
> "Strict separation of config from code. Config varies between deploys; code does not."

Hai cách triển khai phổ biến:
1. **Environment variables** (12-factor purist)
2. **Config Server** (Spring Cloud, Consul KV, AWS Parameter Store)

### 2.3. Spring Cloud Config

Server-side: Một service đọc config từ:
- Git repository (default — branch = profile, file = `{app}-{profile}.yml`)
- Native filesystem (đồ án dùng cách này)
- Vault, JDBC, AWS, ...

Client-side: Service start → call Config Server → load config trước khi tạo bean.

```
Service start
  → Bootstrap context khởi động (trước Application context)
  → Đọc bootstrap.yml: spring.config.import=configserver:http://config-server:8888
  → Fetch {app-name}-{profile}.yml
  → Merge vào Environment
  → Application context tiếp tục với config đầy đủ
```

### 2.4. Profile

`{app}-{profile}.yml` — Spring chọn profile theo `spring.profiles.active`.

Trong dự án:
- `order-service-docker.yml` — khi chạy Docker
- `order-service-local.yml` — khi chạy IDE
- `order-service.yml` (không profile) — config chung

### 2.5. Refresh & Bus

- `@RefreshScope` bean — Khi POST `/actuator/refresh`, bean được tạo lại với config mới
- **Spring Cloud Bus** (Kafka/RabbitMQ): POST `/actuator/busrefresh` → broadcast tới tất cả service
- Trong đồ án: KHÔNG dùng (config thay đổi yêu cầu restart Docker — đơn giản hơn)

### 2.6. Encryption

Config Server hỗ trợ `{cipher}...` syntax:
```yaml
db:
  password: '{cipher}AQA...'  # decrypt khi serve
```
→ Dự án đại học có thể bỏ qua (thay bằng `.env` + Docker secrets)

---

## 3. So Sánh Phương Án

| Cách | Ưu | Nhược |
|------|-----|-------|
| Env vars (12-factor) | Đơn giản, không thêm component | Khó quản lý hàng trăm key, khó audit thay đổi |
| **Spring Cloud Config Server** | Centralized, profile, refresh runtime, encryption | Single point of failure, thêm 1 service |
| HashiCorp Vault | Bảo mật cao (dynamic secrets) | Phức tạp, cần Vault Agent |
| Kubernetes ConfigMap/Secret | Native với K8s | Chỉ áp dụng K8s |
| Consul KV | Multi-datacenter | Kèm Consul cluster |

→ Dự án chọn Config Server vì là phần của Spring Cloud, đơn giản, đủ dùng cho demo.

---

## 4. Cách Áp Dụng Trong Dự Án

### 4.1. config-server module
- Spring Boot app với `@EnableConfigServer`
- Port `8888`
- Profile `native` — đọc file từ classpath thay vì Git
```yaml
spring.profiles.active: native
spring.cloud.config.server.native.search-locations: classpath:/configs/
```

### 4.2. Cấu trúc folder configs
```
config-server/src/main/resources/configs/
  application.yml             ← config dùng chung mọi service
  api-gateway.yml
  api-gateway-docker.yml
  order-service.yml
  order-service-docker.yml
  ...
```

### 4.3. Mỗi service `bootstrap.yml`
```yaml
spring:
  application:
    name: order-service
  config:
    import: optional:configserver:http://config-server:8888
  cloud:
    config:
      fail-fast: true   # nếu Config Server down, service không start
      retry:
        max-attempts: 6
```

### 4.4. Workflow đổi config
1. Edit `config-server/src/main/resources/configs/order-service-docker.yml`
2. Rebuild Config Server: `docker compose up -d --build config-server`
3. Restart service ảnh hưởng: `docker compose restart order-service`

(Trong production: dùng Git repo + auto pull + busrefresh)

---

## 5. Từ Khóa Nghiên Cứu

```
- spring cloud config server native
- twelve factor app config
- externalized configuration pattern
- spring cloud bus refresh
- secret management vault vs aws parameter store
- bootstrap context spring boot 3
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Tại sao không dùng env var trực tiếp như docker-compose env?**
→ Em vẫn dùng `.env` + Docker env cho **secret** (DB password, VNPAY hash). Còn business config (rate limit, timeout) đặt ở Config Server để tách biệt code và config theo 12-factor.

**Q2: Config Server xuống thì sao?**
→ Service đã start sẽ vẫn chạy (cached config). Service mới start sẽ retry rồi fail nếu `fail-fast=true`. Trong production: HA Config Server (3 instance) + git repo backend.

**Q3: Có nguy cơ bảo mật khi tập trung config?**
→ Có — nên kết hợp Vault hoặc encryption. Đồ án dùng `.env` để giữ secret ngoài Git.

**Q4: Bootstrap context khác Application context như thế nào?**
→ Bootstrap chạy trước, nhiệm vụ là load Config Server. Application context dùng config đó để khởi tạo bean nghiệp vụ. Spring Boot 3 thay bootstrap.yml bằng `spring.config.import` syntax.

---

## 7. Tài Liệu Tham Khảo

- The Twelve-Factor App, factor III (Config), Adam Wiggins (Heroku)
- Spring Cloud Config Reference
- "Externalized Configuration" — microservices.io
- "Patterns for Cloud Native Configuration" — Bilgin Ibryam
