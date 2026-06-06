# Thu muc `.docs` - Khung viet bao cao DATN

Thu muc nay la bo tai lieu lam viec de viet bao cao do an tot nghiep cho de tai:

**Xay dung he thong Thuong mai dien tu theo kien truc Microservices voi Spring Boot, Spring Cloud, Kafka, Redis, PostgreSQL, Elasticsearch, Keycloak va Docker Compose.**

Khac voi:

- `.report/`: ghi chu nghien cuu ly thuyet theo tung chu de.
- `.guide/`: huong dan cai dat, van hanh, demo va kiem tra he thong.
- source code: bang chung trien khai thuc te.

`.docs/` tap trung vao **cach bien toan bo tai lieu va source code thanh bao cao 6 chuong**.

## Cau truc de xuat

| File | Vai tro | Nen dung khi |
|---|---|---|
| [00-de-cuong-bao-cao-datn.md](./00-de-cuong-bao-cao-datn.md) | Ban do tong the 6 chuong, so trang, nguon tham chieu, checklist | Doc dau tien |
| [01-chuong-1-tong-quan-de-tai.md](./01-chuong-1-tong-quan-de-tai.md) | Khung viet Chuong 1 | Viet boi canh, muc tieu, pham vi, phuong phap |
| [02-chuong-2-co-so-ly-thuyet.md](./02-chuong-2-co-so-ly-thuyet.md) | Khung viet Chuong 2 | Tong hop ly thuyet cot loi, pattern va nguyen ly thiet ke duoc dung trong de tai |
| [03-chuong-3-giai-phap-cong-nghe.md](./03-chuong-3-giai-phap-cong-nghe.md) | Khung viet Chuong 3 | Giai thich cac cong nghe cu the duoc chon, ly do chon va cach ap dung |
| [04-chuong-4-phan-tich-thiet-ke-he-thong.md](./04-chuong-4-phan-tich-thiet-ke-he-thong.md) | Khung viet Chuong 4 | Phan tich yeu cau, kien truc, DB, API, Kafka, state machine |
| [05-chuong-5-cai-dat-trien-khai.md](./05-chuong-5-cai-dat-trien-khai.md) | Khung viet Chuong 5 | Mo ta cai dat code, Docker, Config, Gateway, service, observability |
| [06-chuong-6-kiem-thu-danh-gia.md](./06-chuong-6-kiem-thu-danh-gia.md) | Khung viet Chuong 6 | Lap test case, chay test, danh gia hieu nang, bao mat, resilience |
| [07-danh-muc-so-do-bang-bieu.md](./07-danh-muc-so-do-bang-bieu.md) | Danh muc so do, bang bieu, screenshot can ve/chup | Lap checklist hinh anh truoc khi viet ban chinh |
| [08-huong-dan-ve-so-do-bao-cao.md](./08-huong-dan-ve-so-do-bao-cao.md) | Huong dan cach ve tung so do, bang bieu va sequence theo source code | Dung khi bat dau ve hinh cho bao cao |
| [09-admin-panel.md](./09-admin-panel.md) | Mo ta Admin Panel Phase 14, role guard, modules, endpoint va gioi han | Viet phan frontend/admin trong Chuong 5 va phu luc screenshot |
| [01-phan-tich-kien-truc-microservices.md](./01-phan-tich-kien-truc-microservices.md) | Tai lieu phan tich kien truc chi tiet da co | Dung lam phu luc/tham khao sau cho Chuong 4 |

## Quy trinh viet bao cao

1. Doc [00-de-cuong-bao-cao-datn.md](./00-de-cuong-bao-cao-datn.md) de nam tong the 6 chuong.
2. Viet theo thu tu: Chuong 1 -> Chuong 3 -> Chuong 4 -> Chuong 5 -> Chuong 6 -> Chuong 2. Chuong 2 chi giu cac ly thuyet cot loi can de giai thich thiet ke; cac cong nghe cu the nhu Spring Boot, Kafka, Redis, PostgreSQL, Elasticsearch, Keycloak va Docker Compose de sang Chuong 3.
3. Moi muc lon can co it nhat mot trong ba bang chung: file `.report`, file `.guide`, hoac source code.
4. Moi so lieu hieu nang, latency, RPS, pass/fail chi lay tu ket qua da chon cho ban nop; cac kich ban mo rong khong dua vao ket qua chinh.
5. Moi so do nen ve lai bang PlantUML/Mermaid/draw.io, khong chup man hinh ASCII trong tai lieu.

## Nguyen tac quan trong

- Khong viet "da trien khai" neu khong co code, test, log hoac screenshot chung minh.
- Khong dua Kubernetes, service mesh, JaCoCo coverage hay cac kich ban load test mo rong vao phan ket qua neu khong nam trong pham vi final.
- Khong paste nguyen file code vao bao cao. Chi trich snippet ngan, co giai thich vi sao snippet do quan trong.
- Cac khai niem lon nhu Microservices, Saga, CAP, OAuth2, JWT, Outbox, Idempotency phai co tai lieu tham khao.
- Cac diem an diem cua de tai nen viet ky: Saga Orchestration, Transactional Outbox, Idempotent Consumer, Flash Sale Redis Lua, API Gateway security/rate limit, Observability.

## Nguon tham chieu nhanh

- Ly thuyet Microservices: `.report/01-kien-truc-microservices.md`
- Spring Boot/Spring Cloud: `.report/02-spring-boot-spring-cloud.md`
- Gateway: `.report/05-api-gateway.md`, `.guide/15-gateway-security.md`
- Bao mat Keycloak/JWT: `.report/06-bao-mat-keycloak-oauth-jwt.md`
- REST/OpenFeign va Kafka: `.report/07-*`, `.report/08-*`, `.guide/12-kafka-topics.md`
- Saga/Outbox/Idempotency: `.report/09-*`, `.report/10-*`
- Flash sale concurrency: `.report/13-flash-sale-concurrency.md`, `.guide/13-state-machines.md`
- Docker/Deploy: `.report/21-docker-container-deployment.md`, `.guide/03-build-va-chay-docker.md`
- Testing: `.report/20-testing-microservices.md`
