# Huong Dan Chay Thu Cong Va Di Sau He Thong

> File nay dung de ban tu tay chay, mo browser, vao tung lop he thong va kiem chung full stack production tren EC2. Tai lieu nay khong phai kich ban noi, khong phai outline slide, va khong tap trung vao copy-paste hang loat. Moi muc duoi day tra loi 3 cau hoi: mo o dau, can thay gi, neu khong dung thi kiem tra tiep o dau.
>
> File nay co the push len git neu chi giu placeholder/env variable. URL public, IP, SSH key path va credential that phai doc tu `aws/config.env`, `/opt/ecommerce/.env.prod`, `/etc/caddy/ecommerce.env` hoac GitHub Actions Secrets khi can. Password/token/secret khong nen ghi truc tiep vao guide va khong nen de lo trong man hinh quay.

---

## 0. Thong Tin Moi Truong Dang Dung

Gia tri thuc te doc tu `aws/config.env` tren laptop:

| Ten | Gia tri |
|---|---|
| AWS region | `ap-southeast-1` |
| EC2 instance id | `${INSTANCE_ID}` |
| Elastic IP | `${ELASTIC_IP}` |
| Elastic IP dashed | `${ELASTIC_IP_DASHED}` |
| SSH key | `${SSH_KEY_PATH}` |
| GitHub owner | `${GITHUB_OWNER}` hoac GitHub repo owner |

URL production nen mo bang browser:

| Thanh phan | URL |
|---|---|
| Storefront + Admin Next.js | `https://app.${ELASTIC_IP_DASHED}.nip.io` |
| Admin panel | `https://app.${ELASTIC_IP_DASHED}.nip.io/admin` |
| API Gateway | `https://api.${ELASTIC_IP_DASHED}.nip.io` |
| Swagger UI | `https://api.${ELASTIC_IP_DASHED}.nip.io/swagger-ui.html` |
| API health | `https://api.${ELASTIC_IP_DASHED}.nip.io/actuator/health` |
| Keycloak | `https://auth.${ELASTIC_IP_DASHED}.nip.io` |
| Grafana | `https://grafana.${ELASTIC_IP_DASHED}.nip.io` |
| Eureka | `http://${ELASTIC_IP}:8761` |
| Config Server | `http://${ELASTIC_IP}:8888` |
| Prometheus | `http://${ELASTIC_IP}:9090` |
| Zipkin | Khong public; mo qua SSH tunnel o muc 10 |

Credential can tu xem:

| Can login | Xem o dau |
|---|---|
| SSH EC2 | local `aws/config.env`, bien `SSH_KEY_PATH` |
| Eureka | `/opt/ecommerce/.env.prod`: `EUREKA_USER`, `EUREKA_PASSWORD` |
| Keycloak admin | `/opt/ecommerce/.env.prod`: `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` |
| Grafana basic auth cua Caddy | `/etc/caddy/Caddyfile` va `/etc/caddy/ecommerce.env`: `GRAFANA_HTPASSWD` |
| Grafana app login | `/opt/ecommerce/.env.prod`: `GF_SECURITY_ADMIN_PASSWORD`; user mac dinh la `admin` neu khong doi |
| App user/admin demo | Keycloak realm `ecommerce`, hoac file seed/test local neu ban da tao |

Lenh nhanh de set bien moi truong tren laptop:

```bash
cd "<PROJECT_ROOT>"
source aws/config.env

export APP_URL="https://app.${ELASTIC_IP_DASHED}.nip.io"
export API_URL="https://api.${ELASTIC_IP_DASHED}.nip.io"
export AUTH_URL="https://auth.${ELASTIC_IP_DASHED}.nip.io"
export GRAFANA_URL="https://grafana.${ELASTIC_IP_DASHED}.nip.io"
export EUREKA_URL="http://${ELASTIC_IP}:8761"
export CONFIG_URL="http://${ELASTIC_IP}:8888"
export PROM_URL="http://${ELASTIC_IP}:9090"
```

---

## 1. Hieu Dung He Thong Dang Chay

Production hien tai la mot EC2 chay Docker Compose:

```text
Internet
  |
  | HTTPS qua nip.io
  v
Caddy tren EC2
  |-- app.${ELASTIC_IP_DASHED}.nip.io      -> frontend container :3000
  |-- api.${ELASTIC_IP_DASHED}.nip.io      -> api-gateway :8080
  |-- auth.${ELASTIC_IP_DASHED}.nip.io     -> keycloak host port :8180
  |-- grafana.${ELASTIC_IP_DASHED}.nip.io  -> grafana host port :3001
  |
  v
Docker network ecommerce-network
  |-- discovery-server / config-server / api-gateway
  |-- 13 business services Spring Boot
  |-- PostgreSQL / Redis / Kafka / Elasticsearch / Keycloak
  |-- Prometheus / Grafana / Zipkin
```

Danh sach service Spring Boot can thay khi he thong day du:

| Nhom | Service |
|---|---|
| Spring Cloud | `config-server`, `api-gateway` |
| Auth/User | `identity-service`, `user-service` |
| Catalog | `product-service`, `search-service`, `review-service`, `content-service` |
| Commerce | `cart-service`, `voucher-service`, `order-service`, `payment-service`, `inventory-service`, `notification-service` |
| Flash sale | `flash-sale-service` |

`discovery-server` la Eureka server. No co the khong tu hien nhu client trong danh sach app; dung `docker compose ps` va `/actuator/health` de kiem tra no.

Port production quan trong:

| Port | Thanh phan | Ghi chu |
|---:|---|---|
| 80/443 | Caddy | Public HTTPS |
| 3000 | Frontend | Host port tu container frontend |
| 3001 | Grafana | Do frontend chiem 3000 nen prod remap Grafana sang 3001 |
| 8080 | API Gateway | Co ca Caddy HTTPS va host port debug |
| 8180 | Keycloak | Caddy reverse proxy qua HTTPS |
| 8761 | Eureka | Debug port, co basic auth |
| 8888 | Config Server | Debug port |
| 9090 | Prometheus | Debug port |
| 9411 | Zipkin | Khong expose host port trong prod override |
| 9200 | Elasticsearch | Khong expose host port trong prod override |
| 5432/6379/9092 | PostgreSQL/Redis/Kafka | Khong expose public trong prod override |

---

## 2. Bat EC2 Va Start Full Stack Bang Tay

Phan nay lam truoc khi vao browser. Neu EC2 dang chay san, chi can vao muc 2.3.

### 2.1 Kiem Tra EC2 Tren Laptop

```bash
cd "<PROJECT_ROOT>"
source aws/config.env

aws ec2 describe-instances \
  --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].State.Name' \
  --output text
```

Can thay:

- `running`: EC2 da bat.
- `stopped`: can start.
- `pending`: doi them 1-2 phut.

Neu dang `stopped`:

```bash
aws ec2 start-instances --instance-ids "$INSTANCE_ID"
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
```

Cho EC2 boot xong, sau do SSH:

```bash
ssh -o StrictHostKeyChecking=accept-new -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP
```

### 2.2 Kiem Tra Folder Production Tren EC2

Tren EC2:

```bash
cd /opt/ecommerce
pwd
ls -la .env.prod docker-compose.yml docker-compose.prod.yml aws/start-stack.sh
```

Can thay:

- Co `.env.prod`.
- Co ca `docker-compose.yml` va `docker-compose.prod.yml`.
- Co `aws/start-stack.sh`.

Neu can xem config khong in secret:

```bash
grep -E '^(GITHUB_OWNER|IMAGE_TAG|ELASTIC_IP|ELASTIC_IP_DASHED|KC_HOSTNAME|NEXT_PUBLIC_API_BASE_URL|NEXT_PUBLIC_APP_URL)=' .env.prod
```

Neu can password/secret, mo file bang editor tren EC2, khong show len man hinh quay:

```bash
nano /opt/ecommerce/.env.prod
```

### 2.3 Start Stack

Tren EC2:

```bash
cd /opt/ecommerce
bash aws/start-stack.sh
```

Neu muon tu thao tac tung buoc:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d --no-build
```

Sau khi start, xem container:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml ps
```

Can thay cac container chinh deu `Up`, gom:

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
- `ecommerce-redis`
- `ecommerce-kafka`
- `ecommerce-elasticsearch`
- `ecommerce-keycloak`
- `ecommerce-prometheus`
- `ecommerce-grafana`
- `ecommerce-zipkin`

