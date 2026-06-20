# Kich Ban Nang Cap Kiem Thu Hieu Nang Va Scale Thu Cong

> File nay la **ban thiet ke kich ban + danh sach viec can lam**, chua phai script chay duoc.
> Muc tieu: nang cap Muc 5.9 (Ket qua kiem thu hieu nang) va Muc 5.10 (Ket qua kiem thu kha nang
> chiu loi) trong bao cao `DATN_.../Chuong/5_Giai_phap_dong_gop.tex`, bang cach (1) day tai k6 len
> 1500-2000 VU thay vi 50-500 VU, va (2) bo sung mot luong demo **scale thu cong** co kiem chung:
> nhin Grafana thay service nong -> chay `scale-up.sh` -> tai phan bo lai -> `scale-down.sh` ve 1 container.
>
> Gia tri that (IP, key, credential) doc tu `aws/config.env`, `/opt/ecommerce/.env.prod`. Khong ghi
> secret vao file nay. File nay an toan de push neu chi giu placeholder.

---

## 0. Tai Sao Lam Phan Nay

So lieu hien tai trong bao cao (Muc 5.9) con khiem ton so voi EC2 16 GB RAM:

| Kich ban hien co | Max VU | Nguon artifact |
|---|---|---|
| Catalog soak | 200 | `.test/results/catalog-soak-*.txt` |
| Checkout stress | 50 | `.test/results/13-checkout-stress-results.txt` |
| Flash-sale spike | 500 | `.test/results/flash-sale-spike-*.txt` |

Hoi dong se hoi 2 cau:
1. **He thong chiu duoc bao nhieu?** -> can mot bai ramp day den diem bao hoa, khong chi 200 VU.
2. **Microservice thi scale the nao?** -> can mot demo *scale that*, chung minh kien truc service-registry
   + client-side load balancing cho phep them instance ma khong sua code, va tai duoc phan bo lai.

Ket qua mong muon dua vao bao cao:
- Bang 5.9 cap nhat 3 dong: Catalog **baseline 300 VU** (so sach), Checkout **150 VU**, Flash-sale
  **1500 VU**. Rieng `Catalog ramp 0->1500 VU` (bao hoa) la bang/anh trong Muc 5.10.
- Mot tieu muc moi **"Kiem chung scale thu cong"** trong Muc 5.10 (hoac cuoi 5.9), kem so lieu
  truoc/sau khi scale (p95 giam, error rate giam, request/s tang) + anh Grafana.
- **Mot file HTML ket qua k6** trinh bay dep, tu sinh tu raw summary (Muc 3B). Dung lam: (a) nguon
  de chup `Hinhve/chuong5/k6-results.png` cho figure `fig:perf-results`, va (b) artifact ban mem dinh kem.
- **Anh bang chung kha nang chiu loi** cho figure `fig:resilience-evidence`
  (`Hinhve/chuong5/resilience-evidence.png`): outbox replay khi Kafka phuc hoi + suy giam/phuc hoi Redis (Muc 3C).

---

## 1. Co Che Scale Hoat Dong The Nao (Phai Hieu Truoc Khi Viet Script)

He thong da co san nen tang de scale ngang **khong can sua code**:

```
k6 (laptop)
  -> http://13.213.118.96:8080  (api-gateway)
  -> Spring Cloud Gateway route lb://PRODUCT-SERVICE
  -> Spring Cloud LoadBalancer doc danh sach instance tu Eureka
  -> chia request cho product-service #1, #2, #3 ...
```

Khi mot service co **nhieu instance cung dang ky vao Eureka duoi cung mot ten app**, Gateway va cac
OpenFeign client tu dong round-robin giua cac instance. Day chinh la diem demo: chi can `docker compose
up --scale product-service=3`, Eureka thay 3 instance UP, tai tu phan bo. Khong build lai, khong sua route.

### 1.1 Hai rao can ky thuat phai xu ly (quan trong)

Cau hinh hien tai **chua scale duoc ngay** vi 2 ly do, deu nam trong `docker-compose.yml`:

1. **`container_name` co dinh.** Vd `container_name: ecommerce-order-service`. Docker khong tao duoc
   2 container trung ten -> `--scale order-service=3` se loi. **Phai bo `container_name`** cho cac
   service muon scale (lam trong file override rieng, xem Muc 3.2).

2. **`EUREKA_INSTANCE_HOSTNAME` co dinh** = ten service (vd `order-service`). Neu 3 replica cung
   hostname + cung port (8086) thi **instance-id trong Eureka trung nhau** -> Eureka chi giu 1 dong,
   3 replica ghi de len nhau -> Gateway van chi thay 1 instance -> **scale gia, tai khong phan bo**.
   Phai lam instance-id duy nhat moi replica. Cach an toan nhat (khong dung ten/port):
   - Bat `eureka.instance.prefer-ip-address=true` (moi container co IP rieng trong docker network), va
   - Dat `eureka.instance.instance-id=${spring.application.name}:${random.uuid}` (Spring resolve trong
     tung JVM nen moi replica mot id).

   Hai gia tri nay co the truyen qua bien moi truong (Spring relaxed binding):
   `EUREKA_INSTANCE_PREFER_IP_ADDRESS=true` va
   `EUREKA_INSTANCE_INSTANCE_ID=...` (luu y escape `$` thanh `$$` trong YAML compose, xem Muc 3.2).

> Vi `expose` (cac business service chi `expose` port nhu `- "8086"`, khong map host port `8086:8086`),
> nhieu replica **khong tranh host port** -> day la diem thuan loi, khong can xu ly gi them ve port.

### 1.2 Service nao nen scale, service nao khong

