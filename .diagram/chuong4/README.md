# Sơ đồ PlantUML - Chương 4

Các file trong thư mục này tương ứng với các placeholder hình ở Chương 4:

- `4_1_overall_use_case.puml`: Hình 4.1, biểu đồ Use Case tổng thể hệ thống.
- `4_2_four_layer_component_architecture.puml`: Hình 4.2, sơ đồ thành phần bốn tầng.
- `4_3_context_map_13_bounded_contexts.puml`: Hình 4.3, Context Map 13 Bounded Context.
- `4_4_main_database_erd.puml`: Hình 4.4, ERD các database chính.
- `4_5_redis_key_model.puml`: Hình 4.5, mô hình khóa Redis.
- `4_6_rest_feign_dependencies.puml`: Hình 4.6, phụ thuộc REST/Feign.
- `4_7_kafka_event_flow.puml`: Hình 4.7, luồng sự kiện Kafka.
- `4_8_gateway_security_flow.puml`: Hình 4.8, luồng xác thực và phân quyền qua API Gateway.
- `4_9_order_state_machine.puml`: Hình 4.9, máy trạng thái đơn hàng.
- `4_10_payment_flashsale_state_machines.puml`: Hình 4.10, máy trạng thái thanh toán và Flash Sale.
- `4_11_sequence_cod_saga.puml`: Hình 4.11, luồng đặt hàng COD theo Saga Orchestration.
- `4_12_sequence_vnpay_ipn.puml`: Hình 4.12, luồng đặt hàng VNPAY với IPN callback.
- `4_13_sequence_flash_sale_purchase.puml`: Hình 4.13, luồng mua Flash Sale với ba lớp bảo vệ.
- `4_14_sequence_cqrs_product_search.puml`: Hình 4.14, luồng tìm kiếm sản phẩm theo CQRS-lite.

Render PNG:

```bash
plantuml .diagram/chuong4/*.puml
```

Render SVG để đưa vào LaTeX:

```bash
plantuml -tsvg .diagram/chuong4/*.puml
```