Neu mot service `Restarting` hoac `Exited`, xem log:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=120 <service-name>
```

Vi du:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=120 order-service
```

### 2.4 Kiem Tra Caddy

Tren EC2:

```bash
systemctl status caddy --no-pager
sudo systemctl cat caddy
```

Can thay:

- `active (running)`.
- Caddy co drop-in `EnvironmentFile=/etc/caddy/ecommerce.env`.
- Listener `:80` va `:443` dang mo:

```bash
sudo ss -ltnp | grep -E ':(80|443)\b'
```

Neu Caddy loi:

```bash
journalctl -u caddy -n 120 --no-pager
sudo systemctl reload caddy
```

Luu y quan trong: khong gan full `/opt/ecommerce/.env.prod` lam `EnvironmentFile` cho Caddy. Caddy service mac dinh chay voi `--environ`, co the in bien moi truong vao journal. Chi dung env rieng `/etc/caddy/ecommerce.env` gom cac bien Caddy can: `ELASTIC_IP_DASHED`, `GRAFANA_HTPASSWD`.

---

## 3. Kiem Tra Lop Ngoai Cung Bang Browser

Mo tung URL nay truoc. Day la cach nhanh nhat de biet he thong da san sang chua.

| Buoc | Mo tren browser | Can thay |
|---:|---|---|
| 1 | `https://app.${ELASTIC_IP_DASHED}.nip.io` | Storefront load duoc, khong phai trang 502 |
| 2 | `https://app.${ELASTIC_IP_DASHED}.nip.io/admin` | Admin route load, neu chua login thi chuyen login/guard |
| 3 | `https://api.${ELASTIC_IP_DASHED}.nip.io/actuator/health` | JSON co `status: "UP"` |
| 4 | `https://api.${ELASTIC_IP_DASHED}.nip.io/swagger-ui.html` | Swagger UI hien API docs |
| 5 | `https://auth.${ELASTIC_IP_DASHED}.nip.io` | Keycloak page load qua HTTPS |
| 6 | `https://grafana.${ELASTIC_IP_DASHED}.nip.io` | Caddy basic auth/Grafana login |
| 7 | `http://${ELASTIC_IP}:8761` | Eureka login |
| 8 | `http://${ELASTIC_IP}:9090/targets` | Prometheus targets |

Trang thai da kiem tra sau hotfix ngay `2026-06-03`:

- `app.${ELASTIC_IP_DASHED}.nip.io` va `http://${ELASTIC_IP}:3000` da tra ve Next.js frontend.
- `grafana.${ELASTIC_IP_DASHED}.nip.io` va host port `3001` la Grafana.
- `api.${ELASTIC_IP_DASHED}.nip.io/swagger-ui.html` redirect sang `/swagger-ui/index.html` va tra `200`.
- `api.${ELASTIC_IP_DASHED}.nip.io/v3/api-docs` tra `200`.

Neu app/api/grafana qua HTTPS bi `502 Bad Gateway`:

1. Kiem tra container dich co `Up` khong.
2. Kiem tra Caddy.
3. Kiem tra host port mapping:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml ps frontend api-gateway grafana keycloak
```

Can thay mapping dung:

```text
frontend  -> 0.0.0.0:3000->3000/tcp
grafana   -> 0.0.0.0:3001->3000/tcp
```

Neu Eureka/Prometheus khong vao duoc:

- Kiem tra security group co cho IP hien tai vao port debug khong.
- Kiem tra ban dang dung IP nha khac voi `MY_HOME_IP` trong `aws/config.env` khong.
- Co the SSH vao EC2 va kiem tra noi bo bang `curl http://localhost:8761/actuator/health`, `curl http://localhost:9090/-/ready`.

---

## 3A. Test Rieng Frontend Production Bang Browser

Muc nay chi tap trung vao FE. Muc tieu la xac nhan Next.js app, route, auth cookie, BFF route va UI workflow chay dung tren browser, khong chi test API backend bang `curl`.

### 3A.1 FE Chay O Cong Nao

Production co 2 cach vao FE:

| Cach vao | URL | Khi nao dung |
|---|---|---|
| Duong dung chinh | `https://app.${ELASTIC_IP_DASHED}.nip.io` | Dung khi test/demo that. Di qua Caddy HTTPS port `443`. |
| Debug truc tiep container FE | `http://${ELASTIC_IP}:3000` | Chi dung de debug khi Caddy/HTTPS loi. Khong dung lam duong demo chinh. |

Neu hai URL tren hien Grafana login thay vi storefront, production dang bi sai mapping port:

- `frontend` chua duoc deploy/start tren EC2, hoac
- `grafana` van giu host port `3000` tu `docker-compose.yml`.

Kiem tra tren EC2:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml ps frontend grafana
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml config --format json \
  | jq '.services.grafana.ports, .services.frontend.ports'
```

Trong `docker-compose.prod.yml`, Grafana prod override phai dung `ports: !override` va chi map `3001:3000`; frontend map `3000:3000`.

Luot request FE production:

```text
Browser
  -> https://app.${ELASTIC_IP_DASHED}.nip.io
  -> Caddy :443
  -> frontend container :3000
  -> FE BFF routes:
       /api/auth/login
       /api/auth/logout
       /api/auth/register
       /api/auth/session
       /api/proxy/<backend-path>
  -> API Gateway:
       https://api.${ELASTIC_IP_DASHED}.nip.io
```

Neu test local dev moi dung:

```text
http://localhost:3000
```

Nhung voi guide nay, uu tien production URL `https://app.${ELASTIC_IP_DASHED}.nip.io`.

### 3A.2 Chuan Bi Browser De Test FE

Nen dung Chrome/Edge va mo DevTools:

1. Mo `https://app.${ELASTIC_IP_DASHED}.nip.io`.
2. Bam `F12` hoac `Cmd+Option+I`.
3. Vao tab `Network`.
4. Tick `Preserve log`.
5. Filter theo `Fetch/XHR`.
6. Neu nghi cache loi, tick `Disable cache` khi DevTools dang mo.

Trong Network tab, FE production nen goi cac endpoint dang:

| Request trong Network | Y nghia |
|---|---|
| `/api/auth/session` | FE kiem tra dang login hay chua |
| `/api/auth/login` | FE BFF login, nhan token tu backend va set cookie |
| `/api/auth/logout` | Xoa session/cookie |
| `/api/proxy/products...` | FE proxy sang API Gateway product-service |
| `/api/proxy/search...` | FE proxy sang search-service |
| `/api/proxy/cart...` | FE proxy sang cart-service |
| `/api/proxy/orders...` | FE proxy sang order-service |
| `/api/proxy/.../admin...` | FE admin goi backend admin APIs |

Neu tren Network thay FE goi thang `http://localhost:8080` hoac URL local nao do, build/env frontend production dang sai. Can kiem tra `NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_APP_URL` trong `/opt/ecommerce/.env.prod` va rebuild/redeploy frontend image.

### 3A.3 Test FE Storefront Public Routes

Chua can login. Mo bang browser va tick pass/fail:

| Route FE | URL | Thao tac | Pass khi |
|---|---|---|---|
| Home | `https://app.${ELASTIC_IP_DASHED}.nip.io` | Mo trang chu | Trang load, khong 502/404, khong trang Caddy default |
| Products | `/products` | Xem list, doi page/filter neu co | Co danh sach product, UI khong vang loi runtime |
| Product detail | `/products/<slug>` | Click 1 product tu list | Hien ten, gia, anh/spec/review/stock neu co |
| Search | `/search` | Nhap keyword va search | Co ket qua hoac empty state dung, Network khong 500 |
| Compare | `/compare` | Them 2-3 product de so sanh | Bang compare hien product/spec/gia |
| Flash sales | `/flash-sales` | Mo list campaign | Hien campaign hoac empty state dung |
| Flash sale detail | `/flash-sales/<id>` | Click campaign neu co | Hien campaign, stock/sold/status |
| Content page | `/content/<slug>` | Mo slug content co san | Hien page/post, neu slug sai thi 404/empty state hop ly |

Checklist FE public:

- Header/nav hien dung.
- Link chuyen trang khong reload sai domain.
- Anh/product card khong bi vo layout.
- Loading state khong treo vo han.
- Empty state co hien thong bao dung, khong crash.
- Network khong co request FE nao `500`.
- Console khong co runtime error nghiem trong.

