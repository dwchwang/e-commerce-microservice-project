# 15. Redis — Cache, Session, Atomic Counter, Distributed Lock

## 1. Mục Tiêu Nghiên Cứu

- Hiểu Redis và các data structure
- Hiểu Cache patterns (Cache-aside, Read-through, Write-through, Write-behind)
- Hiểu Cache invalidation problem
- Hiểu các use case: session, rate limit, counter, lock, pub/sub

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Redis là gì?
**RE**mote **DI**ctionary **S**erver — in-memory key-value store, hỗ trợ data structure phong phú.

Đặc điểm:
- **Single-threaded**: 1 thread xử lý commands → atomic mặc định
- **In-memory + persistence**: snapshot (RDB) + append-only log (AOF)
- **Data structures**: String, Hash, List, Set, Sorted Set, Bitmap, HyperLogLog, Geo, Stream
- **Latency**: sub-millisecond
- **Throughput**: 100K+ ops/sec mỗi node

### 2.2. Use cases trong dự án

| Use case | Redis structure | Service |
|----------|-----------------|---------|
| Cache product detail | String (JSON) | product-service |
| Shopping cart | Hash | cart-service |
| Rate limiter token bucket | String + Lua | api-gateway |
| Flash sale counter | String (atomic INCR/DECR) | flash-sale-service |
| Distributed lock | String (SET NX EX) | flash-sale-service scheduler |
| (Optional) Session store | Hash | n/a (dự án dùng JWT) |

### 2.3. Cache Patterns

**a) Cache-aside (Lazy loading)** — Phổ biến nhất
```
read(key):
  value = redis.get(key)
  if value == null:
    value = db.query(key)
    redis.set(key, value, ttl)
  return value

write(key, value):
  db.save(key, value)
  redis.delete(key)  // invalidate
```
- Pros: Đơn giản, chỉ cache thứ thực sự đọc
- Cons: First read miss, có race condition giữa load và update

**b) Read-through** — Cache lo việc load
```
Cache library tự handle cache miss
```

**c) Write-through** — Ghi đồng thời
```
write(key, value):
  redis.set(key, value)
  db.save(key, value)
```
- Pros: Cache luôn fresh
- Cons: Slow write

**d) Write-behind (Write-back)** — Ghi async
```
write(key, value):
  redis.set(key, value)
  asyncQueue.push(key)  // ghi DB sau
```
- Pros: Fast write
- Cons: Risk mất data khi cache crash

→ Dự án dùng **cache-aside** là chính.

### 2.4. Cache Invalidation — "Two hard things"

> "There are only two hard things in Computer Science: cache invalidation and naming things." — Phil Karlton

Strategies:
- **TTL-based**: Set expire 5–60 phút. Đơn giản, nhưng có lệch trong khoảng TTL
- **Explicit invalidation**: Khi update DB → DELETE key cache. Risk: race condition
- **Write-through**: Update cache cùng lúc DB. An toàn hơn nhưng tăng latency
- **Event-driven invalidation**: Service A update DB → publish Kafka → Service B clear local cache

→ Dự án product-service dùng TTL + explicit DELETE khi update.

### 2.5. Thundering Herd / Cache Stampede

**Vấn đề**: Cache key expire cùng lúc, nhiều request cùng load DB → DB overload.

**Solutions**:
- **Add jitter to TTL**: TTL = base + random(0, 30s)
- **Mutex/lock** khi load: 1 thread load, các thread khác chờ
- **Probabilistic early expiration**: random refresh trước khi hết hạn
- **Hot key cache** with longer TTL

### 2.6. Distributed Lock (đã thảo luận file 12, 13)

```
SET lock:campaign 1 NX EX 30
  NX: only if not exists
  EX: TTL in seconds (auto-release nếu owner crash)
```

→ Cần fencing token (Martin Kleppmann critique) cho fully correct, nhưng đồ án chấp nhận trade-off.

### 2.7. Redis Persistence

- **RDB (Snapshot)**: Dump toàn bộ state mỗi N giây. Compact, nhưng có thể mất N giây data
- **AOF (Append Only File)**: Log mọi write command. Ít mất, nhưng file lớn
- **AOF + RDB combined**: Best of both

→ Cache không cần persistence (chấp nhận rebuild). Atomic counter của flash sale **CẦN persistence** vì restart sẽ mất counter (CampaignScheduler có recovery từ DB).

### 2.8. Redis HA & Cluster

- **Master-Replica**: Replication async
- **Sentinel**: Auto failover
- **Cluster**: Sharding + replication, 16384 hash slots

→ Đồ án dùng single instance Redis 8. Production: ít nhất 1 master + 2 replica + Sentinel.

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. cart-service — Redis only

Cấu trúc key:
```
cart:user:{userId}    → Hash {productId: quantity, productId2: qty2}
cart:session:{sid}    → Hash (cho guest cart)
```

TTL: 30 ngày (refresh on each access).

