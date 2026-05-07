# 14. Database per Service & Flyway Migration

## 1. Mục Tiêu Nghiên Cứu

- Hiểu pattern Database per Service
- Hiểu các vấn đề khi tách DB (joins, transactions cross-DB)
- Hiểu PostgreSQL — RDBMS chính
- Hiểu Flyway — schema versioning
- Phân biệt JPA, Hibernate, Spring Data JPA

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Database per Service Pattern

**Nguyên tắc**: Mỗi service sở hữu DB của nó, **không service khác có thể truy cập DB này trực tiếp** — chỉ qua API.

→ Đây là yêu cầu **bắt buộc** của Microservice (theo Sam Newman, Chris Richardson).

### 2.2. Vì sao bắt buộc?

| Vấn đề khi share DB | Hệ lụy |
|--------------------|--------|
| Tight coupling schema | Đổi schema → phải coordinate nhiều team |
| Khó scale độc lập | DB là bottleneck chung |
| Khó chọn DB phù hợp | Vd: search-service muốn Elasticsearch, không phải Postgres |
| Không có boundary | Mỗi service có thể đọc/ghi data của service khác → spaghetti |

### 2.3. Các vấn đề và cách giải

**1. Joins cross-service**
- KHÔNG join trực tiếp. Thay bằng:
  - **API Composition**: caller gọi nhiều service, gộp dữ liệu (Gateway BFF)
  - **CQRS + materialized view**: 1 service có read-only copy của data từ service khác (search-service trong dự án)
  - **Data replication qua event**: subscribe Kafka event để build local cache

**2. Distributed transaction**
- KHÔNG dùng 2PC. Thay bằng **Saga pattern** (file 09)

**3. Reporting/Analytics**
- Không query trực tiếp 13 DB
- Build **data warehouse** (BigQuery, Snowflake) — ETL từ service DBs
- Hoặc **CDC** (Debezium) → Kafka → DW

### 2.4. Polyglot Persistence

Mỗi service chọn DB phù hợp:

| Service | DB | Lý do |
|---------|-----|-------|
| order-service | PostgreSQL | ACID cho financial data |
| product-service | PostgreSQL + Redis cache | Read-heavy, cần index full-text |
| cart-service | Redis only | Ephemeral, key-value đủ dùng |
| search-service | Elasticsearch | Full-text, ranking |
| flash-sale-service | PostgreSQL + Redis | Atomic counter Redis, audit DB |
| Keycloak | PostgreSQL riêng | Tránh lẫn với app data |

→ Đồ án thực hành đầy đủ polyglot persistence.

### 2.5. PostgreSQL — RDBMS chính

**Tại sao Postgres?**
- ACID đầy đủ
- JSONB support (kết hợp document)
- Full-text search built-in
- Performance tốt cho mid-scale
- Open source, không vendor lock-in

Trong dự án: PostgreSQL 17 với 10 logical databases riêng (`order_db`, `payment_db`, ...). Cùng 1 instance PostgreSQL nhưng 10 schemas tách biệt → tiết kiệm RAM cho local demo.

→ Production thực: mỗi service nên có **physical instance** Postgres riêng. Đồ án để tiết kiệm.

### 2.6. JPA & Hibernate & Spring Data JPA

| Layer | Vai trò |
|-------|---------|
| **JPA (Java Persistence API)** | Spec — JSR 338 (Jakarta Persistence) |
| **Hibernate** | Implementation phổ biến nhất của JPA |
| **Spring Data JPA** | Wrapper trên JPA — Repository pattern, auto-generated queries từ method name |

```java
public interface OrderRepository extends JpaRepository<Order, UUID> {
  List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);  // auto-generated SQL
  
  @Query("SELECT o FROM Order o WHERE o.reservationExpiredAt < :now AND o.status = 'STOCK_RESERVED'")
  List<Order> findExpired(@Param("now") Instant now);
}
```

### 2.7. JPA Best Practices Trong Microservice

- **`@Version` cho optimistic lock** — tránh lost update
- **Lazy loading**: cẩn thận N+1 query → dùng `@EntityGraph` hoặc fetch join
- **DTO projection** — không expose Entity ra ngoài
- **`@CreatedDate` / `@LastModifiedDate`** từ Spring Data Auditing
- **Hikari pool size**: `spring.datasource.hikari.maximum-pool-size = max thread Tomcat × 1.1`
- **No bidirectional reference giữa aggregates** — chỉ bằng ID

### 2.8. Flyway — Database Migration

Vấn đề: DB schema tiến hóa, làm sao apply migration nhất quán giữa dev/staging/prod?

**Flyway** approach:
- Mỗi migration là 1 file `Vx__name.sql` (vd: `V1__create_orders_table.sql`)
- Flyway giữ table `flyway_schema_history` lưu version đã apply
- Khi service start → Flyway check, apply migrations còn thiếu

**Cấu trúc**:
```
order-service/src/main/resources/db/migration/
  V1__create_orders_table.sql
  V2__add_voucher_id_column.sql
  V3__create_outbox_table.sql
  V4__create_processed_events_table.sql
```

**Quy tắc**:
- KHÔNG sửa migration đã chạy production
- Đặt tên rõ: `V20260101__add_index_status.sql` (timestamp prefix)
- Có thể `R__` prefix cho repeatable migration (vd: views)
- Production: dùng `out-of-order: false` cẩn thận

### 2.9. Migration so với alternatives

