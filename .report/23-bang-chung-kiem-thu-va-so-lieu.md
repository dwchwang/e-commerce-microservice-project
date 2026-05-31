# 23. Bang Chung Kiem Thu Va So Lieu Duoc Phep Dua Vao Bao Cao

File nay la checklist chong "so lieu gia". Truoc khi copy bat ky metric nao vao Chuong 6, doi chieu voi cot **Artifact**.

## 1. Nguyen Tac Bang Chung

- Metric performance phai co raw `.json` k6 va terminal transcript `.txt`.
- Ket qua resilience phai co transcript hanh dong, probe truoc/sau va neu can co DB/Redis verification.
- Screenshot Grafana/Zipkin chi dua vao bao cao sau khi co file anh export.
- Scenario inconclusive khong duoc viet la pass.

## 2. Backend Readiness Smoke Test

| Hang muc | Ket qua | Artifact |
|---|---|---|
| Tong ket backend readiness | 9/9 split suites pass sau khi fix bug/test infra | `.test/backend-readiness-final.md` |
| Maven | PASS, 5 pass, 0 fail | `.test/results/01-maven-PASS.md` |
| Infra | PASS, 27 pass, 0 fail | `.test/results/02-infra-PASS.md` |
| Auth/user | PASS, 30 pass, 0 fail | `.test/results/03-auth-user-PASS-after-fix.md` |
| Catalog/search | PASS, 49 pass, 0 fail | `.test/results/04-catalog-search-PASS.md` |
| Cart/voucher | PASS, 57 pass, 0 fail | `.test/results/05-cart-voucher-PASS-after-fix.md` |
| Order COD/review | PASS, 67 pass, 0 fail | `.test/results/06-order-cod-review-PASS.md` |
| VNPAY | PASS, 55 pass, 0 fail | `.test/results/07-vnpay-PASS.md` |
| Flash-sale | PASS, 62 pass, 0 fail | `.test/results/08-flash-sale-PASS-after-fix.md` |
| Security | PASS, 76 pass, 0 fail, 1 warn | `.test/results/09-security-PASS-trimmed-memory.md` hoac `.test/backend-readiness-report.md` |

## 3. Code/Test Infra Fix Duoc Phep Neu Trong Bao Cao

| Van de | Ket luan | Artifact |
|---|---|---|
| Discovery health bi 401 | Real bug da fix: expose health/info cho healthcheck | `.test/backend-readiness-final.md` |
| Guest cart session id bi reject | Real bug da fix: gateway chap nhan opaque session id hop le | `.test/backend-readiness-final.md` |
| Split suite doi zipkin khong start | Test bug da fix trong smoke runner | `.test/backend-readiness-final.md`, `.test/issues.md` |
| Flash-sale scheduler OOM Metaspace | Test infra bug da fix: `MaxMetaspaceSize` 128m -> 256m | `.test/backend-readiness-final.md`, `.test/issues.md` |
| Product cache gay 500 khi load | Runtime fix: JSON serializer va bo cache `PageImpl` list | `.test/results/SUMMARY.md` |

## 4. Performance Metrics Duoc Phep Dung

### 4.1. Catalog Soak

| Metric | Gia tri | Artifact |
|---|---:|---|
| Duration | 34m03s wall-clock | `.test/results/catalog-soak-20260530-215904.txt` |
| Max VU | 200 | `.test/results/catalog-soak-20260530-215904.{json,txt}` |
| Iterations | 55,239 | `.test/results/catalog-soak-20260530-215904.txt` |
| HTTP requests | 220,956 | `.test/results/catalog-soak-20260530-215904.txt` |
| p95 latency | 61.68 ms | `.test/results/catalog-soak-20260530-215904.txt` |
| p99 latency | 85.48 ms | `.test/results/catalog-soak-20260530-215904.txt` |
| Error rate | 0.00% | `.test/results/catalog-soak-20260530-215904.txt` |

### 4.2. Checkout Stress

| Metric | Gia tri | Artifact |
|---|---:|---|
| Duration | 7m00s | `.test/results/checkout-stress-20260531-022416.txt` |
| Max VU | 50 | `.test/results/checkout-stress-20260531-022416.{json,txt}` |
| Iterations | 6,817 | `.test/results/checkout-stress-20260531-022416.txt` |
| HTTP requests | 13,734 | `.test/results/checkout-stress-20260531-022416.txt` |
| Order success check | 100.00% | `.test/results/checkout-stress-20260531-022416.txt` |
| HTTP failure rate | 0.00% | `.test/results/checkout-stress-20260531-022416.txt` |
| p95 latency | 160.92 ms | `.test/results/checkout-stress-20260531-022416.txt` |

