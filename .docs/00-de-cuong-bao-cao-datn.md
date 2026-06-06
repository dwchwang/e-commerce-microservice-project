# De cuong chi tiet bao cao do an tot nghiep

> De tai: Xay dung he thong Thuong mai dien tu theo kien truc Microservices voi Spring Boot 3.5, Spring Cloud 2025 va Java 21  
> Tac gia: Nguyen Duc Bao Hoang  
> Muc tieu tai lieu: lam khung viet bao cao chinh thuc theo 6 chuong.

## 0. Cach dung bo tai lieu nay

Moi chuong trong `.docs` duoc trinh bay theo 5 lop thong tin:

1. **Muc tieu chuong**: chuong do can chung minh dieu gi voi hoi dong.
2. **Cau truc muc/sub-muc**: co the dua gan nhu truc tiep vao bao cao.
3. **Noi dung can nghien cuu**: cac khai niem, luan diem, cau hoi can tra loi.
4. **So do/bang bieu/screenshot can chuan bi**: danh sach hinh anh de bao cao khong bi "noi chay".
5. **Nguon can doc**: map sang `.report`, `.guide` va source code.

Khi viet ban chinh, nen dung `.docs` nhu checklist, con noi dung chi tiet lay tu `.report`, `.guide` va code.

## 1. Cau truc tong the 6 chuong

```text
Loi cam on
Loi cam doan
Tom tat tieng Viet
Abstract tieng Anh
Muc luc
Danh muc hinh ve
Danh muc bang bieu
Danh muc tu viet tat

CHUONG 1: TONG QUAN DE TAI                         10-15 trang
CHUONG 2: CO SO LY THUYET                          30-40 trang
CHUONG 3: GIAI PHAP CONG NGHE                      18-25 trang
CHUONG 4: PHAN TICH & THIET KE HE THONG            30-40 trang
CHUONG 5: CAI DAT & TRIEN KHAI                     25-30 trang
CHUONG 6: KIEM THU & DANH GIA                      15-20 trang

Ket luan va huong phat trien                         3-5 trang
Tai lieu tham khao                                   3-5 trang
Phu luc                                             10-20 trang
```

Tong dung luong hop ly: **130-170 trang** neu tinh ca phu luc; phan chinh van nen giu trong khoang **120-150 trang**.

## 2. Map nhanh chuong -> noi dung -> nguon

| Chuong | Vai tro | Nguon chinh |
|---|---|---|
| 1. Tong quan de tai | Giai thich bai toan, muc tieu, pham vi, dong gop | `README.md`, `.guide/00`, `.report/00`, `.report/01`, `.report/22` |
| 2. Co so ly thuyet | Chung minh nen tang hoc thuat, pattern va nguyen ly thiet ke cot loi | `.report/01` den `.report/21` |
| 3. Giai phap cong nghe | Giai thich vi sao chon Java/Spring/Kafka/Redis/PostgreSQL/Elasticsearch/Keycloak/Docker va cach ap dung | `pom.xml`, `docker-compose.yml`, `.report/02-21`, `.guide/00`, `.guide/05` |
| 4. Phan tich & thiet ke | Phan tich yeu cau, kien truc, DB, API, event, state machine | `.docs/01-phan-tich-kien-truc-microservices.md`, `.guide/09`, `.guide/11-15`, source code |
| 5. Cai dat & trien khai | Mo ta cach hien thuc he thong bang code va Docker | source code cac service, `docker-compose.yml`, `config-server`, `prometheus`, `grafana`, `.guide/02-08` |
| 6. Kiem thu & danh gia | Chung minh he thong chay duoc va danh gia ket qua | `src/test`, `.report/20`, `.guide/04`, `.guide/09`, `.guide/10`, log/screenshot/ket qua do |

## 3. Danh sach diem an diem can lam noi bat

1. He thong tach thanh **13 business microservice** theo bounded context: identity, user, product, inventory, cart, order, payment, voucher, notification, review, search, content, flash-sale.
2. Co **Spring Cloud layer**: API Gateway, Eureka Discovery Server, Config Server.
3. Co **database per service** voi PostgreSQL cho cac service stateful, Redis cho cart/cache/flash-sale, Elasticsearch cho search.
4. Co **event-driven architecture** bang Kafka: order, inventory, payment, notification, product-search indexing, flash-sale.
5. Co **Saga Orchestration**: `order-service` la orchestrator cho luong dat hang.
6. Co **Transactional Outbox** va **Idempotent Consumer**: giam mat event va xu ly trung.
7. Co **Flash Sale high-concurrency**: Redis Lua atomic script, duplicate buyer set, compensation script, reconciliation scheduler.
8. Co **Gateway security**: JWT validation tu Keycloak, forward `X-User-*` headers, route public/authenticated, Redis rate limiter.
9. Co **VNPAY sandbox**: build URL, HMAC-SHA512, Return URL va IPN.
10. Co **Observability**: Actuator, Prometheus, Grafana, Zipkin, log correlation `traceId/spanId`.

