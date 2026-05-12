# Chuong 2: Co so ly thuyet

Muc tieu cua Chuong 2 la trinh bay cac nen tang ly thuyet cot loi dung de hinh thanh thiet ke he thong. Chuong nay chi nen tra loi cac cau hoi: **khai niem la gi, van de nao can giai quyet, nguyen ly hoat dong, uu/nhuoc diem va vi sao ly thuyet do can cho de tai**.

Khong nen dua Chuong 2 thanh noi mo ta cong nghe cu the nhu Spring Boot, Spring Cloud, Kafka, Redis, PostgreSQL, Elasticsearch, Keycloak, Docker Compose hoac chi tiet source code. Cac noi dung nay se duoc trinh bay o Chuong 3 theo huong **giai phap cong nghe duoc lua chon va cach ap dung trong de tai**.

Dung luong goi y: **25-35 trang**.

## Nguyen tac viet Chuong 2

- Chi giu cac ly thuyet cot loi gan truc tiep voi thiet ke trong de tai.
- Moi muc nen viet theo cau truc: khai niem -> van de giai quyet -> nguyen ly/pattern -> uu diem/han che -> lien he voi de tai.
- Uu tien ly thuyet, pattern va nguyen ly thiet ke; khong giai thich sau cach cau hinh framework/tool.
- Moi muc lon can co tai lieu tham khao: sach, paper, RFC, official docs hoac tai lieu chuan.
- Moi muc lon nen co it nhat 1 bang hoac so do minh hoa.
- Phan "Lien he voi de tai" chi can neu ly thuyet do duoc dung cho thiet ke nao; khong di vao code, port, file cau hinh hay lenh chay.

## 2.1. Bai toan thuong mai dien tu va yeu cau he thong phan tan

### Noi dung can nghien cuu

- Dac diem cua he thong thuong mai dien tu: nhieu nhom chuc nang, nhieu loai nguoi dung, du lieu thay doi lien tuc.
- Cac luong nghiep vu cot loi: dang ky/dang nhap, quan ly san pham, gio hang, dat hang, thanh toan, khuyen mai, danh gia, tim kiem, thong bao.
- Yeu cau phi chuc nang: kha nang mo rong, tinh san sang, bao mat, tinh nhat quan du lieu, kha nang quan sat va kha nang bao tri.
- Thach thuc khi traffic tang dot bien, dac biet voi dat hang va flash sale.

### Lien he voi de tai

De tai can mot kien truc co the tach cac mien nghiep vu lon, xu ly giao tiep giua cac thanh phan, bao ve luong dat hang/thanh toan va ho tro cac tinh huong tai cao nhu flash sale.

### So do/bang can co

- Bang mapping yeu cau thuong mai dien tu -> thach thuc ky thuat.
- So do luong nghiep vu tong quat cua khach hang tu xem san pham den hoan tat don hang.

### Nguon

- `.report/00-*` neu co
- `.report/01-kien-truc-microservices.md`
- `README.md`

## 2.2. Kien truc Microservices

### Noi dung can nghien cuu

- Dinh nghia microservice theo Martin Fowler va Sam Newman.
- Dac trung: modeled around business capability, independently deployable, owns its data, decentralized governance, automation.
- So sanh Monolith, SOA va Microservices.
- Uu diem: scale doc lap, deploy doc lap, fault isolation, phu hop chia team theo mien nghiep vu.
- Han che: distributed complexity, network latency, data consistency, observability, testing, operational overhead.
- 8 Fallacies of Distributed Computing.

### Lien he voi de tai

Ly thuyet microservices la co so de chia he thong thanh cac service theo mien nghiep vu nhu identity, user, product, inventory, cart, order, payment, voucher, notification, review, search, content va flash-sale.

### So do/bang can co

- Bang so sanh Monolith/SOA/Microservices.
- Bang uu diem/han che cua microservices trong bai toan e-commerce.

### Nguon

- `.report/01-kien-truc-microservices.md`
- `README.md`

## 2.3. Domain-Driven Design va phan chia bounded context

