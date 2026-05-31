# Kiem thu hieu nang va resilience

Tai lieu nay la nguon chi tiet cho Chuong 6. Nguyen tac: **khong tu tao so lieu**. Moi chi so ben duoi deu tro ve artifact trong `.test/results/`.

## 1. Moi truong test

| Thanh phan | Gia tri |
|---|---|
| He thong duoc test | AWS EC2 chay Docker Compose production stack |
| Entry point | `http://13.213.118.96:8080` |
| Load generator | Laptop chay k6 |
| Dataset | 100 products, 500 users, flash sale 100 stock |
| Thoi gian | 2026-05-30 den 2026-05-31 |

## 2. Chuan bi du lieu

| Du lieu | Ket qua | Bang chung |
|---|---|---|
| Products | 100 products, co stock-in inventory | `.test/results/13-seed-20260530-212704.txt` |
| Users/tokens | 500 users/tokens moi cho spike | `.test/results/13-seed-users-20260531-114142.txt` |
| Flash sale | Campaign `8b602d5a-5d97-4187-bdb0-4bf82249fde8`, stock 100 | `.test/results/13-seed-flash-sale-20260531-114627.txt` |

## 3. Performance results

| Scenario | Profile | Ket qua chinh | Bang chung |
|---|---|---|---|
| Catalog soak | 34 phut, toi da 200 VU | p95 61.68ms, p99 85.48ms, error 0.00%, 55,239 iterations | `catalog-soak-20260530-215904.{json,txt}` |
| Checkout stress | 7 phut, toi da 50 VU | order_created 100%, error 0.00%, p95 160.92ms, 6,817 orders | `checkout-stress-20260531-022416.{json,txt}` |
| Flash-sale spike | 90s, toi da 500 VU | exactly 100 purchase success, 36,134 sold-out responses, no duplicate buyer | `flash-sale-spike-20260531-114752.{json,txt}` |

## 4. Flash-sale verification

Flash-sale invariant can dua vao thesis:

| Check | Expected | Observed |
|---|---:|---:|
| k6 purchase success counter | 100 | 100 |
| Campaign `sold_count` | 100 | 100 |
| `order_db.orders` confirmed rows for campaign | 100 | 100 |
| Duplicate buyer rows | 0 | 0 |

Redis stock key was already expired by the time verification was collected because the campaign had ended. DB campaign/order verification is therefore the durable source for the final result.

## 5. Resilience results

| Scenario | Ket qua | Bang chung |
|---|---|---|
| Kill order-service | During forced 30s downtime, checkout k6 crossed thresholds as expected: order_created 47.15%, HTTP failure 25.94%. Gateway/order health returned 200 after recovery. | `chaos-order-kill-20260531-115007.*` |
| Kill Kafka | Kafka stop/start completed, but the order probe during outage returned empty response; outbox replay was not verified. Do not present this as a pass. | `chaos-kafka-action-20260531-115518.txt` |
| Kill Redis | Product list stayed 200 during Redis outage and after recovery. Cart probe was inconclusive because the token used had expired and returned 401. | `chaos-redis-*-20260531-120329.txt` |
| Inventory failure compensation | Forced SKU `LT-20260530212705-96` to quantity 0, created order `4170422f-fc2d-49f8-aa5f-c7cd1d79266a`, final status `CANCELLED` with insufficient-stock reason. | `chaos-inventory-*-20260531-120641.*` |

## 6. Code/runtime fixes made before final runs

| Area | Fix |
|---|---|
| Product cache | Replaced Redis JDK serializer with JSON serializer and removed Spring `PageImpl` list caching because it caused product-list 500s. |
| Checkout script | Uses real user credentials, token refresh, and order payload compatible with backend DTO. |
| Flash-sale script | Uses pre-seeded JWT tokens and custom k6 counters for exact threshold checks. |
| Seed scripts | Product seed performs inventory stock-in; flash-sale seed uses live product and correct campaign DTO. |
| Chaos scripts | SSH/EC2 config is env-driven; inventory script now uses `ecommerce-postgres`. |

## 7. Con lai truoc khi copy vao thesis

- Export Grafana panels: request latency, JVM heap/GC, Kafka lag, Redis health, circuit breaker state.
- Capture Zipkin traces: checkout happy path and inventory compensation path.
- Re-run Kafka outbox replay with explicit HTTP status capture and DB `outbox_events` verification.
- Re-run Redis cart degradation with a freshly logged-in token if that claim is needed.