## 4. Cac so do bat buoc nen co

Chi tiet xem [07-danh-muc-so-do-bang-bieu.md](./07-danh-muc-so-do-bang-bieu.md). Toi thieu nen co:

| Nhom | So do |
|---|---|
| Tong quan | Component diagram 4 lop, Deployment diagram Docker Compose |
| Yeu cau | Use Case diagram, Actor diagram |
| Domain | Context map 13 bounded context, bang service ownership |
| Data | ERD cho cac DB PostgreSQL, document model Elasticsearch, key model Redis |
| API/Event | Gateway routing diagram, REST/Feign dependency graph, Kafka topic flow |
| Nghiep vu | Sequence dang ky, dat hang COD, dat hang VNPAY, huy don, flash sale, search CQRS |
| Trang thai | Order state machine, Payment state machine, Flash-sale campaign state machine, inventory reservation |
| Van hanh | Observability flow, screenshot Eureka/Grafana/Zipkin/Mailpit/VNPAY |

## 5. Cau truc chi tiet tung chuong

### Chuong 1: Tong quan de tai

File lam viec: [01-chuong-1-tong-quan-de-tai.md](./01-chuong-1-tong-quan-de-tai.md)

Nen viet theo thu tu:

1. Dat van de: thuong mai dien tu, traffic spike, gioi han monolith.
2. Muc tieu tong quat va muc tieu cu the.
3. Pham vi de tai: in-scope va out-of-scope.
4. Doi tuong su dung va doi tuong nghien cuu.
5. Phuong phap nghien cuu.
6. Khao sat he thong/nen tang tuong tu.
7. Dong gop cua de tai.
8. Bo cuc bao cao.

Trong chuong nay, tranh di qua sau vao ky thuat. Chi can giai thich **vi sao bai toan dang lam la dang lam**.

### Chuong 2: Co so ly thuyet

File lam viec: [02-chuong-2-co-so-ly-thuyet.md](./02-chuong-2-co-so-ly-thuyet.md)

Noi dung nen gom:

1. Bai toan thuong mai dien tu va yeu cau he thong phan tan.
2. Microservices.
3. Domain-Driven Design va bounded context.
4. Nguyen ly giao tiep dong bo/bat dong bo, REST va event-driven architecture.
5. API Gateway pattern.
6. Service Discovery va centralized configuration o muc khai niem.
7. Bao mat ung dung va API: OAuth2, OIDC, JWT, RBAC, trusted subsystem.
8. Quan ly du lieu trong microservices: Database per Service, polyglot persistence, cache, read model, CQRS-lite.
9. Distributed Transaction, CAP/PACELC, eventual consistency va Saga.
10. Reliability patterns: Outbox, Idempotency, Circuit Breaker, Retry, Rate Limiter.
11. State machine, scheduler va high concurrency.
12. Observability: metrics, logs, traces.
13. Container hoa va trien khai dich vu.

Chuong 2 nen viet theo huong **ly thuyet/pattern truoc, lien he thiet ke sau**. Khong dua cac cong nghe cu the nhu Spring Boot, Spring Cloud, Kafka, Redis, PostgreSQL, Elasticsearch, Keycloak, Docker Compose thanh muc noi dung chinh; cac cong nghe nay de sang Chuong 3.

### Chuong 3: Giai phap cong nghe

File lam viec: [03-chuong-3-giai-phap-cong-nghe.md](./03-chuong-3-giai-phap-cong-nghe.md)

Day la chuong ban yeu cau tach rieng. Muc tieu la tra loi:

- Vi sao chon Java 21 va Spring Boot 3.5?
- Vi sao chon Spring Cloud Gateway/Eureka/Config Server?
- Vi sao chon Kafka thay vi chi REST?
- Vi sao chon PostgreSQL + Redis + Elasticsearch?
- Vi sao chon Keycloak thay vi tu viet auth?
- Vi sao Docker Compose phu hop voi pham vi DATN?

Chuong nay khong nen lap lai toan bo ly thuyet Chuong 2. Hay trinh bay theo dang **phuong an cong nghe - ly do chon - cach ap dung trong du an - han che**.

