# 22. Tong Hop Phase 10-14 Cho Bao Cao DATN

File nay gom cac phase moi sau backend core. Muc tieu la giup dua noi dung AWS, CI/CD, Frontend, performance/resilience va admin panel vao bao cao mot cach dung muc, khong lan vao cac file ly thuyet.

## 1. Tom Tat Trang Thai

| Phase | Ten | Trang thai de viet bao cao | Nen dua vao chuong |
|---|---|---|---|
| 10 | AWS Infrastructure & Production Readiness | Da co ke hoach va artifact cau hinh production: `docker-compose.prod.yml`, CORS env-driven, JVM memory cap, reverse proxy/HTTPS theo huong Caddy/nip.io | Chuong 5 |
| 11 | CI/CD GitHub Actions | Da co workflow build/push GHCR va deploy EC2 bang GitHub Actions | Chuong 5 |
| 12 | Frontend Next.js Storefront | Storefront FE la phan ung dung nguoi dung, tich hop API qua BFF/proxy va deploy chung container | Chuong 5, screenshot demo |
| 13 | Performance & Resilience Testing | Da co so lieu k6 va chaos/resilience tren AWS; mot so scenario con inconclusive | Chuong 6 |
| 14 | Admin Panel | Admin FE la phan con lai can hoan thien; nen ghi ro la pham vi tiep theo neu chua co UI day du | Ket luan/huong phat trien hoac muc gioi han |

## 2. Phase 10 - AWS Infrastructure

### Noi dung da them vao he thong

- Production deployment target la **single EC2 instance** chay Docker Compose, phu hop scope DATN va gioi han AWS Educate credits.
- CORS khong con hardcode local-only; gateway doc `CORS_ALLOWED_ORIGINS`.
- Production override tach khoi local compose de khong pha moi truong dev.
- JVM memory cap qua `JAVA_TOOL_OPTIONS`/compose thay vi sua tung Dockerfile.
- Huong van hanh co stop/start manual de tiet kiem credit va giu du lieu tren EBS.

### Cach viet vao Chuong 5

Nen trinh bay la **trien khai production-like single-host**, khong goi la production multi-node. Diem manh la he thong gom nhieu service that, co HTTPS/reverse proxy/CI/CD/monitoring; han che la chua co autoscaling, service mesh hay Kubernetes.

### Bang chung can dan

| Bang chung | Duong dan |
|---|---|
| Production override | `docker-compose.prod.yml` |
| CORS env-driven | `api-gateway/src/main/resources/application.yml`, `config-server/src/main/resources/configs/api-gateway.yml` |
| Ke hoach AWS | `.codex/plan/phase-10-aws-infrastructure.md` |

## 3. Phase 11 - CI/CD

### Noi dung da them vao he thong

- GitHub Actions build Maven/JAR va build Docker image.
- Push image len GHCR.
- Deploy manual qua `workflow_dispatch` len EC2 bang SSH.
- Secrets tach khoi image va nam trong GitHub Secrets/EC2 `.env.prod`.

### Cach viet vao Chuong 5

Dat muc "Quy trinh CI/CD va trien khai". Neu co screenshot workflow pass thi dua vao; neu chua co screenshot, chi mo ta theo file workflow va cau hinh repo.

### Bang chung can dan

| Bang chung | Duong dan |
|---|---|
| Build workflow | `.github/workflows/build-and-push.yml` |
| Deploy workflow | `.github/workflows/deploy.yml` |
| Ke hoach CI/CD | `.codex/plan/phase-11-cicd-github-actions.md` |

## 4. Phase 12 - Frontend Storefront

### Noi dung nen viet

- Frontend dung Next.js App Router, route group cho storefront/account/auth.
- BFF/proxy pattern giup token nam trong httpOnly cookie, browser khong can giu access token trong localStorage.
- Cac luong demo chinh: catalog, search, cart guest/user, checkout, orders, flash-sale, review.
- Storefront deploy chung EC2/frontend container, goi API qua Gateway.

### Luu y khi viet

Neu chua co screenshot day du, khong viet la "hoan thien toan bo UI". Nen viet theo muc da demo duoc va gan voi screenshot. Admin panel tach sang Phase 14.

### Bang chung can dan

| Bang chung | Duong dan |
|---|---|
| Ke hoach FE | `.codex/plan/phase-12-frontend-nextjs.md` |
| Source frontend | `frontend/` |
| CORS/API base URL | `frontend/.env.local.example` neu co, `api-gateway/src/main/resources/application.yml` |

## 5. Phase 13 - Performance & Resilience

Day la phan nen dua ky vao Chuong 6 vi da co ket qua thuc nghiem.

### Moi truong test

