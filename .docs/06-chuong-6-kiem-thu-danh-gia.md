# Chuong 6: Kiem thu va danh gia

Muc tieu cua Chuong 6 la chung minh he thong **chay dung, co bang chung kiem thu va duoc danh gia trung thuc**. Trong ban final, cac muc ben duoi nen duoc viet theo dang ket qua da thuc hien: test tu dong, manual/E2E, security, performance, resilience va observability.

Dung luong goi y: **15-20 trang**.

## 6.1. Chien luoc kiem thu

### Noi dung can viet

- Test pyramid va test honeycomb cho microservices.
- Unit test: service logic, util, filter.
- Integration test: database/Testcontainers, Kafka neu co.
- Component test: tung service qua API.
- E2E/manual test: luong qua Gateway va Docker Compose.
- Resilience/security/performance test.

### Nguon

- `.report/20-testing-microservices.md`
- cac thu muc `src/test/`

## 6.2. Hien trang test tu dong trong repo

### Test files hien co nen thong ke

| Module | Test tieu bieu |
|---|---|
| common | `GlobalExceptionHandlerTest` |
| api-gateway | `AuthHeaderFilterTest`, `GuestSessionFilterTest`, `RateLimiterConfigTest` |
| order-service | `OrderServiceImplTest`, `OrderServicePostgresContainerTest` |
| payment-service | `PaymentServiceImplTest`, `VnPayUtilTest`, `PaymentEventOutboxPollerTest` |
| flash-sale-service | `FlashSaleServiceImplTest` |
| review-service | `ReviewServiceImplTest` |
| content-service | `BannerServiceImplTest`, `BlogPostServiceImplTest` |
| search-service | `ProductIndexServiceTest`, `ProductEventConsumerTest` |
| notification-service | `EmailServiceImplTest`, `NotificationServiceImplTest`, `EmailTemplateBuilderTest` |
| product-service | `ProductServiceTest` |

### Lenh chay

```bash
./mvnw test
```

Nen ghi:

- Thoi gian chay.
- Tong so test pass/fail.
- Moi truong chay: OS, Java version, Docker co bat hay khong.
- Neu co testcontainers, ghi image duoc dung.

## 6.3. Test case chuc nang

### Bang test case mau

| ID | Luong | Buoc kiem thu | Expected result | Bang chung |
|---|---|---|---|---|
| TC-01 | Dang ky user | POST `/api/auth/register` | Keycloak co user, user-service co profile | API response, DB, log Kafka |
| TC-02 | Dang nhap | POST `/api/auth/login` | Tra access token/refresh token | API response |
| TC-03 | Gio hang guest | Add item khong login | Co guest session, cart luu Redis | Header/cookie, Redis/log |
| TC-04 | Dat hang COD | POST `/api/orders` | Order confirmed, inventory tru ton, email gui | DB, Mailpit, Zipkin |
| TC-05 | Dat hang VNPAY success | Tao URL, thanh toan sandbox, return/IPN | Payment completed, order confirmed | VNPAY, DB, event |
| TC-06 | VNPAY timeout/fail | Khong thanh toan hoac hash sai | Payment failed/timeout, order cancelled | DB/log |
| TC-07 | Voucher het han/het luot | Dat hang voi voucher khong hop le | Order reject hoac voucher release | API response |
| TC-08 | Inventory khong du | Dat so luong vuot ton | Order cancelled, inventory-failed | Kafka/log |
| TC-09 | Search sync | Cap nhat product roi search | Elasticsearch co document moi | Search API |
| TC-10 | Review | User co confirmed order tao review | Review created; user khong du dieu kien bi reject | API response |
| TC-11 | Flash sale | Nhieu user mua slot gioi han | So don thanh cong khong vuot quantity | DB/Redis/k6 neu co |

### Nguon

- `.guide/09-luong-nghiep-vu.md`
- `.guide/11-api-tham-khao.md`
- `.guide/04-kiem-tra-he-thong.md`

## 6.4. Kiem thu luong Saga

### Luong COD happy path

Bang can co:

| Buoc | Service | Bang chung |
|---|---|---|
| Tao order | order-service | row `orders`, row `outbox` |
| Publish order-created | order-service | log OutboxPoller, Kafka topic |
| Reserve inventory | inventory-service | `reserved_quantity` tang |
| Payment COD completed | payment-service | row payment `COMPLETED` |
| Confirm order | order-service | status `CONFIRMED` |
| Confirm inventory | inventory-service | quantity tru that |
| Gui email | notification-service | Mailpit |

### Luong compensation

Nen test:

- Inventory failed -> order cancelled.
- Payment failed -> order cancelled, inventory release.
- Payment timeout -> scheduler cancel.
- Flash sale order create failed -> Redis compensate.

## 6.5. Kiem thu bao mat

### Test case can co

| ID | Kich ban | Expected |
|---|---|---|
| SEC-01 | Goi endpoint auth-required khong co token | 401 |
| SEC-02 | Goi endpoint voi JWT het han/sai issuer | 401 |
| SEC-03 | User goi endpoint admin | 403 |
| SEC-04 | Gateway forward `X-User-*` sau khi JWT hop le | Backend nhan dung user |
| SEC-05 | VNPAY callback sai secure hash | Reject |
| SEC-06 | Vuot rate limit flash-sale purchase | 429 |

