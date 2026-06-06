# Huong Dan Final Chay Va Van Hanh Full E2E

> Runbook final sau 14 phase. Doc tu tren xuong neu ban muon tu minh bat EC2, chay full stack, vao storefront/admin, xem Grafana/Prometheus/Zipkin, kiem tra DB/Kafka/Redis/Elasticsearch, va demo tat ca luong quan trong.
>
> Nguyen tac bao mat cua file nay: khong ghi truc tiep IP, instance id, password, token, secret, hash, API key, hay gia tri that trong `.env`. Khi can xem gia tri, tai lieu se chi ro file/vi tri de ban tu mo tren may cua minh.

---

## 0. Cach Dung Tai Lieu Nay

Co 2 che do chay:

| Che do | Khi nao dung | File compose | Frontend | Observability |
|---|---|---|---|---|
| Production EC2 | Demo/bao ve/chay so lieu that | `docker-compose.yml` + `docker-compose.prod.yml` | Chay trong Docker, qua Caddy HTTPS | Grafana qua `3001`; Zipkin/ES khong public mac dinh |
| Local dev | Debug code tren laptop | `docker-compose.yml` | Chay `npm run dev` tren host | Grafana local chiem port `3000` |

Tai lieu nay uu tien **Production EC2**. Phan local nam cuoi file.

Quy uoc bien moi truong:

```bash
cd "<PROJECT_ROOT>"
source aws/config.env

export API_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"
export APP_URL="https://app.${ELASTIC_IP_DASHED}.nip.io"
export AUTH_URL="https://auth.${ELASTIC_IP_DASHED}.nip.io"
export GRAFANA_URL="https://grafana.${ELASTIC_IP_DASHED}.nip.io"
```

Khong copy gia tri that cua cac bien nay vao git. File `aws/config.env` la noi ban tu xem IP/instance/key khi can.

---

## 1. Kien Truc Tong Quan

He thong gom:

- 13 business services Spring Boot: identity, user, product, inventory, cart, voucher, order, payment, notification, review, search, content, flash-sale.
- 3 Spring Cloud infrastructure services: discovery-server, config-server, api-gateway.
- Runtime infrastructure: PostgreSQL, Redis, Kafka KRaft, Elasticsearch, Keycloak, Mailpit.
- Observability: Prometheus, Grafana, Zipkin, Actuator metrics/health.
- Frontend Next.js: storefront + admin panel trong cung app `frontend`.
- Deployment: mot EC2 instance, Docker Compose production override, Caddy reverse proxy HTTPS, images tu GHCR.

Luot request production:

```text
Browser
  |
  | HTTPS
  v
Caddy :80/:443
  |-- app.<ELASTIC_IP_DASHED>.nip.io      -> frontend container :3000
  |-- api.<ELASTIC_IP_DASHED>.nip.io      -> api-gateway :8080
  |-- auth.<ELASTIC_IP_DASHED>.nip.io     -> keycloak host port :8180
  |-- grafana.<ELASTIC_IP_DASHED>.nip.io  -> grafana host port :3001
  v
Docker network ecommerce-network
  |
  |-- API Gateway -> Eureka -> 13 business services
  |-- PostgreSQL / Redis / Kafka / Elasticsearch / Keycloak
  |-- Prometheus scrape /actuator/prometheus
  |-- Zipkin receive spans inside Docker network
```

Service map:

| Service | Port noi bo | Gateway route | Vai tro |
|---|---:|---|---|
| `discovery-server` | 8761 | none | Eureka registry |
| `config-server` | 8888 | none | Native config server |
| `api-gateway` | 8080 | entrypoint | JWT, route, CORS, rate limit |
| `identity-service` | 8081 | `/api/auth/**` | Register/login qua Keycloak, publish user event |
| `user-service` | 8082 | `/api/users/**` | Profile, address book, admin users |
| `product-service` | 8083 | `/api/products/**` | Product, category/brand/spec, product events |
| `inventory-service` | 8084 | `/api/inventory/**` | Stock-in/out, reserve, stock-out saga |
| `cart-service` | 8085 | `/api/cart/**` | Redis cart, guest cart, merge cart |
| `order-service` | 8086 | `/api/orders/**` | Checkout, order saga, outbox |
| `payment-service` | 8087 | `/api/payments/**` | COD, VNPAY return/IPN |
| `voucher-service` | 8088 | `/api/vouchers/**` | Voucher CRUD/apply |
| `notification-service` | 8089 | `/api/notifications/**` | Email notification |
| `review-service` | 8090 | `/api/reviews/**` | Review sau khi mua |
| `search-service` | 8091 | `/api/search/**` | Elasticsearch index/search |
| `content-service` | 8092 | `/api/content/**` | Banner, pages, posts |
| `flash-sale-service` | 8093 | `/api/flash-sales/**` | Campaign, Redis Lua purchase |

Kafka flow quan trong:

