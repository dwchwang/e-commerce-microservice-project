# 16. Elasticsearch — Full-text Search & CQRS-lite

## 1. Mục Tiêu Nghiên Cứu

- Hiểu Elasticsearch và inverted index
- Hiểu Lucene — engine bên dưới
- Hiểu cách đồng bộ data từ Postgres sang Elasticsearch qua Kafka
- Hiểu CQRS pattern — Read model riêng cho query phức tạp

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Vì sao SQL `LIKE %keyword%` không đủ?

```sql
SELECT * FROM products WHERE name LIKE '%iphone%';
```
Nhược điểm:
- Full table scan (không dùng index thường)
- Không hỗ trợ stemming (iphone vs iPhones)
- Không ranking (cái nào liên quan hơn?)
- Không synonyms (mobile vs điện thoại)
- Không fuzzy matching (typo tolerance)
- Không tách word language-specific (tiếng Việt)

### 2.2. Inverted Index

Khái niệm cốt lõi của full-text search:
```
Doc 1: "iPhone 15 Pro"
Doc 2: "Samsung Galaxy"
Doc 3: "iPhone 14"

Inverted index:
  "iphone"  → [Doc1, Doc3]
  "15"      → [Doc1]
  "pro"     → [Doc1]
  "samsung" → [Doc2]
  ...
```

Tìm "iphone pro" → intersect [Doc1,Doc3] ∩ [Doc1] = [Doc1].

### 2.3. Apache Lucene + Elasticsearch

- **Lucene**: Java library, low-level inverted index
- **Elasticsearch**: Distributed REST wrapper trên Lucene, JSON-based
- **Solr**: Đối thủ của Elasticsearch, cùng nền Lucene

### 2.4. Khái niệm Elasticsearch

| Term | Tương đương SQL | Ý nghĩa |
|------|-----------------|---------|
| Index | Database/Table | Collection of documents |
| Document | Row | JSON object |
| Mapping | Schema | Field types, analyzers |
| Shard | Partition | Phần tử của index, scale ngang |
| Replica | Replica | Backup shard |
| Analyzer | - | Pipeline tokenize + filter |

### 2.5. Analyzer
Pipeline xử lý text khi index/query:
```
"iPhone 15 Pro Max"
  → Char filter (HTML strip, ...)
  → Tokenizer (split by space)
  → Token filters (lowercase, stemming, stop words)
  → ["iphone", "15", "pro", "max"]
```

Có thể dùng Vietnamese analyzer (vi_analyzer) cho tiếng Việt.

### 2.6. Query DSL

```json
GET /products/_search
{
  "query": {
    "multi_match": {
      "query": "iphone pro",
      "fields": ["name^3", "description"]    // boost name 3x
    }
  },
  "filter": {"term": {"category": "phones"}},
  "sort": [{"_score": "desc"}, {"price": "asc"}]
}
```

### 2.7. CQRS Pattern (Command Query Responsibility Segregation)

> Tách model write (Command) và model read (Query) thành các store riêng.

Trong dự án:
- **Write side (Command)**: product-service ghi vào PostgreSQL (chuẩn relational, ACID)
- **Read side (Query)**: search-service đọc từ Elasticsearch (full-text, fast read)

Đồng bộ qua **event** Kafka — đây là **CQRS-lite** (không full event sourcing).

```
Admin tạo product
  → product-service INSERT product_db
  → product-service publish "product-created" Kafka
  → search-service consume → INDEX product into Elasticsearch
```

→ Eventual consistency: vài giây lag giữa Postgres và Elasticsearch (acceptable cho search).

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. Topology
```
product-service ──[product-created/updated/deleted Kafka]──► search-service ──► Elasticsearch
                          (write side)                          (read side)
                                                                     │
                                       Client (qua Gateway) ─────────┘
                                       GET /api/search?keyword=...
```

### 3.2. search-service module

