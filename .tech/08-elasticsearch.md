# 08. Elasticsearch — Full-text Search

> Elasticsearch là **search engine** chuyên xử lý truy vấn full-text + filter có cấu trúc nhanh trên hàng triệu document. Project dùng nó cho `search-service` để search sản phẩm theo tên, mô tả, category, giá... — những thứ mà `LIKE '%...%'` trong PostgreSQL sẽ chạy chậm khi data lớn.

## 1. Khái niệm cốt lõi

| Khái niệm | Tương đương SQL | Ý nghĩa |
|-----------|-----------------|---------|
| **Index** | Table | Tập hợp document cùng schema |
| **Document** | Row | Bản ghi JSON, có `_id` |
| **Field** | Column | Trường trong document |
| **Mapping** | Schema | Định nghĩa kiểu dữ liệu của field |
| **Analyzer** | — | Pipeline xử lý text (tokenize, lowercase, stem) khi index và query |
| **Inverted Index** | — | Cấu trúc lưu "từ → list document chứa từ đó" — bí quyết tốc độ search |
| **Shard** | Partition | Index chia nhỏ để parallel query |
| **Replica** | Read-replica | Bản sao shard để HA và scale read |
| **Score (`_score`)** | — | Mức độ relevance của document với query (BM25 mặc định) |

## 2. Hệ thống đang dùng Elasticsearch ra sao

### 2.1 Cấu hình thực tế

- **Container**: `docker.elastic.co/elasticsearch/elasticsearch:8.18.8`
- **Port**: `9200` (HTTP REST API)
- **Mode**: single-node (không phải cluster) — chỉ phù hợp dev/demo
- **Security**: tắt (`xpack.security.enabled=false`) — production phải bật
- **Heap**: 256MB (`-Xms256m -Xmx256m`)

### 2.2 Index `products` — schema mapping

Dùng Spring Data Elasticsearch với annotation [ProductDocument.java](../search-service/src/main/java/com/ecommerce/search/document/ProductDocument.java):

```java
@Document(indexName = "products")
public class ProductDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;             // ← full-text search

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;      // ← full-text search

    @Field(type = FieldType.Double)
    private Double price;            // ← range filter

    @Field(type = FieldType.Keyword)
    private String sku;              // ← exact match (không tokenize)

    @Field(type = FieldType.Keyword)
    private String categoryId;       // ← term filter + aggregation

    @Field(type = FieldType.Date)
    private Instant createdAt;       // ← sort + range
    // ...
}
```

**Sự khác nhau Text vs Keyword** — quan trọng nhất:
- **Text** — tokenize ra nhiều term, dùng cho match query: `name = "iPhone 15 Pro"` → tokens `[iphone, 15, pro]`. Search "iphone" sẽ match.
- **Keyword** — giữ nguyên 1 chuỗi, dùng cho exact match, sort, aggregation: `sku = "IP15-PRO-256"` chỉ match đúng cả chuỗi.

> Lỗi kinh điển: đánh `name` là Keyword → search "iphone" không ra "iPhone 15 Pro" vì lưu nguyên cả chuỗi.

### 2.3 Luồng đồng bộ data từ product-service

product-service là **source of truth** (PostgreSQL). search-service chỉ đọc events:

```
product-service                                    search-service
     │                                                  │
     │ Tạo/update sản phẩm                              │
     │   ├─ INSERT INTO products                        │
     │   └─ Publish event "product-created"             │
     │                          │                       │
     │                          └─── Kafka topic ───────►
     │                                                  │ ProductEventConsumer
     │                                                  │   ├─ Idempotent check
     │                                                  │   │   (processed_search_events)
     │                                                  │   └─ Index vào Elasticsearch
     ▼                                                  ▼
PostgreSQL (truth)                              Elasticsearch (denormalized)
```

3 lợi ích:
- product-service không phụ thuộc Elasticsearch (down vẫn bán hàng được)
- search-service có thể rebuild index từ event log mà không cần truy vấn DB
- Idempotent consumer (xem [06-kafka.md](06-kafka.md)) chống index trùng

## 3. Workflow vận hành

### 3.1 Health check và info