| Topic | Publisher | Consumer |
|---|---|---|
| `user-registered` | identity-service | user-service |
| `product-created`, `product-updated`, `product-deleted` | product-service | search-service |
| `order-created` | order-service outbox | inventory-service |
| `inventory-updated`, `inventory-failed` | inventory-service | order-service |
| `payment-requested` | order-service outbox | payment-service |
| `payment-success`, `payment-failed` | payment-service | order-service |
| `order-confirmed`, `order-cancelled` | order-service outbox | inventory-service, notification-service |
| `flash-sale-order-requested` | flash-sale-service | order-service |

---

## 2. Nhung Noi Duoc Phep Xem Config

Khong ghi gia tri that vao guide. Khi can, tu mo cac file/vi tri sau:

| Can xem | Xem o dau |
|---|---|
| EC2 IP, instance id, SSH key path | `aws/config.env` tren local |
| Production env cua stack | `/opt/ecommerce/.env.prod` tren EC2 |
| Local backend env | `.env` tren local |
| Frontend local env | `frontend/.env.local` |
| Caddy reverse proxy | `/etc/caddy/Caddyfile` tren EC2 hoac `aws/Caddyfile` trong repo |
| Caddy systemd env/basic auth | `sudo systemctl cat caddy` tren EC2 |
| GitHub deploy secrets | GitHub repo -> Settings -> Secrets and variables |

Lenh xem nhanh, khong in full secret neu khong can:

```bash
# Local
source aws/config.env
env | grep -E 'AWS_REGION|INSTANCE_ID|ELASTIC_IP|ELASTIC_IP_DASHED|SSH_KEY_PATH|GITHUB_OWNER'

# EC2
ssh -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP
cd /opt/ecommerce
grep -E '^(GITHUB_OWNER|IMAGE_TAG|ELASTIC_IP|ELASTIC_IP_DASHED|KC_HOSTNAME|NEXT_PUBLIC_API_BASE_URL|NEXT_PUBLIC_APP_URL)=' .env.prod
```

Neu can password/token/secret, tu mo `.env.prod` bang editor tren EC2. Khong paste len chat, khong commit.

---

## 3. Checklist Truoc Khi Bat Dau

Tren laptop:

```bash
cd "<PROJECT_ROOT>"
source aws/config.env

aws --version
aws sts get-caller-identity
test -f "$SSH_KEY_PATH" && echo "ssh key file exists"
ssh -o StrictHostKeyChecking=accept-new -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP 'echo ssh-ok'
```

Can co:

- AWS CLI da login.
- SSH key dung voi EC2.
- `jq`, `curl`.
- Neu chay k6: `k6`.
- Neu chay frontend local: Node.js 22+.

---

## 4. Bat EC2 Va Chay Full Production Stack

### 4.1 Kiem tra EC2 state

```bash
cd "<PROJECT_ROOT>"
source aws/config.env

aws ec2 describe-instances \
  --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].State.Name' \
  --output text
```

Neu la `stopped`:

```bash
aws ec2 start-instances --instance-ids "$INSTANCE_ID"
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
sleep 120
```

### 4.2 SSH vao EC2

```bash
ssh -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP
```

Tren EC2:

```bash
cd /opt/ecommerce
ls -la .env.prod docker-compose.yml docker-compose.prod.yml aws/start-stack.sh
```

Neu thieu `.env.prod`, dung `.env.prod.example` lam template va dien gia tri that tren EC2. Khong commit file nay.

### 4.3 Start stack

Tren EC2:

```bash
cd /opt/ecommerce
bash aws/start-stack.sh
```

Script se pull image tu GHCR va chay:

```bash
docker compose --env-file .env.prod \
  -f docker-compose.yml -f docker-compose.prod.yml \
  up -d --no-build
```

Neu muon chay tay:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d --no-build
```

### 4.4 Xem container

Tren EC2:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml ps
```

Can co cac container:

- `ecommerce-frontend`
- `ecommerce-api-gateway`
- `ecommerce-discovery-server`
- `ecommerce-config-server`
- `ecommerce-identity-service`
- `ecommerce-user-service`
- `ecommerce-product-service`
- `ecommerce-inventory-service`
- `ecommerce-cart-service`
- `ecommerce-voucher-service`
- `ecommerce-order-service`
- `ecommerce-payment-service`
- `ecommerce-notification-service`
- `ecommerce-review-service`
- `ecommerce-search-service`
- `ecommerce-content-service`
- `ecommerce-flash-sale-service`
- `ecommerce-postgres`
- `ecommerce-keycloak-db`
- `ecommerce-redis`
- `ecommerce-kafka`
- `ecommerce-elasticsearch`
- `ecommerce-keycloak`
- `ecommerce-prometheus`
- `ecommerce-grafana`
- `ecommerce-zipkin`