- Spring Boot + spring-data-elasticsearch
- Endpoint `GET /api/search?keyword=...&category=...&page=0&size=10`
- Consumer Kafka: `product-created`, `product-updated`, `product-deleted`

```java
@KafkaListener(topics = {"product-created", "product-updated"})
public void onProductChange(ProductEvent event) {
  if (processedRepo.existsById(event.getEventId())) return;
  ProductDocument doc = mapper.toDocument(event);
  productSearchRepo.save(doc);  // Elasticsearch index
  processedRepo.save(...);
}

@KafkaListener(topics = "product-deleted")
public void onProductDeleted(ProductDeletedEvent event) {
  productSearchRepo.deleteById(event.getProductId());
}
```

### 3.3. Document mapping

```java
@Document(indexName = "products")
public class ProductDocument {
  @Id String id;
  @Field(type = Text, analyzer = "standard") String name;
  @Field(type = Text) String description;
  @Field(type = Keyword) String category;        // Keyword = exact match (filter, agg)
  @Field(type = Double) BigDecimal price;
  @Field(type = Date) Instant createdAt;
}
```

### 3.4. Query API

```java
public Page<ProductDocument> search(String keyword, String category, Pageable pageable) {
  Criteria criteria = new Criteria("name").matches(keyword)
      .or(new Criteria("description").matches(keyword));
  if (category != null) criteria = criteria.and(new Criteria("category").is(category));
  
  Query q = new CriteriaQuery(criteria).setPageable(pageable);
  return elasticsearchOperations.search(q, ProductDocument.class)
                                 .map(SearchHit::getContent);
}
```

---

## 4. Trade-offs

| Pros | Cons |
|------|------|
| Full-text search mạnh | Eventual consistency (lag) |
| Scale read independently | Thêm 1 component (Elasticsearch cluster) |
| Aggregation, faceting | Phức tạp khi schema thay đổi |
| Geo, autocomplete features | RAM lớn (mỗi shard 50–80% RAM) |

---

## 5. Từ Khóa Nghiên Cứu

```
- elasticsearch lucene inverted index
- analyzer tokenizer filter
- bm25 ranking function
- elasticsearch query dsl
- cqrs pattern microservices
- event sourcing read model projection
- eventual consistency search index
- spring data elasticsearch
- vietnamese analyzer elasticsearch
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Vì sao không search trực tiếp Postgres full-text?**
→ Postgres FTS (`tsvector`) đủ cho hệ thống nhỏ. Khi cần ranking BM25, fuzzy match, phân tán scale, Elasticsearch tốt hơn. Cũng giảm tải DB.

**Q2: Eventual consistency giữa Postgres và Elasticsearch có vấn đề?**
→ User search có thể không thấy product mới ngay (delay vài giây). Acceptable cho search. Nếu cần consistency cao (vd: ngân hàng), không phù hợp.

**Q3: CQRS có overkill không?**
→ "CQRS-lite" thôi — chỉ tách read model cho search. Không full event sourcing. Phù hợp với scale e-commerce.

**Q4: Khi reindex toàn bộ thì sao?**
→ Có 2 cách:
1. Replay Kafka events (cần infinite retention hoặc Kafka compact topic)
2. Bulk re-index từ Postgres → Elasticsearch (one-time job)

**Q5: Elasticsearch crash, search-service fail consume — sửa thế nào?**
→ Idempotent consumer + processed_events table. Khi ES up, Kafka deliver lại events từ last commit offset, search-service dedupe rồi index.

**Q6: Có dùng autocomplete không?**
→ Có thể thêm bằng `completion` field type của ES. Đồ án bản hiện tại không có nhưng có thể nêu là "future work".

---

## 7. Tài Liệu Tham Khảo

- Clinton Gormley, *Elasticsearch: The Definitive Guide*, O'Reilly
- Radu Gheorghe et al, *Elasticsearch in Action*, Manning
- Apache Lucene documentation
- Martin Fowler — "CQRS" article
- Greg Young — "CQRS Documents" (PDF)
- Spring Data Elasticsearch Reference
