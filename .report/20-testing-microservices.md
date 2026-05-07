# 20. Testing Strategies cho Microservices

## 1. Mục Tiêu Nghiên Cứu

- Hiểu Test Pyramid (Mike Cohn) và Test Honeycomb (Spotify)
- Hiểu Unit, Integration, Contract, E2E test
- Hiểu Testcontainers — "test với real dependencies"
- Hiểu Mockito, MockMvc, WebTestClient

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Test Pyramid (Mike Cohn, 2009)

```
       /\
      /E2E\         ← ít, slow, brittle
     /------\
    /Integ.  \      ← vừa
   /----------\
  / Unit Test  \    ← nhiều, fast, focused
 /--------------\
```

Nguyên tắc: nhiều unit test, ít e2e — vì e2e test chậm, dễ flaky.

### 2.2. Test Honeycomb (Spotify) — Cho Microservice

```
   ┌──────┐
   │ Few  │  ← Integrated tests (e2e qua nhiều service)
   └──┬───┘
   ┌──▼──────────────────┐
   │  Many               │  ← Integration tests (1 service + DB/Kafka)
   │  Integration tests  │
   └──┬─────────────────┘
   ┌──▼───┐
   │ Some │  ← Implementation detail tests (unit pure)
   └──────┘
```

→ Phù hợp microservice hơn pyramid: Service nhiều I/O, code logic ít. Test integration **với real components** (Postgres, Kafka qua Testcontainers) cho tự tin cao hơn mock.

### 2.3. Loại test trong Spring Boot

| Loại | Mục đích | Tool |
|------|---------|------|
| **Unit** | Test 1 class, mock dependencies | JUnit 5 + Mockito |
| **Slice** | Test 1 lớp (Controller, Repository, ...) | `@WebMvcTest`, `@DataJpaTest` |
| **Integration** | Service + real DB/Kafka | Testcontainers |
| **Contract** | API contract giữa services | Spring Cloud Contract, Pact |
| **E2E** | Full system | Cucumber, Selenium, Postman |

### 2.4. Testcontainers

Library Java khởi động **Docker container** trong test:
```java
@Testcontainers
class OrderServiceIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
  
  @Container
  static KafkaContainer kafka = new KafkaContainer("confluentinc/cp-kafka:8.2.0");
  
  @DynamicPropertySource
  static void props(DynamicPropertyRegistry reg) {
    reg.add("spring.datasource.url", postgres::getJdbcUrl);
    reg.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }
  
  @Test
  void shouldCreateOrder() { ... }
}
```

**Pros**:
- Test với DB thật → catch bug Hibernate/SQL
- Cleanup tự động
- CI/CD friendly (Docker available)

**Cons**:
- Chậm hơn pure unit (5–30s startup)
- Cần Docker

### 2.5. Mockito

Mock external dependencies trong unit test:
```java
@Mock InventoryClient inventoryClient;
@InjectMocks OrderService orderService;

@Test
void shouldCreateOrder() {
  when(inventoryClient.checkStock(any())).thenReturn(true);
  
  Order order = orderService.create(request);
  
  verify(inventoryClient).checkStock(any());
  assertThat(order.getStatus()).isEqualTo(PENDING);
}
```

### 2.6. Spring Boot Test Slice

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
  @Autowired MockMvc mockMvc;
  @MockBean OrderService orderService;
  
  @Test
  void shouldReturn201OnCreate() throws Exception {
    when(orderService.create(any())).thenReturn(mockOrder());
    
    mockMvc.perform(post("/api/orders")
                    .contentType(APPLICATION_JSON)
                    .content("{...}"))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").exists());
  }
}
```

### 2.7. Contract Testing (cho microservice)

Vấn đề: Service A đổi API → Service B fail nhưng không phát hiện đến deploy.

**Pact** / **Spring Cloud Contract**: Producer định nghĩa contract → consumer test against contract → producer verify contract trong CI.

→ Dự án không có contract test (overhead lớn cho đồ án), nhưng có thể nêu là "future work".

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. Maven test command

```bash
./mvnw test                              # all tests
./mvnw -pl order-service -am test         # one module + dependencies
./mvnw -pl flash-sale-service -am test
docker compose config --quiet              # validate compose file
```

Khi viết báo cáo, ghi rõ kết quả thực tế sau khi chạy lệnh: tổng số test, số test pass/fail, thời gian chạy, và module nào có test quan trọng. Không ghi coverage nếu chưa generate report.

### 3.2. Testcontainers smoke test

Dự án có Testcontainers smoke test với `postgres:17-alpine` (aligned với runtime version), hiện tập trung ở order-service để kiểm tra schema/migration với PostgreSQL thật.

```java
@SpringBootTest
@Testcontainers
class SmokeTest {
  @Container
  static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17-alpine");
  
