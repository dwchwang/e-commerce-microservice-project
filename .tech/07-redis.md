# 07. Redis — Cache, Cart, Rate Limit, Flash Sale

> Redis là **in-memory key-value store** đa dụng. Project dùng Redis cho 4 mục đích khác nhau, mỗi cái khai thác một tính năng riêng của Redis.

## 1. Khái niệm cốt lõi

| Khái niệm | Ý nghĩa |
|-----------|---------|
| **Key-Value** | Lưu key (string) + value (string/hash/list/set/zset) |
| **TTL (Time-To-Live)** | Mỗi key có thể auto-expire sau N giây |
| **Atomic operation** | INCR/DECR/SETNX là atomic — không cần lock |
| **Lua script** | Đoạn script chạy nguyên block atomic trong Redis, không bị interleave |
| **Pub/Sub** | Cơ chế gửi message — project không dùng (đã có Kafka) |
| **Eviction policy** | Khi đầy memory thì xóa key nào (LRU, LFU, random...) |

## 2. Hệ thống đang dùng Redis ra sao

### 2.1 Cấu hình thực tế

- **Container**: `redis:8-alpine`, port `6379`
- **Persistence**: volume `redis_data` (mặc định Redis dùng RDB snapshot)
- **No password** trong dev — production phải bật `requirepass`

Service nào dùng Redis: **api-gateway, cart-service, product-service, flash-sale-service**.

### 2.2 Use case 1 — Rate Limiting (api-gateway)

[api-gateway.yml](../config-server/src/main/resources/configs/api-gateway.yml) dùng filter `RequestRateLimiter` của Spring Cloud Gateway, backed by Redis:

```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 10    # ← 10 req/s steady
      redis-rate-limiter.burstCapacity: 20    # ← cho phép burst tối đa 20
      key-resolver: "#{@userKeyResolver}"     # ← rate limit theo userId
```

Algorithm: **Token Bucket**. Bucket có 20 token, refill 10 token/giây. Mỗi request lấy 1 token. Hết token → 429 Too Many Requests.

[RateLimiterConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/RateLimiterConfig.java) định nghĩa 2 key resolver:
- `userKeyResolver` — lấy `X-User-Id` (đã do gateway set từ JWT) → rate limit per-user
- `ipKeyResolver` — lấy IP → dùng cho `/api/auth/**` (chưa login nên không có userId)

Áp dụng:
- `/api/auth/**` — 10 req/s, 20 burst, theo IP
- `/api/orders/**` — 10 req/s, 20 burst, theo user
- `/api/flash-sales/*/purchase` — **3 req/s, 5 burst** theo user (siết chặt nhất vì là endpoint quan trọng nhất)

### 2.3 Use case 2 — Cart (cart-service)

cart-service không dùng PostgreSQL, **lưu cart trực tiếp vào Redis** vì:
- Cart là dữ liệu volatile (user thêm/xóa nhanh, không cần history)
- Cần truy cập nhanh khi user click nhiều lần
- Có thể auto-expire sau X ngày inactive

Pattern key:
```
cart:{userId}      → Hash chứa { productId: quantity }
cart:guest:{sessionId}  → Cart cho user chưa login
```

### 2.4 Use case 3 — Cache sản phẩm (product-service)

Pattern điển hình **Cache-Aside**:
```java
public ProductResponse getProduct(Long id) {
    // 1. Check cache
    String key = "product:" + id;
    ProductResponse cached = (ProductResponse) redisTemplate.opsForValue().get(key);
    if (cached != null) return cached;

    // 2. Miss → query DB
    Product product = productRepository.findById(id).orElseThrow(...);
    ProductResponse response = mapper.toResponse(product);

    // 3. Save cache với TTL
    redisTemplate.opsForValue().set(key, response, Duration.ofMinutes(15));
    return response;
}
```

Khi **update/delete** sản phẩm → **invalidate** cache:
```java
redisTemplate.delete("product:" + id);
```

