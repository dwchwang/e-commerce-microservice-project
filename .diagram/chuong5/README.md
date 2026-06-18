# Sơ đồ PlantUML - Chương 5

Các file trong thư mục này tương ứng với các placeholder hình ở Chương 5:

- `5_1_project_structure_modules.puml`: Hình 5.1, cấu trúc thư mục dự án và module chính.
- `5_2_spring_cloud_layer.puml`: Hình 5.2, Spring Cloud layer.
- `5_3_outbox_poller_kafka.puml`: Hình 5.3, OutboxPoller đọc outbox và phát Kafka.
- `5_4_flash_sale_redis_lua_reservation_compensation.puml`: Hình 5.4, Redis Lua reservation và compensation.
- `5_5_security_flow_gateway_keycloak_backend.puml`: Hình 5.5, security flow.
- `5_6_storefront_screen_flow.puml`: Hình 5.6, sơ đồ màn hình/luồng Storefront.
- `5_7_admin_panel_screen_flow.puml`: Hình 5.7, sơ đồ màn hình/luồng Admin Panel.
- `5_8_docker_compose_local_deployment.puml`: Hình 5.8, Docker Compose local deployment.
- `5_9_eureka_registered_services.puml`: Hình 5.9, mô phỏng Eureka dashboard service registry.
- `5_10_aws_ec2_production_like_deployment.puml`: Hình 5.10, AWS EC2 production-like stack.
- `5_11_github_actions_ghcr_pipeline.puml`: Hình 5.11, GitHub Actions build/push GHCR.
- `5_12_k6_performance_results.puml`: Hình 5.12, tổng hợp kết quả k6.
- `5_13_resilience_outbox_redis_recovery.puml`: Hình 5.13, resilience evidence.

Render PNG:

```bash
plantuml .diagram/chuong5/*.puml
```

Render SVG để đưa vào LaTeX:

```bash
plantuml -tsvg .diagram/chuong5/*.puml
```