Neu service loi:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=150 api-gateway
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=150 order-service
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=150 keycloak
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=150 frontend
```

### 4.5 Health check tu laptop

Tren laptop:

```bash
cd "<PROJECT_ROOT>"
source aws/config.env
export API_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"

curl -s "$API_URL/actuator/health" | jq .
curl -s "$API_URL/actuator/health/readiness" | jq .
curl -s "$API_URL/api/products?size=1&page=0" | jq .
```

Neu HTTPS loi, test direct API Gateway qua debug port, neu Security Group cho phep:

```bash
curl -s "http://${ELASTIC_IP}:8080/actuator/health" | jq .
```

### 4.6 Kiem tra Eureka

Eureka co basic auth. Password nam trong `/opt/ecommerce/.env.prod`.

Tren EC2:

```bash
cd /opt/ecommerce
EUREKA_PASSWORD="$(grep '^EUREKA_PASSWORD=' .env.prod | cut -d= -f2-)"
curl -s -u "eureka:${EUREKA_PASSWORD}" http://localhost:8761/eureka/apps \
  | grep -o '<app>[^<]*' | sort -u
```

Can thay `API-GATEWAY` va 13 business services.

---

## 5. Truy Cap Cac Giao Dien

### 5.1 Storefront

Tren laptop:

```bash
source aws/config.env
echo "https://app.${ELASTIC_IP_DASHED}.nip.io"
```

Mo URL vua in ra. Kiem tra:

- Home page load duoc.
- `/products` co catalog.
- `/search?q=laptop` co search page.
- `/flash-sales` co danh sach flash sale neu co campaign.
- `/compare` hoat dong sau khi them san pham vao compare.
- `/content/<slug>` hoat dong neu admin da tao content page/post.

Neu frontend load nhung goi API loi:

```bash
# Laptop
curl -I "https://api.${ELASTIC_IP_DASHED}.nip.io/actuator/health"

# EC2
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=100 frontend
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=100 api-gateway
```

### 5.2 Admin Panel

URL:

```bash
source aws/config.env
echo "https://app.${ELASTIC_IP_DASHED}.nip.io/admin"
```

Admin can JWT co `ROLE_ADMIN`.

Cach tao/gán admin:

1. Tao user qua frontend register hoac API register.
2. Mo Keycloak URL:
   ```bash
   echo "https://auth.${ELASTIC_IP_DASHED}.nip.io"
   ```
3. Dang nhap Keycloak bang admin credentials trong `/opt/ecommerce/.env.prod`.
4. Chon realm `ecommerce`.
5. Users -> chon user -> Role mapping -> Assign role.
6. Chon realm role `ROLE_ADMIN` -> Assign.
7. Dang xuat/dang nhap lai storefront de JWT moi co role.

Module admin can kiem tra:

| Module | Route | Can test |
|---|---|---|
| Dashboard | `/admin/dashboard` | KPI, revenue chart, status counts, recent orders, low stock |
| Products | `/admin/products` | list, create, edit, delete, specs, image URL |
| Inventory | `/admin/inventory` | stock-in, stock-out, movement history |
| Orders | `/admin/orders` | list, filter, detail, update status |
| Users | `/admin/users` | list user, detail user, related orders/reviews |
| Vouchers | `/admin/vouchers` | create/edit/delete/deactivate, validate date/value |
| Flash sales | `/admin/flash-sales` | create scheduled campaign, cancel |
| Content banners | `/admin/content/banners` | create/update banner |
| Content pages | `/admin/content/pages` | create/update page/post |
| Reviews | `/admin/reviews` | list/delete review |

### 5.3 Keycloak

URL:

```bash
source aws/config.env
echo "https://auth.${ELASTIC_IP_DASHED}.nip.io"
```

Dung de:

- Gan `ROLE_ADMIN`.
- Kiem tra realm `ecommerce`.
- Kiem tra client `ecommerce-client`.
- Kiem tra client service account `identity-service-admin`.

Credentials xem trong `/opt/ecommerce/.env.prod`. Khong ghi ra guide.

### 5.4 Swagger UI

```bash
source aws/config.env
echo "https://api.${ELASTIC_IP_DASHED}.nip.io/swagger-ui.html"
```

API public smoke:

```bash
source aws/config.env
export API_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"