### Nguon

- `.guide/15-gateway-security.md`
- `.report/06-bao-mat-keycloak-oauth-jwt.md`
- `.report/19-vnpay-payment-gateway.md`

## 6.6. Kiem thu resilience

### Kich ban da thuc hien/nen trinh bay

| ID | Cach test | Expected |
|---|---|---|
| RES-01 | Stop product-service khi order-service validate product | Feign fallback/circuit breaker, khong lam sap order-service |
| RES-02 | Stop Kafka sau khi tao order | Outbox giu row chua publish; restart Kafka thi poller publish lai va order tiep tuc confirm |
| RES-03 | Stop payment-service giua luong VNPAY | Order chua confirmed; scheduler timeout/cancel theo cau hinh |
| RES-04 | Duplicate Kafka event | Consumer bo qua event da co trong `processed_events` |
| RES-05 | Redis flash-sale reserve xong nhung order fail | Compensate stock va remove buyer set |

### Bang chung

- Log truoc/sau.
- DB snapshot.
- Screenshot Prometheus/Grafana neu co.

## 6.7. Kiem thu hieu nang va flash sale

### Nguyen tac

- Chi dua ket qua neu da chay bang script that.
- Ghi ro cau hinh may, so service replica, dataset, so user ao, thoi gian test.
- Trong ban final, tap trung vao cac kich ban da chon: catalog browse, checkout stress va flash-sale spike.

### Kich ban da thuc hien

| Scenario | Load | Endpoint | Chi so |
|---|---:|---|---|
| Catalog browse | Max 200 VU | `GET /api/products`, `GET /api/search` | p95/p99, error rate, throughput |
| Checkout stress | Max 50 VU | `POST /api/orders` | order success rate, p95, error rate |
| Flash sale spike | Max 500 VU, stock 100 | `POST /api/flash-sales/{id}/purchase` | success count = stock, confirmed orders, duplicate buyer = 0 |

### Ket qua flash-sale can chung minh

- So request thanh cong khong vuot `quantity`.
- `sold_count <= quantity`.
- Redis stock khong am.
- Moi user chi co toi da mot don trong campaign.
- So don trong `order-service` khop voi campaign sau reconciliation.

## 6.8. Kiem thu observability

### Can chup/manh chung

- Prometheus targets UP cho Gateway va 13 services.
- Grafana JVM/Spring Boot/Saga dashboard.
- Zipkin trace cho order flow.
- Log co `traceId/spanId`.
- Actuator health/readiness.

### Cau hoi can tra loi

- Khi mot request dat hang cham, dung Zipkin de biet cham o service nao nhu the nao?
- Khi error rate tang, Prometheus/Grafana the hien ra sao?
- Log correlation giup trace loi theo request nhu the nao?

## 6.9. Danh gia theo muc tieu ban dau

### Bang danh gia mau

| Muc tieu | Ket qua | Bang chung | Trang thai |
|---|---|---|---|
| 13 business microservice | Da co module va Docker service | `pom.xml`, `docker-compose.yml` | Dat |
| Gateway + Eureka + Config | Da trien khai | screenshot, source | Dat |
| Saga order | Da trien khai | sequence, log, DB | Dat |
| Outbox + Idempotency | Da trien khai | schema, poller, tests | Dat |
| Flash sale Redis Lua | Da trien khai | source, test/load test | Dat |
| VNPAY sandbox | Da trien khai | URL/callback screenshot | Dat |
| Frontend storefront/admin | Da trien khai | `frontend/`, screenshot/demo | Dat |
| AWS + CI/CD | Da trien khai | `docker-compose.prod.yml`, Caddy, GitHub Actions | Dat |
| Observability | Da cau hinh | Prometheus/Grafana/Zipkin | Dat |
| Performance result | Da co ket qua do | k6 report, dashboard/screenshot | Dat |

## 6.10. Han che va rui ro con lai

Nen viet trung thuc:

- Chay single-host bang Docker Compose, chua co Kubernetes/autoscaling.
- Frontend la web app phuc vu demo DATN; chua co mobile app rieng.
- CI/CD da co build/push/deploy len EC2, nhung chua phai GitOps/Kubernetes rolling deployment quy mo lon.
- Chua co contract testing giua cac service.
- Chua co schema registry cho Kafka event.
- Load test phu hop quy mo single-host DATN, chua phai capacity planning cho production lon.
- Bao mat production can HTTPS, secret manager, network isolation, audit log, token rotation.

## Checklist Chuong 6

- [ ] Da chay `./mvnw test` va ghi ket qua that.
- [ ] Co bang test case chuc nang.
- [ ] Co test saga happy path va compensation.
- [ ] Co test security 401/403/429/VNPAY hash.
- [ ] Co test resilience va neu ro ket qua Kafka/Redis/order-service/inventory compensation.
- [ ] Khong co so lieu gia.
- [ ] Co bang so sanh muc tieu ban dau voi ket qua dat duoc.