  @Test
  void contextLoads() {
    // verify ApplicationContext start với DB thật
  }
}
```

### 3.3. Test naming convention

```java
// Pattern: should{Behavior}When{Condition}
@Test void shouldRejectOrderWhenInventoryInsufficient()
@Test void shouldCancelOrderAfterReservationExpiry()
@Test void shouldReturnFallbackWhenProductServiceDown()
```

### 3.4. JUnit 5 features

```java
@DisplayName("Order Saga Flow")
class OrderSagaTest {
  @Nested @DisplayName("Happy path")
  class HappyPath {
    @Test @DisplayName("Should confirm order after payment success") ...
  }
  @Nested @DisplayName("Compensation")
  class Compensation {
    @ParameterizedTest
    @ValueSource(strings = {"INVENTORY_FAIL", "PAYMENT_FAIL", "TIMEOUT"})
    void shouldCancelOrderOnFailure(String reason) ...
  }
}
```

### 3.5. Test cho Saga

Cách test full saga (without spinning all 13 services):
- **Unit**: Test orchestration logic (mock Kafka, Feign)
- **Integration**: Test 1 service + Testcontainers Kafka, verify event published correctly
- **E2E manual**: Postman collection chạy qua Docker compose, kiểm tra order state thay đổi

---

## 4. Test Coverage & Mutation Testing

| Tool | Mục đích |
|------|---------|
| **JaCoCo** | Code coverage report (line, branch) |
| **Pitest** | Mutation testing — thay đổi code, kiểm tra test có catch không |

→ JaCoCo/Pitest là khuyến nghị để nâng chất lượng báo cáo. Nếu repo chưa cấu hình JaCoCo thì không đưa ảnh coverage vào phần kết quả; có thể ghi là hướng cải tiến kiểm thử.

---

## 5. Từ Khóa Nghiên Cứu

```
- test pyramid mike cohn
- test honeycomb spotify microservices
- testcontainers junit 5
- mockito best practices
- spring boot test slice
- contract testing pact spring cloud contract
- mutation testing pitest
- test naming convention should given when
- jacoco code coverage
- chaos engineering microservices
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Tại sao chọn Testcontainers thay vì H2 in-memory DB?**
→ H2 không 100% compatible Postgres (JSONB, full-text, sequence). Test với Postgres thật catch bug schema, dialect. Trade-off: chậm hơn 5–10s startup.

**Q2: Mock vs real dependencies, khi nào?**
→ Unit test: mock để focus 1 unit. Integration test: real để verify wiring. Microservice nên có nhiều integration test hơn unit (test honeycomb).

**Q3: Em đo coverage bao nhiêu?**
→ Mục tiêu > 70% line cho business logic (order saga, payment, flash-sale). Auto-generated DTO không cần cover.

**Q4: Saga có khó test không?**
→ Có. Cách em test:
1. Unit: orchestrator logic (mock Kafka publisher)
2. Integration: full saga 1 service với Testcontainers Kafka
3. Cẩn thận với timing — dùng `Awaitility` await condition

**Q5: Có chaos test không?**
→ Đồ án không. Production: Chaos Monkey, Litmus — kill service ngẫu nhiên, kiểm tra resilience. Em có thể demo bằng `docker stop product-service` rồi xem Circuit Breaker hoạt động.

**Q6: E2E test khó duy trì, em tránh thế nào?**
→ Tập trung integration test cho mỗi service. E2E chỉ cho golden path (đặt đơn end-to-end). Postman collection chạy manual sau mỗi release.

---

## 7. Tài Liệu Tham Khảo

### Sách
- Mike Cohn, *Succeeding with Agile* — Test Pyramid
- Lasse Koskela, *Effective Unit Testing*, Manning
- Vladimir Khorikov, *Unit Testing Principles, Practices, and Patterns*, Manning, 2020

### Bài viết
- Martin Fowler — "Testing strategies in a microservice architecture"
- Spotify Engineering — "Testing of Microservices" (Test Honeycomb)
- Toby Clemson — "Testing Strategies in a Microservice Architecture" (martinfowler.com)

### Tools docs
- testcontainers.org
- junit.org/junit5
- mockito.org
- pact.io / spring-cloud-contract
