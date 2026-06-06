# 22. Tong Hop Phase 10-14 Cho Bao Cao DATN

File nay gom cac phase moi sau backend core. Muc tieu la giup dua noi dung AWS, CI/CD, Frontend, performance/resilience va admin panel vao bao cao mot cach dung muc, khong lan vao cac file ly thuyet.

## 1. Tom Tat Trang Thai

| Phase | Ten | Trang thai de viet bao cao | Nen dua vao chuong |
|---|---|---|---|
| 10 | AWS Infrastructure & Production Readiness | Da co ke hoach va artifact cau hinh production: `docker-compose.prod.yml`, CORS env-driven, JVM memory cap, reverse proxy/HTTPS theo huong Caddy/nip.io | Chuong 5 |
| 11 | CI/CD GitHub Actions | Da co workflow build/push GHCR va deploy EC2 bang GitHub Actions | Chuong 5 |
| 12 | Frontend Next.js Storefront | Storefront FE la phan ung dung nguoi dung, tich hop API qua BFF/proxy va deploy chung container | Chuong 5, screenshot demo |
| 13 | Performance & Resilience Testing | Da co so lieu k6 va resilience tren AWS: catalog soak, checkout stress, flash-sale spike, order-service recovery, Kafka replay, Redis degradation/recovery, inventory compensation | Chuong 6 |
| 14 | Admin Panel | Admin Panel Next.js da trien khai trong cung frontend, co ROLE_ADMIN guard va cac module quan tri chinh | Chuong 5, Chuong 6/demo, phu luc screenshot |

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

Dat muc "Quy trinh CI/CD va trien khai". Dua workflow, cau hinh repo va screenshot workflow/deploy vao bao cao neu chon chung lam hinh minh hoa.

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

Viet storefront theo muc da trien khai va gan voi screenshot/demo. Admin panel tach sang Phase 14 de khong tron luong nguoi dung va luong quan tri.

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
| Thoi gian | 2026-05-30 den 2026-06-01 |
| He thong | AWS EC2 chay Docker Compose production stack |
| Base URL | API Gateway trên EC2, đọc từ `aws/config.env` (`ELASTIC_IP`) |
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
| Kill Kafka | Pass: order duoc tao khi Kafka down, outbox row pending duoc ghi nhan, sau restart Kafka cac event duoc process va final order status la `CONFIRMED` | `.test/results/chaos-kafka-*-20260601-231131.*` |
| Kill Redis | Pass theo ky vong degradation: product list van 200, Redis-backed cart tra 500 trong outage, sau Redis restart product list va cart deu 200 | `.test/results/chaos-redis-*-20260601-231236.*` |
| Inventory failure compensation | Pass: order `4170422f-fc2d-49f8-aa5f-c7cd1d79266a` chuyen `CANCELLED`, inventory `quantity=0`, `reserved_quantity=0` | `.test/results/chaos-inventory-*-20260531-120641.*` |

## 6. Phase 14 - Admin Panel

### Noi dung da trien khai

- Admin panel nam trong cung Next.js codebase, route `/admin/*`.
- Bao ve bang `ROLE_ADMIN`.
- Module chinh: dashboard, products, inventory, orders, users, vouchers, flash-sales, content/banners/pages, reviews.
- Backend co cac admin controller/endpoint bo sung cho orders, inventory, users va reviews; cac module con lai dung endpoint mutation san co duoi Gateway.

### Cach viet trong bao cao

Nen dua Admin Panel vao phan ket qua trien khai, nhung van ghi dung gioi han demo:

- Admin Panel phuc vu demo quan tri san pham, kho, don hang, voucher, flash-sale, content va review.
- Upload anh dang theo cach paste URL, chua co object storage rieng.
- Moderation review va ban/unban user la huong mo rong neu can quy trinh production sau nay.

## 7. Mapping Vao Bao Cao 6 Chuong

| Chuong | Noi dung them sau Phase 10-14 |
|---|---|
| Chuong 1 | Cap nhat dong gop: deploy AWS, CI/CD, FE storefront, test performance/resilience co so lieu |
| Chuong 2 | Khong can them nhieu; chi lien he ly thuyet resilience/high concurrency voi ket qua Phase 13 |
| Chuong 3 | Them ly do chon AWS EC2 + Docker Compose, GitHub Actions, Next.js |
| Chuong 4 | Them actor Admin/Customer tren FE, sequence storefront/flash-sale neu can |
| Chuong 5 | Viet ky AWS, CI/CD, frontend container, admin panel, deployment/runbook |
| Chuong 6 | Dua bang k6, smoke backend readiness, resilience va danh gia han che production-like |

## 8. Cac Cau Khong Nen Viet Qua Da

| Khong nen viet | Viet dung hon |
|---|---|
| "He thong production-ready hoan toan" | "He thong duoc trien khai production-like tren single EC2, phu hop quy mo DATN" |
| "He thong khong bao gio mat event khi Kafka down" | "Trong resilience test da chay, outbox replay thanh cong sau khi Kafka restart" |
| "Redis outage khong anh huong he thong" | "Read path Postgres-backed van tra 200; Redis-backed cart degrade trong outage va recover sau restart" |
| "Admin panel dat chuan production CMS" | "Admin panel da du cho demo/quan tri chinh; upload object storage, moderation workflow va ban/unban user la huong mo rong" |
| "Flash-sale khong bao gio oversell" | "Trong test 500 VU/100 stock, ket qua durable cho thay 100 confirmed orders va 0 duplicate buyer" |