### 3A.4 Test FE Auth Routes

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/login
https://app.${ELASTIC_IP_DASHED}.nip.io/register
```

Test login:

1. Mo `/login`.
2. Nhap user demo.
3. Submit.
4. Sau login, FE chuyen ve home/profile/cart hoac route truoc do.
5. Mo DevTools -> Application -> Cookies.
6. Kiem tra co cookie session/auth cua app. Khong can mo gia tri token.
7. Refresh page.
8. User van dang login.

Pass khi:

- `/api/auth/login` tra 200.
- `/api/auth/session` sau login tra user/session hop le.
- Header doi trang thai sang user da login.
- Refresh khong bi mat login.
- Logout xoa session va quay ve trang public/login.

Test register neu can:

1. Mo `/register`.
2. Dang ky user demo moi.
3. Sau register/login, vao Keycloak hoac user-service de doi chieu user neu can.

Neu login FE loi nhung API auth dung:

- Xem Network `/api/auth/login` response.
- Kiem tra cookie domain/path/secure.
- Kiem tra `NEXT_PUBLIC_APP_URL` co dung `https://app.${ELASTIC_IP_DASHED}.nip.io` khong.
- Kiem tra Keycloak issuer/callback URL.

### 3A.5 Test FE Account/User Routes

Can login user thuong.

| Route FE | URL | Thao tac | Pass khi |
|---|---|---|---|
| Profile | `/profile` | Mo profile | Hien thong tin user, sua/luu neu UI co |
| Addresses | `/addresses` | Tao/sua/xoa dia chi | Dia chi moi hien lai sau refresh |
| Cart | `/cart` | Add product, tang/giam quantity, xoa item | Tong tien/quantity cap nhat dung |
| Checkout | `/checkout` | Chon address, voucher, COD | Submit tao order, chuyen sang order/result/detail |
| Order result | `/order/result` | Mo sau checkout/payment return | Hien ket qua thanh toan/dat hang dung |
| Orders | `/orders` | Xem list order | Order moi xuat hien |
| Order detail | `/orders/<id>` | Mo order moi | Hien item, tong tien, status |

Thu tu test user FE de it loi nhat:

1. Login user.
2. Vao `/products`, add 1 product con stock.
3. Vao `/cart`, tang/giam quantity.
4. Vao `/addresses`, tao dia chi neu user chua co.
5. Vao `/checkout`, chon COD.
6. Submit order.
7. Vao `/orders`, mo order detail.
8. Refresh page order detail sau vai giay de xem status.

Pass khi:

- UI khong bi logout giua chung.
- Cart van giu item sau refresh/login.
- Checkout khong bi double submit.
- Sau submit, user tim thay order moi trong `/orders`.
- Order detail status khop voi backend/admin.

### 3A.6 Test FE Review Flow

Dieu kien:

- User da login.
- User co order `CONFIRMED` chua product do.

Thao tac:

1. Mo `/orders`.
2. Chon order da confirmed.
3. Click product trong order hoac mo product detail.
4. Gui rating/comment.
5. Refresh product detail.
6. Review moi hien tren product.

Pass khi:

- User da mua moi review duoc.
- User chua mua bi FE/backend chan dung.
- Review moi hien trong product detail.
- Admin `/admin/reviews` thay review moi.

### 3A.7 Test FE Flash Sale Flow

Can co campaign active.

Thao tac:

1. Mo `/flash-sales`.
2. Chon campaign active.
3. Bam mua flash sale.
4. Neu mua thanh cong, vao `/orders` de xem order tao ra.
5. Neu het hang, UI hien sold-out/het hang dung.

Pass khi:

- Button mua disable/hien dung theo status campaign.
- Sold count/stock cap nhat sau mua hoac refresh.
- User khong mua vuot limit neu campaign co limit.
- Sold-out la ket qua hop le khi stock het, khong phai FE crash.

### 3A.8 Test FE Admin Routes

Can login admin co role admin.

Mo route goc:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/admin
```

Neu login admin thanh cong, test cac route:

| Route admin FE | URL | Thao tac | Pass khi |
|---|---|---|---|
| Dashboard | `/admin/dashboard` | Xem KPI/chart/recent orders | Dashboard load, khong vong loading vo han |
| Products | `/admin/products` | Tao/sua product test | Product hien tren storefront/search sau do |
| Inventory | `/admin/inventory` | Stock-in product/SKU | Quantity cap nhat, movement hien neu co |
| Orders | `/admin/orders` | Mo order detail/filter status | Order moi tu checkout hien ra |
| Vouchers | `/admin/vouchers` | Tao/deactivate voucher test | Voucher apply duoc/bi chan dung |
| Flash sales | `/admin/flash-sales` | Tao/sua campaign | Campaign hien o storefront `/flash-sales` |
| Reviews | `/admin/reviews` | Xem/xoa review test neu can | Review list load dung |
| Users | `/admin/users` | Xem user list/detail | User demo hien dung |

Test phan quyen FE admin:

1. Logout admin.
2. Login user thuong.
3. Mo `/admin/dashboard`.
4. Pass khi user thuong bi chuyen ve login/forbidden/home, khong xem duoc admin data.
5. Login lai admin.
6. Mo `/admin/dashboard`, admin vao duoc.

Neu admin UI load nhung data trong:

- Mo DevTools Network.
- Filter `admin`.
- Xem request nao 401/403/500.
- Neu 401: session/cookie/token loi.
- Neu 403: role admin trong Keycloak chua dung.
- Neu 500: backend service tuong ung loi, xem log service do.

### 3A.9 Test FE Bang DevTools Network

Khi test FE, voi moi luong quan trong nen xem them Network:

| Luong FE | Request can thay | Status mong doi |
|---|---|---|
| Login | `POST /api/auth/login` | 200 |
| Session sau refresh | `GET /api/auth/session` | 200 |
| Products | `GET /api/proxy/products...` | 200 |
| Search | `GET /api/proxy/search...` | 200 |
| Cart | `GET/POST/PUT /api/proxy/cart...` | 200/201 |
| Address | `GET/POST/PUT /api/proxy/users/me/addresses...` | 200/201 |
| Checkout | `POST /api/proxy/orders` | 200/201 |
| Orders | `GET /api/proxy/orders...` | 200 |
| Admin products | `/api/proxy/products...` voi admin mutation | 200/201 |
| Admin orders | `/api/proxy/orders/admin...` | 200 |
| Admin inventory | `/api/proxy/inventory/admin...` | 200 |
| Admin flash sales | `/api/proxy/flash-sales...` | 200/201 |

Neu FE hien loi nhung Network 200:

- Loi co the nam o render/state FE.
- Xem Console error.
- Thu refresh hard `Cmd+Shift+R`.
- Thu incognito de loai cache/session cu.

Neu Network 401/403:

- Test lai login/session.
- Kiem tra role Keycloak.
- Kiem tra cookie co bi browser block khong.

Neu Network 500:

- Khong ket luan FE sai ngay.
- Copy endpoint loi.
- Sang cac muc sau cua guide de soi backend: Gateway log, Eureka, service log, DB/Kafka/Redis.

### 3A.10 Checklist FE Pass Truoc Khi Di Sau Backend

FE duoc coi la pass khi:

- `https://app.${ELASTIC_IP_DASHED}.nip.io` load qua HTTPS.
- `/products`, `/search`, `/compare`, `/flash-sales` load dung.
- User login/logout/register/session chay dung.
- User add cart, checkout COD, xem order duoc.
- User tao/sua address duoc.
- User review product da mua duoc.
- Admin vao `/admin/dashboard` duoc.
- Admin products/inventory/orders/vouchers/flash-sales/reviews/users load duoc.
- User thuong khong vao duoc admin.
- DevTools Network khong co request FE quan trong bi 500.
- Console khong co runtime error lam vo man hinh.

Sau checklist nay moi di tiep muc 4 tro di de show/kiem chung he thong sau FE: Eureka, Config Server, Gateway, Keycloak, Grafana, Prometheus, Zipkin, Kafka, DB, Redis, Elasticsearch.

---

## 4. Di Sau Service Registry Bang Eureka

Eureka la diem can show de chung minh microservices dang register that, khong chi frontend/API chay.

### 4.1 Mo Eureka

Mo:

```text
http://${ELASTIC_IP}:8761
```

Login bang:

- Username: xem `EUREKA_USER` trong `/opt/ecommerce/.env.prod`, mac dinh local la `eureka`.
- Password: xem `EUREKA_PASSWORD` trong `/opt/ecommerce/.env.prod`.