curl -s "$API_URL/api/products?size=5&page=0" | jq .
curl -s "$API_URL/api/search?q=laptop" | jq .
curl -s "$API_URL/api/flash-sales" | jq .
curl -s "$API_URL/api/content/banners" | jq .
```

---

## 6. Observability: Grafana, Prometheus, Zipkin

### 6.1 Grafana

Production URL qua Caddy:

```bash
source aws/config.env
echo "https://grafana.${ELASTIC_IP_DASHED}.nip.io"
```

Direct debug URL neu Security Group mo port `3001` cho IP cua ban:

```bash
echo "http://${ELASTIC_IP}:3001"
```

Dang nhap co the co 2 lop:

1. Caddy basic auth tren `grafana.<ip>.nip.io`.
2. Grafana login `admin` + password trong `/opt/ecommerce/.env.prod`.

Neu Caddy hoi Basic Auth ma ban khong nho:

```bash
# Tren EC2
sudo systemctl cat caddy
sudo grep -n 'GRAFANA_HTPASSWD\|basic_auth' /etc/caddy/Caddyfile
```

Khong ghi hash/password ra git. Caddy chi nen doc env rieng `/etc/caddy/ecommerce.env`, khong nen doc full `/opt/ecommerce/.env.prod` vi service Caddy mac dinh co `--environ` va co the in env vao journal. Neu thieu `GRAFANA_HTPASSWD`, tao/set lai tren EC2:

```bash
cd /opt/ecommerce
GRAFANA_HASH=$(caddy hash-password --plaintext '<password-ban-tu-chon>')

printf 'ELASTIC_IP_DASHED=%s\nGRAFANA_HTPASSWD=%s\n' "$ELASTIC_IP_DASHED" "$GRAFANA_HASH" \
  | sudo tee /etc/caddy/ecommerce.env >/dev/null
sudo chown root:root /etc/caddy/ecommerce.env
sudo chmod 600 /etc/caddy/ecommerce.env

sudo mkdir -p /etc/systemd/system/caddy.service.d
printf '[Service]\nEnvironmentFile=/etc/caddy/ecommerce.env\n' \
  | sudo tee /etc/systemd/system/caddy.service.d/10-ecommerce-env.conf >/dev/null
```

Sau do reload:

```bash
sudo systemctl daemon-reload
sudo systemctl restart caddy
sudo systemctl status caddy
```

Dashboards:

| Dashboard | Xem gi |
|---|---|
| `Spring Boot Overview` | HTTP throughput, latency, status code |
| `JVM Overview` | Heap, GC, CPU, threads |
| `E-commerce Saga Overview` | Order/payment/inventory/saga metrics |

Cach xem:

1. Mo Grafana.
2. Dashboards -> folder `E-Commerce`.
3. Chon dashboard.
4. Set time range `Last 30 minutes`, `Last 1 hour`, hoac range luc chay k6.
5. Chon variable `job` neu dashboard co, vi du `api-gateway`, `order-service`, `flash-sale-service`.

PromQL hay dung:

```promql
up
rate(http_server_requests_seconds_count[1m])
sum by (job, status) (rate(http_server_requests_seconds_count[1m]))
histogram_quantile(0.95, sum by (le, job) (rate(http_server_requests_seconds_bucket[5m])))
jvm_memory_used_bytes{area="heap"}
process_cpu_usage
hikaricp_connections_active
```

### 6.2 Prometheus

Prometheus production van expose host port `9090` trong prod compose, nhung Security Group nen chi mo cho IP nha.

```bash
source aws/config.env
echo "http://${ELASTIC_IP}:9090/targets"
```

Query tu laptop neu port mo:

```bash
curl -s "http://${ELASTIC_IP}:9090/api/v1/query?query=up" \
  | jq '.data.result[] | {job:.metric.job, value:.value[1]}'
```

Neu port khong mo voi laptop, query tu EC2 host:

```bash
cd /opt/ecommerce
curl -s 'http://localhost:9090/api/v1/query?query=up' \
  | jq '.data.result[] | {job:.metric.job, value:.value[1]}'
```

Targets can `UP`:

- `api-gateway`
- 13 business services
- `prometheus`

### 6.3 Zipkin

Quan trong: trong `docker-compose.prod.yml`, Zipkin **khong expose host port** mac dinh. Vi vay URL direct `http://<EC2>:9411` chi dung neu ban da mo port/tunnel rieng.

Cach dung dung trong production: SSH tunnel tu laptop.

```bash
source aws/config.env
ssh -N -L 19411:localhost:9411 \
  -o StrictHostKeyChecking=accept-new \
  -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP
```

Giu terminal tunnel do dang chay. Mo terminal khac:

```bash
open "http://localhost:19411"
```

Neu khong dung `open`, copy URL vao browser:

```text
http://localhost:19411
```

Tao trace moi:

```bash
source aws/config.env
export API_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"

curl -s "$API_URL/api/products?size=1&page=0" > /dev/null
curl -s "$API_URL/api/search?q=laptop" > /dev/null
```

Trong Zipkin UI:

1. Lookback: `1h`.
2. Service: `api-gateway`, `product-service`, `order-service`, hoac de trong.
3. Click Run Query.
4. Mo trace de xem waterfall.

Lay trace bang API qua tunnel:

```bash
curl -s "http://localhost:19411/api/v2/traces?serviceName=order-service&lookback=3600000&limit=10" \
  | jq '.[0][0] | {traceId,id,name,timestamp,duration,tags}'
```

Zipkin storage la in-memory; restart Zipkin se mat trace cu.