```bash
curl http://localhost:9200
# → version info JSON

curl http://localhost:9200/_cluster/health?pretty
# Status:
# - green: tất cả shard primary + replica đều OK
# - yellow: primary OK nhưng có replica chưa assign (single-node là yellow)
# - red: có shard primary chết → mất data
```

### 3.2 Quản lý index

```bash
# Liệt kê tất cả index
curl http://localhost:9200/_cat/indices?v

# Xem mapping của index products
curl http://localhost:9200/products/_mapping?pretty

# Xem stats
curl http://localhost:9200/products/_stats?pretty | jq .indices.products.primaries.docs

# Đếm document
curl http://localhost:9200/products/_count
```

### 3.3 Query trực tiếp bằng REST API

#### A. Match query (full-text)

```bash
curl -X GET "http://localhost:9200/products/_search?pretty" \
  -H 'Content-Type: application/json' -d '{
    "query": {
      "match": { "name": "iphone" }
    }
  }'
```

#### B. Multi-match (search nhiều field)

```bash
curl -X POST "http://localhost:9200/products/_search?pretty" \
  -H 'Content-Type: application/json' -d '{
    "query": {
      "multi_match": {
        "query": "iphone pro",
        "fields": ["name^3", "description"]
      }
    }
  }'
# name^3 = boost field name lên 3x quan trọng hơn description
```

#### C. Bool query (kết hợp filter)

```bash
curl -X POST "http://localhost:9200/products/_search?pretty" \
  -H 'Content-Type: application/json' -d '{
    "query": {
      "bool": {
        "must":     [ { "match": { "name": "iphone" } } ],
        "filter":   [
          { "term":  { "categoryId": "phones" } },
          { "term":  { "isActive": true } },
          { "range": { "price": { "gte": 500, "lte": 2000 } } }
        ]
      }
    },
    "sort": [
      { "_score": "desc" },
      { "createdAt": "desc" }
    ],
    "from": 0,
    "size": 20
  }'
```

> **`must` vs `filter`**: cả hai đều là điều kiện AND. Nhưng `filter` không tính score (nhanh hơn, được cache). Dùng `filter` cho điều kiện chính xác (price, status), `must` cho text match.

#### D. Aggregation (group by + count)

```bash
curl -X POST "http://localhost:9200/products/_search?pretty" \
  -H 'Content-Type: application/json' -d '{
    "size": 0,
    "aggs": {
      "by_category": {
        "terms": { "field": "categoryId", "size": 10 }
      },
      "price_stats": {
        "stats": { "field": "price" }
      }
    }
  }'
```

#### E. Suggestion / Autocomplete

```bash
# Bằng prefix query
curl -X POST "http://localhost:9200/products/_search?pretty" \
  -H 'Content-Type: application/json' -d '{
    "query": {
      "prefix": { "name": "iph" }
    },
    "size": 5
  }'
```

### 3.4 Insert / update document thủ công (test)

```bash
# Index 1 document (POST → auto-id, PUT /id → fixed-id)
curl -X PUT "http://localhost:9200/products/_doc/test-1" \
  -H 'Content-Type: application/json' -d '{
    "id": "test-1",
    "name": "Test iPhone",
    "description": "Manual seed for testing",
    "price": 999.0,
    "categoryId": "phones",
    "isActive": true,
    "createdAt": "2026-05-24T00:00:00Z"
  }'

# Verify
curl http://localhost:9200/products/_doc/test-1?pretty

# Delete
curl -X DELETE http://localhost:9200/products/_doc/test-1
```

### 3.5 Reindex toàn bộ (rebuild)

Khi đổi mapping hoặc data drift:

```bash
# 1. Tạo index mới với mapping cập nhật
curl -X PUT http://localhost:9200/products_v2 -H 'Content-Type: application/json' -d '{...}'

# 2. Copy data từ index cũ sang
curl -X POST http://localhost:9200/_reindex -H 'Content-Type: application/json' -d '{
  "source": { "index": "products" },
  "dest":   { "index": "products_v2" }
}'

# 3. Đổi alias để service không cần redeploy
curl -X POST http://localhost:9200/_aliases -H 'Content-Type: application/json' -d '{
  "actions": [
    { "remove": { "index": "products",    "alias": "products_alias" } },
    { "add":    { "index": "products_v2", "alias": "products_alias" } }
  ]
}'
```

