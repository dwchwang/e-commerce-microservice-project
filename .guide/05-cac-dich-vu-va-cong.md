# Danh Sách Dịch Vụ và Cổng (Services & Ports)

## Cổng Expose Ra Host (Có thể truy cập từ localhost)

| Cổng | Container | Mục đích |
|------|-----------|----------|
| **8080** | ecommerce-api-gateway | API chính — điểm vào duy nhất cho client |
| **8761** | ecommerce-discovery-server | Eureka Dashboard |
| **8888** | ecommerce-config-server | Config Server API |
| **8180** | ecommerce-keycloak | Keycloak Admin UI |
| **5432** | ecommerce-postgres | PostgreSQL (dùng để debug trực tiếp) |
| **6379** | ecommerce-redis | Redis (dùng để debug trực tiếp) |
| **9092** | ecommerce-kafka | Kafka broker (external) |
| **9200** | ecommerce-elasticsearch | Elasticsearch API |
| **8025** | ecommerce-mailpit | Mailpit Web UI (xem email) |
| **9411** | ecommerce-zipkin | Zipkin Tracing UI |
| **9090** | ecommerce-prometheus | Prometheus UI |
| **3000** | ecommerce-grafana | Grafana Dashboard |

## Business Services (Chỉ expose nội bộ, không ra host)

Khi chạy Docker, các service sau chỉ có thể truy cập qua API Gateway:

| Service | Internal Port | Route qua Gateway |
|---------|--------------|-------------------|
| identity-service | 8081 | `/api/auth/**` |
| user-service | 8082 | `/api/users/**` |
| product-service | 8083 | `/api/products/**` |
| inventory-service | 8084 | `/api/inventory/**` |
| cart-service | 8085 | `/api/cart/**` |
| order-service | 8086 | `/api/orders/**` |
| payment-service | 8087 | `/api/payments/**` |
| voucher-service | 8088 | `/api/vouchers/**` |
| notification-service | 8089 | `/api/notifications/**` |
| review-service | 8090 | `/api/reviews/**` |
| search-service | 8091 | `/api/search/**` |
| content-service | 8092 | `/api/content/**` |
| flash-sale-service | 8093 | `/api/flash-sales/**` |

> Khi chạy local development (không Docker), các service lắng nghe trực tiếp trên port trên.

## URL Truy Cập

### Giao Diện Web
| Giao diện | URL | Đăng nhập |
|-----------|-----|-----------|
| API Gateway (entry point) | http://localhost:8080 | JWT Token |
| Swagger UI tổng hợp | http://localhost:8080/swagger-ui.html | Không cần |
| Eureka Dashboard | http://localhost:8761 | eureka / EUREKA_PASSWORD |
| Keycloak Admin | http://localhost:8180 | admin / KEYCLOAK_ADMIN_PASSWORD |
| Grafana | http://localhost:3000 | admin / GF_SECURITY_ADMIN_PASSWORD |
| Prometheus | http://localhost:9090 | Không cần |
| Zipkin | http://localhost:9411 | Không cần |
| Mailpit | http://localhost:8025 | Không cần |

### Swagger UI Từng Service (Khi Chạy Local Dev)
| Service | Swagger URL |
|---------|------------|
| identity-service | http://localhost:8081/swagger-ui.html |
| user-service | http://localhost:8082/swagger-ui.html |
| product-service | http://localhost:8083/swagger-ui.html |
| inventory-service | http://localhost:8084/swagger-ui.html |
| cart-service | http://localhost:8085/swagger-ui.html |
| order-service | http://localhost:8086/swagger-ui.html |
| payment-service | http://localhost:8087/swagger-ui.html |
| voucher-service | http://localhost:8088/swagger-ui.html |
| notification-service | http://localhost:8089/swagger-ui.html |
| review-service | http://localhost:8090/swagger-ui.html |
| search-service | http://localhost:8091/swagger-ui.html |
| content-service | http://localhost:8092/swagger-ui.html |
| flash-sale-service | http://localhost:8093/swagger-ui.html |

## Rate Limiting (API Gateway)

Một số route có giới hạn request:

| Route | Giới hạn |
|-------|---------|
| `/api/auth/**` | 10 req/giây, burst 20 |
| `/api/orders/**` | Có rate limiting |
| `/api/flash-sales/**/purchase` | Có rate limiting (flash sale) |

Nếu vượt quá giới hạn, gateway trả về HTTP **429 Too Many Requests**.

## Kết Nối Database Trực Tiếp

```bash
# Kết nối PostgreSQL từ host
psql -h localhost -p 5432 -U postgres

# Chọn database cụ thể
\c product_db

# Hoặc dùng docker exec
docker compose exec postgres psql -U postgres -d order_db
```

```bash
# Kết nối Redis từ host
redis-cli -h localhost -p 6379

# Xem tất cả keys
KEYS *
```