| Tool | Pros | Cons |
|------|------|------|
| **Flyway** | Đơn giản, SQL-first | Phải tự viết DDL |
| **Liquibase** | XML/YAML/SQL, rollback support | Phức tạp hơn |
| **JPA `ddl-auto: update`** | Tự sinh schema | KHÔNG dùng production — drift, không deterministic |

→ Dự án dùng Flyway, `ddl-auto: validate` (Hibernate verify schema match Entity).

---

## 3. Áp Dụng Trong Dự Án

### 3.1. 10 PostgreSQL databases

| DB | Service | Purpose |
|------|---------|---------|
| user_db | user-service | User profile |
| product_db | product-service | Catalog |
| inventory_db | inventory-service | Stock + reserved |
| voucher_db | voucher-service | Promotion codes |
| order_db | order-service | Orders + outbox + processed_events |
| payment_db | payment-service | Payments + payment_outbox |
| notification_db | notification-service | Email log |
| review_db | review-service | Product reviews |
| content_db | content-service | Banners, CMS |
| flash_sale_db | flash-sale-service | Campaigns, products in sale |

(Keycloak có DB riêng nữa)

### 3.2. init-db/

```
init-db/init.sql
  → script chạy 1 lần khi PostgreSQL container khởi tạo
  → CREATE DATABASE user_db; CREATE DATABASE product_db; ...
```

Docker Compose mount:
```yaml
postgres:
  volumes:
    - ./init-db:/docker-entrypoint-initdb.d
```

### 3.3. Connection per service

Mỗi service config:
```yaml
spring.datasource:
  url: jdbc:postgresql://postgres:5432/order_db
  username: postgres
  password: ${POSTGRES_PASSWORD}
spring.jpa:
  hibernate.ddl-auto: validate
  properties.hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
```

### 3.4. HikariCP connection pool

Default Spring Boot DB pool. Cấu hình:
```yaml
spring.datasource.hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

Theo dõi qua Prometheus: `hikaricp_connections_active`, `hikaricp_connections_pending`.

### 3.5. Auditing fields

`common/BaseEntity`:
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
  @Id @GeneratedValue UUID id;
  @CreatedDate Instant createdAt;
  @LastModifiedDate Instant updatedAt;
  @Version Long version;  // optimistic lock
}
```

---

## 4. Trade-offs

| Pros | Cons |
|------|------|
| Loose coupling | Không thể join SQL cross-service |
| Polyglot persistence | Distributed transactions phức tạp (Saga) |
| Scale độc lập | Reporting cần ETL |
| Schema evolution độc lập | Data duplication (CQRS) |
| Failure isolation | More DB instances → ops cost |

---

## 5. Từ Khóa Nghiên Cứu

```
- database per service pattern microservices
- polyglot persistence
- shared database antipattern
- api composition pattern
- cqrs command query responsibility segregation
- data replication kafka cdc
- flyway versioned migration
- hibernate jpa best practices
- hikari connection pool tuning
- postgresql jsonb full text search
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Em có 10 DB physical hay logical?**
→ Logical — cùng 1 PostgreSQL instance, 10 databases khác nhau (CREATE DATABASE). Lý do: tiết kiệm RAM cho demo. Production phải tách physical, có thể khác cluster, version riêng.

**Q2: Khi cần report tổng hợp (vd: doanh thu theo sản phẩm)?**
→ KHÔNG join SQL. Có thể:
- API composition (Gateway gọi nhiều service)
- ETL ra DW (BigQuery), query OLAP riêng
- Materialized view qua CDC

**Q3: ddl-auto là gì?**
→ Hibernate cấu hình:
- `none`: không làm gì
- `validate`: kiểm tra Entity match schema
- `update`: tự thêm column thiếu
- `create`: drop & recreate
→ Em dùng `validate` + Flyway migration. KHÔNG bao giờ dùng `update`/`create` production.

**Q4: Vì sao dùng Flyway thay JPA tự sinh schema?**
→ Tự sinh không deterministic, không track migration. Flyway version-controlled, audit-able, rollback-able.

**Q5: Vì sao chọn PostgreSQL?**
→ ACID, JSONB hybrid, full-text search built-in, open source, performance tốt. So với MySQL: Postgres mạnh hơn ở JSON, indexing nâng cao (GIN, GIST), CTE. So với NoSQL: dự án cần ACID cho order/payment.

**Q6: HikariCP pool size em đặt bao nhiêu?**
→ Tomcat thread max = 200, em đặt pool = 20–30 (10–15% Tomcat). Postgres `max_connections` mặc định 100 — phải tính tổng pool tất cả service không vượt.

**Q7: N+1 query problem em xử lý thế nào?**
→ Dùng `@EntityGraph` hoặc fetch join trong query. Cẩn thận với LAZY relationship khi serialize ra DTO. Tốt nhất: query DTO trực tiếp qua projection.

**Q8: Có dùng read replica không?**
→ Đồ án không. Production thực cho đọc-nhiều service (product-service, search-service): primary cho write, replica cho read. Spring Data hỗ trợ qua `@Transactional(readOnly = true)`.

---

## 7. Tài Liệu Tham Khảo

### Sách
- Vlad Mihalcea, *High-Performance Java Persistence*, 2nd ed.
- Martin Kleppmann, *Designing Data-Intensive Applications*, Chapter 1–3
- Pramod Sadalage, Martin Fowler, *NoSQL Distilled* — polyglot persistence

### Documentation
- PostgreSQL 17 Documentation
- Flyway documentation — flywaydb.org
- Spring Data JPA Reference
- Hibernate User Guide

### Pattern
- Chris Richardson — "Database per Service" microservices.io
- Pat Helland — "Data on the Outside vs. Data on the Inside"