| Thanh phan | Gia tri |
|---|---|
| Thoi gian | 2026-05-30 den 2026-05-31 |
| He thong | AWS EC2 chay Docker Compose production stack |
| Base URL | `http://13.213.118.96:8080` |
| Load generator | Laptop chay k6 |
| Dataset | 100 products, 500 users/tokens, flash-sale stock 100 |

### Ket qua performance co the dua vao bao cao

| Scenario | Ket qua chinh | Bang chung |
|---|---|---|
| Catalog soak | 34 phut, max 200 VU, 55,239 iterations, 220,956 HTTP requests, p95 61.68 ms, p99 85.48 ms, error 0.00% | `.test/results/catalog-soak-20260530-215904.{json,txt}` |
| Checkout stress | 7 phut, max 50 VU, 6,817 iterations, 13,734 HTTP requests, order success 100.00%, p95 160.92 ms, error 0.00% | `.test/results/checkout-stress-20260531-022416.{json,txt}` |
| Flash-sale spike | 90s, max 500 VU, 78,448 requests, exactly 100 purchases cho 100 stock, 36,134 sold-out responses, confirmed orders 100, duplicate buyer 0 | `.test/results/flash-sale-spike-20260531-114752.{json,txt}` |

### Ket qua resilience nen viet trung thuc

| Scenario | Ket luan | Bang chung |
|---|---|---|
| Kill order-service | He thong fail trong downtime co chu dich, sau restart health 200; day la minh chung recovery, khong phai zero-downtime | `.test/results/chaos-order-kill-20260531-115007.*` |
| Kill Kafka | Da stop/start Kafka nhung order probe trong outage tra empty response; **chua verify outbox replay**, khong ghi la pass | `.test/results/chaos-kafka-action-20260531-115518.txt` |
| Kill Redis | Product list van 200 trong/after outage; cart probe inconclusive vi token expired 401 | `.test/results/chaos-redis-*-20260531-120329.txt` |
| Inventory failure compensation | Pass: order `4170422f-fc2d-49f8-aa5f-c7cd1d79266a` chuyen `CANCELLED`, inventory `quantity=0`, `reserved_quantity=0` | `.test/results/chaos-inventory-*-20260531-120641.*` |

## 6. Phase 14 - Admin Panel

### Noi dung da co trong scope

- Admin panel nam trong cung Next.js codebase, route `/admin/*`.
- Bao ve bang `ROLE_ADMIN`.
- Module du kien: dashboard, products, inventory, orders, users, vouchers, flash-sales, content/banners, reviews moderation.
- Phase nay chu yeu tieu thu backend endpoint da co, han che sua logic backend.

### Cach viet trong bao cao neu admin FE chua xong

Khong dua vao "ket qua dat duoc" nhu mot tinh nang hoan thien. Nen ghi:

- Storefront da dap ung demo nguoi dung.
- Admin FE la phan con lai/hang muc hoan thien tiep theo.
- Backend da co nen tang admin qua ROLE_ADMIN, gateway authorization va cac endpoint admin/service.

## 7. Mapping Vao Bao Cao 6 Chuong

| Chuong | Noi dung them sau Phase 10-14 |
|---|---|
| Chuong 1 | Cap nhat dong gop: deploy AWS, CI/CD, FE storefront, test performance/resilience co so lieu |
| Chuong 2 | Khong can them nhieu; chi lien he ly thuyet resilience/high concurrency voi ket qua Phase 13 |
| Chuong 3 | Them ly do chon AWS EC2 + Docker Compose, GitHub Actions, Next.js |
| Chuong 4 | Them actor Admin/Customer tren FE, sequence storefront/flash-sale neu can |
| Chuong 5 | Viet ky AWS, CI/CD, frontend container, deployment/runbook |
| Chuong 6 | Dua bang k6, smoke backend readiness, resilience va han che con lai |

## 8. Cac Cau Khong Nen Viet Qua Da

| Khong nen viet | Viet dung hon |
|---|---|
| "He thong production-ready hoan toan" | "He thong duoc trien khai production-like tren single EC2, phu hop quy mo DATN" |
| "Outbox da duoc verify khi Kafka down" | "Kafka stop/start da thuc hien, nhung outbox replay chua duoc verify bang probe day du" |
| "Redis outage khong anh huong he thong" | "Read path Postgres-backed van tra 200; cart degradation chua ket luan vi token probe expired" |
| "Admin panel da hoan thien" | "Admin panel la phan con lai can hoan thien, backend da co nen role/admin endpoint" |
| "Flash-sale khong bao gio oversell" | "Trong test 500 VU/100 stock, ket qua durable cho thay 100 confirmed orders va 0 duplicate buyer" |
