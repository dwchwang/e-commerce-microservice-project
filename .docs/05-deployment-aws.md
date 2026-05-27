# 5.x. Triển khai hệ thống lên AWS EC2

Tài liệu này mô tả quy trình triển khai toàn bộ hệ thống 16 microservice + infrastructure lên một EC2 instance duy nhất sử dụng Docker Compose, Caddy reverse proxy, và HTTPS qua Let's Encrypt.

---

## 5.x.1. Tổng quan kiến trúc triển khai

```
Internet (HTTPS)
       │
       ▼
┌──────────────────────────────────────┐
│  Caddy Reverse Proxy (:80/:443)      │
│  - api.<ip>.nip.io → localhost:8080  │
│  - app.<ip>.nip.io → localhost:3000  │
│  - auth.<ip>.nip.io → localhost:8180 │
│  - grafana.<ip>.nip.io → :3001       │
│  - Let's Encrypt auto TLS            │
└──────────┬───────────────────────────┘
           │
           ▼
┌──────────────────────────────────────┐
│  Docker Network: ecommerce-network   │
│                                      │
│  ┌─────────────────────────────┐     │
│  │ Infrastructure Services     │     │
│  │ • PostgreSQL 17             │     │
│  │ • Redis 8                   │     │
│  │ • Kafka KRaft               │     │
│  │ • Elasticsearch 8           │     │
│  │ • Keycloak 26               │     │
│  │ • Mailpit (dev SMTP)        │     │
│  └─────────────────────────────┘     │
│                                      │
│  ┌─────────────────────────────┐     │
│  │ Spring Cloud Infrastructure │     │
│  │ • Discovery Server :8761    │     │
│  │ • Config Server :8888       │     │
│  │ • API Gateway :8080         │     │
│  └─────────────────────────────┘     │
│                                      │
│  ┌─────────────────────────────┐     │
│  │ 13 Business Services        │     │
│  │ auth • user • product       │     │
│  │ inventory • cart • order    │     │
│  │ payment • voucher • notif   │     │
│  │ review • search • content   │     │
│  │ flash-sale                  │     │
│  └─────────────────────────────┘     │
│                                      │
│  ┌─────────────────────────────┐     │
│  │ Observability               │     │
│  │ • Zipkin :9411              │     │
│  │ • Prometheus :9090          │     │
│  │ • Grafana :3001             │     │
│  └─────────────────────────────┘     │
└──────────────────────────────────────┘
```

---

## 5.x.2. Tài nguyên AWS sử dụng

| Resource     | Configuration                    | Purpose                         |
| ------------ | -------------------------------- | ------------------------------- |
| EC2 Instance | `t3.xlarge` (4 vCPU, 16GB RAM)   | Chạy toàn bộ stack              |
| AMI          | Ubuntu 24.04 LTS (HVM, x86_64)   | Hệ điều hành                    |
| Region       | `ap-southeast-1` (Singapore)     | Gần Việt Nam, latency thấp      |
| EBS Root     | gp3 60GB, 3000 IOPS, 125 MB/s    | Lưu trữ dữ liệu + Docker images |
| Elastic IP   | Cố định khi stop/start           | IP công khai cho nip.io DNS     |
| Key Pair     | `ecommerce-thesis-key` (ED25519) | SSH access                      |
| Swap         | 4GB file-based                   | Safety net khi peak memory      |

### Security Group Rules

| Port | Protocol | Source     | Purpose                      |
| ---- | -------- | ---------- | ---------------------------- |
| 22   | TCP      | Home IP/32 | SSH admin                    |
| 80   | TCP      | 0.0.0.0/0  | HTTP → HTTPS redirect + ACME |
| 443  | TCP      | 0.0.0.0/0  | HTTPS (Caddy)                |
| 3000 | TCP      | Home IP/32 | Frontend dev access          |
| 3001 | TCP      | Home IP/32 | Grafana dev access           |
| 8080 | TCP      | Home IP/32 | API Gateway debug            |
| 8761 | TCP      | Home IP/32 | Eureka dashboard             |
| 9090 | TCP      | Home IP/32 | Prometheus                   |
| 9411 | TCP      | Home IP/32 | Zipkin UI                    |

> Tất cả port debug (3000-9411) chỉ mở cho IP nhà, không public ra Internet.

---

## 5.x.3. Quy trình bootstrap instance