### 6.4 Capture Grafana/Zipkin screenshots

Script capture can browser puppeteer da cai o phase 13. Neu chua co, cai lai theo ghi chu phase 13.

Grafana:

```bash
source aws/config.env
GRAFANA_URL="http://${ELASTIC_IP}:3001" \
GRAFANA_USER=admin \
GRAFANA_PASSWORD="<xem GF_SECURITY_ADMIN_PASSWORD trong .env.prod>" \
node .test/scripts/phase13-capture-observability.mjs
```

Zipkin qua tunnel:

```bash
CAPTURE_GRAFANA=false \
ZIPKIN_URL=http://localhost:19411 \
node .test/scripts/phase13-capture-observability.mjs
```

Output vao `.test/results/`.

---

## 7. Tang Du Lieu: DB, Redis, Kafka, Elasticsearch

Tat ca lenh duoi day chay tren EC2:

```bash
ssh -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP
cd /opt/ecommerce
```

### 7.1 PostgreSQL

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec postgres \
  psql -U postgres -c "\l"

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec postgres \
  psql -U postgres -d product_db \
  -c "SELECT id, sku, name, price FROM products ORDER BY created_at DESC LIMIT 5;"

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec postgres \
  psql -U postgres -d inventory_db \
  -c "SELECT sku, quantity, reserved_quantity FROM inventory ORDER BY updated_at DESC LIMIT 10;"

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec postgres \
  psql -U postgres -d order_db \
  -c "SELECT id, status, payment_method, created_at FROM orders ORDER BY created_at DESC LIMIT 10;"
```

### 7.2 Redis

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec redis redis-cli INFO memory
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec redis redis-cli DBSIZE
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec redis redis-cli KEYS "cart:*"
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec redis redis-cli KEYS "flashsale:*"
```

### 7.3 Kafka

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec kafka \
  kafka-topics --bootstrap-server localhost:9092 --list | sort

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec kafka \
  kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

DLT neu nghi consumer loi:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec kafka \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-created.DLT --from-beginning --max-messages 5
```

### 7.4 Elasticsearch

Quan trong: production reset port Elasticsearch, nen khong dung `http://<EC2>:9200` tru khi ban mo port rieng. Dung exec:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec elasticsearch \
  curl -s http://localhost:9200/_cluster/health

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec elasticsearch \
  curl -s 'http://localhost:9200/_cat/indices?v'
```

---

## 8. Chuan Bi Account Va Seed Data

### 8.1 Tao admin an toan

Khong hardcode account/password vao guide. Ban co 2 cach:

1. Tao admin qua UI:
   - Vao storefront register bang email/password ban tu chon.
   - Vao Keycloak, gan `ROLE_ADMIN`.
   - Dang xuat/dang nhap lai frontend.
2. Tao admin qua API:
   ```bash
   source aws/config.env
   export API_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"

   curl -s -X POST "$API_URL/api/auth/register" \
     -H "Content-Type: application/json" \
     -d '{"email":"<email-cua-ban>","password":"<password-cua-ban>","fullName":"Admin Demo"}' | jq .
   ```
   Sau do van phai gan `ROLE_ADMIN` trong Keycloak.

### 8.2 Lay admin token

```bash
source aws/config.env
export API_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"

export ADMIN_TOKEN="$(
  curl -s -X POST "$API_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"email":"<email-admin-cua-ban>","password":"<password-admin-cua-ban>"}' \
  | jq -r '.data.accessToken'
)"

test -n "$ADMIN_TOKEN" && echo "ADMIN_TOKEN loaded"
```

Khong in full token ra terminal neu terminal duoc ghi log.

### 8.3 Seed product/user/flash sale dung thu tu

Chay tu laptop, sau khi da co `ADMIN_TOKEN`:

```bash
source aws/config.env
export BASE_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"

.test/seed/seed-products.sh 100
.test/seed/seed-users.sh 500
.test/seed/seed-flash-sale.sh 100
```

Luu y:

- `seed-products.sh` va `seed-flash-sale.sh` can `ADMIN_TOKEN`.
- `seed-users.sh` tao `.test/seed/users.json` va `.test/seed/tokens.json` cho k6.
- `seed-products.sh` tao `.test/seed/products.json` cho k6.
- `seed-flash-sale.sh` tao `.test/seed/flash_sale_id.txt`.

Kiem tra seed:

```bash
jq 'length, .[0]' .test/seed/products.json
jq 'length, .[0]' .test/seed/users.json
cat .test/seed/flash_sale_id.txt
```

Neu storefront van trong:

```bash
curl -s "$BASE_URL/api/products?size=5&page=0" | jq .
```

---

## 9. Full E2E Bang UI

Lam theo thu tu nay de khong bi ket data/quyen.

### 9.1 Auth va role

1. Mo `APP_URL`.
2. Register user thuong.
3. Login bang email.
4. Kiem tra menu user hien dung.
5. Vao `/admin`, user thuong phai bi chan.
6. Gan `ROLE_ADMIN` cho admin trong Keycloak.
7. Login admin, vao `/admin/dashboard`.

### 9.2 Catalog, search, filter, detail

1. Vao `/products`.
2. Kiem tra product list.
3. Dung filter category/brand/price/spec neu co.
4. Mo product detail.
5. Kiem tra gallery, price, description, specs, review tab.
6. Vao `/search?q=<keyword>`.

Neu search chua ra product moi tao, doi vai giay cho Kafka event duoc consume, hoac xem:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=150 search-service
```