| Nhom | Service | Scale duoc? | Ghi chu |
|---|---|---|---|
| Read path (uu tien demo) | `product-service`, `search-service`, `review-service`, `content-service` | Co | Stateless, doc DB/ES/Redis cache. Day la noi VU cao se don nong dau tien. |
| Gateway | `api-gateway` | Co (nang) | La diem vao, CPU cao khi VU lon. Co the scale nhung host port 8080 chi map 1 -> can xu ly rieng, **khong khuyen nghi cho demo dau tien**. |
| Saga | `order-service`, `inventory-service`, `payment-service` | Co | Stateless ve scale. OutboxPoller dung `FOR UPDATE SKIP LOCKED` nen nhieu instance an toan. Cac consumer cung `group-id` -> Kafka tu chia partition. Tang DB pool neu can (xem Muc 5). |
| Stateful / ha tang | `postgres`, `redis`, `kafka`, `elasticsearch`, `keycloak`, `discovery-server`, `config-server` | **Khong** | Single-node trong pham vi DATN. Khong scale, neu khong se hong dl/registry. |
| Flash-sale | `flash-sale-service` | Han che | Co the scale nhung CampaignScheduler dung Redis lock 1 instance kich hoat -> van OK, nhung khong phai trong tam demo scale. |

**Khuyen nghi cho demo:** chon `product-service` (va/hoac `search-service`) lam nhan vat chinh de scale,
vi bai ramp VU cao danh chu yeu vao read path -> de thay ro tren Grafana truoc/sau.

### 1.3 Ngan sach RAM tren EC2 16 GB

Tong `mem_limit` cua stack hien tai (uoc luong tu 2 file compose) ~ **11.5-12 GB**. Con trong
~4 GB. Moi instance Spring them ton ~384-512 MB. => **Co the them toi da khoang 6-8 instance**.
An toan: scale 1-2 service len 3 replica (them ~4-6 instance). **Khong scale dong loat tat ca** ->
se cham mem va EC2 swap, lam so lieu xau di. Day cung la mot diem trung thuc nen ghi vao bao cao:
scale ngang co gioi han boi single-host 16 GB.

---

## 2. Bo So Lieu Chot + Runbook Chi Tiet 3 Kich Ban

### 2.0 Co so chon so (tu chinh ket qua da do)

Bai catalog ramp 0->2000 da chay cho thay: **throughput cham tran ~277 req/s**, p95 9.15s, p99 25s,
loi 5% o dinh -> **diem nghen (knee) ~300 VU** cho read path, nut that la `product-service` (list/detail).
Tat ca so duoi day chon quanh moc nay. Nguyen tac chung:
- Tro thang `http://13.213.118.96:8080` (bo qua TLS Caddy de p95 phan anh he thong).
- Rate limiter la **theo user** (`X-User-Id`): `/api/orders` 10 r/s, flash-sale 3 r/s **moi user**. Vi moi
  VU dung 1 user rieng nen tai phan tan **khong** dinh 429 o checkout -> co the day VU that su.

| # | Kich ban | Script | VU chot | Thoi luong | Vai tro trong bao cao |
|---|---|---|---|---|---|
| 1 | Catalog baseline | `catalog-browse.js` | **300** ben | ~7 phut | So sach cho Bang 5.9 (vung khoe) |
| 2 | Catalog ramp + SCALE | `catalog-ramp.js` / `catalog-browse.js` | **0->1500** ramp, **~800** ben khi scale | ~18 phut / ~12 phut | Muc 5.10: bao hoa + scale ngang |
| 3 | Checkout stress | `checkout-stress.js` | **150** ben | ~8 phut | Bang 5.9 (duong ghi/Saga) |
| 4 | Flash-sale spike | `flash-sale-spike.js` | **1500** / 100 slot | ~90 giay | Bang 5.9 (tinh dung dan duoi spike) |

> Tat ca default trong script DA set dung cac so tren, nen chay chi can `BASE_URL`. Cac lenh duoi co the
> copy-chay truc tiep. Dat truoc: `BASE="http://13.213.118.96:8080"`.

---

### 2.1 Kich ban 1 — Catalog baseline 300 VU (so sach)

**Muc dich:** chung minh read path phuc vu tot o tai thuc te (duoi knee). Day la dong "Catalog" trong Bang 5.9.

```bash
BASE="http://13.213.118.96:8080"
# Smoke 1 phut truoc (tuy chon)
BASE_URL=$BASE CATALOG_TARGET_VUS=20 CATALOG_HOLD=30s CATALOG_RAMP_UP=10s CATALOG_RAMP_DOWN=10s \
  k6 run .test/load/catalog-browse.js

# Chay that: 300 VU ben 5 phut (default da la 300/5m)
BASE_URL=$BASE \
  k6 run --out json=.test/results/catalog-baseline-$(date +%Y%m%d-%H%M).json \
  .test/load/catalog-browse.js | tee .test/results/catalog-baseline-$(date +%Y%m%d-%H%M).txt
```

- **Nguong (trong script):** `http_req_duration p95<800ms, p99<2000ms`, `http_req_failed rate<0.01`.
- **Ky vong:** p95 ~vai tram ms, loi ~0% -> verdict PASS. (300 VU tren think-time 5s = ~230 req/s < tran 277.)
- **Grafana:** Spring Boot Overview — throughput on dinh, p95 thap, error rate 0; JVM heap khong tang vot.
- **Scale:** **KHONG can** — day la vung khoe, muc dich la so dep.
- Neu p95 lai cao bat thuong (>1s): kiem tra Redis cache product (cache miss?) hoac mang laptop.

---

### 2.2 Kich ban 2 — Catalog ramp 0->1500 + DEMO SCALE (Muc 5.10)

Gom 2 phan: (A) ramp tim diem gay, (B) giu tai bao hoa du lau de scale thu cong.

**Phan A — Ramp 0->1500 (bang chung bao hoa):** bac thang 150/450/900/1500 VU, threshold noi long.

```bash
BASE_URL=$BASE \
  k6 run --out json=.test/results/catalog-ramp-$(date +%Y%m%d-%H%M).json \
  .test/load/catalog-ramp.js | tee .test/results/catalog-ramp-$(date +%Y%m%d-%H%M).txt

# (Tuy chon) bieu do time-series cho duong cong bao hoa:
K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT=.test/results/catalog-ramp-dashboard.html \
  BASE_URL=$BASE k6 run .test/load/catalog-ramp.js
```

- **Ky vong:** p95 leo dan, tu ~bac 900-1500 VU bat dau gay (p95 vai giay), `product-service` nong nhat
  -> chinh la dong luc scale.

**Phan B — Giu tai bao hoa ~800 VU de SCALE (phan demo chinh):** ramp chi giu dinh 2 phut, qua ngan de
thao tac. Nen dung `catalog-browse.js` giu **800 VU lien tuc ~12 phut** (800 VU x think 5s = ~615 req/s,
gap doi tran 277 -> bao hoa on dinh, du thoi gian scale + chup truoc/sau).