### 2.5 Use case 4 — Flash Sale (flash-sale-service) — phức tạp nhất

Đây là use case quan trọng nhất của Redis trong project. Vấn đề kỹ thuật:
- 1000 user click "mua" cùng lúc, chỉ có 100 slot
- Phải đảm bảo **không oversell**, không user nào mua được 2 lần

Giải pháp: **Lua script atomic**. Xem [FlashSaleServiceImpl.java](../flash-sale-service/src/main/java/com/ecommerce/flashsale/service/impl/FlashSaleServiceImpl.java):

```lua
-- KEYS[1] = "flash:stock:{campaignId}"     (số slot còn lại)
-- KEYS[2] = "flash:buyers:{campaignId}"    (Set chứa userId đã mua)
-- ARGV[1] = userId
-- ARGV[2] = TTL seconds

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
  return -2          -- user đã mua rồi
end
local stock = redis.call('GET', KEYS[1])
if not stock then
  return -3          -- chưa có stock, campaign chưa active
end
stock = tonumber(stock)
if stock <= 0 then
  return -1          -- hết hàng
end
local remaining = redis.call('DECR', KEYS[1])     -- atomic decrement
redis.call('SADD', KEYS[2], ARGV[1])              -- ghi nhận user đã mua
redis.call('EXPIRE', KEYS[2], ARGV[2])
return remaining
```

3 đặc điểm quan trọng:
1. **Atomic** — toàn bộ script chạy như 1 block, không bị 2 request "đọc cùng stock=1, cùng giảm thành 0" gây oversell
2. **Idempotent per user** — `SISMEMBER` check trước khi DECR → 1 user spam click chỉ đặt được 1 lần
3. **Compensating script** — khi Kafka publish fail thì rollback bằng script INCR + SREM

Service tách biệt **fast-path (Redis)** với **slow-path (DB)**:
- Redis decrement đảm bảo concurrency cho purchase
- DB `incrementSoldCount` chạy bất đồng bộ, có scheduler reconciliation đối soát

## 3. Workflow vận hành

### 3.1 Kết nối redis-cli

```bash
docker compose exec redis redis-cli

# Trong shell:
PING                    # → PONG
INFO server | head -20
CLIENT LIST             # ai đang connect
```

### 3.2 Khám phá data

```bash
# Liệt kê key (DEV ONLY — KEYS chậm trên production)
KEYS *
KEYS cart:*
KEYS flash:*

# An toàn cho production: SCAN
SCAN 0 MATCH "cart:*" COUNT 100

# Xem giá trị + TTL
GET product:1
TTL product:1

# Xem hash (cart)
HGETALL cart:9c1f-uuid

# Xem set (flash sale buyers)
SMEMBERS flash:buyers:campaign-uuid
SCARD flash:buyers:campaign-uuid    # đếm size

# Xem type
TYPE flash:stock:campaign-uuid
```

### 3.3 Test rate limit

```bash
# Spam 30 request /api/orders trong 1 giây
for i in {1..30}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/orders
done
# Sẽ thấy 200 ban đầu rồi chuyển sang 429
```

Kiểm tra Redis state khi đang rate limit:
```bash
docker compose exec redis redis-cli KEYS "request_rate_limiter*"
# Sẽ thấy các key mà Spring Cloud Gateway tạo ra để track token bucket
```

### 3.4 Reset stock cho 1 flash sale campaign (debug)

```bash
docker compose exec redis redis-cli
> SET flash:stock:<campaign-uuid> 100
> DEL flash:buyers:<campaign-uuid>
> EXPIRE flash:stock:<campaign-uuid> 3600
```

### 3.5 Monitor performance

```bash
# Real-time stats
docker compose exec redis redis-cli --stat

# Output mỗi giây:
# keys mem clients blocked requests connections
# 142  2.5M 12      0       452 (+12)  342

# Slowlog — query nào chậm
docker compose exec redis redis-cli SLOWLOG GET 10

# MONITOR — log mọi command (DEV ONLY, rất nặng)
docker compose exec redis redis-cli MONITOR
```