### 9.3 Compare product

1. Vao `/products`.
2. Them 2-4 san pham vao compare.
3. Vao `/compare`.
4. Kiem tra bang so sanh specs/prices.
5. Xoa san pham khoi compare, refresh lai trang de kiem tra Zustand/localStorage persist.

### 9.4 Guest cart -> user cart merge

1. Logout.
2. Them san pham vao gio hang.
3. Vao `/cart`, kiem tra item.
4. Login user.
5. Kiem tra gio guest merge vao gio user.
6. Kiem tra tang/giam/xoa item.

Neu cart loi 400, xoa cookie `guest_session_id` cu va reload.

### 9.5 Profile va address book

1. Login user.
2. Vao `/profile`, cap nhat thong tin neu UI ho tro.
3. Vao `/addresses`.
4. Tao dia chi moi.
5. Sua dia chi.
6. Dat lam default neu UI co.
7. Xoa dia chi test neu can.

### 9.6 Voucher

1. Admin vao `/admin/vouchers`.
2. Tao voucher active voi thoi gian hop le.
3. User vao cart/checkout.
4. Nhap voucher.
5. Kiem tra tong tien giam dung.
6. Admin deactivate/xoa voucher sau test.

### 9.7 Checkout COD va saga

1. User co san pham trong cart va product co ton kho.
2. Vao `/checkout`.
3. Chon/nhap dia chi.
4. Chon COD.
5. Dat hang.
6. Vao `/orders/<id>`.
7. Kiem tra status di tu `PENDING` -> `STOCK_RESERVED` -> `CONFIRMED`.
8. Xem Grafana/Zipkin/log de minh hoa saga.

Kiem tra DB tren EC2:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec postgres \
  psql -U postgres -d order_db \
  -c "SELECT id, status, total_amount, payment_method, created_at FROM orders ORDER BY created_at DESC LIMIT 5;"
```

### 9.8 VNPAY sandbox

Chi demo VNPAY neu `.env.prod` da co sandbox credentials that.

Kiem tra cac bien can co trong `/opt/ecommerce/.env.prod`:

- `VNPAY_TMN_CODE`
- `VNPAY_HASH_SECRET`
- `VNPAY_PAY_URL`
- `VNPAY_RETURN_URL`
- `FRONTEND_ORDER_RESULT_URL`

Khong ghi gia tri vao guide. Neu dang dung dummy/demo secret, bo qua VNPAY va demo COD.

Quy trinh:

1. User checkout.
2. Chon VNPAY.
3. Redirect sang VNPAY sandbox.
4. Dung the test tu `.guide/02-lay-credentials.md`.
5. Sau return, kiem tra order result/order detail.
6. Order phai ve `CONFIRMED` neu callback/IPN thanh cong.

### 9.9 Flash sale

1. Admin vao `/admin/flash-sales/new`.
2. Tao campaign voi product co stock, start time trong tuong lai gan.
3. Doi den start time.
4. User vao `/flash-sales`.
5. Mo detail campaign.
6. Bam mua.
7. Kiem tra sold count/stock/order.

Kiem tra DB/Redis:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec redis redis-cli KEYS "flashsale:*"
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec postgres \
  psql -U postgres -d flash_sale_db \
  -c "SELECT id, sku, quantity, sold_count, status, start_time, end_time FROM flash_sales ORDER BY created_at DESC LIMIT 5;"
```

### 9.10 Review

1. User co order `CONFIRMED`.
2. Mo product da mua.
3. Tab review hien form.
4. Gui rating/comment.
5. Admin vao `/admin/reviews`.
6. Xem/xoa review test.

### 9.11 Content va banners

1. Admin vao `/admin/content/banners`.
2. Tao banner.
3. Kiem tra storefront hien banner neu UI co vung hien.
4. Admin vao `/admin/content/pages`.
5. Tao page/post voi slug rieng.
6. Mo `/content/<slug>`.

### 9.12 Notification/email

Production khong public Mailpit UI mac dinh. De kiem tra notification:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=200 notification-service
```

Local dev co Mailpit UI tai `http://localhost:8025`.

---

## 10. Automated E2E / Smoke Scripts

### 10.1 Phase 14 API/Admin smoke