### Noi dung can nghien cuu

- Domain, subdomain, bounded context, ubiquitous language.
- Entity, value object, aggregate va aggregate root.
- Context map va quan he giua cac bounded context.
- Nguyen tac service ownership: moi service so huu logic va du lieu cua mien nghiep vu rieng.
- Rui ro khi chia service sai ranh gioi: chatty communication, coupling cao, chia tach du lieu kho khan.

### Lien he voi de tai

DDD giup giai thich vi sao he thong duoc chia theo cac mien nhu order, payment, inventory, voucher va flash-sale, thay vi chia theo tang ky thuat nhu controller/service/repository.

### So do/bang can co

- Context map tong quat 13 bounded context.
- Bang service -> bounded context -> du lieu so huu -> chuc nang chinh.

### Nguon

- `.report/01-kien-truc-microservices.md`
- `.docs/01-phan-tich-kien-truc-microservices.md`

## 2.4. Nguyen ly giao tiep trong he thong phan tan

### Noi dung can nghien cuu

- Giao tiep dong bo va bat dong bo.
- RESTful API: resource, HTTP method, status code, statelessness, idempotency cua method.
- Event-driven architecture: event, producer, consumer, topic/channel, publish-subscribe.
- Message delivery semantics: at-most-once, at-least-once, exactly-once va exactly-once effect.
- Consumer group, ordering, duplicate message va eventual consistency.
- Tieu chi lua chon sync vs async: can response ngay, muc do coupling, latency, kha nang retry, side effect.

### Lien he voi de tai

He thong can giao tiep dong bo cho cac buoc can phan hoi ngay va giao tiep bat dong bo cho cac side effect, quy trinh dat hang, cap nhat tim kiem, thong bao va xu ly su kien.

### So do/bang can co

- Bang so sanh giao tiep dong bo va bat dong bo.
- So do publish-subscribe tong quat.
- Bang tieu chi chon REST hay event cho tung nhom nghiep vu.

### Nguon

- `.report/07-giao-tiep-rest-openfeign.md`
- `.report/08-event-driven-kafka.md`
- `.guide/12-kafka-topics.md`

## 2.5. API Gateway Pattern

### Noi dung can nghien cuu

- API Gateway la single entry point cho client.
- Cac nhiem vu cot loi: routing, authentication, authorization, rate limiting, CORS, request/response transformation, aggregation.
- Gateway trong he microservices va quan he voi service discovery.
- Public routes, authenticated routes va admin routes.
- Rui ro: gateway thanh bottleneck, single point of failure, logic nghiep vu bi day sai vao gateway.

### Lien he voi de tai

API Gateway Pattern la co so thiet ke lop vao duy nhat cho client, tap trung routing, xac thuc token, truyen thong tin nguoi dung noi bo va gioi han tan suat cho cac endpoint nhay cam.

### So do/bang can co

- So do request tu client qua gateway den service.
- Bang nhom route public/authenticated/admin.

### Nguon

- `.report/05-api-gateway.md`
- `.guide/15-gateway-security.md`

## 2.6. Service Discovery va cau hinh tap trung

### Noi dung can nghien cuu

- Van de service location trong moi truong nhieu service va dia chi thay doi.
- Client-side discovery va server-side discovery.
- Service registry, heartbeat, health check, service lookup.
- Externalized configuration va cau hinh theo moi truong.
- Lien he Twelve-Factor App: config nen tach khoi code.

### Lien he voi de tai

Ly thuyet service discovery va centralized configuration giai thich cach cac service tim thay nhau bang ten logic va cach quan ly cau hinh cho nhieu moi truong chay.

### So do/bang can co

- Sequence service dang ky vao registry.
- Sequence service lay cau hinh khi khoi dong.

### Nguon

- `.report/03-service-discovery-eureka.md`
- `.report/04-config-server.md`

## 2.7. Bao mat ung dung va API

### Noi dung can nghien cuu