```bash
# 1) Mo san terminal nay (giu chay suot demo):
BASE_URL=$BASE CATALOG_TARGET_VUS=800 CATALOG_RAMP_VUS=400 \
  CATALOG_RAMP_UP=1m CATALOG_HOLD=12m CATALOG_RAMP_DOWN=1m \
  k6 run .test/load/catalog-browse.js | tee .test/results/catalog-scale-demo-$(date +%Y%m%d-%H%M).txt
```

Trinh tu thao tac trong khi terminal tren dang chay:

| Phut | Lam gi | Quan sat |
|---|---|---|
| 0-3 | Cho tai len dinh 800 VU | Xac dinh service NONG THAT bang `docker stats` + Grafana (xem o duoi); throughput phang ~277 req/s |
| ~3 | **Chup "truoc scale"**: Grafana p95 + Eureka (1 instance cua service X) | p95 cao (vai giay), 1 instance |
| ~4 | TREN EC2: `bash aws/scale-up.sh <X> 3` (X = service nong that) | ~5-10s recreate |
| 4-6 | Cho 3 instance UP tren Eureka | Eureka thay 3 dong cua X |
| ~7 | **Chup "sau scale"**: Grafana p95 (theo instance) | p95 giam ro, throughput tang, tai chia 3 |
| ~11 | `bash aws/scale-down.sh <X>` | Ve 1 container |
| 12 | k6 ket thuc | Luu .txt |

- **CACH CHON DUNG SERVICE DE SCALE (doc ky — rat de bi danh lua bang p95 Grafana):**
  Tuyet doi **khong chon theo p95 cao nhat mot cach may moc**. Co 2 bay:
  1. **api-gateway hau nhu luon co p95 cao nhat — KHONG phai tin hieu scale gateway.** Gateway la cua vao;
     p95 cua no = overhead proxy + vong di-ve xuong backend cham nhat. No chi bao "co gi do phia duoi cham",
     khong chi ra cai nao. **Bo gateway ra khi chon service backend de scale.**
  2. **Service co p95 cao nhung req/s ~0 la NHIEU/du am, khong phai nut that.** Bai catalog (browse thuan)
     CHI goi `product-service` / `search-service` / `review-service`. Da kiem chung trong code: product-service
     **khong goi dong bo** sang service nao. Vi vay neu thay `order-service`/`payment-service`/`inventory-service`
     p95 cao trong bai nay -> do la **du am tu lan chay checkout truoc** con ket trong cua so `rate[5m]` cua
     Grafana, hoac vai request le. **Bo qua moi service co req/s thap.**
  -> **Tin hieu quyet dinh la CPU, khong phai p95.** Tren EC2 chay `docker stats` trong luc giu 800 VU:
     container nao CPU% pegged cao nhat (thuong ~80-100%) **chinh la nut that** -> scale dung cai do.
     Doi chieu them panel Grafana **"req/s theo service"** (chi cac service req/s cao moi dang ke) +
     **"Latency p95 & p99 theo service"**. Ung vien thuc te chi nam trong product/search/review.
  Meo: cho tai chay >= 2 phut roi hay doc Grafana (de cua so `rate[5m]` xa het du am cu); hoac thu hep ve `[1m]`.

- **MUC SCALE (khi nao bam scale-up):** o tai dinh 800 VU, sau khi xac dinh service nong that **X** (qua
  `docker stats` CPU + req/s), bam khi thay **MOT** trong: (1) CPU container X > ~80%, (2) p95 cua X > ~2-3s,
  (3) throughput phang khong tang du VU van tang (da cham tran). Chay `bash aws/scale-up.sh <X> 3`.
- **So lieu ghi lai cho bao cao:** p95 + req/s + CPU/heap cua service X **truoc (1 instance)** va
  **sau (3 instance)** o cung 800 VU -> bang so sanh trong Muc 5.10.