> Best practice: code dùng alias `products_alias`, không dùng tên index trực tiếp → reindex zero-downtime.

### 3.6 Test qua API của search-service

```bash
# Endpoint public (không cần token, xem SecurityConfig)
curl "http://localhost:8080/api/search/products?q=iphone&category=phones&minPrice=500&page=0&size=20"
```

### 3.7 Rebuild data cho dev (clear + replay events)

```bash
# 1. Xóa index
curl -X DELETE http://localhost:9200/products

# 2. Reset consumer offset của search-service về earliest
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group search-service \
  --topic product-created \
  --reset-offsets --to-earliest --execute --all-topics

# 3. Restart service — index sẽ được tạo lại từ event log
docker compose restart search-service
```

## 4. Troubleshooting

### 4.1 Search không trả kết quả mà data có trong DB

Kiểm tra theo thứ tự:

```bash
# A. Document có trong Elasticsearch không?
curl "http://localhost:9200/products/_count"
curl "http://localhost:9200/products/_search?q=*&size=3&pretty"

# B. Mapping đúng kiểu chưa?
curl http://localhost:9200/products/_mapping?pretty
```

Common bug: `name` đánh nhầm `Keyword` thay vì `Text` → `match` query không tách từ.

### 4.2 Search "iphone" không match "iPhone"

Mặc định analyzer `standard` đã lowercase. Nếu vẫn fail:
- Document có thể đã index từ trước khi sửa mapping → cần reindex
- Field type là `Keyword` → đổi sang `Text` rồi reindex

```bash
# Phân tích xem 1 chuỗi được tokenize ra sao
curl -X POST "http://localhost:9200/products/_analyze?pretty" \
  -H 'Content-Type: application/json' -d '{
    "field": "name",
    "text": "iPhone 15 Pro Max"
  }'
```

### 4.3 Service không đẩy được data vào Elasticsearch

```bash
# Test connectivity từ trong container
docker compose exec search-service \
  wget -qO- http://elasticsearch:9200/_cluster/health

# Xem log indexing
docker compose logs search-service | grep -i "elasticsearch\|index"

# Check Kafka consumer lag — service có nhận event không?
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 --group search-service --describe
```

### 4.4 Data bị duplicate sau reset

Đảm bảo dùng **`@Id` cố định** (id của Product trong PostgreSQL) khi index. Nếu để Elasticsearch auto-generate id → mỗi lần consume cùng event sẽ tạo document mới.

Đồng thời pattern `processed_search_events` (đã có trong project) ngăn process trùng cùng event.

### 4.5 Memory cao, query chậm

```bash
# Stats heap
curl http://localhost:9200/_nodes/stats/jvm?pretty | jq '.nodes[].jvm.mem'

# Slowlog (nếu đã enable)
curl http://localhost:9200/products/_settings?pretty
```

Tăng heap:
```yaml
elasticsearch:
  environment:
    - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
```
> Rule: heap = 50% RAM, không vượt 32GB.

### 4.6 Reset toàn bộ Elasticsearch

```bash
docker compose down
docker volume rm ecommerce-microservice-project_elasticsearch_data
docker compose up -d elasticsearch
```

### 4.7 Tiếng Việt unaccent

Standard analyzer **không** xử lý tiếng Việt có dấu tốt (search "dien thoai" không ra "điện thoại"). Khi cần:
- Dùng plugin `analysis-icu` để tách dấu (folding)
- Hoặc index thêm 1 field `name_unaccent` đã strip dấu sẵn từ phía Java

## 5. Best practices đang được áp dụng

- **CQRS pattern**: PostgreSQL ghi (truth), Elasticsearch chỉ đọc (search-optimized)
- **Event-driven sync** qua Kafka — search-service down không ảnh hưởng product-service
- **Idempotent consumer** với `processed_search_events` — replay event không tạo duplicate
- **Mapping rõ ràng Text/Keyword** — không dùng `dynamic mapping` để tránh mapping bùng nổ
- **Document `@Id` = Product.id** — cùng event ghi nhiều lần → cùng document, không duplicate
- **Health group `elasticsearch,kafka`** trong readiness — service chỉ ready khi cả 2 đều OK