Sau khi EC2 instance được launch, chạy script bootstrap để cài đặt toàn bộ runtime:

```bash
# Từ local machine
source aws/config.env
scp -i $SSH_KEY_PATH aws/bootstrap.sh ubuntu@$ELASTIC_IP:~/bootstrap.sh
ssh -i $SSH_KEY_PATH ubuntu@$ELASTIC_IP 'bash ~/bootstrap.sh'
```

Script `aws/bootstrap.sh` thực hiện:

1. Cài đặt Docker Engine + Docker Compose plugin
2. Tạo swap file 4GB
3. Cài đặt Caddy web server
4. Tạo thư mục `/opt/ecommerce`

---

## 5.x.4. Cấu hình production

### 5.x.4.1. File `.env.prod`

Chứa tất cả biến môi trường cho production: GitHub owner, Elastic IP, CORS origins, database passwords, Keycloak hostname, VNPAY sandbox keys, SMTP config. Template có sẵn tại `.env.prod.example`.

Các biến quan trọng:

| Variable                               | Purpose                                            |
| -------------------------------------- | -------------------------------------------------- |
| `GITHUB_OWNER`                         | Tên tổ chức/cá nhân trên GitHub Container Registry |
| `IMAGE_TAG`                            | Tag của Docker image (mặc định: `latest`)          |
| `ELASTIC_IP` / `ELASTIC_IP_DASHED`     | IP công khai + dạng gạch ngang cho nip.io          |
| `CORS_ALLOWED_ORIGINS`                 | Danh sách origin được phép CORS                    |
| `KC_HOSTNAME`                          | Public hostname của Keycloak                       |
| `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` | Thông tin sandbox VNPAY                            |

### 5.x.4.2. Docker Compose Production Override

File `docker-compose.prod.yml` override các thiết lập từ `docker-compose.yml`:

- **`image:`** thay thế `build:` — pull pre-built images từ GHCR thay vì build local.
- **`mem_limit` / `mem_reservation`** — giới hạn RAM mỗi container, tránh một service chiếm hết RAM.
- **`build: !reset null`** — xóa `build:` directive, chỉ dùng `image:`.
- **`ports: !reset []`** — xóa port mapping cho các service không cần public (Postgres, Redis, Kafka, ES...).

Tổng memory limit: ~10.5GB cho application services + ~2GB infrastructure = ~12.5GB, phù hợp với instance 16GB.

### 5.x.4.3. JVM Heap Configuration

Tất cả 16 Spring Boot service được cấu hình qua YAML anchor trong `docker-compose.yml`:

```yaml
x-java-env: &java-env
  JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
```

- `MaxRAMPercentage=50.0` — JVM heap tối đa 50% RAM container (vd: 256MB với container 512MB).
- `UseG1GC` — Garbage collector tối ưu cho latency thấp.
- `ExitOnOutOfMemoryError` — Fail-fast khi OOM, container được restart bởi Docker.

---

## 5.x.5. HTTPS và Reverse Proxy với Caddy

### 5.x.5.1. nip.io Dynamic DNS

nip.io là dịch vụ DNS miễn phí, tự động resolve `<anything>.<ip>.nip.io` về `<ip>`. Không cần mua domain name. Ví dụ: `api.13-213-118-96.nip.io` resolve về `13.213.118.96`.

### 5.x.5.2. Caddyfile Configuration

```
api.{ELASTIC_IP_DASHED}.nip.io    → API Gateway  (:8080)
app.{ELASTIC_IP_DASHED}.nip.io    → Frontend     (:3000)
auth.{ELASTIC_IP_DASHED}.nip.io   → Keycloak     (:8180)
grafana.{ELASTIC_IP_DASHED}.nip.io → Grafana     (:3001)
```

Caddy tự động xin và renew Let's Encrypt certificate qua HTTP-01 challenge. Certificate được cache, hoạt động ngay sau khi instance restart.

### 5.x.5.3. Keycloak Production Configuration

```yaml
KC_HOSTNAME: auth.<elastic-ip-dashed>.nip.io
KC_HOSTNAME_STRICT: "false"
KC_PROXY_HEADERS: xforwarded
KC_HTTP_ENABLED: "true"
```

