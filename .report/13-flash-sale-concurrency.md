# 13. High Concurrency — Flash Sale, Atomic Counter, Distributed Lock

> Module flash-sale là "killer feature" của đồ án. Trong báo cáo, đây là chương cho thấy bạn hiểu **race condition**, **atomic operation**, **eventual consistency**.
>
> Cập nhật sau Phase 13: flash-sale đã có spike test thật trên AWS với 500 VU, 100 stock và kết quả durable là 100 purchase thành công, 100 confirmed orders, 0 duplicate buyer. Đây là số liệu mạnh nhất nên đưa vào Chương 6.

## 1. Mục Tiêu Nghiên Cứu

- Hiểu race condition và oversell problem
- Hiểu atomic operation (Redis DECR, INCR)
- Hiểu cách Redis tránh oversell ở scale
- Hiểu Redis Lua script (atomic check-and-decrement)
- Hiểu pattern "Cache + DB + Reconciliation"

---

## 2. Bài Toán

### 2.1. Yêu cầu nghiệp vụ
- Admin tạo campaign: 100 cái iPhone, giảm giá 50%, từ 12:00 đến 12:05
- 12:00:00.000 — 10,000 user cùng click "Mua"
- Phải đảm bảo: **đúng 100 cái được bán**, không ít hơn, **không nhiều hơn**

### 2.2. Race Condition trong DB

Code naive:
```java
@Transactional
public void purchase(Long campaignId, Long userId) {
  Campaign c = campaignRepo.findById(campaignId);
  if (c.getSoldCount() < c.getQuantity()) {
    c.setSoldCount(c.getSoldCount() + 1);
    campaignRepo.save(c);
    publishOrderEvent(...);
  } else {
    throw new SoldOutException();
  }
}
```

→ **OVERSELL**! Hai request đọc soldCount=99 cùng lúc, cả hai đều thấy `99 < 100`, đều tăng lên 100 → bán 101 cái.

### 2.3. Giải pháp DB-level (KHÔNG TỐI ƯU)

**Option A: Pessimistic Lock**
```sql
SELECT * FROM campaigns WHERE id = ? FOR UPDATE;
```
→ Lock toàn bảng/row → 10,000 user xếp hàng → mỗi tx ~5ms → 50 giây mới hết queue → user timeout.

**Option B: Optimistic Lock + Retry**
```sql
UPDATE campaigns SET sold_count = sold_count + 1 
WHERE id = ? AND sold_count < quantity;
```
→ Nhanh hơn (atomic UPDATE) nhưng vẫn hit DB mỗi request → DB CPU saturate ở 10K RPS.

**Option C: Redis Atomic Counter (SOLUTION CHÍNH)**

→ Redis single-threaded, command atomic, in-memory → millions ops/sec.

---

## 3. Giải Pháp Trong Dự Án — Redis Atomic Decrement

### 3.1. Setup khi campaign chuyển ACTIVE

```
CampaignScheduler (5s) phát hiện campaign sắp ACTIVE:
  remaining = quantity - soldCount  (vd: 100)
  Redis: SET flash_sale:stock:{campaignId} {remaining} EX (endTime - NOW)
```

### 3.2. Purchase flow

```java
public PurchaseResult purchase(Long campaignId, Long userId) {
  // (1) Lua script: check duplicate buyer + stock exists + stock > 0 + DECR
  Long result = redisTemplate.execute(RESERVE_SCRIPT,
      List.of(stockKey(campaignId), buyersKey(campaignId)),
      userId, ttlSeconds);

  if (result == -2) {
    throw new AlreadyPurchasedException();
  }
  if (result == -1) {
    throw new SoldOutException();
  }
  if (result == -3) {
    throw new BusinessException("Campaign stock is not ready");
  }
  
  // (2) Publish Kafka đồng bộ để order-service tạo đơn
  try {
    eventProducer.publishSync(new FlashSaleOrderRequestedEvent(...));
  } catch (Exception ex) {
    compensateReservedSlot(campaignId, userId); // INCR stock + SREM buyer
    throw new SlotReservationException("Cannot create flash sale order");
  }

  // (3) Tăng soldCount trong DB để audit/reporting
  campaignRepo.incrementSoldCount(campaignId);
  
  return PurchaseResult.success();
}
```

### 3.3. Vì sao Redis DECR atomic?

- Redis single-threaded event loop
- Command DECR thực hiện hoàn chỉnh trước khi xử lý command tiếp theo
- Trả về **giá trị sau khi decrement** → caller biết kết quả mà không cần round trip thứ 2

→ 10,000 request đến cùng lúc:
- Redis xử lý tuần tự (microsecond mỗi command)
- Tối đa 100 request reserve slot thành công
- Các request còn lại nhận sold-out hoặc duplicate-purchase

### 3.4. Vấn đề "INCR rollback" có race

Đây là vấn đề của bản đơn giản dùng `DECR` rồi `INCR` thủ công:

