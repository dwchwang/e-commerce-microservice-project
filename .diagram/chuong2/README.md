# Sơ đồ PlantUML - Chương 2

Các file trong thư mục này tương ứng với các placeholder hình ở Chương 2:

- `2_1_distributed_computing_fallacies.puml`: Hình 2.1, tám ngộ nhận trong tính toán phân tán và yêu cầu thiết kế.
- `2_2_ddd_bounded_context_aggregate_context_map.puml`: Hình 2.2, Bounded Context, Aggregate và Context Map.
- `2_3_saga_choreography_vs_orchestration.puml`: Hình 2.3, so sánh Saga Choreography và Saga Orchestration.
- `2_4_transactional_outbox_pattern.puml`: Hình 2.4, luồng Transactional Outbox Pattern.
- `2_5_circuit_breaker_state.puml`: Hình 2.5, state machine của Circuit Breaker.
- `2_6_trusted_subsystem_keycloak_gateway.puml`: Hình 2.6, Trusted Subsystem Pattern với Keycloak và API Gateway.
- `2_7_observability_three_pillars.puml`: Hình 2.7, ba trụ cột Observability.

Render PNG:

```bash
plantuml .diagram/chuong2/*.puml
```

Render SVG để đưa vào LaTeX:

```bash
plantuml -tsvg .diagram/chuong2/*.puml
```