- `KC_PROXY_HEADERS: xforwarded` — Keycloak đọc `X-Forwarded-Proto: https` từ Caddy để tạo issuer URL đúng (`https://auth...`).
- `KC_HTTP_ENABLED: "true"` — Cho phép Caddy kết nối qua HTTP internal, Keycloak vẫn biết external URL là HTTPS.

---

## 5.x.6. CORS Configuration

API Gateway hỗ trợ multi-origin CORS qua biến môi trường `CORS_ALLOWED_ORIGINS`:

```java
@Value("${gateway.cors.allowed-origins:http://localhost:3000}")
private String allowedOriginsCsv;
```

- **Local dev:** default `http://localhost:3000` (không cần set env var).
- **Production:** `CORS_ALLOWED_ORIGINS=https://app.<ip>.nip.io,http://localhost:3000`.

Cấu hình trong Config Server (`configs/api-gateway.yml`):

```yaml
gateway:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

---

## 5.x.7. Quy trình Stop/Start (tiết kiệm credit)

### Dừng hệ thống

```bash
source aws/config.env

# 1. Dừng containers
ssh -i $SSH_KEY_PATH ubuntu@$ELASTIC_IP \
  'cd /opt/ecommerce && docker compose -f docker-compose.yml -f docker-compose.prod.yml down'

# 2. Dừng EC2 instance
aws ec2 stop-instances --instance-ids $INSTANCE_ID
aws ec2 wait instance-stopped --instance-ids $INSTANCE_ID
```

Khi instance stopped: chỉ tính phí EBS storage (~$5/tháng cho 60GB gp3), không tính phí compute.

### Khởi động lại

```bash
source aws/config.env

# 1. Start instance
aws ec2 start-instances --instance-ids $INSTANCE_ID
aws ec2 wait instance-running --instance-ids $INSTANCE_ID

# 2. Start stack
ssh -i $SSH_KEY_PATH ubuntu@$ELASTIC_IP \
  'cd /opt/ecommerce && bash aws/start-stack.sh'

# 3. Kiểm tra (~3 phút)
sleep 180
curl -s https://api.${ELASTIC_IP_DASHED}.nip.io/actuator/health | jq .
```

### Cơ chế tự phục hồi

- **`restart: unless-stopped`** trên tất cả services — Docker daemon tự động khởi động lại containers khi instance boot.
- **Postgres/Kafka/ES volumes** persist trên EBS — dữ liệu nguyên vẹn qua các lần stop/start.
- **Caddy systemd** — certificate đã cache, HTTPS hoạt động ngay khi instance khởi động.

---

## 5.x.8. Xử lý sự cố thường gặp

| Vấn đề                        | Nguyên nhân                                | Cách khắc phục                                     |
| ----------------------------- | ------------------------------------------ | -------------------------------------------------- |
| `curl https://api...` timeout | Security Group chưa mở port 443            | Kiểm tra SG inbound rules                          |
| Certificate error             | Caddy chưa kịp xin cert                    | Đợi 1-2 phút, cert tự động renew                   |
| Keycloak issuer mismatch      | `KC_HOSTNAME` sai hoặc thiếu proxy headers | Kiểm tra env vars, Caddy `X-Forwarded-Proto`       |
| Container OOM loop            | `mem_limit` quá thấp                       | Tăng `mem_limit` trong `docker-compose.prod.yml`   |
| Postgres connection refused   | Volume bị corrupt hoặc chưa init DB        | Kiểm tra `init-db/` scripts, xóa volume và restart |
| Elastic IP thay đổi sau stop  | IP không phải Elastic IP                   | Allocate Elastic IP và associate với instance      |

---

## 5.x.9. Danh sách file cấu hình liên quan

| File                                        | Purpose                                     |
| ------------------------------------------- | ------------------------------------------- |
| `docker-compose.yml`                        | Base compose với YAML anchors, env var refs |
| `docker-compose.prod.yml`                   | Production override (image, mem_limit)      |
| `.env.prod.example`                         | Template biến môi trường production         |
| `aws/config.env`                            | AWS resource IDs, IP, key paths             |
| `aws/bootstrap.sh`                          | Script cài đặt runtime trên EC2             |
| `aws/Caddyfile`                             | Caddy reverse proxy config                  |
| `aws/start-stack.sh`                        | Script khởi động stack trên EC2             |
| `api-gateway/.../SecurityConfig.java`       | CORS configuration                          |
| `config-server/.../configs/api-gateway.yml` | Gateway routes + CORS config                |
