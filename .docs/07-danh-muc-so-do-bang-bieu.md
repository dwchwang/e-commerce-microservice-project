# Danh muc so do, bang bieu va screenshot can chuan bi

File nay la checklist hinh anh cho bao cao. Nen ve so do bang PlantUML, Mermaid hoac draw.io; screenshot chi dung cho man hinh chay thuc te nhu Eureka, Grafana, Zipkin, Mailpit, VNPAY.

## 1. So do cho Chuong 1

| Ma | Ten | Loai | Muc dich |
|---|---|---|---|
| H1.1 | Boi canh traffic spike trong e-commerce | Chart minh hoa | Giai thich van de flash sale |
| B1.1 | Monolith vs Microservices trong e-commerce | Bang | Neu ly do chon microservices |
| B1.2 | In-scope va out-of-scope | Bang | Lam ro pham vi |

## 2. So do cho Chuong 2

| Ma | Ten | Loai | Nguon |
|---|---|---|---|
| H2.1 | Monolith, SOA, Microservices | Architecture comparison | `.report/01` |
| H2.2 | Bounded Context va service ownership | Context map | `.report/01` |
| H2.3 | Service Discovery flow | Sequence | `.report/03` |
| H2.4 | Config Server flow | Sequence | `.report/04` |
| H2.5 | API Gateway filter chain | Flow | `.report/05` |
| H2.6 | OAuth2/JWT validation flow | Sequence | `.report/06` |
| H2.7 | REST vs Kafka communication | Comparison diagram | `.report/07`, `.report/08` |
| H2.8 | Saga orchestration model | Sequence/Concept | `.report/09` |
| H2.9 | Transactional Outbox pattern | Sequence | `.report/10` |
| H2.10 | Circuit Breaker state machine | State diagram | `.report/11` |
| H2.11 | Database per Service | Component | `.report/14` |
| H2.12 | Redis Lua atomic execution | Flow | `.report/13`, `.report/15` |
| H2.13 | Elasticsearch inverted index | Concept diagram | `.report/16` |
| H2.14 | Observability 3 pillars | Component | `.report/17`, `.report/18` |
| H2.15 | Docker Compose network concept | Deployment | `.report/21` |

## 3. So do cho Chuong 3

| Ma | Ten | Loai | Ghi chu |
|---|---|---|---|
| H3.1 | Technology solution overview | Component | Client -> Gateway -> Services -> Infra |
| B3.1 | Tech stack va phien ban | Bang | Lay tu `pom.xml`, `docker-compose.yml` |
| H3.2 | Spring Cloud solution | Component | Gateway, Eureka, Config |
| H3.3 | Communication strategy | Flow | REST/Feign va Kafka |
| B3.2 | Cong nghe -> ly do chon -> han che | Bang | Diem quan trong cua Chuong 3 |
| H3.4 | Persistence strategy | Component | PostgreSQL, Redis, Elasticsearch |
| H3.5 | Security solution | Sequence | Keycloak + Gateway + backend |
| H3.6 | Observability solution | Component | Actuator -> Prometheus/Grafana/Zipkin |

## 4. So do cho Chuong 4