### 4.3. Flash-Sale Spike

| Metric | Gia tri | Artifact |
|---|---:|---|
| Duration | 1m30s | `.test/results/flash-sale-spike-20260531-114752.txt` |
| Max VU | 500 | `.test/results/flash-sale-spike-20260531-114752.{json,txt}` |
| Iterations/HTTP requests | 78,448 | `.test/results/flash-sale-spike-20260531-114752.txt` |
| Successful purchases | 100/100 stock | `.test/results/flash-sale-spike-20260531-114752.txt` |
| Sold-out responses | 36,134 | `.test/results/flash-sale-spike-20260531-114752.txt` |
| Campaign sold_count | 100 | `.test/results/SUMMARY.md` |
| Confirmed flash-sale orders | 100 | `.test/results/SUMMARY.md` |
| Duplicate buyer rows | 0 | `.test/results/SUMMARY.md` |
| p95 latency | 1.04s overall, 335.55ms expected responses | `.test/results/SUMMARY.md` |

## 5. Resilience Metrics Duoc Phep Dung

| Scenario | Trang thai | Ket qua duoc phep viet | Artifact |
|---|---|---|---|
| Kill order-service | Completed | Trong 30s downtime, order_created 47.15%, HTTP failure 25.94%; sau restart gateway/order health 200 | `.test/results/chaos-order-kill-20260531-115007.*` |
| Kill Kafka | Attempted | Kafka stop/start completed, nhung outbox replay chua verify; khong danh dau pass | `.test/results/chaos-kafka-action-20260531-115518.txt` |
| Kill Redis | Partial | Product list 200 trong/after outage; cart probe inconclusive vi expired token 401 | `.test/results/chaos-redis-*-20260531-120329.txt` |
| Inventory failure compensation | Pass | Order `4170422f-fc2d-49f8-aa5f-c7cd1d79266a` -> `CANCELLED`, inventory `quantity=0`, `reserved_quantity=0` | `.test/results/chaos-inventory-*-20260531-120641.*` |

## 6. Artifact Con Thieu Truoc Khi Copy Vao Thesis

| Hang muc | Ly do can bo sung |
|---|---|
| Grafana request latency/JVM/Kafka/Redis/circuit breaker screenshots | Chuong 6 can hinh minh hoa metric, hien `.test/results/SUMMARY.md` ghi pending |
| Zipkin trace checkout happy path | Can minh hoa distributed tracing end-to-end |
| Zipkin trace inventory-failed compensation | Minh hoa trace cho failure path |
| Kafka outbox replay retest | Scenario hien tai chua verify row outbox pending/replay sau Kafka restart |
| Redis cart degradation retest | Probe cu dung token expired nen khong ket luan duoc |
| Admin FE screenshots | Neu admin panel duoc dua vao ket qua, can screenshot va workflow demo |

## 7. Doan Viet Mau Cho Chuong 6

> Cac bai kiem thu hieu nang duoc thuc hien tu ngay 30/05/2026 den 31/05/2026 tren moi truong AWS EC2 chay Docker Compose production stack. Load generator la laptop ca nhan chay k6, dataset gom 100 san pham, 500 user/token va mot flash-sale campaign co 100 slot. Nguyen tac danh gia la moi so lieu trong bao cao deu phai doi chieu duoc voi artifact raw trong `.test/results`.

> Ket qua flash-sale spike voi 500 virtual users trong 90 giay cho thay co dung 100 purchase thanh cong tren 100 slot, `sold_count` cua campaign bang 100, so don confirmed cho campaign bang 100 va khong co duplicate buyer. Dieu nay chung minh trong kich ban thuc nghiem da chay, co che Redis Lua atomic reservation ket hop buyer set va reconciliation khong gay oversell.

## 8. Doan Can Tranh

- Khong viet "Kafka outage pass" cho den khi co artifact verify outbox replay.
- Khong viet "Redis cart van hoat dong khi Redis down" vi probe hien tai bi 401 expired token.
- Khong viet "Grafana/Zipkin da capture" neu chua co file anh.
- Khong viet "admin panel hoan thien" neu moi co plan/chua co UI day du.
