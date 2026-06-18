# Sơ đồ PlantUML - Chương 1

Các file trong thư mục này tương ứng với các placeholder hình ở Chương 1:

- `1_1_flash_sale_traffic_spike.puml`: Hình 1.1, minh họa traffic spike trong sự kiện Flash Sale so với ngày thường.
- `1_2_six_technical_pillars.puml`: Hình 1.2, sơ đồ sáu trụ cột kỹ thuật của hệ thống.

Render thử bằng PlantUML:

```bash
plantuml .diagram/chuong1/*.puml
```

Nếu muốn xuất SVG để đưa vào LaTeX:

```bash
plantuml -tsvg .diagram/chuong1/*.puml
```
