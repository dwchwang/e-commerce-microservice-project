# Kiem thu hieu nang va resilience

Tai lieu nay la nguon chi tiet cho Chuong 6. Nguyen tac: **khong tu tao so lieu**. Moi chi so ben duoi deu tro ve artifact trong `.test/results/`.

## 1. Moi truong test

| Thanh phan | Gia tri |
|---|---|
| He thong duoc test | AWS EC2 chay Docker Compose production stack |
| Entry point | API Gateway trên EC2, đọc từ `aws/config.env` (`ELASTIC_IP`) |
| Load generator | Laptop chay k6 |
| Dataset | 100 products, 500 users, flash sale 100 stock |
| Thoi gian | 2026-05-30 den 2026-06-01 |

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
| Kill Kafka | Order `e460bab0-0856-4121-8f02-9e318623a1aa` was created while Kafka was stopped. `ORDER_CREATED` outbox row was captured as `processed=f`; after Kafka restarted, `ORDER_CREATED`, `PAYMENT_REQUESTED`, and `ORDER_CONFIRMED` were `processed=t`. Final order status was `CONFIRMED`; inventory recorded `RESERVE` and `STOCK_OUT`. | `chaos-kafka-*-20260601-231131.*` |
| Kill Redis | Guest cart returned 200 before outage. During Redis outage, product list stayed 200 while Redis-backed cart returned 500. After Redis restart, product list and cart both returned 200. | `chaos-redis-*-20260601-231236.*` |
| Inventory failure compensation | Forced SKU `LT-20260530212705-96` to quantity 0, created order `4170422f-fc2d-49f8-aa5f-c7cd1d79266a`, final status `CANCELLED` with insufficient-stock reason. | `chaos-inventory-*-20260531-120641.*` |

## 6. Observability evidence

| Evidence | Ket qua | Bang chung |
|---|---|---|
| Prometheus metrics | Captured `up`, request rate, p95 latency, and JVM heap from AWS Prometheus. Use this as the metric source if Grafana UI screenshot is unavailable. | `prometheus-phase13-metrics-20260601-234147.txt` |
| Zipkin trace | Captured recent Zipkin traces and one `order-service` outbox poller detail with `outcome=SUCCESS`. | `zipkin-trace-detail-20260601-234035.png`, `zipkin-order-service-traces-20260601-234021.json` |
| Grafana dashboard | Dashboard folder `E-commerce` contains `E-commerce Saga Overview`, `JVM Overview`, and `Spring Boot Overview`; use captured Grafana screenshots as thesis figures, with Prometheus transcript as raw metric evidence. | `grafana-*-20260601-*.png` |

## 7. Code/runtime fixes made before final runs

| Area | Fix |
|---|---|
| Product cache | Replaced Redis JDK serializer with JSON serializer and removed Spring `PageImpl` list caching because it caused product-list 500s. |
| Checkout script | Uses real user credentials, token refresh, and order payload compatible with backend DTO. |
| Flash-sale script | Uses pre-seeded JWT tokens and custom k6 counters for exact threshold checks. |
| Seed scripts | Product seed performs inventory stock-in; flash-sale seed uses live product and correct campaign DTO. |
| Chaos scripts | SSH/EC2 config is env-driven; inventory script now uses `ecommerce-postgres`. |
| Phase 13 scripts | Added reproducible Kafka replay, Redis degradation, and observability capture helpers under `.test/scripts/`. |

## 8. Con lai truoc khi copy vao thesis

- Choose final Grafana panels for thesis figures: `E-commerce Saga Overview`, `JVM Overview`, and `Spring Boot Overview`.
- Optional: capture more recent Zipkin checkout/inventory compensation traces if the EC2 stack is restarted because Zipkin storage is in-memory.