| Ma | Ten | Loai | Bat buoc |
|---|---|---|---|
| H4.1 | Use Case tong the | Use Case | Co |
| H4.2 | Component diagram 4 lop | Component | Co |
| H4.3 | Context map 13 service | Context map | Co |
| B4.1 | Service ownership | Bang | Co |
| H4.4 | REST/Feign dependency graph | Component/Graph | Co |
| H4.5 | Kafka topic flow | Flow | Co |
| H4.6 | Gateway routing/security | Flow | Co |
| H4.7 | ERD user-service | ERD | Co |
| H4.8 | ERD product-service | ERD | Co |
| H4.9 | ERD inventory-service | ERD | Co |
| H4.10 | ERD order-service | ERD | Co |
| H4.11 | ERD payment-service | ERD | Co |
| H4.12 | ERD voucher-service | ERD | Co |
| H4.13 | ERD notification-service | ERD | Nen co |
| H4.14 | ERD review-service | ERD | Nen co |
| H4.15 | ERD content-service | ERD | Nen co |
| H4.16 | ERD flash-sale-service | ERD | Co |
| H4.17 | Redis key model cart/flash-sale | Data model | Co |
| H4.18 | Elasticsearch product document | Data model | Co |
| H4.19 | Sequence dang ky user | Sequence | Co |
| H4.20 | Sequence dat hang COD | Sequence | Co |
| H4.21 | Sequence dat hang VNPAY | Sequence | Co |
| H4.22 | Sequence cancel/compensation | Sequence | Co |
| H4.23 | Sequence flash sale purchase | Sequence | Co |
| H4.24 | Sequence product search CQRS | Sequence | Co |
| H4.25 | Order state machine | State | Co |
| H4.26 | Payment state machine | State | Co |
| H4.27 | Flash-sale campaign state machine | State | Co |
| B4.2 | Scheduler catalog | Bang | Co |

## 5. So do va screenshot cho Chuong 5

| Ma | Ten | Loai | Bang chung |
|---|---|---|---|
| H5.1 | Maven multi-module structure | Diagram | `pom.xml` |
| H5.2 | Docker Compose deployment | Deployment | `docker-compose.yml` |
| S5.1 | `docker compose ps` | Screenshot | Containers running |
| S5.2 | Eureka Dashboard | Screenshot | Services registered |
| S5.3 | Gateway Swagger UI | Screenshot | API docs |
| S5.4 | Keycloak realm/client/user | Screenshot | Auth setup |
| S5.5 | Prometheus targets | Screenshot | Metrics scrape |
| S5.6 | Grafana Spring/JVM/Saga dashboard | Screenshot | Monitoring |
| S5.7 | Zipkin trace | Screenshot | Distributed tracing |
| S5.8 | Mailpit email | Screenshot | Notification |
| S5.9 | VNPAY sandbox/return | Screenshot | Payment |
| B5.1 | Service -> source files quan trong | Bang | Code evidence |
| B5.2 | Docker service -> port -> healthcheck | Bang | Deploy evidence |

## 6. So do, bang va bang chung cho Chuong 6

| Ma | Ten | Loai |
|---|---|---|
| B6.1 | Test strategy matrix | Bang |
| B6.2 | Danh sach test tu dong trong repo | Bang |
| S6.1 | Ket qua `./mvnw test` | Screenshot/log |
| B6.3 | Functional test cases | Bang |
| B6.4 | Saga happy path verification | Bang |
| B6.5 | Saga compensation verification | Bang |
| B6.6 | Security test cases | Bang |
| B6.7 | Resilience test cases | Bang |
| H6.1 | Performance test setup | Diagram |
| B6.8 | Performance/load test result | Bang, chi dien neu do that |
| S6.2 | Grafana during load test | Screenshot, neu co |
| B6.9 | Requirement vs implementation | Bang |
| B6.10 | Han che va huong khac phuc | Bang |

## 7. Goi y cong cu ve

| Cong cu | Phu hop |
|---|---|
| PlantUML | Use Case, Sequence, State, Component |
| Mermaid | Sequence/flow nhanh trong Markdown |
| draw.io | Deployment, context map, so do dep de dua vao Word |
| DBeaver/DataGrip | ERD tu database/schema |
| IntelliJ diagrams | Class/package diagram |
| Grafana/Zipkin/Prometheus UI | Screenshot runtime |

## 8. Checklist chat luong hinh anh

- [ ] Tat ca hinh co so thu tu va caption.
- [ ] Hinh khong bi mo, chu doc duoc khi in A4.
- [ ] So do thiet ke khong phu thuoc screenshot terminal.
- [ ] Screenshot runtime co ngay/ngu canh ro rang.
- [ ] Bang bieu co nguon du lieu.
- [ ] Khong dua hinh khong lien quan chi de tang so luong.
- [ ] Cac so do sequence khop voi code va topic/API that.
