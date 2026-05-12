# Chuong 1: Tong quan de tai

Muc tieu cua Chuong 1 la giup hoi dong hieu **bai toan nao dang duoc giai quyet, vi sao bai toan co y nghia, de tai lam den dau va dong gop nam o dau**. Chuong nay khong can di sau vao code, nhung moi khang dinh ve he thong phai khop voi repo hien tai.

Dung luong goi y: **10-15 trang**.

## 1.1. Dat van de

### Noi dung can viet

- Boi canh thuong mai dien tu: nhieu nguoi dung, nhieu giao dich, nhu cau tim kiem, dat hang, thanh toan, voucher, flash sale.
- Van de tai cao diem: campaign 11/11, Black Friday, gio vang, so luong request tang dot bien.
- Gioi han cua monolith:
  - Kho scale rieng tung module nhu search, order, flash-sale.
  - Loi o mot module co the anh huong toan he thong.
  - Deploy chung lam giam toc do phat trien.
  - Database chung kho dam bao ownership va thay doi schema.
- Huong tiep can cua de tai: microservices, event-driven, cache/Redis, Kafka, observability.

### Cau hoi can tra loi trong bai

- Vi sao e-commerce la bai toan phu hop de minh hoa microservices?
- Tai sao flash sale la tinh huong tieu bieu cho high concurrency?
- Vi sao khong chi can CRUD san pham/don hang don gian?

### So do/bang nen co

- Bang so sanh monolith va microservices trong bai toan e-commerce.
- Hinh minh hoa traffic spike trong flash sale, co the ve bang draw.io hoac bieu do gia lap, khong can so lieu thuc neu ghi ro la minh hoa.

### Nguon can doc

- `.report/01-kien-truc-microservices.md`
- `.report/13-flash-sale-concurrency.md`
- `README.md`

## 1.2. Muc tieu de tai

### Muc tieu tong quat

Xay dung mot he thong thuong mai dien tu theo kien truc microservices, co kha nang mo rong theo tung mien nghiep vu, tich hop bao mat, thanh toan, tim kiem, flash sale, event-driven processing va observability.

### Muc tieu cu the

1. Thiet ke he thong gom API Gateway, Discovery Server, Config Server va 13 business services.
2. Ap dung bounded context cho cac mien: identity, user, product, inventory, cart, order, payment, voucher, notification, review, search, content, flash-sale.
3. Ap dung REST/OpenFeign cho truy van dong bo va Kafka cho xu ly bat dong bo.
4. Trien khai order saga voi `order-service` lam orchestrator.
5. Su dung Transactional Outbox va processed-event table de tang do tin cay cua event processing.
6. Tich hop Keycloak/JWT cho xac thuc va phan quyen qua API Gateway.
7. Tich hop VNPAY sandbox cho luong thanh toan truc tuyen.
8. Su dung Redis cho cart, cache, rate-limit va atomic stock reservation trong flash sale.
9. Su dung Elasticsearch cho full-text search theo huong CQRS-lite.
10. Trien khai observability voi Actuator, Prometheus, Grafana va Zipkin.
11. Dong goi va chay local bang Docker Compose.

### Nguon can doc

- `.guide/00-tong-quan.md`
- `.guide/05-cac-dich-vu-va-cong.md`
- `pom.xml`
- `docker-compose.yml`

## 1.3. Pham vi de tai

### In-scope

| Nhom | Pham vi |
|---|---|
| Business services | 13 service: identity, user, product, inventory, cart, order, payment, voucher, notification, review, search, content, flash-sale |
| Spring Cloud | API Gateway, Eureka Discovery Server, Config Server |
| Persistence | PostgreSQL, Redis, Elasticsearch |
| Messaging | Kafka KRaft, cac topic nghiep vu |
| Security | Keycloak, OAuth2 Resource Server/JWT validation o Gateway, trusted identity headers |
| Payment | VNPAY sandbox, Return URL, IPN, HMAC-SHA512 |
| Observability | Actuator, Prometheus, Grafana, Zipkin, log traceId/spanId |
| Deployment | Docker Compose single-host local demo |
| Testing | Unit test, integration test co Testcontainers, manual/E2E scenario |

### Out-of-scope