Giả sử state Redis = 0:
- Request A: DECR → -1 (fail)
- Request B: DECR → -2 (fail)
- Request A: INCR → -1
- Request B: INCR → 0

→ Counter cuối có thể đúng nhưng có khoảng thời gian giá trị âm. Dự án tránh vấn đề này bằng Lua script: chỉ `DECR` khi `stock > 0`, nên không cần rollback âm trong happy path.

### 3.5. Lua Script Atomic (best practice)

Để gộp decrement + check + rollback thành 1 atomic operation:

```lua
local current = redis.call('DECR', KEYS[1])
if current < 0 then
    redis.call('INCR', KEYS[1])
    return -1
end
return current
```

```java
String script = "local c = redis.call('DECR', KEYS[1]); " +
                "if c < 0 then redis.call('INCR', KEYS[1]); return -1 end; " +
                "return c;";
Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                                    Collections.singletonList(key));
```

→ Đảm bảo nếu DECR < 0, INCR rollback **trước khi** consumer khác chạy.

### 3.6. Consistency Redis ↔ Kafka ↔ DB

Sau khi reserve slot thành công, code phải:
1. Publish Kafka `flash-sale-order-requested` để order-service tạo đơn
2. Tăng `soldCount` trong DB để audit/reporting

Nếu publish Kafka fail → dự án gọi compensation script để hoàn lại Redis stock và xóa user khỏi buyer set.
Nếu Kafka ACK thành công nhưng tăng `soldCount` DB fail → có thể drift giữa DB và số đơn thực tế.

→ Cách xử lý trong dự án:
- `publishSync` đợi Kafka ACK trước khi trả success
- Compensation Redis khi publish fail
- `ReconciliationScheduler` (5 phút) so `soldCount` với số đơn thực tế và sửa drift
- Outbox là phương án production-grade có thể bổ sung nếu muốn đảm bảo mạnh hơn cho flash-sale-service

---

## 4. Distributed Lock Pattern (Khi Nào Cần?)

Atomic counter (Redis DECR) là **đủ** cho oversell problem. Distributed lock cần khi:
- Nhiều shared resource cần mutex (vd: chỉ 1 instance scheduler chạy)
- Operation phức tạp không thể compress thành 1 atomic command

### 4.1. Redis SET NX

```
SET key value NX EX ttl
  NX: chỉ set nếu key không tồn tại
  EX: TTL (giây) — quan trọng để release lock nếu owner crash

→ trả OK = lock acquired
→ trả nil = lock đang bị giữ
```

### 4.2. Vấn đề và Redlock

- Single Redis: Redis fail → lock state lost. Acceptable cho non-critical.
- Multi Redis Redlock (Salvatore Sanfilippo): Set lock trên 5 nodes, cần >50% acquire → consensus.
- Critique by Martin Kleppmann: clock skew, GC pause có thể violate. Khuyên dùng fencing token.

### 4.3. Trong dự án

- CampaignScheduler dùng `SET NX EX 30s` ở key `scheduler-lock:campaign`
- Single Redis instance → chấp nhận risk vô cùng nhỏ trong demo

---

## 5. Kiến Trúc Tổng Thể Flash Sale

```
┌──────────────────────────────────────────────────────────────┐
│                      flash-sale-service                       │
│  ┌─────────────────┐  ┌──────────────────────┐                │
│  │ REST Controller │  │ CampaignScheduler 5s │                │
│  │ POST /purchase  │  │ Activate / End        │                │
│  └────────┬────────┘  │ Recover Redis         │                │
│           │            │ Distributed lock      │                │
│           ▼            └──────────────────────┘                │
│  ┌─────────────────────────┐                                   │
│  │ Redis (atomic counter)   │  ◄─── Reconciliation scheduler   │
│  │  flash_sale:stock:{id}   │       (5 min)                    │
│  └────────┬────────────────┘                                   │
│           │                                                    │
│           ▼                                                    │
│  ┌──────────────────┐    ┌───────────────────────────────┐    │
│  │ DB: soldCount++  │    │ Kafka: flash-sale-order-       │    │
│  │ audit/reporting  │    │ requested                     │    │
│  └──────────────────┘    └───────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
                                          │
                                          ▼
                                  ┌────────────┐
                                  │ order-svc  │ → tạo đơn
                                  └────────────┘
```

---

## 6. Đo Lường & Tối Ưu

### 6.1. Kết quả đã đo trong Phase 13

Môi trường: AWS EC2 chạy Docker Compose production stack, k6 chạy từ laptop, ngày 31/05/2026.

| Metric | Expected | Observed |
|---|---:|---:|
| VU profile | 0 -> 500 -> 0 | max 500 VU |
| Duration | 90s | 1m30s |
| HTTP requests | - | 78,448 |
| Successful purchases | 100 exactly | 100 |
| Sold-out responses | > 0 | 36,134 |
| Campaign sold_count | 100 | 100 |
| Confirmed flash-sale orders | 100 | 100 |
| Duplicate buyer rows | 0 | 0 |
| p95 latency | - | 1.04s overall, 335.55ms expected responses |

