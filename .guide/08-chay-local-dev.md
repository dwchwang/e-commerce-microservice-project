# Chạy Local Development (Không Docker)

## Khi Nào Dùng

Dùng chế độ này khi:
- Đang **phát triển và debug** một service cụ thể
- Muốn chạy với IDE (IntelliJ, VS Code) và đặt breakpoint
- Cần hot-reload khi thay đổi code

## Chiến Lược Khuyến Nghị

**Chạy infrastructure bằng Docker, còn service đang phát triển chạy local.**

```bash
# Bước 1: Khởi động infrastructure (DB, Kafka, Redis, Keycloak, Elasticsearch)
docker compose up -d postgres keycloak-db redis kafka elasticsearch mailpit keycloak zipkin prometheus grafana

# Bước 2: Khởi động Spring Cloud core
docker compose up -d discovery-server config-server

# Bước 3: Nạp biến môi trường từ .env để password/secret khớp Docker
set -a
source .env
set +a

# Khi service chạy trên máy host, Eureka URL phải trỏ localhost, không phải discovery-server
export EUREKA_DEFAULT_ZONE="http://${EUREKA_USER}:${EUREKA_PASSWORD}@localhost:8761/eureka/"

# Bước 4: Chạy service bạn muốn dev local (ví dụ: product-service)
./mvnw spring-boot:run -pl product-service
# Hoặc chạy main class trong IDE với các environment variables tương tự
```

---

## Cấu Hình Profile

Mỗi service hiện có cấu hình mặc định cho host-local và profile `docker` cho container:

| Profile | Dùng khi | Config từ |
|---------|----------|-----------|
| mặc định | Chạy trực tiếp trên máy | `application.yml` + Config Server, các default đều trỏ `localhost` |
| `docker` | Chạy trong Docker container | `application-docker.yml` + Config Server + env vars |

### Cách Chọn Profile

**Chạy local từ terminal:**
```bash
set -a
source .env
set +a
export EUREKA_DEFAULT_ZONE="http://${EUREKA_USER}:${EUREKA_PASSWORD}@localhost:8761/eureka/"
./mvnw spring-boot:run -pl product-service
```

**Từ IntelliJ:**
- Edit Run Configuration
- Không cần Active profile cho service chạy trên máy host
- Environment variables: copy các biến từ `.env`, tối thiểu `POSTGRES_USER`, `POSTGRES_PASSWORD`, `EUREKA_DEFAULT_ZONE`, các secret liên quan service

**Biến môi trường:**
```bash
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres123
export EUREKA_DEFAULT_ZONE=http://eureka:eureka123@localhost:8761/eureka/
./mvnw spring-boot:run -pl product-service
```

---

## Cấu Hình Local Điển Hình

Repo hiện không có `application-local.yml` riêng. Các file `application.yml` đã có default trỏ về localhost, ví dụ:

```yaml
# Ví dụ: product-service/src/main/resources/application.yml
spring:
  datasource:
    url: ${PRODUCT_DB_URL:jdbc:postgresql://localhost:5432/product_db}
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_DEFAULT_ZONE:http://eureka:eureka@localhost:8761/eureka/}

management:
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
```

> Nếu `.env` dùng password khác default, bắt buộc export biến môi trường trước khi chạy local.

---

## Chạy Tất Cả Services Local (Không Khuyến Nghị)

Nếu bạn muốn chạy toàn bộ hệ thống không dùng Docker (chỉ để nghiên cứu):

```bash
# Cần chạy theo đúng thứ tự này
# 1. Infrastructure (phải cài PostgreSQL, Redis, Kafka, Elasticsearch trên máy)

# 2. Core services
./mvnw spring-boot:run -pl discovery-server &
sleep 10
./mvnw spring-boot:run -pl config-server &
sleep 10

# 3. Business services (chạy song song)
./mvnw spring-boot:run -pl identity-service &
./mvnw spring-boot:run -pl user-service &
./mvnw spring-boot:run -pl product-service &
# ... tiếp tục các service còn lại

# 4. Gateway (chạy sau cùng)
./mvnw spring-boot:run -pl api-gateway &
```

> **Lưu ý**: Cách này phức tạp và dễ lỗi. Khuyến nghị dùng Docker cho infrastructure.

---

## Debug Với IntelliJ IDEA

1. Mở project (import Maven project từ `pom.xml` gốc)
2. IntelliJ sẽ tự nhận diện 17 modules
3. Chạy từng service:
   - Navigate đến `*Application.java` của service cần debug
   - Click icon **Run** hoặc **Debug** bên cạnh `main` method
   - Không set `spring.profiles.active` cho service chạy trên host
   - Set environment variables từ `.env` để database, Eureka, Keycloak secret khớp Docker

### Remote Debug (Service trong Docker)

Thêm vào `docker-compose.yml` của service muốn debug:
```yaml
product-service:
  environment:
    JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
  ports:
    - "5005:5005"  # Debug port
```

Sau đó trong IntelliJ:
- **Run** → **Edit Configurations** → **+** → **Remote JVM Debug**
- Host: `localhost`, Port: `5005`

---

## Chạy Tests

### Chạy tất cả tests:
```bash
./mvnw test
```

### Chạy tests của một service:
```bash
./mvnw test -pl order-service
./mvnw test -pl api-gateway
./mvnw test -pl flash-sale-service
```

> **Lưu ý**: Testcontainers sẽ tự khởi động PostgreSQL và Kafka trong Docker để chạy integration tests. Cần Docker đang chạy.

### Chạy một test class cụ thể:
```bash
./mvnw test -pl order-service -Dtest=OrderServiceTest
```

### Bỏ qua tests khi build:
```bash
./mvnw package -DskipTests
```