### 4.2 Nhung Thu Can Kiem Tra Tren Man Hinh Eureka

Trong bang `Instances currently registered with Eureka`, can thay cac app sau:

| App tren Eureka | Instance nen thay |
|---|---|
| `API-GATEWAY` | `api-gateway:api-gateway:8080` hoac tuong duong |
| `CONFIG-SERVER` | `config-server:config-server:8888` hoac tuong duong |
| `IDENTITY-SERVICE` | `identity-service:identity-service:8081` |
| `USER-SERVICE` | `user-service:user-service:8082` |
| `PRODUCT-SERVICE` | `product-service:product-service:8083` |
| `INVENTORY-SERVICE` | `inventory-service:inventory-service:8084` |
| `CART-SERVICE` | `cart-service:cart-service:8085` |
| `ORDER-SERVICE` | `order-service:order-service:8086` |
| `PAYMENT-SERVICE` | `payment-service:payment-service:8087` |
| `VOUCHER-SERVICE` | `voucher-service:voucher-service:8088` |
| `NOTIFICATION-SERVICE` | `notification-service:notification-service:8089` |
| `REVIEW-SERVICE` | `review-service:review-service:8090` |
| `SEARCH-SERVICE` | `search-service:search-service:8091` |
| `CONTENT-SERVICE` | `content-service:content-service:8092` |
| `FLASH-SALE-SERVICE` | `flash-sale-service:flash-sale-service:8093` |

Can nhin:

- Cot `Status` la `UP`.
- Khong co app critical nao bi mat.
- Port cua instance dung voi service map.
- `DS Replicas` khong can quan tam vi he thong chi dung mot Eureka server.

### 4.3 Neu Mot Service Khong Xuat Hien Tren Eureka

Thu tu debug:

1. Tren EC2 xem container co chay khong:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml ps <service-name>
```

2. Xem log service:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=160 <service-name>
```

3. Tim loi thuong gap:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=200 <service-name> \
  | grep -Ei 'ERROR|Exception|Connection refused|Cannot|failed|OutOfMemory|No servers available|Config'
```

4. Kiem tra service lay duoc config khong:

```bash
curl -s http://localhost:8888/<service-name>/docker | head -c 1000
```

Vi du:

```bash
curl -s http://localhost:8888/order-service/docker | head -c 1000
```

5. Kiem tra Eureka credentials trong env cua container:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T <service-name> env \
  | grep -E 'SPRING_PROFILES_ACTIVE|EUREKA_DEFAULT_ZONE|EUREKA_INSTANCE_HOSTNAME'
```

---

## 5. Di Sau Config Server

Config Server chung minh service khong hardcode toan bo cau hinh trong image.

### 5.1 Mo Bang Browser

Mo cac URL:

```text
http://${ELASTIC_IP}:8888/api-gateway/docker
http://${ELASTIC_IP}:8888/order-service/docker
http://${ELASTIC_IP}:8888/search-service/docker
http://${ELASTIC_IP}:8888/flash-sale-service/docker
```

Can thay JSON/YAML config tra ve. Neu browser hoi download/plain text cung duoc.

Port `8888` la debug port, khong mo public cho tat ca Internet. Security Group hien chi mo `tcp/8888` cho IP trong `aws/config.env`:

```text
MY_HOME_IP=<MY_HOME_IP>
```

Neu doi mang nha/4G va cac URL tren khong vao duoc, cap nhat `MY_HOME_IP` roi mo lai inbound rule:

```bash
source aws/config.env
aws ec2 authorize-security-group-ingress \
  --region "$AWS_REGION" \
  --group-id "$SECURITY_GROUP_ID" \
  --protocol tcp \
  --port 8888 \
  --cidr "${MY_HOME_IP}/32"
```

Neu AWS bao duplicate rule thi rule da ton tai, chi can thu lai browser/curl.

### 5.2 Diem Can Nhin

| Config | Can thay |
|---|---|
| `api-gateway/docker` | route `lb://...`, rate limiter, security/CORS |
| `order-service/docker` | datasource `order_db`, Kafka bootstrap, Eureka hostname |
| `search-service/docker` | Elasticsearch URI, Kafka consumer product events |
| `flash-sale-service/docker` | Redis host, Kafka bootstrap, scheduler/campaign config |

Khong can show secret. Neu config co bien dang `${POSTGRES_PASSWORD:...}` thi chi can noi service inject tu `.env.prod`.

### 5.3 Neu Config Server Loi

Tren EC2:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=160 config-server
curl -s http://localhost:8888/actuator/health
```

Neu Config Server down, cac service Spring Boot moi start lai co the fail vi khong import duoc config.

---

## 6. Di Sau API Gateway Va Swagger

Gateway la entrypoint backend cho frontend. Muc nay giup ban thay request di qua gateway, khong goi truc tiep service.

### 6.1 Mo Swagger

Mo:

```text
https://api.${ELASTIC_IP_DASHED}.nip.io/swagger-ui.html
```

Can thay cac nhom API:

- Auth: `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`
- User: `/api/users/me`, `/api/users/me/addresses`
- Product: `/api/products`, `/api/products/{id}`, category/brand
- Search: `/api/search`, `/api/search/suggestions`
- Cart: `/api/cart`, `/api/cart/items`, `/api/cart/merge`
- Voucher: `/api/vouchers`, `/api/vouchers/active`
- Order: `/api/orders`, `/api/orders/{id}`, `/api/orders/{id}/status`
- Payment: `/api/payments/vnpay/create`, `/api/payments/vnpay/return`
- Inventory admin: `/api/inventory/admin`, `/api/inventory/stock-in`, `/api/inventory/stock-out`
- Review: `/api/reviews`, `/api/reviews/product/{productId}`
- Content: `/api/content/banners`, `/api/content/posts`
- Flash sale: `/api/flash-sales`, `/api/flash-sales/{id}/purchase`

### 6.2 Kiem Tra Gateway Health Va Route

Browser:

```text
https://api.${ELASTIC_IP_DASHED}.nip.io/actuator/health
```

CLI tren laptop:

```bash
curl -s https://api.${ELASTIC_IP_DASHED}.nip.io/actuator/health
curl -s 'https://api.${ELASTIC_IP_DASHED}.nip.io/api/products?size=3&page=0' | jq
curl -s 'https://api.${ELASTIC_IP_DASHED}.nip.io/api/search?keyword=phone&size=3&page=0' | jq
```

Can thay:

- Health `UP`.
- Product/search tra JSON hop le.
- Neu endpoint public tra 401/403 la route/security config co van de.

### 6.3 Xem Gateway Log Theo Request

Tren EC2, mo mot terminal log:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs -f --tail=80 api-gateway
```

Sau do tren browser lam:

- Mo product list.
- Search keyword.
- Login.
- Checkout.

Can thay log gateway co request tuong ung. Neu log format co `traceId`, ghi lai trace id de tim trong Zipkin/log service khac.

---

## 7. Di Sau Auth Bang Keycloak

Keycloak quan ly user, role va token issuer. Day la diem can kiem tra khi admin guard, login, refresh token loi.

### 7.1 Mo Keycloak

Mo:

```text
https://auth.${ELASTIC_IP_DASHED}.nip.io
```

Login admin bang credential trong `.env.prod`:

- `KEYCLOAK_ADMIN`
- `KEYCLOAK_ADMIN_PASSWORD`

Chon realm:

```text
ecommerce
```

### 7.2 Kiem Tra Realm, Users, Roles

Trong Keycloak:

1. Realm settings:
   - Realm la `ecommerce`.
   - Issuer phai la URL public HTTPS: `https://auth.${ELASTIC_IP_DASHED}.nip.io/realms/ecommerce`.

2. Clients:
   - Tim client frontend/backend dang dung trong `.env.prod`, thuong la `ecommerce-client`.
   - Neu identity-service dung admin client, kiem tra client secret khop voi `.env.prod`.

3. Realm roles:
   - Can co role user/admin, trong app thuong map thanh `ROLE_USER`, `ROLE_ADMIN`.

4. Users:
   - User demo co role user.
   - Admin demo co role admin.
   - Email/username dung voi tai khoan ban dang login tren app.

### 7.3 Kiem Chung Phan Quyen Bang Browser

Lam bang tay:

1. Mo storefront, logout.
2. Login user thuong.
3. Mo `https://app.${ELASTIC_IP_DASHED}.nip.io/admin`.
4. Ky vong: user thuong bi chan, khong vao duoc dashboard admin.
5. Logout user thuong.
6. Login admin.
7. Mo `https://app.${ELASTIC_IP_DASHED}.nip.io/admin/dashboard`.
8. Ky vong: admin vao duoc dashboard.