- Authentication vs authorization.
- OAuth 2.0: resource owner, client, authorization server, resource server, access token.
- OpenID Connect va ID Token.
- JWT: header, payload, signature, claim, exp, issuer, audience.
- JWKS va signature verification.
- Role-based access control.
- Trusted Subsystem Pattern: gateway xac thuc request, backend chi tin nguon noi bo da duoc kiem soat.
- Cac rui ro can neu: token leakage, token expiration, expose service noi bo, thieu HTTPS, secret management.

### Lien he voi de tai

Ly thuyet OAuth2/OIDC/JWT va RBAC la co so de thiet ke xac thuc, phan quyen, bao ve API va truyen danh tinh nguoi dung tu gateway den cac service noi bo.

### So do/bang can co

- Sequence dang nhap va dung access token goi API.
- Bang authentication vs authorization.
- Bang cac nhom quyen nguoi dung trong he thong.

### Nguon

- `.report/06-bao-mat-keycloak-oauth-jwt.md`
- `.guide/15-gateway-security.md`

## 2.8. Quan ly du lieu trong microservices

### Noi dung can nghien cuu

- Database per Service.
- Data ownership va khong truy cap truc tiep database cua service khac.
- Polyglot Persistence: moi loai du lieu co the can mot mo hinh luu tru phu hop.
- ACID local transaction va gioi han khi buoc qua nhieu service.
- Schema migration va versioning.
- Cache, read model va full-text search nhu cac mo hinh ho tro hieu nang/truy van.
- CQRS-lite: tach command model va query/read model o muc vua phai.

### Lien he voi de tai

Ly thuyet nay la co so cho thiet ke moi service so huu du lieu rieng, dung luu tru quan he cho nghiep vu chinh, cache/bo dem cho du lieu truy cap nhanh va read model rieng cho tim kiem.

### So do/bang can co

- Bang service -> loai du lieu -> mo hinh luu tru phu hop.
- So do data ownership trong microservices.
- So do command side va query side cho tim kiem san pham.

### Nguon

- `.report/14-database-per-service-flyway.md`
- `.report/15-redis-cache-distributed-lock.md`
- `.report/16-elasticsearch-fulltext-search.md`

## 2.9. Distributed Transaction va Saga Pattern

### Noi dung can nghien cuu

- Vi sao transaction ACID cuc bo khong du khi moi service so huu database rieng.
- 2PC/XA va han che trong he microservices.
- CAP theorem va PACELC.
- Eventual consistency.
- Saga Pattern: chuoi local transaction va compensating action.
- Choreography vs Orchestration.
- Dieu kien de thiet ke compensation dung: buoc nao co the dao nguoc, buoc nao can doi soat thu cong, buoc nao can idempotency.

### Lien he voi de tai

Saga la co so thiet ke luong dat hang co nhieu buoc nhu tao don, giu hang, giu voucher, tao thanh toan, xac nhan hoac huy va bu cac tai nguyen da giu khi co loi.

### So do/bang can co

- Sequence Saga tong quat.
- Bang choreography vs orchestration.
- Bang local transaction va compensating action trong luong dat hang.

### Nguon

- `.report/09-saga-pattern-distributed-transaction.md`
- `.guide/09-luong-nghiep-vu.md`
- `.guide/13-state-machines.md`

## 2.10. Reliability Patterns

### Transactional Outbox

- Van de dual-write: ghi database thanh cong nhung publish event that bai, hoac nguoc lai.
- Outbox: ghi business data va event vao cung transaction cuc bo.
- Poller/relay doc outbox va phat event ra message broker.
- Uu diem: giam mat event; han che: co do tre va can xu ly trung.

### Idempotent Consumer

- Message co the bi giao lai trong co che at-least-once.
- Consumer can phat hien event da xu ly de tranh lap side effect.
- Muc tieu la exactly-once effect o tang nghiep vu.

### Circuit Breaker, Retry va Rate Limiter

- Circuit Breaker: CLOSED, OPEN, HALF_OPEN.
- Retry co gioi han, backoff va dieu kien retry.
- Rate Limiter bao ve endpoint nhay cam khoi qua tai hoac abuse.
- Can tranh retry storm va cascading failure.