```java
public void addItem(String userId, String productId, int quantity) {
  String key = "cart:user:" + userId;
  redisTemplate.opsForHash().increment(key, productId, quantity);
  redisTemplate.expire(key, Duration.ofDays(30));
}
```

**Đặc điểm**: KHÔNG có DB. Cart là ephemeral — mất cũng không sao (user re-add).

### 3.2. product-service — Cache product detail

```java
public ProductDto getProduct(String id) {
  String key = "product:" + id;
  String cached = redis.get(key);
  if (cached != null) return parse(cached);
  
  ProductDto p = productRepo.findById(id);
  redis.set(key, toJson(p), Duration.ofMinutes(15));
  return p;
}

public void updateProduct(ProductDto p) {
  productRepo.save(p);
  redis.delete("product:" + p.getId());  // invalidate
  
  // Optional: publish Kafka product-updated → search-service reindex
  kafkaTemplate.send("product-updated", p.getId(), p);
}
```

### 3.3. api-gateway — Rate limiter

Spring Cloud Gateway tự dùng Redis (Token Bucket Lua script). Key format:
```
request_rate_limiter.{routeId}.{userId}.tokens
request_rate_limiter.{routeId}.{userId}.timestamp
```

### 3.4. flash-sale-service — Atomic counter + lock

```
flash_sale:stock:{campaignId}            → counter, TTL = endTime - now
scheduler-lock:campaign                  → distributed lock cho CampaignScheduler
```

Operations:
```java
// Check & decrement atomic
Long remaining = redisTemplate.opsForValue().decrement(stockKey);
// (Lua script for full atomicity if needed)

// Lock acquire
Boolean locked = redisTemplate.opsForValue()
  .setIfAbsent(lockKey, "1", Duration.ofSeconds(30));
```

### 3.5. Kết nối Spring Boot

```yaml
spring.data.redis:
  host: redis
  port: 6379
  password: ${REDIS_PASSWORD:}
  timeout: 2s
  lettuce.pool:
    max-active: 16
    max-idle: 8
    min-idle: 2
```

Default client: **Lettuce** (Netty-based, thread-safe). Alternative: **Jedis** (older, blocking).

---

## 4. Trade-offs Cache

### Khi nào nên cache?
- Read-heavy + tolerant to staleness
- Compute expensive (aggregation, full-text search)
- DB load high

### Khi nào KHÔNG nên cache?
- Write-heavy
- Strong consistency required (financial)
- Data thay đổi liên tục (real-time)

→ Dự án: cache product (read-heavy, OK stale 15min). KHÔNG cache order detail, payment status (cần fresh).

---

## 5. Từ Khóa Nghiên Cứu

```
- redis data structures
- cache aside pattern
- thundering herd cache stampede
- cache invalidation strategies
- redis persistence rdb aof
- redis cluster sentinel
- distributed lock redlock
- redis lua scripting atomic
- redis pub sub vs kafka
- session storage redis
- spring data redis lettuce vs jedis
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Em chọn data structure gì cho cart?**
→ Hash — 1 key chứa nhiều field productId. Atomic increment per field (HINCRBY). TTL trên key.

**Q2: Cache invalidation khi update product có race không?**
→ Có. Vd: T1 read DB → T2 update DB → T2 invalidate cache → T1 set cache (stale). Giảm thiểu: TTL ngắn (15min), hoặc dùng transactional invalidation (Kafka event).

**Q3: Tại sao TTL flash-sale counter = endTime - NOW?**
→ Tự cleanup khi campaign hết. Không cần job xóa key. Nếu Redis restart trong campaign, scheduler recovery.

**Q4: Lettuce hay Jedis?**
→ Lettuce — thread-safe, non-blocking (Netty), default Spring Boot. Jedis cũ hơn, mỗi connection 1 thread, nhưng đơn giản dễ hiểu.

**Q5: Khi cache miss, có race nhiều thread cùng load DB?**
→ Có (thundering herd). Solution: mutex per key (Redis SETNX với short TTL), hoặc probabilistic early expiration. Đồ án mức độ traffic không cần optimization này.

**Q6: Vì sao session cart không cần DB?**
→ Cart là ephemeral state, user có thể re-add. Tradeoff: nếu Redis crash, user mất cart. TTL 30 ngày là fair compromise.

**Q7: Redis có atomic transaction (MULTI/EXEC) không?**
→ Có. Nhưng đồ án đa số dùng atomic command đơn (DECR, HINCRBY). Lua script tốt hơn cho compound atomic ops.

**Q8: Có dùng Redis pub/sub không?**
→ Không — đã có Kafka. Redis pub/sub fire-and-forget (không persist), không phù hợp business event.

---

## 7. Tài Liệu Tham Khảo

- redis.io/documentation
- Salvatore Sanfilippo, *Redis in Action* (cũ nhưng nền tảng)
- Josiah Carlson, *Redis in Action*, Manning
- Spring Data Redis Reference
- AWS — "Cache strategies" — caching best practices
- Martin Kleppmann — distributed lock essay