### Chuong 4: Phan tich & thiet ke he thong

File lam viec: [04-chuong-4-phan-tich-thiet-ke-he-thong.md](./04-chuong-4-phan-tich-thiet-ke-he-thong.md)

Noi dung chinh:

1. Phan tich yeu cau chuc nang va phi chuc nang.
2. Actor va Use Case.
3. Kien truc tong the.
4. Phan chia bounded context va 13 service.
5. Thiet ke database, Redis key, Elasticsearch document.
6. Thiet ke API, internal API va response format.
7. Thiet ke event/Kafka topics.
8. Thiet ke state machine.
9. Thiet ke luong nghiep vu chinh bang sequence diagram.
10. Thiet ke bao mat, resilience va observability.

Day la chuong phai co nhieu so do nhat.

### Chuong 5: Cai dat & trien khai

File lam viec: [05-chuong-5-cai-dat-trien-khai.md](./05-chuong-5-cai-dat-trien-khai.md)

Noi dung chinh:

1. Moi truong phat trien va phien ban cong nghe.
2. Cau truc Maven multi-module.
3. Cai dat Docker Compose infrastructure.
4. Cai dat Discovery Server va Config Server.
5. Cai dat API Gateway.
6. Cai dat tung business service.
7. Cai dat Kafka event, Outbox, scheduler.
8. Cai dat observability.
9. Quy trinh build/deploy local.
10. Danh sach screenshot demo.

Chuong nay nen dung nhieu bang mapping: service -> file code -> chuc nang -> bang chung.

### Chuong 6: Kiem thu & danh gia

File lam viec: [06-chuong-6-kiem-thu-danh-gia.md](./06-chuong-6-kiem-thu-danh-gia.md)

Noi dung chinh:

1. Chien luoc kiem thu microservices.
2. Unit test, integration test, E2E/manual test.
3. Test case cho cac luong nghiep vu.
4. Ket qua `mvnw test`.
5. Kich ban va ket qua performance test.
6. Resilience test.
7. Security test.
8. Danh gia muc do hoan thanh muc tieu.
9. Han che va rui ro con lai.

Trong ban final, khong tu tao so lieu. Chi dua cac ket qua performance/resilience da chon vao Chuong 6; cac kich ban mo rong de vao huong phat trien.

## 6. Thu tu lam viec de viet bao cao nhanh

1. Hoan thien danh muc so do trong [07-danh-muc-so-do-bang-bieu.md](./07-danh-muc-so-do-bang-bieu.md).
2. Ve truoc 8 so do cot loi: component, deployment, use case, Kafka flow, order saga, VNPAY, flash sale, state machines.
3. Viet Chuong 4 dua tren so do da ve.
4. Viet Chuong 5 dua tren code va screenshot chay he thong.
5. Viet Chuong 6 sau khi da chay test va gom bang chung.
6. Viet Chuong 3 de giai thich vi sao stack cong nghe phu hop voi thiet ke.
7. Viet Chuong 2 sau cung, chi giu nhung ly thuyet thuc su can de giai thich cac pattern da dung.
8. Quay lai Chuong 1 va Ket luan de dong goi dong gop.

## 7. Checklist truoc khi nop bao cao

- [ ] Da viet dung 6 chuong theo cau truc yeu cau.
- [ ] Chuong 3 da tach rieng phan "Giai phap cong nghe", khong tron vao Chuong 4.
- [ ] Co toi thieu 30 hinh/so do/screenshot.
- [ ] Co toi thieu 15 bang tong hop.
- [ ] Moi service quan trong co file source code minh chung.
- [ ] Moi pattern quan trong co ly thuyet va code minh chung.
- [ ] Khong co placeholder dang `X ms`, `Y RPS`, `TODO`, `...`.
- [ ] Cac so lieu hieu nang deu co ngay chay test, cau hinh may, script va screenshot.
- [ ] Tai lieu tham khao toi thieu 30 muc, uu tien sach, paper, RFC, official docs.
- [ ] Phu luc co API reference, Kafka topics, Flyway schema, Docker Compose, screenshot demo.

## 8. Phu luc nen co

| Phu luc | Noi dung |
|---|---|
| A | Huong dan cai dat va chay he thong |
| B | Docker Compose va bien moi truong |
| C | API reference/Postman/Swagger |
| D | Kafka topic catalog |
| E | Flyway DDL schema |
| F | Keycloak realm/client/role |
| G | VNPAY sandbox va test card |
| H | Ket qua test/log/screenshot |
| I | Cau hoi phan bien va tra loi ngan |