Script `.test/scripts/phase14-e2e.sh` hien hardcode admin test account trong script. Truoc khi chay:

```bash
sed -n '85,95p' .test/scripts/phase14-e2e.sh
```

Neu account do khong ton tai hoac ban khong muon dung account do, sua script/test account rieng truoc khi chay.

Khuyen nghi chay tren EC2 vi Ubuntu co `/usr/bin/curl` va `/usr/bin/jq` dung voi script:

```bash
ssh -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP
cd /opt/ecommerce
BASE_URL=http://localhost:8080 .test/scripts/phase14-e2e.sh
```

Neu chay tren macOS, script co the fail vi hardcode `/usr/bin/jq`. Khi do chay tren EC2 hoac sua script dung `curl`/`jq` tu `PATH`.

### 10.2 Backend smoke local

Dung khi muon verify local stack:

```bash
.test/scripts/smoke-backend.sh --suite full --down-first
```

Lenh nay nang, co the build/chay nhieu container va tao report `.test/backend-readiness-report.md`.

---

## 11. Phase 13 Performance Va Resilience

Nguyen tac: chi dua metric vao bao cao khi co artifact trong `.test/results/`.

### 11.1 Artifacts da co

Xem:

```bash
sed -n '1,220p' .test/results/SUMMARY.md
sed -n '1,220p' .docs/08-testing-and-evaluation.md
```

Khong tu tao so lieu. Neu can so lieu, chay lai script va luu raw output.

### 11.2 Chay k6 smoke ngan

Can co `.test/seed/products.json` va `.test/seed/users.json`.

```bash
source aws/config.env
export BASE_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"

CATALOG_RAMP_UP=10s \
CATALOG_HOLD=30s \
CATALOG_RAMP_DOWN=10s \
CATALOG_TARGET_VUS=20 \
CATALOG_RAMP_VUS=10 \
k6 run .test/load/catalog-browse.js

CHECKOUT_RAMP_UP=10s \
CHECKOUT_HOLD=30s \
CHECKOUT_RAMP_DOWN=10s \
CHECKOUT_TARGET_VUS=10 \
k6 run .test/load/checkout-stress.js
```

### 11.3 Chay full performance

```bash
source aws/config.env
export BASE_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"

k6 run --out json=.test/results/catalog-soak-$(date +%Y%m%d-%H%M).json \
  .test/load/catalog-browse.js

k6 run --out json=.test/results/checkout-stress-$(date +%Y%m%d-%H%M).json \
  .test/load/checkout-stress.js

export FLASH_SALE_ID="$(cat .test/seed/flash_sale_id.txt)"
k6 run --out json=.test/results/flash-sale-spike-$(date +%Y%m%d-%H%M).json \
  -e FLASH_SALE_ID="$FLASH_SALE_ID" \
  .test/load/flash-sale-spike.js
```

### 11.4 Resilience scripts

Chi chay khi chap nhan service production-like bi stop/restart tam thoi.

```bash
source aws/config.env
export BASE_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"
export EC2_HOST="$ELASTIC_IP"
export SSH_KEY="$SSH_KEY_PATH"

.test/scripts/phase13-kafka-outbox-replay.sh
.test/scripts/phase13-redis-degradation.sh
.test/chaos/kill-order-service.sh
.test/chaos/force-inventory-fail.sh
```

---

## 12. CI/CD Va Deploy Ban Moi

Workflow:

| Workflow | Muc dich |
|---|---|
| `.github/workflows/build-and-push.yml` | Build/push backend images |
| `.github/workflows/build-frontend.yml` | Build/push frontend image |
| `.github/workflows/deploy.yml` | Manual deploy len EC2 |

Trigger deploy:

```bash
gh workflow run deploy.yml -f image_tag=latest
gh run list --workflow deploy.yml --limit 5
gh run watch
```

Rollback:

```bash
gh workflow run deploy.yml -f image_tag=<image-tag-cu>
```

Verify sau deploy:

```bash
source aws/config.env
export API_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"
curl -s "$API_URL/actuator/health" | jq .
curl -s "$API_URL/api/products?size=1&page=0" | jq .
```

---

## 13. Stop Stack Va Stop EC2

Dung container, giu data:

```bash
source aws/config.env
ssh -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP \
  'cd /opt/ecommerce && docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml down'
```

Stop EC2:

```bash
aws ec2 stop-instances --instance-ids "$INSTANCE_ID"
aws ec2 wait instance-stopped --instance-ids "$INSTANCE_ID"
```

Khong dung `down -v` tren EC2 tru khi muon xoa sach DB/Kafka/Redis/Elasticsearch.

---

## 14. Local Dev / Local E2E

### 14.1 Backend local

```bash
cp .env.example .env
# Dien gia tri trong .env theo .guide/02-lay-credentials.md

./mvnw clean package -DskipTests
docker compose up -d --build
docker compose ps
curl -s http://localhost:8080/actuator/health/readiness | jq .
```

