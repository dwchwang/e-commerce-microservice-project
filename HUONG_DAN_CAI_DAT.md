# Hướng Dẫn Cài Đặt & Chạy Hệ Thống E-Commerce Microservice

> Tài liệu đóng gói kèm chương trình đồ án tốt nghiệp.
> Hệ thống gồm **13 microservice** (Spring Boot 3.5 / Spring Cloud 2025 / Java 21) cùng đầy đủ hạ tầng (PostgreSQL, Redis, Kafka, Elasticsearch, Keycloak, Prometheus, Grafana, Zipkin) và **giao diện web** (Next.js storefront + admin panel).
>
> Toàn bộ hệ thống được khởi động bằng **Docker Compose** — chỉ cần một máy cài Docker và Java là chạy được.

---

## Mục Lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Yêu cầu hệ thống](#2-yêu-cầu-hệ-thống)
3. [Cấu hình biến môi trường (`.env`)](#3-cấu-hình-biến-môi-trường-env)
4. [Build & khởi động backend bằng Docker](#4-build--khởi-động-backend-bằng-docker)
5. [Kiểm tra hệ thống đã sẵn sàng](#5-kiểm-tra-hệ-thống-đã-sẵn-sàng)
6. [Tạo tài khoản & cấp quyền ADMIN](#6-tạo-tài-khoản--cấp-quyền-admin)
7. [Seed dữ liệu mẫu](#7-seed-dữ-liệu-mẫu)
8. [Chạy giao diện web (Frontend + Admin)](#8-chạy-giao-diện-web-frontend--admin)
9. [Danh sách cổng & URL truy cập](#9-danh-sách-cổng--url-truy-cập)
10. [Dừng hệ thống](#10-dừng-hệ-thống)
11. [Xử lý lỗi thường gặp](#11-xử-lý-lỗi-thường-gặp)

---

## 1. Tổng Quan Kiến Trúc

```
            Trình duyệt / Client
                    │
                    ▼
          ┌──────────────────┐
          │   API Gateway    │  :8080  — Cổng vào duy nhất, xác thực JWT, rate limiting
          └────────┬─────────┘
                   │
   ┌───────────────┴──────────────────────────┐
   │  Spring Cloud:                            │
   │    Discovery Server (Eureka)  :8761       │
   │    Config Server              :8888       │
   └───────────────┬──────────────────────────┘
                   │
   ┌───────────────┴──────────────────────────────────────────┐
   │  13 Business Services (chỉ truy cập qua Gateway):         │
   │    identity   :8081   user      :8082   product  :8083    │
   │    inventory  :8084   cart      :8085   order    :8086    │
   │    payment    :8087   voucher   :8088   notification:8089 │
   │    review     :8090   search    :8091   content  :8092    │
   │    flash-sale :8093                                       │
   └───────────────────────────────────────────────────────────┘
```

**Hạ tầng đi kèm:** PostgreSQL 17 (10 database), Redis 8, Kafka (KRaft), Elasticsearch 8.18, Keycloak 26 (Identity Provider), Mailpit (email test), Prometheus, Grafana, Zipkin.

**Mô hình bảo mật:** Client → Gateway xác thực JWT (do Keycloak cấp) → Gateway forward header `X-User-Id`, `X-User-Roles`, `X-User-Email` → các service backend tin tưởng các header này.

**Giao tiếp:** đồng bộ qua REST (OpenFeign) + bất đồng bộ qua Kafka events.

---

## 2. Yêu Cầu Hệ Thống

### Phần mềm bắt buộc

| Phần mềm | Phiên bản | Ghi chú |
|---|---|---|
| **Java** | 21 | Dùng để build JAR (`./mvnw`) |
| **Docker Desktop** | Engine 24+, Compose **V2** | Dùng `docker compose`, KHÔNG phải `docker-compose` (V1) |
| **Node.js** | 18+ (khuyến nghị 20 LTS) | Chỉ cần khi chạy giao diện web |
| **Maven** | Không cần cài | Dự án có sẵn Maven Wrapper `./mvnw` |

### Kiểm tra nhanh

```bash
java -version            # Phải là 21.x
docker --version         # 24.x trở lên
docker compose version   # v2.x trở lên
node -v                  # 18+ (cho frontend)
```

### Cài đặt (nếu chưa có)

- **Java 21** — macOS: `brew install openjdk@21` · Ubuntu: `sudo apt install openjdk-21-jdk` · Windows: tải Eclipse Temurin 21 từ https://adoptium.net
- **Docker Desktop** — https://www.docker.com/products/docker-desktop (Linux: cài Docker Engine + Compose Plugin)
- **Node.js** — https://nodejs.org

### Tài nguyên Docker khuyến nghị

Hệ thống chạy đầy đủ **~26 container**. Vào Docker Desktop → **Settings → Resources** để cấp:

| Tài nguyên | Tối thiểu | Khuyến nghị |
|---|---|---|
| CPU | 4 cores | 6+ cores |
| RAM | 8 GB | 12+ GB |
| Disk | 20 GB | 30+ GB |

> Máy yếu RAM: có thể giảm bộ nhớ Elasticsearch trong `docker-compose.yml` (`ES_JAVA_OPTS=-Xms128m -Xmx128m`).

### Hệ điều hành hỗ trợ

macOS 12+ · Ubuntu 20.04+ · Windows 10/11 (cần WSL2).

---

## 3. Cấu Hình Biến Môi Trường (`.env`)

File `.env` chứa toàn bộ mật khẩu và cấu hình bí mật. Tạo từ template có sẵn:

```bash
cp .env.example .env
```

Để chạy thử nhanh trên máy local, có thể dùng nguyên nội dung mẫu dưới đây (mật khẩu nên đổi khi triển khai thật):

```env
# PostgreSQL — database chính
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123

# Keycloak DB
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=keycloak123

# Keycloak — Identity Provider
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
KEYCLOAK_CLIENT_ID=ecommerce-client
KEYCLOAK_CLIENT_SECRET=local-dev-ecommerce-secret
KEYCLOAK_ADMIN_CLIENT_ID=identity-service-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=local-dev-identity-admin-secret

# Eureka — Service Discovery
EUREKA_USER=eureka
EUREKA_PASSWORD=eureka123
EUREKA_DEFAULT_ZONE=http://eureka:eureka123@discovery-server:8761/eureka/

# Grafana
GF_SECURITY_ADMIN_PASSWORD=grafana123

# VNPAY — thanh toán (sandbox để test)
VNPAY_TMN_CODE=DEMO
VNPAY_HASH_SECRET=DEMOSECRET
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/api/payments/vnpay/return
FRONTEND_ORDER_RESULT_URL=http://localhost:3000/order/result

# Email — Mailpit (chạy local, giữ nguyên)
SMTP_HOST=mailpit
SMTP_PORT=1025
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_AUTH=false
SMTP_STARTTLS=false
```

> **Lưu ý quan trọng:**
> - `EUREKA_DEFAULT_ZONE` phải khớp với `EUREKA_USER`/`EUREKA_PASSWORD`. Ví dụ password là `eureka123` thì zone là `http://eureka:eureka123@discovery-server:8761/eureka/`.
> - Keycloak được **import tự động** từ `keycloak/realm-export.json` khi khởi động lần đầu — các client secret đã định nghĩa sẵn. Để dùng secret thật, lấy theo [Mục 6](#6-tạo-tài-khoản--cấp-quyền-admin).
> - Để thanh toán VNPAY thật, thay `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` bằng credential từ tài khoản VNPAY sandbox của bạn.

---

## 4. Build & Khởi Động Backend Bằng Docker

### Bước 1 — Build JAR cho tất cả service

```bash
./mvnw clean package -DskipTests
```

> Lần đầu có thể mất 5–15 phút để tải dependencies. Các lần sau nhanh hơn nhờ cache.

### Bước 2 — Khởi động toàn bộ hệ thống

```bash
docker compose up -d --build
```

- `-d`: chạy nền · `--build`: build image từ Dockerfile.
- Lần đầu mất 5–10 phút để pull image và build. Docker Compose tự quản lý thứ tự khởi động (hạ tầng → Eureka → Config → Gateway → các service nghiệp vụ).

### Bước 3 — Theo dõi quá trình khởi động

```bash
docker compose ps                       # Trạng thái tất cả container
docker compose logs -f api-gateway      # Log một service cụ thể
```

Sau khoảng 3–5 phút, các container hạ tầng và service chính phải ở trạng thái `Up (healthy)`.

---

## 5. Kiểm Tra Hệ Thống Đã Sẵn Sàng

### Hạ tầng

```bash
docker compose exec postgres psql -U postgres -c "\l"   # Thấy 10 database
docker compose exec redis redis-cli ping                # PONG
curl http://localhost:9200/_cluster/health              # status: green/yellow
```

### Spring Cloud & Gateway

```bash
curl http://localhost:8080/actuator/health/readiness    # {"status":"UP"}
```

- **Eureka Dashboard:** http://localhost:8761 (đăng nhập `eureka` / `EUREKA_PASSWORD`) — phải thấy đủ 14 service đã đăng ký.
- **Swagger UI tổng hợp:** http://localhost:8080/swagger-ui.html
- **Config Server:** `curl http://localhost:8888/api-gateway/docker`

### Script kiểm tra tổng hợp (tùy chọn)

Lưu thành `check-system.sh` rồi chạy `bash check-system.sh`:

```bash
#!/bin/bash
echo "=== Kiểm tra hệ thống E-Commerce ==="
check() {
  if curl -s -o /dev/null -w "%{http_code}" "$2" | grep -qE "^(200|401|302)$"; then
    echo "✓ $1 - OK"; else echo "✗ $1 - FAILED ($2)"; fi
}
check "API Gateway"   "http://localhost:8080/actuator/health/liveness"
check "Eureka"        "http://localhost:8761"
check "Keycloak"      "http://localhost:8180"
check "Prometheus"    "http://localhost:9090"
check "Grafana"       "http://localhost:3000"
check "Zipkin"        "http://localhost:9411"
check "Mailpit"       "http://localhost:8025"
check "Elasticsearch" "http://localhost:9200"
check "Swagger UI"    "http://localhost:8080/swagger-ui.html"
```

---

## 6. Tạo Tài Khoản & Cấp Quyền ADMIN

### Đăng ký user (mặc định nhận `ROLE_USER`)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Test@123","fullName":"Quan Tri Vien"}'
```

### Cấp `ROLE_ADMIN` để vào Admin Panel

1. Mở Keycloak Admin Console: http://localhost:8180 (đăng nhập `admin` / `KEYCLOAK_ADMIN_PASSWORD`).
2. Chọn realm **`ecommerce`** (dropdown góc trên bên trái).
3. **Users** → chọn user vừa đăng ký → tab **Role mapping** → **Assign role** → lọc realm roles → chọn **`ROLE_ADMIN`** → **Assign**.

### Lấy Keycloak Client Secret (khi cần gọi API trực tiếp)

```bash
cat keycloak/realm-export.json | python3 -c "
import json, sys
data = json.load(sys.stdin)
for c in data.get('clients', []):
    if c.get('clientId') in ['ecommerce-client','identity-service-admin']:
        print(c['clientId'], ':', c.get('secret','N/A'))"
```

### Lấy JWT token để test API

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" -d "client_id=ecommerce-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=admin@example.com" -d "password=Test@123" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

curl http://localhost:8080/api/users/me -H "Authorization: Bearer $TOKEN"
```

---

## 7. Seed Dữ Liệu Mẫu

Database khởi tạo **trống** — storefront sẽ không có sản phẩm để hiển thị. Có hai cách:

- **Cách đơn giản (khuyến nghị):** sau khi chạy frontend (Mục 8), đăng nhập bằng tài khoản `ROLE_ADMIN` và thêm sản phẩm trực tiếp trong **Admin Panel** (`/admin`).
- **Cách dùng script:** thư mục `.test/seed/` có sẵn các script seed. Cần `ADMIN_TOKEN` (lấy ở Mục 6):

```bash
export ADMIN_TOKEN=$TOKEN
bash .test/seed/seed-products.sh 100     # Tạo 100 sản phẩm mẫu
```

---

## 8. Chạy Giao Diện Web (Frontend + Admin)

`docker-compose.yml` mặc định **không** kèm frontend — khi phát triển/demo, frontend chạy local bằng `npm run dev`.

### Bước 1 — Cấu hình `.env.local`

```bash
cp frontend/.env.local.example frontend/.env.local
```

Sửa lại để **chạy local trỏ vào backend Docker** (đây là điểm dễ sai nhất):

```env
# frontend/.env.local
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

### Bước 2 — Cài dependency & chạy

```bash
cd frontend
npm install
npm run dev          # Mặc định chạy tại http://localhost:3000
```

- **Storefront:** http://localhost:3000
- **Admin Panel:** http://localhost:3000/admin (cần đăng nhập tài khoản có `ROLE_ADMIN`)

> **Xung đột cổng 3000:** Frontend dev và Grafana đều mặc định 3000. Khi chạy frontend, sẽ không vào được Grafana qua 3000. Nếu cần Grafana đồng thời, đổi cổng frontend: `npm run dev -- -p 3005` (đồng thời cập nhật `NEXT_PUBLIC_APP_URL` và `FRONTEND_ORDER_RESULT_URL` cho khớp).

---

## 9. Danh Sách Cổng & URL Truy Cập

| Giao diện / Dịch vụ | URL | Đăng nhập |
|---|---|---|
| Frontend storefront | http://localhost:3000 | — |
| Admin Panel | http://localhost:3000/admin | tài khoản `ROLE_ADMIN` |
| API Gateway (entry point) | http://localhost:8080 | JWT token |
| Swagger UI tổng hợp | http://localhost:8080/swagger-ui.html | — |
| Eureka Dashboard | http://localhost:8761 | `eureka` / `EUREKA_PASSWORD` |
| Keycloak Admin | http://localhost:8180 | `admin` / `KEYCLOAK_ADMIN_PASSWORD` |
| Grafana | http://localhost:3000 *(xung đột cổng FE)* | `admin` / `GF_SECURITY_ADMIN_PASSWORD` |
| Prometheus | http://localhost:9090 | — |
| Zipkin (tracing) | http://localhost:9411 | — |
| Mailpit (email test) | http://localhost:8025 | — |
| Elasticsearch | http://localhost:9200 | — |
| PostgreSQL | localhost:5432 | `postgres` / `POSTGRES_PASSWORD` |
| Redis | localhost:6379 | — |

Các business service (`/api/auth`, `/api/products`, `/api/orders`, …) chỉ truy cập **qua Gateway** ở cổng 8080.

---

## 10. Dừng Hệ Thống

```bash
docker compose down       # Dừng nhưng GIỮ lại data (database, kafka, ...)
docker compose down -v    # Dừng và XÓA toàn bộ data (reset sạch)
```

> **Cảnh báo:** `-v` xóa tất cả volume. Chỉ dùng khi muốn cài lại từ đầu.
> Dừng frontend: nhấn `Ctrl+C` ở terminal đang chạy `npm run dev`.

---

## 11. Xử Lý Lỗi Thường Gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `docker compose` báo lệnh không tồn tại | Đang dùng Compose V1 | Cập nhật Docker Desktop; dùng `docker compose` (có dấu cách) |
| Service liên tục restart / unhealthy | Thiếu RAM cho Docker | Tăng RAM trong Docker Desktop → Settings → Resources (≥ 8 GB) |
| Service không đăng ký được Eureka, lỗi 401 | `EUREKA_DEFAULT_ZONE` không khớp user/password | Sửa `.env` cho khớp `EUREKA_USER`/`EUREKA_PASSWORD`, rồi `docker compose up -d` lại |
| Lỗi 401 JWT / `invalid issuer` sau `down -v` | Keycloak re-import lại realm | Khởi động lại hệ thống và đăng ký lại user (xem Mục 6) |
| Storefront trống trơn | Chưa seed sản phẩm | Thực hiện Mục 7 |
| Frontend không gọi được API | `.env.local` sai `API_BASE_URL` | Phải là `http://localhost:8080` khi chạy `npm run dev` trên host (Mục 8) |
| Không vào được Grafana ở cổng 3000 | Trùng cổng với frontend | Dừng FE hoặc chạy FE ở cổng khác (`-p 3005`) |
| Build Maven chậm/lỗi tải dependency | Lần đầu tải về | Kiểm tra mạng; chạy lại `./mvnw clean package -DskipTests` |

### Lệnh chẩn đoán hữu ích

```bash
docker compose ps                          # Trạng thái container
docker compose logs --tail=100 <service>   # 100 dòng log cuối
docker compose restart <service>           # Khởi động lại 1 service
docker stats                               # Theo dõi CPU/RAM
```

---

## Tóm Tắt Lệnh Cài Đặt (Quick Start)

```bash
# 1. Cấu hình
cp .env.example .env                  # rồi điền secret (xem Mục 3)

# 2. Build & chạy backend
./mvnw clean package -DskipTests
docker compose up -d --build

# 3. Kiểm tra
docker compose ps
curl http://localhost:8080/actuator/health/readiness

# 4. Chạy frontend
cp frontend/.env.local.example frontend/.env.local   # sửa API_BASE_URL=http://localhost:8080
cd frontend && npm install && npm run dev

# 5. Dừng
docker compose down
```

Mở **http://localhost:3000** để bắt đầu sử dụng.