### Lien he voi de tai

Cac pattern nay giup he thong dat hang, thanh toan, ton kho, thong bao va flash sale hoat dong ben vung hon trong dieu kien loi mang, loi service phu thuoc, event bi giao lai hoac request tang dot bien.

### So do/bang can co

- So do Transactional Outbox.
- Bang loi thuong gap -> pattern xu ly.
- State diagram Circuit Breaker.

### Nguon

- `.report/10-outbox-idempotency.md`
- `.report/11-resilience-circuit-breaker-rate-limit.md`
- `.guide/15-gateway-security.md`

## 2.11. State Machine, Scheduler va high concurrency

### Noi dung can nghien cuu

- Finite State Machine: state, transition, event, guard condition.
- Ap dung state machine cho don hang, thanh toan, chien dich khuyen mai/flash sale.
- Scheduler cho timeout, activation, reconciliation va background jobs.
- Race condition, lost update, duplicate request va oversell.
- Atomic operation, distributed lock/counter va compare-and-set o muc khai niem.
- Reconciliation giua bo dem nhanh va du lieu ben vung.

### Lien he voi de tai

State machine giup mo ta vong doi order, payment va campaign. Ly thuyet concurrency giup thiet ke co che giu slot flash sale, chan mua trung va doi soat lai du lieu khi co loi.

### So do/bang can co

- State machine order.
- State machine payment.
- State machine flash-sale campaign.
- Bang rui ro concurrency -> bien phap thiet ke.

### Nguon

- `.report/12-state-machine-scheduler.md`
- `.report/13-flash-sale-concurrency.md`
- `.guide/13-state-machines.md`
- `.guide/14-scheduler-jobs.md`

## 2.12. Observability trong he thong phan tan

### Noi dung can nghien cuu

- Monitoring vs observability.
- 3 tru cot: metrics, logs, traces.
- RED method va USE method.
- Health check, readiness/liveness o muc khai niem.
- Distributed tracing: trace, span, context propagation, W3C Trace Context.
- Log correlation bang traceId/spanId.
- Vai tro cua dashboard va alert trong van hanh.

### Lien he voi de tai

Microservices lam tang do kho khi debug vi mot request co the di qua nhieu service. Observability la co so de thiet ke health endpoint, metric, log correlation va trace cho cac luong nhu dat hang, thanh toan va flash sale.

### So do/bang can co

- So do metrics/logs/traces trong microservices.
- Sequence trace cua mot request qua gateway va nhieu service.
- Bang RED/USE metric can theo doi.

### Nguon

- `.report/17-observability-prometheus-grafana.md`
- `.report/18-distributed-tracing-zipkin.md`

## 2.13. Container hoa va trien khai dich vu

### Noi dung can nghien cuu

- Container vs virtual machine.
- Image, layer, container, network, volume, healthcheck.
- Multi-container application.
- Nguyen tac dong goi ung dung va phu thuoc ha tang.
- Gioi han cua single-host deployment so voi orchestration platform.

### Lien he voi de tai

Container hoa la co so de dong goi nhieu service va dependency ha tang trong mot moi truong demo/thuc nghiem nhat quan, phu hop pham vi do an.

### So do/bang can co

- Bang container vs VM.
- Deployment diagram muc khai niem cho multi-container microservices.

### Nguon

- `.report/21-docker-container-deployment.md`
- `.guide/03-build-va-chay-docker.md`

## Checklist Chuong 2

- [ ] Moi muc la ly thuyet/pattern cot loi, khong phai mo ta tool/framework.
- [ ] Cac cong nghe cu the duoc chuyen sang Chuong 3.
- [ ] Co citation cho cac khai niem lon.
- [ ] Co bang/so do cho moi nhom ly thuyet.
- [ ] Co doan lien he voi de tai sau moi muc.
- [ ] Khong copy nguyen van qua dai tu `.report`.
- [ ] Khong di vao chi tiet code, file cau hinh, port, lenh chay.