### 3.6 Memory analysis

```bash
docker compose exec redis redis-cli INFO memory

# Phân tích key nào tốn RAM nhất
docker compose exec redis redis-cli --bigkeys

# Memory của 1 key cụ thể
docker compose exec redis redis-cli MEMORY USAGE flash:buyers:campaign-uuid
```

### 3.7 Clear toàn bộ database (dev only)

```bash
docker compose exec redis redis-cli FLUSHALL
# hoặc chỉ flush 1 logical DB:
docker compose exec redis redis-cli -n 0 FLUSHDB
```

## 4. Troubleshooting

### 4.1 Service báo "Connection refused" Redis

```bash
docker compose ps redis
docker compose exec service-name nslookup redis
# Hostname phải là "redis" (tên container), không phải "localhost"
```

Common bug: env `REDIS_HOST` để mặc định `localhost` → service không tìm thấy.

### 4.2 Cache invalidation lỗi → user thấy data cũ

Pattern phổ biến mà sai:
```java
// SAI — update DB rồi mới invalidate cache.
// Nếu invalidate fail, user vẫn thấy data cũ.
productRepository.save(product);
redisTemplate.delete("product:" + id);
```

Cách đúng (1 trong các options):
- **Cache-aside + TTL ngắn** — chấp nhận stale tối đa N phút, đơn giản
- **Write-through** — cache update đồng bộ với DB
- **Pub/Sub invalidation** — service publish event "product-updated", các instance khác listen và xóa cache local

### 4.3 Flash sale oversell

Nguyên nhân duy nhất: **không dùng Lua script** mà làm 2 step:
```java
// SAI — race condition
Long stock = redisTemplate.opsForValue().get("flash:stock:1");
if (stock > 0) redisTemplate.opsForValue().decrement("flash:stock:1");
```

→ 2 request có thể cùng đọc stock=1 → cùng decrement → stock=-1.

Đúng = dùng **Lua script** như project đang làm.

### 4.4 Memory đầy → Redis evict key bất ngờ

Mặc định Redis `maxmemory-policy=noeviction` → không xóa, chỉ reject write mới. Cho cache thuần thì nên đổi:
```bash
docker compose exec redis redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

> Cho data quan trọng (cart, flash sale buyers), nên có TTL phù hợp thay vì để LRU evict.

### 4.5 TTL bị reset bất ngờ

Một số command tự reset TTL:
- `SET key value` (không kèm `KEEPTTL`) → TTL reset thành -1 (forever)
- `RENAME key1 key2` → TTL của key1 chuyển nguyên sang key2

Để giữ TTL khi update:
```
SET product:1 "{...}" EX 900
hoặc
SET product:1 "{...}" KEEPTTL
```

### 4.6 Persistence: data mất sau restart

Redis 8 mặc định dùng RDB snapshot mỗi vài phút. Để giảm risk:
- Bật **AOF** (Append-Only File) — log mọi write, recovery chính xác hơn
  ```yaml
  command: redis-server --appendonly yes --save "60 1"
  ```
- Hoặc chấp nhận data có thể mất 5-15 phút (acceptable cho cache, cart)

> Cart user mất là khó chịu nhưng không critical → RDB là OK cho project.

## 5. Best practices đang được áp dụng

- **Lua script cho atomic flash sale** — đảm bảo không oversell
- **Token Bucket rate limit qua Redis** — share state giữa nhiều instance Gateway
- **Per-resource rate limit** — siết flash-sale chặt hơn order, order chặt hơn auth
- **TTL cho mọi cache** — không có cache "vĩnh viễn"
- **Key namespace có prefix** (`product:`, `cart:`, `flash:stock:`, `flash:buyers:`) — dễ debug và mass-delete
- **Compensating script** khi business logic phía sau fail — rollback Redis state