| Hang muc | Ly do |
|---|---|
| Frontend web/mobile hoan chinh | De tai tap trung backend microservices |
| Kubernetes/Helm | Phu hop huong phat trien, khong phai pham vi chinh |
| Payment production | Chi dung sandbox de demo an toan |
| Logistics/refund production | Vuot qua pham vi backend e-commerce loi |
| CI/CD production | Neu repo chua co workflow that, dua vao huong phat trien |
| Service mesh/mTLS | Nang cao, phu hop huong phat trien |

## 1.4. Doi tuong su dung va doi tuong nghien cuu

### Doi tuong su dung

- Khach hang: dang ky/dang nhap, xem san pham, tim kiem, gio hang, dat hang, thanh toan, danh gia, mua flash sale.
- Quan tri vien: quan ly san pham, danh muc, thuong hieu, voucher, banner/blog, ton kho, chien dich flash sale.
- He thong ben ngoai: Keycloak, VNPAY, Mailpit/SMTP.

### Doi tuong nghien cuu

- Kien truc microservices cho e-commerce.
- Giao tiep dong bo va bat dong bo trong he phan tan.
- Xu ly giao dich phan tan bang Saga/Outbox/Idempotency.
- Chong oversell trong flash sale bang Redis atomic script.
- Observability va kiem thu he thong microservices.

## 1.5. Phuong phap nghien cuu

| Phuong phap | Cach ap dung trong de tai | Bang chung |
|---|---|---|
| Nghien cuu ly thuyet | Doc sach, paper, RFC, official docs ve microservices va distributed systems | `.report/` |
| Phan tich thiet ke | Chia bounded context, thiet ke API, event, DB, state machine | `.docs/04-*`, `.guide/09-15` |
| Thuc nghiem xay dung | Implement cac service bang Spring Boot/Spring Cloud | source code |
| Kiem thu | Unit, integration, manual flow, performance/resilience neu co | `src/test`, log, screenshot |
| Danh gia | So sanh ket qua voi muc tieu ban dau | Chuong 6 |

## 1.6. Khao sat he thong va giai phap tuong tu

### Nen khao sat

- Cac san thuong mai dien tu: Shopee, Lazada, Tiki, Amazon.
- Cac e-commerce open-source: Saleor, Spree, Medusa, Reaction Commerce.
- Cac reference architecture: microservices.io e-commerce patterns, Spring Cloud samples.

### Cach viet tranh lan man

Chi can so sanh theo cac tieu chi:

| Tieu chi | He thong tham khao | De tai nay |
|---|---|---|
| Kien truc | Monolith/modular/microservices | Microservices |
| Giao tiep | REST/event-driven | REST + Kafka |
| Search | SQL/Elasticsearch | Elasticsearch |
| Thanh toan | Payment gateway | VNPAY sandbox |
| Flash sale | High concurrency mechanism | Redis Lua atomic |
| Observability | Monitoring/tracing | Prometheus/Grafana/Zipkin |

## 1.7. Dong gop cua de tai

Nen trinh bay theo 3 nhom:

1. **Dong gop thiet ke**: mo hinh tham chieu cho backend e-commerce microservices.
2. **Dong gop ky thuat**: Saga, Outbox, Idempotency, Redis Lua flash-sale, Gateway security/rate-limit, observability.
3. **Dong gop thuc nghiem**: source code chay duoc bang Docker Compose, co test va screenshot minh chung.

Luu y: neu chua chay load test that, khong viet "da chung minh he thong chiu duoc 1000 user". Hay viet "he thong co co che thiet ke de giam rui ro oversell va can duoc danh gia bang kich ban tai trong o Chuong 6".

## 1.8. Bo cuc bao cao

Viet ngan gon:

- Chuong 1 trinh bay tong quan de tai.
- Chuong 2 trinh bay co so ly thuyet.
- Chuong 3 trinh bay giai phap cong nghe va ly do lua chon stack.
- Chuong 4 phan tich yeu cau va thiet ke he thong.
- Chuong 5 mo ta cai dat va trien khai.
- Chuong 6 trinh bay kiem thu, danh gia, han che.

## Checklist Chuong 1

- [ ] Co neu ro bai toan e-commerce va flash sale.
- [ ] Co muc tieu tong quat va muc tieu cu the.
- [ ] Co in-scope/out-of-scope.
- [ ] Khong dua so lieu thi truong neu khong co nguon trich dan.
- [ ] Co bang so sanh hoac hinh minh hoa.
- [ ] Co lien ket voi cac chuong sau.