Nguồn: `.test/results/flash-sale-spike-20260531-114752.{json,txt}`, `.test/results/SUMMARY.md`.

### 6.2. Cách diễn giải đúng trong báo cáo

Có thể kết luận:
- Trong kịch bản 500 VU tranh mua 100 slot, hệ thống không oversell theo nguồn kiểm chứng durable: campaign `sold_count = 100`, số đơn flash-sale confirmed = 100, duplicate buyer = 0.
- Redis Lua atomic reservation và buyer set hoạt động đúng trong kịch bản thực nghiệm đã chạy.
- Sold-out response lớn là hành vi kỳ vọng vì số request cao hơn nhiều so với 100 slot.

Không nên kết luận:
- "Không bao giờ oversell trong mọi điều kiện production" vì bài test chỉ đại diện cho cấu hình, dataset và thời gian đã chạy.
- "Kafka/outbox replay đã pass khi Kafka down" vì scenario đó chưa verify được trong Phase 13.

### 6.3. Tools test
- **Apache Bench (ab)**: đơn giản, ít control concurrency
- **Apache JMeter**: GUI, chi tiết
- **k6** (Grafana): script JS, chuyên load test
- **Gatling**: Scala, mạnh

### 6.4. Bottleneck thường gặp
- Tomcat thread pool full → tăng `server.tomcat.threads.max`
- DB connection pool full → tăng `spring.datasource.hikari.maximum-pool-size`
- Kafka producer batch quá lớn → tune `linger.ms`, `batch.size`

---

## 7. Từ Khóa Nghiên Cứu

```
- race condition flash sale oversell
- redis atomic decrement INCR DECR
- redis lua script atomic transaction
- pessimistic lock vs optimistic lock vs atomic counter
- distributed lock redlock antirez
- martin kleppmann distributed locking critique
- inventory reservation pattern eventually consistent
- single thread event loop redis
- thundering herd cache stampede
- backpressure rate limit flash sale
```

---

## 8. Câu Hỏi Phản Biện

**Q1: Vì sao không dùng database lock?**
→ DB lock không scale ở 10K RPS — DB CPU sẽ saturate. Redis in-memory single-threaded, atomic ops, đạt 100K+ ops/sec dễ dàng.

**Q2: Nếu Redis chết thì sao?**
→ CampaignScheduler có recovery: Redis key bị mất → quản lý phục hồi từ DB (`quantity - soldCount`). Trong giai đoạn Redis down, purchase API trả error.

**Q3: Em đảm bảo soldCount DB và actual orders đồng bộ thế nào?**
→ Dự án dùng Kafka publish đồng bộ, compensation Redis khi publish fail, và ReconciliationScheduler 5 phút để sửa drift giữa `soldCount` và actual orders. Outbox cho flash-sale-service là hướng nâng cấp production-grade; order-service/payment-service đã dùng outbox cho saga-critical events.

**Q4: Lua script vs DECR + INCR rollback, khác gì?**
→ Lua script gộp check duplicate, check stock và decrement thành một thao tác atomic. Bản DECR + INCR thủ công có khoảng giữa state âm/rollback; Lua script tránh được khoảng này.

**Q5: Em có dùng locking khi dùng Redis counter không?**
→ Không cần — Redis DECR đã atomic. Lock chỉ cần khi operation phức tạp không express được trong 1 command.

**Q6: Bot flood thì sao?**
→ Gateway rate limit 3 req/s/userId. Cấp độ deeper: CAPTCHA, behavioral analysis. Đồ án chỉ rate limit + reCAPTCHA tùy chọn.

**Q7: Sau campaign kết thúc, dữ liệu Redis xử lý thế nào?**
→ Redis key có TTL = (endTime - NOW). Tự expire khi campaign hết. CampaignScheduler cũng update status ENDED.

**Q8: Có thể dùng Kafka stream thay Redis không?**
→ Về lý thuyết có thể (Kafka ksqlDB hoặc compact topic), nhưng latency cao hơn (ms vs μs). Redis là lựa chọn cổ điển cho counter.

**Q9: Có thể oversell trong reconciliation không?**
→ Reconciliation **chỉ release** orphan slot (soldCount > actualOrders sau grace 30s). Nếu actualOrders > soldCount, bump soldCount lên — không tạo thêm đơn. An toàn.

---

## 9. Tài Liệu Tham Khảo

### Sách
- Martin Kleppmann, *Designing Data-Intensive Applications*, Chapter 7 — Transactions
- Salvatore Sanfilippo, *Redis in Action*

### Bài viết
- antirez.com — "Distributed locks with Redis" (Redlock algorithm)
- Martin Kleppmann — "How to do distributed locking" (critique)
- Redis Labs — "Atomic operations in Redis"
- Alibaba Engineering — "How Tmall handles Singles Day flash sale"
- Shopify — "How we built the flash sale system"

### Tools
- redis.io/commands — DECR, INCR, EVAL (Lua)
- "Redis Lua scripting" — redis.io documentation