- **Truong hop gateway moi la nut that thuc su** (CPU api-gateway pegged, backend con ranh): scale mot backend
  se KHONG cai thien p95 — day la **phat hien gia tri**, ghi trung thuc ("o read path 800 VU, nut that la
  api-gateway single-instance, khong phai backend"). Khi do co the (a) ket luan gioi han single-host 16 GB,
  hoac (b) demo scale gateway rieng (luu y host port 8080 chi map 1 -> can xu ly rieng, x-Muc 1.2).
- Neu sau scale X p95 **khong** giam: nut that da chuyen sang tang khac (gateway/Postgres/Redis) — cung ghi
  trung thuc; co the thu scale them service khac hoac dung lai o ket luan "gioi han single-host".

---

### 2.3 Kich ban 3 — Checkout stress 150 VU (duong ghi/Saga)

**Muc dich:** stress luong dat hang (cart -> order -> inventory -> Kafka -> payment -> notification).

> **BAT BUOC truoc khi chay — nap ton kho.** 150 VU x ~8 phut sinh hang chuc nghin don that, neu het hang
> giua chung thi don fail (loi nghiep vu, khong phai loi hieu nang) lam hong so lieu. Nap stock cao:
> ```bash
> # Vi du nap qua admin API hoac seed; bao dam moi SKU co >= 200 ton.
> ADMIN_TOKEN="<admin-jwt>" BASE_URL=$BASE .test/seed/seed-products.sh 100   # neu seed co set stock lon
> # hoac stock-in tung SKU qua /api/inventory/stock-in (xem guide 18 muc Admin Inventory).
> ```

```bash
BASE_URL=$BASE \
  k6 run --out json=.test/results/checkout-stress-$(date +%Y%m%d-%H%M).json \
  .test/load/checkout-stress.js | tee .test/results/checkout-stress-$(date +%Y%m%d-%H%M).txt
```

- **Nguong (trong script):** `http_req_failed rate<0.05`, `checks{type:order_created} rate>0.95`.
- **Ky vong:** p95 ~0.5-2s (nang hon catalog), order success ~100% **neu du stock**. 150 VU ~ sat knee cua
  duong ghi (~140 VU) nen day la stress that su, khong phai vung khoe.
- **Grafana:** Saga Overview — throughput/latency `order-service`, `inventory-service`, `payment-service`;
  do tre Kafka consumer; khong co spike error.
- **Scale (tuy chon):** neu `order-service` la nut that (p95 cao, CPU cao), co the demo scale no:
  mo comment `order-service` trong `docker-compose.scale.yml` roi `bash aws/scale-up.sh order-service 2`.
  Luu y: cac instance cung Kafka group-id -> tu chia partition; OutboxPoller SKIP LOCKED an toan; co the
  can tang `DB_MAX_POOL_SIZE`. Khong bat buoc cho demo — uu tien scale product-service o KB2 cho ro rang.

---

### 2.4 Kich ban 4 — Flash-sale spike 1500 VU / 100 slot (tinh dung dan)

**Muc dich:** chung minh duoi spike "1500 nguoi tranh 100 suat", Redis Lua atomic khong over-sell.

Chuan bi:

```bash
# 1) Seed campaign 100 slot, lay id
ADMIN_TOKEN="<admin-jwt>" BASE_URL=$BASE .test/seed/seed-flash-sale.sh 100
export FLASH_SALE_ID=$(cat .test/seed/flash_sale_id.txt)
# 2) Doi campaign toi gio start (script seed in ra thoi diem start; thuong cho ~60-70s)
```

```bash
BASE_URL=$BASE EXPECTED_SUCCESS=100 \
  k6 run --out json=.test/results/flash-sale-spike-$(date +%Y%m%d-%H%M).json \
  -e FLASH_SALE_ID=$FLASH_SALE_ID \
  .test/load/flash-sale-spike.js | tee .test/results/flash-sale-spike-$(date +%Y%m%d-%H%M).txt
```

- **Nguong (trong script):** `flash_sale_purchase_successes count==100`, `sold_out count>0`.
- **Ky vong:** **dung 100** mua thanh cong, phan con lai sold-out / `already purchased` / 429 (deu hop le,
  script da phan loai). p95 tong the co the cao (~1s+) do thundering herd — khong sao, **metric chinh la
  tinh dung dan**, khong phai latency.
- **Verify sau khi chay** (tren EC2, doi chieu khong over-sell):
  ```bash
  docker exec ecommerce-postgres psql -U postgres -d flash_sale_db -c \
    "SELECT COUNT(*) FROM flash_sale_orders WHERE flash_sale_id='${FLASH_SALE_ID}' AND status='SUCCESS';"   # = 100
  docker exec ecommerce-postgres psql -U postgres -d flash_sale_db -c \
    "SELECT user_id,COUNT(*) FROM flash_sale_orders WHERE flash_sale_id='${FLASH_SALE_ID}' GROUP BY user_id HAVING COUNT(*)>1;"  # 0 dong
  docker exec ecommerce-redis redis-cli GET "flash-sale:${FLASH_SALE_ID}:stock"   # = 0
  ```
- **Scale:** **KHONG** — day la test dung dan, khong phai throughput. Tang VU chi lam herd lon hon, ket qua
  dung van la 100.

---

### 2.5 Sinh HTML tong hop + cap nhat bao cao

Sau khi chay xong 3 kich ban bang 5.9 (catalog baseline, checkout, flash-sale) — moi cai da de lai
`*-summary-latest.json` qua `handleSummary`:

```bash
node .test/load/report/build-k6-report.mjs > .test/results/k6-report.html
# Mo browser -> chup full-page -> DATN_.../Hinhve/chuong5/k6-results.png
```

So can dua vao Bang `table:perf-results`: Catalog 300 VU (p95 sach), Checkout 150 VU, Flash-sale 1500 VU.
Catalog ramp + scale (KB2) la bang/anh rieng trong Muc 5.10.

---

## 3. Thiet Ke Luong Scale Thu Cong

### 3.1 Y tuong demo (the hien truoc hoi dong)

1. Khoi dong bai k6 ramp (Muc 2). Mo Grafana song song.
2. Khi VU len ~1200-2000, mo dashboard `Spring Boot Overview` + `JVM Overview`, chi cho thay co:
   - `product-service` (hoac service nong) co request rate cao nhat, p95 tang, CPU/heap cao.
3. Noi: "Toi se scale ngang service nay bang tay" -> chay `aws/scale-up.sh product-service 3`.
4. Doi ~30-60s cho 2 instance moi UP tren Eureka (mo `http://13.213.118.96:8761` chi 3 instance
   PRODUCT-SERVICE).
5. Quay lai Grafana: request tu chia cho 3 instance, p95 cua service giam, error/queue giam.
6. Ket thuc: chay `aws/scale-down.sh product-service` de ve 1 container nhu ban dau.

So lieu can ghi lai: p95 + req/s + heap cua service **truoc** (1 instance) va **sau** (3 instance) khi
VU giu o moc cao -> dua thanh bang so sanh trong bao cao.

### 3.2 File scale override (DA TAO): `docker-compose.scale.yml` + fallback

Override **thu 3** (xep sau `docker-compose.yml` va `docker-compose.prod.yml`), chi lam 2 viec cho cac
service muon scale: **bo `container_name`** (`!reset null`) va **lam instance-id duy nhat** moi replica.
Da tao 2 file ung voi 2 phuong an instance-id (xem kich ban test o Muc 3.4 de chon):

| File | Phuong an | instance-id | Y tuong |
|---|---|---|---|
| `docker-compose.scale.yml` | **1 (mac dinh)** | `<svc>:$${HOSTNAME}` | HOSTNAME = container id, DUY NHAT + ON DINH, bien OS chuan luon co |
| `docker-compose.scale-fallback.yml` | **2 (du phong)** | `<svc>:$${random.uuid}` | Spring sinh uuid moi JVM |

Ca hai file deu bat `EUREKA_INSTANCE_PREFER_IP_ADDRESS=true` (Eureka quang ba IP container, moi replica
mot IP). Luu y `$$`: de Docker Compose **khong** nuot bien; container nhan chuoi `${HOSTNAME}` /
`${random.uuid}` va chinh **Spring** moi la nguoi resolve.

Da kiem tai may: `docker compose ... config` parse OK ca 2 file, `container_name` bi bo, instance-id
render dung. **Diem chua chac chan duy nhat** la Spring co resolve placeholder tu env tren EC2 that khong
-> day chinh la phan can **chay kich ban test o Muc 3.4** truoc khi demo.

> Mac dinh `scale-up.sh`/`scale-down.sh` dung `docker-compose.scale.yml`. Doi phuong an chi can dat bien:
> `SCALE_FILE=docker-compose.scale-fallback.yml bash aws/scale-up.sh product-service 3`.

### 3.3 Script scale (DA TAO): `aws/scale-up.sh`, `aws/scale-down.sh`

Da tao va chmod +x. Tom tat hanh vi:

- `aws/scale-up.sh <svc> [N=3]`: `up -d --no-build --force-recreate --scale <svc>=N` voi bo 3 file compose
  (base + prod + `$SCALE_FILE`). Dung `--force-recreate` de ca N replica chay cung cau hinh scale
  (instance-id duy nhat) -> service co ~5-10s gian doan, con minh hoa duoc recovery tren Grafana.
  Nhan bien `SCALE_FILE` (mac dinh `docker-compose.scale.yml`) de doi phuong an.
- `aws/scale-down.sh <svc>`: `rm -sf <svc>` (stop+remove TAT CA replica, ke ca ten `<project>-<svc>-1/2/3`)
  roi `up -d` tu **bo compose khong co file scale** -> ve dung 1 container ten goc `ecommerce-<svc>`,
  khong de orphan. (Cach `rm -sf` deterministic hon `--scale=1` + recreate.)

```bash
bash aws/scale-up.sh product-service 3        # scale len 3
bash aws/scale-down.sh product-service        # ve 1, ten goc
SCALE_FILE=docker-compose.scale-fallback.yml bash aws/scale-up.sh product-service 3   # phuong an 2
```

---

### 3.4 Kich Ban Test instance-id Truoc Khi Demo (chay trc, KHONG trong luc demo)

Day la phan kiem chung **diem rui ro nhat**: Spring co resolve `${HOSTNAME}` / `${random.uuid}` tu env
de tao instance-id DUY NHAT moi replica khong. Neu khong, Eureka ghi de cac instance trung id -> chi
con 1 instance -> "scale gia", tai khong phan bo, demo mat y nghia.

#### 3.4.1 Script tu dong: `aws/verify-scale-eureka.sh` (DA TAO)

> **Truoc tien — dong bo file len EC2.** Phan chay TREN EC2 la 5 file: `docker-compose.scale.yml`,
> `docker-compose.scale-fallback.yml`, `aws/scale-up.sh`, `aws/scale-down.sh`, `aws/verify-scale-eureka.sh`.
> Chon MOT trong 2 cach:
>
> **Cach A — KHONG push, copy thang bang scp (don gian nhat):**
> ```bash
> source aws/config.env
> scp -i "$SSH_KEY_PATH" docker-compose.scale.yml docker-compose.scale-fallback.yml \
>   ubuntu@$ELASTIC_IP:/opt/ecommerce/
> scp -i "$SSH_KEY_PATH" aws/scale-up.sh aws/scale-down.sh aws/verify-scale-eureka.sh \
>   ubuntu@$ELASTIC_IP:/opt/ecommerce/aws/
> ssh -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP 'chmod +x /opt/ecommerce/aws/*.sh'
> ```
>
> **Cach B — da push len GitHub roi keo ve (start-stack.sh KHONG tu git pull):**
> ```bash
> ssh -i $SSH_KEY_PATH ubuntu@$ELASTIC_IP
> cd /opt/ecommerce && git fetch --all && git reset --hard origin/main && chmod +x aws/*.sh
> ```
> Luu y: neu chua push thi KHONG chay Cach B — `git reset --hard origin/main` se keo ve ban main cu
> (khong co 5 file scale), tham chi xoa mat ban da scp. Trigger workflow "Deploy to AWS EC2" cung la
> Cach B (can Security Group mo port 22 cho runner — xem note dau `deploy.yml`).
>
> **Luu y:** thu muc `.test/` bi `.gitignore` nen cac k6 script (`catalog-ramp.js`, `lib/summary.js`,
> `report/build-k6-report.mjs`) **khong len GitHub/EC2** — dieu nay DUNG vi k6 chay tu **laptop** (load
> generator), tro vao `http://13.213.118.96:8080`. Cac chaos script `.test/scripts/phase13-*.sh` cung
> chay tu laptop va tu SSH vao EC2. Push cac file scale KHONG khop path build-and-push nen **khong rebuild
> image** (dung mong doi: scale chi la override bien moi truong, khong can build lai service).

Script tu thu lan luot tung file scale: voi moi file -> scale len N -> doi Eureka -> dem **so instanceId
duy nhat** cua app. File dau tien dat `>=N` la "winner". Cuoi cung dua service ve 1 container.

```bash
# TREN EC2, trong /opt/ecommerce:
bash aws/verify-scale-eureka.sh                  # product-service, N=2, thu ca 2 phuong an
bash aws/verify-scale-eureka.sh search-service 3
SCALE_FILE=docker-compose.scale.yml bash aws/verify-scale-eureka.sh product-service 2   # chi thu 1 file
```

Script doc Eureka credential (`EUREKA_USER`/`EUREKA_PASSWORD`) tu `.env.prod`, goi
`http://.../eureka/apps/PRODUCT-SERVICE`, dem `<instanceId>` duy nhat (neu phien ban Eureka khong co tag
do thi fallback dem `<ipAddr>`). In **PASS/FAIL cho tung phuong an** va ket luan nen dung file scale nao.

#### 3.4.2 Doc ket qua va quyet dinh

| Truong hop | Y nghia | Lam gi |
|---|---|---|
| Phuong an 1 (`scale.yml`, HOSTNAME) PASS | Spring resolve `${HOSTNAME}` OK | Dung mac dinh, khong can sua gi |
| PA1 FAIL, PA2 (`scale-fallback.yml`, uuid) PASS | `${HOSTNAME}` khong resolve nhung `${random.uuid}` thi co | Demo voi `SCALE_FILE=docker-compose.scale-fallback.yml`, hoac doi mac dinh trong 2 script |
| CA HAI FAIL | Spring khong resolve placeholder tu env -> sang **Phuong an 3** | Xem 3.4.3 |

#### 3.4.3 Phuong an 3 (du phong cuoi) — khong dung placeholder

Neu ca HOSTNAME lan random.uuid deu khong resolve, **bo han** `EUREKA_INSTANCE_INSTANCE_ID`, chi giu
`EUREKA_INSTANCE_PREFER_IP_ADDRESS=true`. Khi do Spring Cloud dung instance-id **mac dinh**, vốn da gan
hostname/IP cua container (Docker cap rieng cho moi replica) nen thuong da duy nhat san. Sua nhanh: trong
file scale, xoa 2 dong `EUREKA_INSTANCE_INSTANCE_ID`, giu lai `container_name: !reset null` +
`EUREKA_INSTANCE_PREFER_IP_ADDRESS: "true"`, roi chay lai `verify-scale-eureka.sh`.

Neu Phuong an 3 van FAIL: kha nang base compose ep `EUREKA_INSTANCE_HOSTNAME=<svc>` lam default
instance-id trung. Khi do them vao file scale dong `EUREKA_INSTANCE_HOSTNAME: "$${HOSTNAME}"` (tra hostname
ve container id) — luc nay neu Spring resolve duoc thi tuong duong Phuong an 1; neu khong resolve duoc thi
xac nhan Spring tren image nay khong resolve placeholder tu env, can xu ly o tang config-service
(them `eureka.instance.instance-id: ${random.uuid}` vao `configs/<svc>.yml` va rebuild image).

#### 3.4.4 Kiem chung thu cong (neu khong dung script)

```bash
# Sau khi scale len 2-3 instance:
source <(grep -E '^EUREKA_(USER|PASSWORD)=' /opt/ecommerce/.env.prod)
curl -s "http://${EUREKA_USER}:${EUREKA_PASSWORD}@localhost:8761/eureka/apps/PRODUCT-SERVICE" \
  | grep -oE '<instanceId>[^<]*' | sort -u
# Phai thay N dong instanceId khac nhau. Hoac mo UI: http://<ec2-ip>:8761 -> muc PRODUCT-SERVICE co N dong UP.
```

---

## 3B. Tao File HTML Ket Qua k6 (de dua vao do an)

Muc tieu: tu raw output cua k6, sinh ra **mot file HTML trinh bay dep** (bang tong hop + bieu do +
verdict pass/fail) de (a) chup thanh `Hinhve/chuong5/k6-results.png` cho figure `fig:perf-results`,
va (b) nop kem ban mem. Co 2 lop, dung ca hai:

### 3B.1 HTML tung kich ban — qua `handleSummary` (DA TAO: `lib/summary.js`)

Lib dung chung `.test/load/lib/summary.js` cho **moi script k6** tu xuat HTML + JSON summary khi chay
xong (dung k6-reporter + k6-summary tu CDN, can internet). Ham `report(data, opts)` voi `opts` gom
`name/label/kind/maxVU/notes` — them khoi `meta` vao summary de builder co nhan/ngu canh.

Da gan `handleSummary` vao ca 4 script. Vi du trong `catalog-ramp.js`:

```js
import { report } from './lib/summary.js';
export function handleSummary(data) {
  return report(data, { name: 'catalog-ramp', label: 'Catalog ramp 0->1500 VU', kind: 'ramp', maxVU: 1500 });
}
```

Moi lan `k6 run` (tu thu muc goc repo) de lai trong `.test/results/`:
`<name>-report-<ts>.html` (xem tren browser), `<name>-summary-<ts>.json` (luu lich su) va
`<name>-summary-latest.json` (builder 3B.2 doc file nay).

> Cach thay the khong can lib ngoai (k6 >= 0.49): bat web dashboard tich hop va export HTML:
> `K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT=.test/results/catalog-ramp-dash.html k6 run ...`
> Cho bieu do time-series dep cua duong ramp. Co the dung song song voi handleSummary.

### 3B.2 HTML tong hop 4 kich ban — `build-k6-report.mjs` (file chinh dua vao do an)

Vi do an can **mot** file goi gon, viet mot script Node khong phu thuoc package
(`.test/load/report/build-k6-report.mjs`) doc tat ca `.test/results/*-summary-*.json` (ban moi nhat
moi loai) va render ra **mot** `k6-report.html` co:

- Header de tai: ten do an, ngay chay, moi truong (EC2 16 GB, base URL `:8080`, dataset 100 SP / 500 user).
- Mot bang tong hop moi kich ban: Max VU, duration, iterations, http_reqs, req/s, **p95 / p99**,
  error rate (kem thanh bar mau), va verdict PASS/FAIL doi chieu threshold.
- Khoi **phan ra theo loai request** (list/detail/search/reviews) cho Catalog ramp — tu tagged
  sub-metric `http_req_duration{name:...}` -> thay request nao nong (service nao can scale).
- Khoi **chi so nghiep vu** cho Flash-sale (purchase successes, sold-out responses).

> Luu y: builder tong hop p95/p99 tren TOAN bai chay (khong phai p95 theo tung bac VU). De thay
> **duong cong bao hoa theo thoi gian/VU**, dung HTML cua k6 web dashboard
> (`K6_WEB_DASHBOARD_EXPORT=...`) — chay song song khi run `catalog-ramp.js` (xem README).

Lenh sinh:

```bash
node .test/load/report/build-k6-report.mjs > .test/results/k6-report.html
# Mo browser, chup full-page -> luu thanh DATN_.../Hinhve/chuong5/k6-results.png
```

> Vi do an la PDF tu LaTeX, **khong nhung truc tiep HTML**. Quy trinh: mo `k6-report.html` tren browser
> -> chup (Cmd+Shift+4 hoac full-page screenshot tren DevTools) -> luu PNG vao `Hinhve/chuong5/` ->
> `\includegraphics`. Giu file HTML lam artifact ban mem nop kem.

### 3B.3 Tao 4 bo summary

De HTML tong hop co du 4 dong, chay (it nhat 1 lan, sau khi da co `handleSummary`):
`catalog-ramp.js` (moi), va re-chay `catalog-browse.js` / `checkout-stress.js` / `flash-sale-spike.js`
de cap nhat luon so lieu trong Bang `table:perf-results`. Neu khong muon re-chay het, co the giu so cu
cho 3 bai va chi them dong ramp moi — nhung khi do HTML tong hop chi co 1-2 dong moi.

---

## 3C. Lay Anh Bang Chung Kha Nang Chiu Loi (figure `fig:resilience-evidence`)

Hai chaos script da co san va da tao **artifact text** (chua phai anh):

| Kich ban | Script co san | Artifact text sinh ra |
|---|---|---|
| Kafka outbox replay | `.test/scripts/phase13-kafka-outbox-replay.sh` | `chaos-kafka-outbox-before-*.txt` (processed=`f`), `chaos-kafka-outbox-after-*.txt` (processed=`t`), `chaos-kafka-verify-*.txt` (order CONFIRMED) |
| Redis degradation | `.test/scripts/phase13-redis-degradation.sh` | `chaos-redis-probes-*.txt` (product=200 / cart=5xx khi down / cart=200 sau recover) |

Tu cac artifact nay, **lay anh** theo 2 lop (nen co ca hai trong figure ghep):

### 3C.1 Anh A — bang chung du lieu (terminal / SQL)

- **Kafka:** chay lai script roi chup man hinh terminal hien **2 bang outbox**: truoc (cot `processed`
  = `f`, tuc pending khi Kafka down) va sau (`processed` = `t`) + dong `orders.status = CONFIRMED`.
  Cach chup gon: mo 2 file canh nhau va chup:
  ```bash
  EC2_HOST=13.213.118.96 SSH_KEY="$HOME/.ssh/ecommerce-thesis-key-v2.pem" \
    .test/scripts/phase13-kafka-outbox-replay.sh
  # roi mo .test/results/chaos-kafka-outbox-before-*.txt va -after-*.txt de chup
  ```
- **Redis:** chup `chaos-redis-probes-*.txt` hien ro:
  `product_list_during_redis_down=200`, `cart_during_redis_down=5xx`, `cart_after_redis_recovery=200`.
  ```bash
  EC2_HOST=13.213.118.96 SSH_KEY="$HOME/.ssh/ecommerce-thesis-key-v2.pem" \
    .test/scripts/phase13-redis-degradation.sh
  ```

> Anh A la bang chung "cung" nhat (du lieu DB/HTTP code that), nen co trong figure.

### 3C.2 Anh B — bang chung truc quan tren Grafana (khuyen nghi, an tuong hon)

Chup dashboard trong **dung cua so thoi gian** chaos de thay duong cong tut roi phuc hoi:

1. Truoc khi chay chaos, sinh mot it traffic nen (vd chay `catalog-browse.js` muc thap, hoac click vai
   request) de bieu do co duong nen.
2. Chay chaos script (Kafka hoac Redis).
3. Vao Grafana `https://grafana.13-213-118-96.nip.io`, set time range = `Last 15 minutes` trum cua so chaos.
   - **Kafka:** mo `E-commerce Saga Overview` — nhin throughput/latency cua `order-service`/`inventory-service`
     dip khi Kafka stop va bat lai sau restart; hoac panel consumer lag tang roi ve 0 (neu co).
   - **Redis:** mo `Spring Boot Overview` — error rate cua `cart-service` nhay len trong outage roi ve 0;
     `product-service` error rate phang (van phuc vu duoc) -> the hien graceful degradation co chon loc.
4. Chup panel (kem truc thoi gian de thay moc su kien). Co the them Grafana **annotation** danh dau
   "Kafka down" / "Kafka up" cho ro.

### 3C.3 Ghep figure

Ghep Anh A (bang/text) + Anh B (Grafana) thanh **mot** `Hinhve/chuong5/resilience-evidence.png`
(2 panel doc hoac ngang). Caption hien tai trong `.tex` da phu hop:
"Bang chung resilience: Kafka outbox replay va Redis degradation/recovery".

> Luu y: chaos script **dung container that** tren EC2. Chi chay khi khong demo truc tiep cho hoi dong,
> va sau khi xong kiem tra lai stack Up (theo `18-demo-thu-cong-hoi-dong.md`). Kafka down ~30s, Redis down ~20s.

---

## 4. Runbook Demo Truoc Hoi Dong (tom tat)

| Buoc | Lam gi | Thay gi |
|---|---|---|
| 1 | EC2 dang chay full stack (theo `18-demo-thu-cong-hoi-dong.md` Muc 2) | Tat ca container Up |
| 2 | Mo Grafana + Eureka song song | Baseline: moi service 1 instance UP |
| 3 | Chay `catalog-ramp.js` | VU tang dan, request rate len Grafana |
| 4 | Khi VU ~1500-2000, chi service nong | p95/heap/CPU cua product-service tang |
| 5 | `bash aws/scale-up.sh product-service 3` | Eureka hien 3 PRODUCT-SERVICE UP |
| 6 | Quay lai Grafana sau ~1 phut | Tai chia 3, p95 giam |
| 7 | k6 ramp-down xong, doc summary | Ghi so p95/req trc-sau |
| 8 | `bash aws/scale-down.sh product-service` | Ve 1 container nhu cu |

---

## 5. Tinh Chinh Co The Can De So Lieu Dep Hon

- **DB pool**: `order-service.yml` dang `maximum-pool-size: 3` (`DB_MAX_POOL_SIZE`). Khi scale order-service
  3 instance, tong ket noi = 9, postgres mac dinh max 100 -> con OK. Read path (product) tang VU lon co the
  can nang pool product-service neu thay timeout. Tang qua bien `DB_MAX_POOL_SIZE` khong can build lai.
- **Grafana**: dam bao dashboard tach metric **theo instance** (label `instance`/`pod`), neu khong se
  khong thay ro tai chia ra 3. Can kiem tra panel co `by (instance)` khong; neu chua, them.
- **Prometheus scrape**: cac instance moi (IP rieng) co duoc Prometheus phat hien khong? Neu Prometheus
  scrape theo target tinh (container_name), instance scale moi co the **khong bi scrape**. Can kiem tra
  `prometheus/prometheus.yml` dung service discovery (dns/docker) hay static. Day la **rui ro can verify**:
  neu static thi Grafana se khong thay instance moi -> demo mat tac dung. Xem Muc 6 buoc 3.

---

## 6. Danh Sach Viec Can Lam (TODO)

Theo thu tu thuc hien. Nhom A = hieu nang + scale, Nhom B = HTML report, Nhom C = anh resilience, Nhom D = bao cao.

> **Trang thai code (cap nhat):** cac file code da duoc tao va kiem tra cu phap/parse tai may:
> `.test/load/catalog-ramp.js`, `.test/load/lib/summary.js`, `.test/load/report/build-k6-report.mjs`
> (da test render HTML voi summary mau), `docker-compose.scale.yml` (PA1, `${HOSTNAME}`) +
> `docker-compose.scale-fallback.yml` (PA2, `${random.uuid}`) — ca 2 da `docker compose config` OK
> (container_name bi bo, instance-id render dung), `aws/scale-up.sh`, `aws/scale-down.sh`,
> `aws/verify-scale-eureka.sh` (test tu dong instance-id), `handleSummary` da gan vao 3 script cu,
> `.test/load/README.md` da cap nhat.
> **Con lai la chay/verify tren EC2 + chup anh + cap nhat `.tex`** (cac muc co dau [ ] ben duoi).

**A. Hieu nang & scale thu cong**
1. [x] **Viet `.test/load/catalog-ramp.js`** — DA XONG: ramp bac thang 0->1500 (default PEAK_VUS=1500),
       `RAMP_PROFILE` smoke/full, threshold noi long, tag request theo list/detail/search/reviews.
2. [ ] **Verify instance-id duy nhat tren EC2** — file scale (PA1+PA2) + `verify-scale-eureka.sh` DA tao.
       Con: deploy len EC2 va chay **`bash aws/verify-scale-eureka.sh product-service 2`** -> script tu
       thu ca 2 phuong an va ket luan dung file nao. Neu ca 2 fail -> Phuong an 3 (xem Muc 3.4.3).
3. [ ] **Verify Prometheus thay instance moi** — sau khi scale, vao `http://13.213.118.96:9090/targets`
       xem co target product-service thu 2/3 khong. Neu khong -> sua `prometheus.yml` sang docker/dns SD.
4. [ ] **Verify Grafana tach theo instance** — chinh panel `by (instance)` neu can.
5. [~] **`aws/scale-up.sh` va `aws/scale-down.sh`** — DA viet (executable). scale-down dung `rm -sf` roi
       tao lai tu base de ve dung container_name goc, khong orphan. Con: test chuoi up/down tren EC2.
6. [ ] **Chay that bai ramp (1500) + scale demo (giu 800 VU)** — luu `.json` + `.txt` vao `.test/results/`,
       chup Grafana truoc/sau scale, luu vao `.test/results/`. (Chi tiet: Muc 2.2.)
7. [ ] **Do so lieu truoc/sau scale** — bang: p95, req/s, error rate, heap cua service nong @ VU cao,
       1 instance vs 3 instance.

**B. File HTML ket qua k6**
8. [x] **`.test/load/lib/summary.js`** — DA xong (`handleSummary` -> HTML + summary.json + summary-latest.json),
       da gan vao `catalog-ramp.js` va ca 3 script cu (catalog-soak/checkout-stress/flash-sale-spike).
9. [x] **`.test/load/report/build-k6-report.mjs`** — DA xong (Node, khong dep), da test render HTML voi
       summary mau: bang tong hop p95/p99 + verdict + error bar + phan ra endpoint + counter flash-sale.
10. [ ] **Sinh & chup HTML** (sau khi co data that) — `node .test/load/report/build-k6-report.mjs >
       .test/results/k6-report.html`, mo browser, chup full-page -> `DATN_.../Hinhve/chuong5/k6-results.png`.

**C. Anh bang chung resilience**
11. [ ] **Chay `phase13-kafka-outbox-replay.sh`** -> chup outbox before(`f`)/after(`t`) + order CONFIRMED (Anh A).
12. [ ] **Chay `phase13-redis-degradation.sh`** -> chup probe product=200 / cart 5xx->200 (Anh A).
13. [ ] **Chup Grafana cua so chaos** (Saga Overview cho Kafka, Spring Boot Overview cho Redis) (Anh B).
14. [ ] **Ghep Anh A + B -> `DATN_.../Hinhve/chuong5/resilience-evidence.png`**.

**D. Cap nhat tai lieu**
15. [ ] **Cap nhat bao cao** `5_Giai_phap_dong_gop.tex`:
       - Muc 5.9: cap nhat Bang `table:perf-results` (Catalog baseline 300, Checkout 150, Flash-sale 1500);
         thay figure `fig:perf-results` bang `k6-results.png` that.
       - Muc 5.10: them bang/anh `Catalog ramp 0->1500 VU` (bao hoa) + 1 doan phan tich diem gay.
       - Them tieu muc moi (cuoi 5.9 hoac dau 5.10) **"Kiem chung scale ngang thu cong"**: mo ta co che
         Eureka + client LB, bang so sanh truoc/sau scale, anh Grafana, va ghi ro **gioi han single-host
         16 GB** (chi scale duoc vai instance).
       - Cap nhat cau "load generator la may ca nhan ... 50-500 VU" cho khop so moi.
       - Thay figure `fig:resilience-evidence` bang `resilience-evidence.png` that.
16. [x] **Cap nhat `.test/load/README.md`** — DA xong: them dong `catalog-ramp.js`, muc HTML report,
       muc scale thu cong.
17. [ ] **(Tuy chon)** ghi mot muc ngan vao `18-demo-thu-cong-hoi-dong.md` tro toi file nay cho phan demo scale.

---

## 7. Rui Ro / Luu Y Trung Thuc (nen ghi ca vao bao cao)

- Scale ngang tren single-host bi chan boi RAM 16 GB: chi minh hoa duoc co che, khong phai HA that.
- Load generator don le (laptop) co the la nut that truoc EC2 -> neu vay ghi ro va/hoac chay k6 tren EC2.
- Rate limiter lam checkout khong the day VU rat cao (dung thiet ke) -> bai bao hoa danh read path.
- Stateful (postgres/redis/kafka/es) khong scale -> o VU rat cao co the chinh postgres/kafka moi la nut
  that, khong phai service Spring. Neu gap, do la phat hien dang gia tri de phan tich, khong phai loi.