Neu admin bi chan:

- Kiem tra role mapping trong Keycloak.
- Kiem tra token/cookie moi da refresh chua: logout/login lai.
- Kiem tra `identity-service` va `api-gateway` log.
- Kiem tra frontend app route `/api/auth/session`.

---

## 8. Chay Sau Cac Luong Storefront

Muc nay dung browser la chinh. Sau moi luong, co diem soi sau backend de biet service nao dang tham gia.

### 8.1 Catalog Va Product Detail

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/products
```

Thao tac:

1. Xem danh sach product.
2. Doi page/size neu UI co pagination.
3. Filter theo category/brand/gia neu co data.
4. Mo mot product detail.
5. Xem specs, image, stock, reviews.

Backend lien quan:

| Thao tac | Service |
|---|---|
| List/detail product | `product-service` |
| Stock hien thi | `inventory-service` hoac API inventory |
| Review/rating | `review-service` |
| Cache product | Redis neu service dung cache |

Kiem tra sau bang API:

```bash
curl -s 'https://api.${ELASTIC_IP_DASHED}.nip.io/api/products?size=5&page=0' | jq '.data.content[] | {id, sku, name, price}'
```

Neu UI trong:

- Kiem tra product-service health.
- Kiem tra DB `product_db.products`.
- Kiem tra gateway route `/api/products/**`.

### 8.2 Search Va Elasticsearch

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/search
```

Thao tac:

1. Nhap keyword co trong product name.
2. Thu search suggestions neu UI co.
3. So sanh ket qua voi product list.

Backend lien quan:

| Thanh phan | Vai tro |
|---|---|
| `product-service` | publish `product-created`, `product-updated`, `product-deleted` |
| Kafka | dua event product sang search |
| `search-service` | consume event va upsert Elasticsearch document |
| Elasticsearch | primary read model cho search |

Kiem tra search API:

```bash
curl -s 'https://api.${ELASTIC_IP_DASHED}.nip.io/api/search?keyword=phone&size=5&page=0' | jq
curl -s 'https://api.${ELASTIC_IP_DASHED}.nip.io/api/search/suggestions?keyword=ph' | jq
```

Neu product co trong catalog nhung search khong ra:

1. Vao Kafka xem topic product.
2. Xem log `search-service`.
3. Kiem tra Elasticsearch index o muc 14.

### 8.3 Compare Product

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/products
https://app.${ELASTIC_IP_DASHED}.nip.io/compare
```

Thao tac:

1. Chon 2-3 product de compare.
2. Mo compare page.
3. Kiem tra bang so sanh co name, price, brand, specs.

Backend lien quan:

- Compare chu yeu la frontend state + product API.
- Neu compare mat item khi refresh, day la han che frontend state/local storage, khong phai loi backend.

### 8.4 Guest Cart Va Merge Cart Khi Login

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/products
https://app.${ELASTIC_IP_DASHED}.nip.io/cart
```

Thao tac:

1. Logout.
2. Add mot product vao cart.
3. Mo cart, xac nhan item co trong guest cart.
4. Login user demo.
5. Quay lai cart, item van ton tai.
6. Tang/giam quantity.

Backend lien quan:

| Thao tac | Service |
|---|---|
| Add/update/delete cart item | `cart-service` |
| Guest cart/session cart | Redis |
| Login va merge | `identity-service`, `cart-service`, frontend BFF |
| Product validation | `cart-service` goi `product-service` |

Kiem tra API:

```bash
curl -i 'https://api.${ELASTIC_IP_DASHED}.nip.io/api/cart'
```

Neu chua login co the can cookie session tu browser nen CLI khong thay y het UI. Khi debug that, dung DevTools Network de xem request `/api/proxy/cart`.

### 8.5 Address Book Va Profile

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/profile
https://app.${ELASTIC_IP_DASHED}.nip.io/addresses
```

Thao tac:

1. Xem profile user.
2. Tao dia chi moi.
3. Sua dia chi.
4. Dat default neu UI co.
5. Xoa dia chi test neu can.

Backend lien quan:

- `user-service`: `user_profiles`, `delivery_addresses`.
- Gateway forward `X-User-Id` va role tu JWT.

Neu address tao xong khong hien:

- Refresh page.
- Kiem tra Network tab request `/api/users/me/addresses`.
- Kiem tra log `user-service`.

### 8.6 Voucher Va Checkout COD

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/cart
https://app.${ELASTIC_IP_DASHED}.nip.io/checkout
https://app.${ELASTIC_IP_DASHED}.nip.io/orders
```

Thao tac:

1. Login user.
2. Dam bao cart co product con stock.
3. Nhap voucher active neu co.
4. Vao checkout.
5. Chon dia chi.
6. Chon payment method COD.
7. Submit order.
8. Mo order detail.
9. Refresh vai giay den khi status ve trang thai terminal mong doi, thuong la `CONFIRMED`.

Backend lien quan theo thu tu:

```text
frontend checkout
  -> api-gateway
  -> order-service tao order PENDING + ghi outbox ORDER_CREATED
  -> Kafka topic order-created
  -> inventory-service reserve stock
  -> Kafka topic inventory-updated hoac inventory-failed
  -> order-service tao outbox PAYMENT_REQUESTED hoac ORDER_CANCELLED
  -> payment-service xu ly COD, ghi payment_outbox
  -> Kafka topic payment-success
  -> order-service confirm order, ghi ORDER_CONFIRMED
  -> inventory-service stock-out
  -> notification-service gui notification/email
```

Kiem tra sau trong admin:

- Admin orders co order moi.
- Admin inventory stock/reserved thay doi hop ly.
- Notification-service log co order confirmed.

Neu order dung o `PENDING`:

- Xem Kafka topic/consumer.
- Xem `order_service.outbox`.
- Xem `inventory_service.processed_events`.
- Xem log `order-service`, `inventory-service`, `payment-service`.

### 8.7 VNPAY Neu Can

VNPAY phu thuoc sandbox credential va callback public. Chay bang tay khi can:

1. Checkout chon VNPAY.
2. App redirect sang sandbox.
3. Thanh toan bang the test sandbox.
4. VNPAY return ve `app.${ELASTIC_IP_DASHED}.nip.io/order/result` hoac URL tu config.
5. Kiem tra order/payment status.

Neu sandbox loi, khong coi la loi core system neu COD flow da pass. Debug:

- `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, return URL trong `.env.prod`.
- Log `payment-service`.
- Gateway route `/api/payments/vnpay/**`.

### 8.8 Review Sau Khi Mua

Dieu kien:

- User phai co order `CONFIRMED` chua product do.

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/orders
https://app.${ELASTIC_IP_DASHED}.nip.io/products/<slug>
```

Thao tac:

1. Mo product da mua.
2. Gui rating/comment.
3. Refresh product detail de thay review/rating.
4. Vao admin reviews kiem tra review moi.

Backend lien quan:

- `review-service` goi `order-service` de check confirmed purchase.
- DB `review_db.reviews`.

Neu user chua mua ma review duoc la loi logic. Neu da mua ma review khong duoc:

- Kiem tra order status co `CONFIRMED`.
- Kiem tra `review-service` log.
- Kiem tra endpoint `/api/orders/user/{userId}/product/{productId}/confirmed`.

### 8.9 Content Page Va Banner

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/content/<slug>
```

Thao tac:

1. Vao admin content/page neu da co UI.
2. Tao hoac sua blog/page/banner.
3. Mo lai storefront page theo slug.
4. Xem banner/home page neu UI hien banner.

Backend lien quan:

- `content-service`
- DB `content_db.blog_posts`, `content_db.banners`

---

## 9. Chay Sau Admin Panel

Admin panel nam chung Next.js app, nhung route bat dau bang `/admin` va yeu cau role admin.

### 9.1 Dashboard

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/admin/dashboard
```

Can thay:

- KPI tong quan.
- Recent orders.
- Revenue/status charts neu co data.
- Low stock hoac canh bao ton kho neu co.

Backend lien quan:

- `order-service`: `/api/orders/admin/analytics/summary`, revenue, status-counts, top-products.
- `inventory-service`: low stock.
- `user-service`: admin users neu dashboard co thong ke user.

Neu dashboard trong:

- Tao vai order truoc.
- Set date range neu UI co.
- Kiem tra Network tab API nao fail.

### 9.2 Admin Products

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/admin/products
```

Thao tac:

1. Xem list product.
2. Tao product test neu can.
3. Sua gia/name/spec.
4. Luu.
5. Quay lai storefront/search de thay thay doi.

Backend lien quan:

- `product-service` ghi DB.
- Product event sang Kafka.
- `search-service` consume event de update Elasticsearch.

Neu product tao duoc nhung search khong thay:

- Kiem tra Kafka topic `product-created`/`product-updated`.
- Kiem tra `search-service` log.
- Kiem tra Elasticsearch index.

### 9.3 Admin Inventory

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/admin/inventory
```

Thao tac:

1. Tim SKU/product.
2. Stock-in mot so luong nho.
3. Kiem tra quantity tang.
4. Neu co movement history, xem movement moi.

Backend lien quan:

- `inventory-service`
- DB `inventory_db.inventory`, `inventory_db.stock_movements`

Neu checkout bao het hang trong khi admin thay con hang:

- Kiem tra reserved quantity.
- Kiem tra order dang pending giu hang.
- Kiem tra stock movements.

### 9.4 Admin Orders

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/admin/orders
```

Thao tac:

1. Xem order list.
2. Filter theo status neu UI co.
3. Mo order detail.
4. Doi status neu UI cho phep va phu hop.

Backend lien quan:

- `order-service`
- DB `order_db.orders`, `order_db.order_items`, `order_db.outbox`, `order_db.processed_events`

Can chu y:

- Checkout COD binh thuong nen di den `CONFIRMED`.
- Inventory fail nen order co the `CANCELLED`.
- Saga la async, status co the can vai giay.

### 9.5 Admin Vouchers

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/admin/vouchers
```

Thao tac:

1. Xem voucher list.
2. Tao voucher active nho de test.
3. Apply voucher trong checkout.
4. Quay lai admin xem usage/reservation neu UI co.

Backend lien quan:

- `voucher-service`
- DB `voucher_db.vouchers`, `voucher_db.voucher_reservations`, `voucher_db.voucher_usages`

Neu voucher apply loi:

- Kiem tra voucher active, thoi gian, min order, usage limit.
- Kiem tra order-service goi internal reserve/commit/release dung khong.

### 9.6 Admin Flash Sales

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/admin/flash-sales
https://app.${ELASTIC_IP_DASHED}.nip.io/flash-sales
```

Thao tac:

1. Tao campaign hoac chon campaign active.
2. Kiem tra product, stock, start/end time, limit per user.
3. Mo storefront flash sale.
4. User mua mot lan.
5. Refresh campaign de xem sold count.

Backend lien quan:

| Thanh phan | Vai tro |
|---|---|
| `flash-sale-service` | campaign, purchase endpoint |
| Redis | atomic stock reservation bang Lua |
| Kafka | topic `flash-sale-order-requested` |
| `order-service` | consume event va tao order flash sale |
| `inventory-service` | reserve/stock-out sau order |
| Reconciliation scheduler | dong bo sold count voi order count khi can |

Neu bam mua bi sold out:

- Co the la ket qua dung neu stock da het.
- Kiem tra `flash_sale_db.flash_sale_campaigns.sold_count`.
- Kiem tra Redis key flash sale o muc 13.

### 9.7 Admin Users, Reviews, Content

Mo:

```text
https://app.${ELASTIC_IP_DASHED}.nip.io/admin/users
https://app.${ELASTIC_IP_DASHED}.nip.io/admin/reviews
```

Can kiem tra:

- Users: danh sach user/profile tu `user-service`.
- Reviews: review moi tu `review-service`.
- Content pages/banners: neu version frontend deploy co UI content admin thi mo trong admin layout; neu khong, kiem tra bang Swagger/API content.

Frontend route trong repo hien co storefront `content/[slug]`. Neu admin content route chua hien tren menu, backend content-service van kiem duoc qua Swagger:

```text
https://api.${ELASTIC_IP_DASHED}.nip.io/swagger-ui.html
```

Tim cac endpoint:

- `/api/content/posts`
- `/api/content/posts/{slug}`
- `/api/content/banners/active`
- `/api/content/banners`

---

## 10. Di Sau Observability: Grafana, Prometheus, Zipkin

### 10.1 Grafana

Mo:

```text
https://grafana.${ELASTIC_IP_DASHED}.nip.io
```

Co the gap 2 lop login:

1. Caddy basic auth: user `admin`, password nam trong Caddy/Grafana basic auth config.
2. Grafana login: user thuong la `admin`, password la `GF_SECURITY_ADMIN_PASSWORD` trong `.env.prod`.

Sau khi vao Grafana:

1. Mo folder `E-commerce`.
2. Mo dashboard `Spring Boot Overview`.
3. Mo dashboard `JVM Overview`.
4. Mo dashboard `E-commerce Saga Overview`.
5. Set time range `Last 1 hour` hoac `Last 6 hours`.

Can thay:

| Dashboard | Can xem |
|---|---|
| Spring Boot Overview | HTTP throughput, p95/p99 latency, error rate theo job |
| JVM Overview | Heap/non-heap memory, GC, threads |
| E-commerce Saga Overview | throughput/latency cho `order-service`, `inventory-service`, `payment-service` |

Neu dashboard trong:

- Tao traffic: browse product, search, checkout 1 order.
- Doi Prometheus scrape 15-30 giay.
- Kiem tra Prometheus targets o muc 10.2.

### 10.2 Prometheus

Mo:

```text
http://${ELASTIC_IP}:9090/targets
```

Can thay targets `UP`:

- `api-gateway`
- `identity-service`
- `user-service`
- `product-service`
- `inventory-service`
- `cart-service`
- `order-service`
- `payment-service`
- `voucher-service`
- `notification-service`
- `review-service`
- `search-service`
- `content-service`
- `flash-sale-service`

Vao tab Graph/Query va chay cac query:

```promql
up
```

```promql
sum by (job) (rate(http_server_requests_seconds_count[5m]))
```

```promql
histogram_quantile(0.95, sum by (le, job) (rate(http_server_requests_seconds_bucket[5m])))
```

```promql
sum by (job) (jvm_memory_used_bytes{area="heap"})
```

Neu target `DOWN`:

1. Click target de xem error.
2. Kiem tra service actuator co expose `/actuator/prometheus`.
3. Tren EC2:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=120 prometheus
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T prometheus wget -qO- http://order-service:8086/actuator/prometheus | head
```

### 10.3 Zipkin

Trong production override, Zipkin **khong expose public host port**. Co 2 cach xem.

#### Cach A: SSH Tunnel Qua Container IP

Tren laptop, lay IP container Zipkin tren EC2:

```bash
cd "<PROJECT_ROOT>"
source aws/config.env

ssh -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP \
  "docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ecommerce-zipkin"
```

Lenh tren in ra mot IP noi bo Docker, vi du `172.x.x.x`. Dung IP do de mo tunnel:

```bash
ssh -N -L 19411:<ZIPKIN_CONTAINER_IP>:9411 -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP
```

Giu terminal tunnel nay mo. Tren browser laptop mo:

```text
http://localhost:19411
```

Neu he thong cua ban van expose host port 9411 trong mot lan deploy cu, tunnel don gian nay cung co the chay:

```bash
ssh -N -L 19411:localhost:9411 -i "$SSH_KEY_PATH" ubuntu@$ELASTIC_IP
```

Nhung voi `docker-compose.prod.yml` hien tai, cach dung container IP la dung hon.

#### Cach B: Dung Artifact Neu Khong Can Live UI

Mo:

```text
.test/results/zipkin-trace-detail-20260601-234035.png
.test/results/zipkin-order-service-traces-20260601-234021.json
```

Zipkin dung in-memory storage, nen sau restart EC2/Zipkin trace cu co the mat. Neu muon trace moi:

1. Mo tunnel.
2. Tao request moi: product search, checkout COD, flash sale purchase.
3. Trong Zipkin chon service `api-gateway`, `order-service` hoac `flash-sale-service`.
4. Lookback `15m` hoac `1h`.
5. Click trace de xem span tree/waterfall.

### 10.4 Correlate Log Voi Trace

Log pattern Spring Boot co `traceId`/`spanId`:

```text
[service-name,traceId,spanId]
```

De debug mot checkout:

1. Mo log gateway:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs -f --tail=80 api-gateway
```

2. Tao order tren browser.
3. Copy `traceId` tu log gateway.
4. Tim trong order/inventory/payment:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=300 order-service inventory-service payment-service \
  | grep '<TRACE_ID>'
```

5. Tim trace do trong Zipkin neu trace con ton tai.

---

## 11. Di Sau Kafka Va Event-Driven Saga

Kafka khong public ra Internet trong production. Soi bang SSH vao EC2 va dung tool trong container Kafka.

### 11.1 List Topics

Tren EC2:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T kafka \
  kafka-topics --bootstrap-server localhost:9092 --list
```

Topics quan trong can thay:

| Topic | Publisher | Consumer |
|---|---|---|
| `user-registered` | `identity-service` | `user-service` |
| `product-created` | `product-service` | `search-service` |
| `product-updated` | `product-service` | `search-service` |
| `product-deleted` | `product-service` | `search-service` |
| `order-created` | `order-service` outbox | `inventory-service` |
| `inventory-updated` | `inventory-service` | `order-service` |
| `inventory-failed` | `inventory-service` | `order-service` |
| `payment-requested` | `order-service` outbox | `payment-service` |
| `payment-success` | `payment-service` outbox | `order-service` |
| `payment-failed` | `payment-service` outbox | `order-service` |
| `order-confirmed` | `order-service` outbox | `inventory-service`, `notification-service` |
| `order-cancelled` | `order-service` outbox | `inventory-service`, `notification-service` |
| `flash-sale-order-requested` | `flash-sale-service` | `order-service` |
| `*.DLT` | Kafka error handler | Dead-letter evidence khi consumer loi |

### 11.2 Xem Consumer Groups

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T kafka \
  kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

Can thay groups:

- `user-service`
- `search-service`
- `inventory-service`
- `order-service`
- `payment-service`
- `notification-service`

Describe group:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T kafka \
  kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group order-service
```

Can xem:

- `LAG` gan `0` khi he thong idle.
- Neu lag tang va khong giam, consumer co van de.

### 11.3 Xem Message Gan Nhat

Chi nen consume topic nho hoac gioi han message:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T kafka \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-created --from-beginning --max-messages 3
```

Voi DLT:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T kafka \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-created.DLT --from-beginning --max-messages 1
```

Neu DLT co message, xem log consumer tuong ung va fix payload/logic.

### 11.4 Soi Outbox Khi Checkout

Order-service dung outbox table de tranh mat event khi Kafka down.

Tren EC2:

```bash
cd /opt/ecommerce
set -a
. ./.env.prod
set +a

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d order_db \
  -c "select id, aggregate_id, event_type, processed, created_at, processed_at from outbox order by created_at desc limit 10;"
```

Can thay:

- Event moi ban dau co the `processed = f`.
- Sau poller chay/Kafka OK, chuyen `processed = t`.
- Event type thuong gap: `ORDER_CREATED`, `PAYMENT_REQUESTED`, `ORDER_CONFIRMED`, `ORDER_CANCELLED`.

Payment-service co outbox rieng:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d payment_db \
  -c "select id, aggregate_id, event_type, topic, status, created_at, published_at from payment_outbox order by created_at desc limit 10;"
```

Can thay `status = PUBLISHED` khi event da duoc publish.

---

## 12. Di Sau Database PostgreSQL

PostgreSQL khong public. Soi bang `docker compose exec postgres`.

### 12.1 List Database

Tren EC2:

```bash
cd /opt/ecommerce
set -a
. ./.env.prod
set +a

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -c "\l"
```

Can co cac DB theo `init-db/01-create-databases.sql`:

- `user_db`
- `product_db`
- `inventory_db`
- `voucher_db`
- `order_db`
- `payment_db`
- `notification_db`
- `review_db`
- `content_db`
- `flash_sale_db`

Identity-service khong co `identity_db` rieng trong init-db hien tai; identity dung Keycloak va publish event `user-registered`. Neu mot DB business khong ton tai, service tuong ung se fail Flyway/datasource khi start.

### 12.2 Cac Query Nen Biet

Product:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d product_db \
  -c "select id, sku, name, price, active, created_at from products order by created_at desc limit 10;"
```

Inventory:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d inventory_db \
  -c "select sku, quantity, reserved_quantity, updated_at from inventory order by updated_at desc limit 10;"
```

Orders:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d order_db \
  -c "select id, user_id, status, payment_method, total_amount, created_at from orders order by created_at desc limit 10;"
```

Order items:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d order_db \
  -c "select order_id, product_id, sku, quantity, unit_price from order_items order by id desc limit 10;"
```

Payments:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d payment_db \
  -c "select id, order_id, payment_method, amount, status, provider, created_at from payments order by created_at desc limit 10;"
```

Vouchers:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d voucher_db \
  -c "select code, discount_type, discount_value, active, used_count, usage_limit from vouchers order by created_at desc limit 10;"
```

Flash sale:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d flash_sale_db \
  -c "select id, product_id, stock_quantity, sold_count, status, start_time, end_time from flash_sale_campaigns order by created_at desc limit 10;"
```

Reviews:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d review_db \
  -c "select id, user_id, product_id, rating, created_at from reviews order by created_at desc limit 10;"
```

Content:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d content_db \
  -c "select slug, title, published, updated_at from blog_posts order by updated_at desc limit 10;"
```

Notifications:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d notification_db \
  -c "select id, user_id, type, status, created_at from notifications order by created_at desc limit 10;"
```

### 12.3 Query Di Theo Mot Order Cu The

Khi ban tao mot order trong UI, copy order id. Sau do:

```bash
ORDER_ID="<PASTE_ORDER_ID>"

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d order_db \
  -c "select id, status, payment_method, total_amount, cancel_reason, created_at, updated_at from orders where id = '$ORDER_ID';"

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d order_db \
  -c "select event_type, processed, created_at, processed_at from outbox where aggregate_id = '$ORDER_ID' order by created_at;"

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d payment_db \
  -c "select order_id, payment_method, amount, status, created_at, updated_at from payments where order_id = '$ORDER_ID';"
```

Day la cach soi sau order saga ro nhat: UI status, DB order, outbox, payment phai khop nhau.

---

## 13. Di Sau Redis

Redis dung cho cart/cache/flash sale atomic stock. Redis khong public, vao qua EC2.

### 13.1 Kiem Tra Redis Health

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T redis redis-cli ping
```

Can thay:

```text
PONG
```

### 13.2 Xem Key Tong Quan

Dung `SCAN`, khong dung `KEYS *` khi data lon:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T redis \
  redis-cli --scan | head -50
```

Can thay key lien quan cart/cache/flash sale tuy data hien tai:

- Cart/session cart.
- Product cache neu dang co.
- Flash sale reservation/stock key.

### 13.3 Xem Redis Khi Cart

1. Logout tren browser.
2. Add product vao cart.
3. Tren EC2:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T redis \
  redis-cli --scan | grep -Ei 'cart|guest|session' | head -20
```

Neu khong thay key, co the key prefix khac hoac cart item da gan user id. Xem log `cart-service` va Network tab.

### 13.4 Xem Redis Khi Flash Sale

Sau khi mo/mua flash sale:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T redis \
  redis-cli --scan | grep -Ei 'flash|sale|campaign|stock|reservation' | head -50
```

Neu can xem TTL:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T redis \
  redis-cli ttl '<KEY>'
```

Neu Redis down:

- Product list Postgres-backed co the van 200.
- Cart/flash sale se fail hoac degrade.
- Phase 13 da co evidence kill Redis: product list 200, cart 500 trong outage, recover sau restart.

---

## 14. Di Sau Elasticsearch

Elasticsearch khong expose host port trong prod. Vao qua container.

### 14.1 Cluster Health

Tren EC2:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T elasticsearch \
  curl -s 'http://localhost:9200/_cluster/health?pretty'
```

Can thay:

- `status`: `green` hoac `yellow` voi single-node deu chap nhan duoc.
- `number_of_nodes`: `1`.

Neu image khong co `curl`, thu:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T elasticsearch \
  wget -qO- 'http://localhost:9200/_cluster/health?pretty'
```

### 14.2 Xem Indices

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T elasticsearch \
  curl -s 'http://localhost:9200/_cat/indices?v'
```

Can thay index product/search, ten co the la `products` hoac theo mapping cua `search-service`.

### 14.3 Search Thang Trong Elasticsearch

Neu index la `products`, chay:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T elasticsearch \
  curl -s 'http://localhost:9200/products/_search?q=phone&pretty' | head -80
```

Neu khong ro index:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T elasticsearch \
  curl -s 'http://localhost:9200/_cat/indices?h=index'
```

Neu API search khong co ket qua:

1. Kiem tra index co document khong.
2. Kiem tra Kafka product events.
3. Kiem tra log `search-service`.

---

## 15. Di Sau Notification Va Email

Production khong nen expose Mailpit UI public. Notification-service van co DB/log.

### 15.1 Xem Notification DB

Tren EC2:

```bash
cd /opt/ecommerce
set -a
. ./.env.prod
set +a

docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d notification_db \
  -c "select id, user_id, email, type, status, created_at from notifications order by created_at desc limit 10;"
```

### 15.2 Xem Log Notification

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=160 notification-service
```

Can thay khi order confirmed/cancelled:

- Consumer nhan `order-confirmed` hoac `order-cancelled`.
- Tao notification.
- Goi SMTP/Mail sender. Neu SMTP config demo khong gui that, DB/log van la evidence backend da xu ly event.

---

## 16. Chay Sau Theo Tung Luong Nghiep Vu Va Doi Chieu Backend

Bang nay la checklist nhanh khi ban vua thao tac tren UI xong.

| Luong | Browser thao tac | Soi sau nen lam |
|---|---|---|
| Login/register | `/login`, `/register` | Keycloak users, `identity-service` log, `user-service` consume `user-registered` |
| Catalog | `/products`, product detail | API `/api/products`, DB `product_db.products`, Prometheus job `product-service` |
| Search | `/search` | API `/api/search`, Kafka product topics, Elasticsearch index |
| Cart | `/cart` | Redis keys, `cart-service` log |
| Address/profile | `/addresses`, `/profile` | DB `user_db.delivery_addresses`, `user_db.user_profiles` |
| Voucher | checkout apply code | DB `voucher_db.vouchers`, reservations/usages |
| Checkout COD | `/checkout`, `/orders/<id>` | `order_db.orders`, `outbox`, Kafka groups, `payment_db.payments` |
| Inventory | admin inventory | `inventory_db.inventory`, `stock_movements` |
| Review | product review | `review_db.reviews`, `review-service` log |
| Flash sale | `/flash-sales`, purchase | Redis flash keys, Kafka `flash-sale-order-requested`, `flash_sale_db.flash_sale_campaigns` |
| Admin dashboard | `/admin/dashboard` | order analytics endpoints, Grafana/Prometheus |
| Content | `/content/<slug>` | `content_db.blog_posts`, `content_db.banners` |

---

## 17. Performance Va Resilience Evidence Da Co

Ket qua Phase 13 hien nam trong:

```text
.test/results/SUMMARY.md
```

So lieu quan trong da co artifact:

| Scenario | Ket qua |
|---|---|
| Catalog soak | 34 phut, max 200 VU, 55,239 iterations, p95 `61.68 ms`, p99 `85.48 ms`, error `0.00%` |
| Checkout stress | 7 phut, max 50 VU, 6,817 orders, p95 `160.92 ms`, order success `100%`, HTTP failure `0.00%` |
| Flash-sale spike | 500 VU, 100 stock, exactly 100 purchases, 36,134 sold-out responses, duplicate buyer `0 rows` |
| Kill order-service | Checkout fail trong downtime, health recover 200 sau restart |
| Kill Kafka | Outbox pending khi Kafka down, replay processed sau restart, final order `CONFIRMED` |
| Kill Redis | Product list van 200, cart fail trong outage, cart recover sau restart |
| Inventory failure | Order chuyen `CANCELLED`, inventory giu `quantity=0`, `reserved_quantity=0` |
| Observability | Prometheus transcript va Zipkin screenshot da co; Grafana screenshot can capture manual neu dua vao thesis |

Neu can mo raw evidence:

```text
.test/results/catalog-soak-20260530-215904.txt
.test/results/checkout-stress-20260531-022416.txt
.test/results/flash-sale-spike-20260531-114752.txt
.test/results/prometheus-phase13-metrics-20260601-234147.txt
.test/results/zipkin-trace-detail-20260601-234035.png
```

---

## 18. Troubleshooting Theo Trieu Chung

| Trieu chung | Kiem tra dau tien | Kiem tra tiep |
|---|---|---|
| `app...nip.io` 502 | `docker compose ps frontend`, `systemctl status caddy` | frontend log, Caddy log |
| `app...nip.io` hoac `:3000` hien Grafana | `docker compose ps frontend grafana` | `docker compose config` xem Grafana co con bind `3000` khong; prod override phai co `ports: !override` |
| `api...nip.io/actuator/health` down | `api-gateway` container/log | Eureka, Config Server, Redis rate limiter |
| Swagger `/swagger-ui.html` bi `401` | `api-gateway` security config | permit `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/webjars/**`; rebuild/recreate api-gateway |
| User login fail | Keycloak realm/client, `identity-service` log | issuer URL, client secret, cookie/session |
| Admin bi 403 | Keycloak role mapping | logout/login lai, gateway forwarded roles |
| Eureka thieu service | service container/log | config-server endpoint, Eureka default zone |
| Prometheus target down | target error trong `/targets` | actuator endpoint cua service |
| Grafana dashboard trong | Prometheus target/query | tao traffic, set time range `Last 1 hour` |
| Zipkin khong vao duoc | Zipkin port khong public | tunnel qua container IP, hoac dung artifact |
| Search khong ra product moi | Kafka product events | search-service log, Elasticsearch index |
| Checkout dung `PENDING` | `order_db.outbox` | Kafka lag, inventory/payment logs |
| Flash sale oversold/nghi ngo | `flash_sale_db.sold_count`, Redis key | order count by flash sale, Phase 13 artifact |
| Cart loi | Redis health | cart-service log, session/user cookie |
| Inventory sai | `inventory_db.inventory`, `stock_movements` | pending orders/reserved quantity |
| VNPAY callback loi | `.env.prod` VNPAY config | payment-service log, public return URL |

Lenh log tong hop hay dung:

```bash
cd /opt/ecommerce
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=200 api-gateway order-service inventory-service payment-service
```

Lenh tim loi:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs --tail=500 \
  | grep -Ei 'ERROR|Exception|OutOfMemory|failed|Connection refused|No servers available|timeout|DLT'
```

Lenh restart mot service:

```bash
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml restart <service-name>
```

---

## 19. Checklist Full He Thong Da San Sang

Danh sach nay dung de tu tick truoc khi quay/chay live.

| Muc | Dat khi |
|---|---|
| EC2 | Instance `${INSTANCE_ID}` dang `running` |
| Docker | Tat ca container production `Up` |
| Caddy | `active (running)`, HTTPS app/api/auth/grafana load duoc |
| FE storefront public | Home/products/product detail/search/compare/flash-sales/content load duoc |
| FE auth/account | Login/logout/session, profile, addresses, cart, checkout, orders load va thao tac duoc |
| FE admin | Admin login bang role admin; dashboard/products/inventory/orders/vouchers/flash-sales/reviews/users load duoc |
| FE permission | User thuong khong vao duoc admin routes |
| API | Gateway health `UP`, Swagger load duoc |
| Eureka | 15 app clients `UP`: gateway, config, 13 business services |
| Config Server | Mo duoc config cua gateway/order/search/flash-sale |
| Keycloak | Realm `ecommerce`, user/admin role dung |
| PostgreSQL | DB cua cac service ton tai, query order/product/inventory duoc |
| Redis | `PING -> PONG`, cart/flash-sale keys co khi thao tac |
| Kafka | Topics va consumer groups co, lag ve 0 khi idle |
| Elasticsearch | Cluster health `green/yellow`, index product co document |
| Prometheus | Targets cua gateway va 13 business services `UP` |
| Grafana | 3 dashboard `Spring Boot Overview`, `JVM Overview`, `E-commerce Saga Overview` co data |
| Zipkin | Mo duoc qua tunnel hoac co artifact trace |
| Checkout COD | Tao order moi va status ve `CONFIRMED` |
| Flash sale | Purchase thanh cong hoac sold-out dung voi stock |
| Review | User da mua gui review, admin xem duoc |
| Evidence | `.test/results/SUMMARY.md` va raw artifacts ton tai |

Khi checklist nay dat, he thong khong chi "vao duoc frontend", ma da kiem chung du cac lop: edge proxy, service discovery, config, auth, gateway, business services, database, cache, message broker, search engine, metrics, dashboard va tracing.
