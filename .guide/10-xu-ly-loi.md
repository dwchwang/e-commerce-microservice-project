# Xử Lý Lỗi Thường Gặp (Troubleshooting)

## Công Cụ Debug Chung

```bash
# Xem trạng thái tất cả container
docker compose ps

# Xem log của service lỗi
docker compose logs --tail=100 -f <service-name>

# Xem log tất cả (lọc ERROR)
docker compose logs | grep -i error

# Restart service cụ thể
docker compose restart <service-name>
```

---

## Lỗi 1: Service Không Khởi Động (Exit hoặc Restarting)

### Triệu chứng:
```
ecommerce-order-service   Exit 1
```

### Kiểm tra:
```bash
docker compose logs order-service | tail -50
```

### Nguyên nhân và cách sửa:

**a) Config Server chưa sẵn sàng:**
```
Connection refused: config-server:8888
```
→ Đợi Config Server khởi động xong rồi restart:
```bash
docker compose restart order-service
```

**b) Database chưa sẵn sàng:**
```
Unable to connect to jdbc:postgresql://postgres:5432/order_db
```
→ Kiểm tra postgres:
```bash
docker compose ps postgres
# Phải là: Up (healthy)
docker compose restart order-service
```

**c) Thiếu biến môi trường:**
```
Could not resolve placeholder '${VNPAY_TMN_CODE}'
```
→ Kiểm tra file `.env`:
```bash
cat .env | grep VNPAY
```
→ Thêm biến còn thiếu vào `.env`, sau đó:
```bash
docker compose up -d payment-service
```

---

## Lỗi 2: Keycloak Không Import Realm

### Triệu chứng:
- Không thấy realm `ecommerce` trong Keycloak Admin UI

### Kiểm tra:
```bash
docker compose logs keycloak | grep -i "import\|realm\|error"
```

### Cách sửa:
1. Kiểm tra file `keycloak/realm-export.json` tồn tại:
   ```bash
   ls keycloak/
   ```
2. Nếu Keycloak đã khởi động trước khi có file, cần reset:
   ```bash
   docker compose stop keycloak keycloak-db
   docker compose rm -f keycloak keycloak-db
   docker volume ls | grep keycloak_db_data
   docker volume rm TEN_VOLUME_KEYCLOAK_DB_TU_LENH_TREN
   docker compose up -d keycloak-db keycloak
   ```

---

## Lỗi 3: Services Không Đăng Ký Vào Eureka

### Triệu chứng:
- Eureka Dashboard (http://localhost:8761) không thấy service

### Kiểm tra:
```bash
docker compose logs product-service | grep -i "eureka\|register"
```

### Nguyên nhân phổ biến:

**a) EUREKA_DEFAULT_ZONE sai trong .env:**
```bash
# Phải khớp: user:password@discovery-server:8761
grep EUREKA .env
```
→ Sửa `EUREKA_DEFAULT_ZONE=http://eureka:YOUR_PASSWORD@discovery-server:8761/eureka/`

**b) Discovery Server chưa chạy:**
```bash
docker compose ps discovery-server
```
→ Nếu chưa chạy: `docker compose up -d discovery-server`

---

## Lỗi 4: API Gateway Trả 401 Unauthorized

### Triệu chứng:
```json
{"error": "Unauthorized", "status": 401}
```

### Nguyên nhân:
1. Chưa gửi token
2. Token hết hạn (access token mặc định 5 phút)
3. Keycloak chưa sẵn sàng

### Cách sửa:
```bash
# Lấy token mới
curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=refresh_token" \
  -d "client_id=ecommerce-client" \
  -d "client_secret=YOUR_SECRET" \
  -d "refresh_token=YOUR_REFRESH_TOKEN"
```

---

## Lỗi 5: API Gateway Trả 429 Too Many Requests

### Triệu chứng:
```json
{"error": "Too Many Requests", "status": 429}
```

### Nguyên nhân: Rate limiting kích hoạt

### Cách sửa (tạm thời trong dev):
Đợi vài giây và thử lại. Hoặc điều chỉnh rate limit trong `config-server/src/main/resources/configs/api-gateway.yml`.

---

## Lỗi 6: Kafka Connection Failed

### Triệu chứng:
```
org.apache.kafka.common.errors.TimeoutException
```

### Kiểm tra:
```bash
docker compose ps kafka
# Phải: Up (healthy)

docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Cách sửa:
```bash
docker compose restart kafka
# Đợi kafka healthy rồi restart services phụ thuộc
docker compose restart order-service payment-service notification-service
```

---

## Lỗi 7: Elasticsearch Không Index

### Triệu chứng:
- `/api/search` không trả về kết quả
- search-service log có lỗi kết nối Elasticsearch

### Kiểm tra:
```bash
curl http://localhost:9200/_cluster/health
docker compose logs search-service | grep -i "elasticsearch\|error"
```

### Cách sửa:
```bash
docker compose restart elasticsearch
docker compose restart search-service
```

---

## Lỗi 8: Build Thất Bại (Maven)

### Lỗi: Tests thất bại khi build
```bash
# Bỏ qua tests khi build
./mvnw clean package -DskipTests
```

### Lỗi: Module không tìm thấy
```bash
# Build module common trước
./mvnw install -pl common -am -DskipTests
# Sau đó build lại
./mvnw package -DskipTests
```

### Lỗi: Java version không đúng
```bash
java -version  # Phải là Java 21
# Trên macOS: export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

---

## Lỗi 9: Port Bị Chiếm

### Triệu chứng:
```
Bind for 0.0.0.0:8080 failed: port is already allocated
```

### Tìm process đang dùng port:
```bash
# macOS / Linux
lsof -i :8080
# Windows
netstat -ano | findstr :8080
```

### Cách sửa:
```bash
# Dừng process chiếm port (thay PID bằng số thực)
kill -9 <PID>
# Hoặc dừng toàn bộ Docker
docker compose down
```

---

## Lỗi 10: Hết RAM / CPU

### Triệu chứng:
- Container liên tục restart (OOMKilled)
- Hệ thống chậm, lag

### Kiểm tra:
```bash
docker stats --no-stream
```

### Cách giảm tài nguyên:
1. Giảm RAM Elasticsearch:
   ```yaml
   # docker-compose.yml
   elasticsearch:
     environment:
       - "ES_JAVA_OPTS=-Xms128m -Xmx128m"
   ```
2. Giảm RAM Kafka:
   ```yaml
   kafka:
     environment:
       KAFKA_HEAP_OPTS: "-Xmx256m -Xms128m"
   ```
3. Tăng RAM Docker Desktop: Settings → Resources → Memory

---

## Reset Hoàn Toàn (Nuclear Option)

Khi mọi thứ đều lỗi và muốn bắt đầu lại từ đầu:

```bash
# CẢNH BÁO: Xóa toàn bộ data
docker compose down -v --remove-orphans
docker system prune -f

# Build lại và khởi động
./mvnw clean package -DskipTests
docker compose up -d --build
```