### 14.2 Port 3000 conflict local

Local `docker-compose.yml` expose Grafana tai `localhost:3000`. Frontend dev mac dinh cung dung `3000`.

Chon 1 trong 2 cach:

**Cach A: Chay frontend port 3005**

```bash
cp frontend/.env.local.example frontend/.env.local
```

Sua `frontend/.env.local`:

```env
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_APP_URL=http://localhost:3005
```

Chay:

```bash
cd frontend
npm install
npm run dev -- -p 3005
```

Mo:

```text
http://localhost:3005
http://localhost:3005/admin
```

Grafana local van o:

```text
http://localhost:3000
```

**Cach B: Dung frontend port 3000, khong xem Grafana local cung luc**

Dung khi chi test UI:

```bash
docker compose stop grafana
cd frontend
npm run dev
```

### 14.3 Frontend local tro backend EC2

Dung khi muon debug UI local nhung backend dang chay tren EC2:

```env
API_BASE_URL=https://api.<ELASTIC_IP_DASHED>.nip.io
NEXT_PUBLIC_API_BASE_URL=https://api.<ELASTIC_IP_DASHED>.nip.io
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

Lay `<ELASTIC_IP_DASHED>` tu `aws/config.env`. Khong ghi gia tri that vao file guide.

---

## 15. Checklist Demo Final

### 15.1 Infrastructure

- [ ] EC2 state `running`.
- [ ] `/opt/ecommerce/.env.prod` ton tai tren EC2.
- [ ] `docker compose ... ps` tat ca container chinh `Up`.
- [ ] API health `UP`.
- [ ] Eureka co `API-GATEWAY` + 13 business services.
- [ ] Caddy HTTPS app/api/auth/grafana hoat dong.
- [ ] Prometheus targets `UP`.
- [ ] Grafana co data dashboard.
- [ ] Zipkin tunnel mo duoc va co trace moi.

### 15.2 Storefront

- [ ] Register/login/logout bang email.
- [ ] Home/catalog/product detail load duoc.
- [ ] Search co ket qua.
- [ ] Filter product hoat dong.
- [ ] Compare 2-4 san pham hoat dong.
- [ ] Guest cart hoat dong.
- [ ] Guest cart merge vao user cart sau login.
- [ ] Address book tao/sua/xoa duoc.
- [ ] Profile page load/cap nhat neu UI ho tro.
- [ ] Voucher apply dung.
- [ ] COD checkout tao order.
- [ ] Order status ve `CONFIRMED`.
- [ ] VNPAY sandbox thanh cong neu co credentials that.
- [ ] Flash sale mua duoc va khong oversell.
- [ ] Review san pham da mua tao duoc.
- [ ] Content page/post xem duoc.

### 15.3 Admin

- [ ] User thuong bi chan `/admin`.
- [ ] Admin vao `/admin/dashboard`.
- [ ] Products CRUD.
- [ ] Inventory stock-in/stock-out/movements.
- [ ] Orders list/detail/status update.
- [ ] Users list/detail.
- [ ] Vouchers CRUD/deactivate.
- [ ] Flash sales create/cancel.
- [ ] Content banners/pages CRUD.
- [ ] Reviews list/delete.

### 15.4 Data/Message/Logs

- [ ] PostgreSQL co product/order/inventory rows moi.
- [ ] Redis co cart/flashsale keys khi test.
- [ ] Kafka topics ton tai.
- [ ] Khong co DLT bat thuong sau demo.
- [ ] Search-service index product moi.
- [ ] Notification-service log email/order event.
- [ ] Browser console khong co loi nghiem trong.
- [ ] Service logs khong co loop error/OOM.

### 15.5 Phase 13 evidence

- [ ] `.test/results/` co raw k6 JSON/TXT neu trinh bay performance.
- [ ] Grafana screenshots co trong `.test/results/`.
- [ ] Zipkin screenshots/JSON co trong `.test/results/`.
- [ ] `.docs/08-testing-and-evaluation.md` chi ghi metric co artifact that.

---

## 16. Tai Lieu Lien Quan

- `.guide/16-chay-end-to-end.md`: E2E local cu the tung luong.
- `.guide/07-monitoring-observability.md`: monitoring local.
- `.guide/11-api-tham-khao.md`: API endpoints.
- `.guide/12-kafka-topics.md`: Kafka topics.
- `.guide/13-state-machines.md`: state machines.
- `.guide/14-scheduler-jobs.md`: scheduled jobs.
- `.guide/15-gateway-security.md`: security/routing/rate limit.
- `.docs/05-deployment-aws.md`: AWS deployment.
- `.docs/07-frontend-nextjs.md`: storefront architecture.
- `.docs/09-admin-panel.md`: admin architecture.
- `.docs/08-testing-and-evaluation.md`: Phase 13 results.
- `.test/results/SUMMARY.md`: raw evidence summary.
