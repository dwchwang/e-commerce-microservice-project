# 03. Grafana — Dashboard & Visualization

> Grafana là **lớp visualization** trên Prometheus. Nó truy vấn PromQL và vẽ thành dashboard có panel, alert, biến (template variable). Trong project này, Grafana được **provisioning sẵn** — datasource và dashboard tự động load khi container start.

## 1. Khái niệm cốt lõi

| Khái niệm | Ý nghĩa |
|-----------|---------|
| **Datasource** | Nguồn dữ liệu Grafana đọc (Prometheus, Loki, Elasticsearch...) |
| **Dashboard** | Một file JSON chứa nhiều panel + biến |
| **Panel** | Một widget (timeseries, stat, gauge, table, heatmap...) |
| **Provisioning** | Cách load datasource/dashboard từ file YAML/JSON khi start, không cần click chuột |
| **Variable** | Dropdown trên đầu dashboard để filter (ví dụ chọn `job=order-service`) |
| **Alert** | Rule chạy định kỳ, gửi notification khi metric vượt ngưỡng |

## 2. Hệ thống đang dùng Grafana ra sao

### 2.1 Cấu hình thực tế

- **Container**: `grafana/grafana:13.0.1`, port `3000`
- **Login**: `admin` / `${GF_SECURITY_ADMIN_PASSWORD}` (xem `.env`)
- **Volume mount**:
  - [grafana/provisioning](../grafana/provisioning) → `/etc/grafana/provisioning`
  - [grafana/dashboards](../grafana/dashboards) → `/var/lib/grafana/dashboards`
  - `grafana_data` (named volume) → `/var/lib/grafana` (lưu user, preferences)

### 2.2 Datasource đã provisioning

[grafana/provisioning/datasources/datasource.yml](../grafana/provisioning/datasources/datasource.yml):
```yaml
datasources:
  - name: Prometheus
    uid: prometheus              # ← các dashboard reference uid này
    type: prometheus
    url: http://prometheus:9090   # ← service name trong docker network
    isDefault: true
```

> Vì sao có `uid: prometheus` cố định? — Vì các dashboard JSON reference cụ thể `"uid": "prometheus"`. Nếu để Grafana tự sinh uid random, dashboard sẽ không tìm thấy datasource sau khi reload.

### 2.3 Dashboard đã provisioning

[grafana/provisioning/dashboards/dashboard.yml](../grafana/provisioning/dashboards/dashboard.yml) khai báo Grafana scan thư mục `/var/lib/grafana/dashboards` mỗi vài giây và auto-import mọi file JSON tìm được.

3 dashboard có sẵn (folder "E-commerce" trong UI):

| File | Nội dung |
|------|----------|
| [spring-boot-overview.json](../grafana/dashboards/spring-boot-overview.json) | HTTP throughput, error rate, latency p95 cho từng service |
| [jvm-overview.json](../grafana/dashboards/jvm-overview.json) | Heap, GC, thread, CPU JVM |
| [ecommerce-saga-overview.json](../grafana/dashboards/ecommerce-saga-overview.json) | Saga services HTTP throughput + p95 latency có biến `$job` |

## 3. Workflow vận hành

### 3.1 Truy cập và xem dashboard có sẵn

1. Mở http://localhost:3000, login bằng `admin`
2. Sidebar trái → **Dashboards** → folder **E-commerce**
3. Mở **E-commerce Saga Overview** → top bar có dropdown `Job` để filter từng service

### 3.2 Tạo panel mới — pattern thực hành

Trong dashboard → **Add → Visualization**:

1. **Chọn datasource** = Prometheus
2. **Nhập PromQL** ở ô query (xem [02-prometheus.md](02-prometheus.md) cho ví dụ)
3. **Legend format** dùng `{{label_name}}` để hiển thị tên đẹp:
   ```
   Query:  sum by (job) (rate(http_server_requests_seconds_count[1m]))
   Legend: {{job}}
   ```
4. **Unit** (right panel) — chọn đơn vị: `req/s`, `seconds`, `bytes (IEC)`...
5. **Visualization type** — `Time series` cho line chart, `Stat` cho 1 con số to, `Gauge` cho %, `Bar gauge` cho ranking

### 3.3 Tạo template variable

Variables giúp 1 dashboard dùng được cho mọi service. Vào **Dashboard settings → Variables → New variable**:

| Field | Giá trị mẫu |
|-------|-------------|
| Name | `job` |
| Type | `Query` |
| Datasource | Prometheus |
| Query | `label_values(http_server_requests_seconds_count, job)` |
| Multi-value | ON (cho phép chọn nhiều) |
| Include All option | ON |

Sau đó query trong panel dùng `{job=~"$job"}`:
```promql
sum by (job) (rate(http_server_requests_seconds_count{job=~"$job"}[5m]))
```

### 3.4 Export dashboard sang JSON (để commit vào repo)

Dashboard settings → **JSON Model** → copy → paste vào file mới trong [grafana/dashboards/](../grafana/dashboards/).

> **Quan trọng**: Trước khi paste, set `"id": null` và đổi `"uid"` thành slug riêng (vd `"uid": "my-dashboard"`). Để datasource luôn link được, đảm bảo:
> ```json
> "datasource": { "type": "prometheus", "uid": "prometheus" }
> ```

Sau đó:
```bash
docker compose restart grafana
# hoặc đợi vài giây — provisioning auto-reload
```

### 3.5 Tạo alert (ví dụ: error rate > 5%)

1. Trong panel timeseries → tab **Alert** → **New alert rule**
2. **Expression A** (Reduce → Last):
   ```promql
   sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
   /
   sum(rate(http_server_requests_seconds_count[5m]))
   ```
3. **Threshold**: IS ABOVE `0.05`
4. **Evaluation**: Every 1m, For 5m (tránh false alarm khi spike ngắn)
5. **Labels** (cho routing): `severity=warning`
6. **Notification policy** — sidebar **Alerting → Contact points** thêm Slack/Email/Webhook

> Trong môi trường demo này có thể dùng `webhook` trỏ về Mailpit để test mà không cần SMTP thật.

### 3.6 Cấu hình notification qua Mailpit (cho local demo)

Sidebar → **Alerting → Contact points → New**:
- Type: `Email`
- SMTP: dùng env `GF_SMTP_*`. Thêm vào docker-compose service grafana:
  ```yaml
  environment:
    GF_SMTP_ENABLED: "true"
    GF_SMTP_HOST: "mailpit:1025"
    GF_SMTP_FROM_ADDRESS: "grafana@ecommerce.local"
    GF_SMTP_SKIP_VERIFY: "true"
  ```

Email gửi sẽ xuất hiện ở http://localhost:8025

## 4. Troubleshooting

### 4.1 Dashboard hiển thị "Datasource not found"

Nguyên nhân: dashboard JSON reference `uid` không khớp với datasource đã provisioning.

```bash
# Kiểm tra uid datasource hiện tại
curl -s -u admin:$GF_SECURITY_ADMIN_PASSWORD \
  http://localhost:3000/api/datasources | jq '.[] | {name, uid}'
```

Sửa: trong file dashboard JSON đổi tất cả `"uid": "..."` của datasource thành `"prometheus"`.

### 4.2 Provisioning không load dashboard mới

```bash
# Xem log provisioning
docker compose logs grafana | grep -i "provisioning\|dashboard\|error"

# Force reload
docker compose restart grafana
```

Common pitfalls:
- File JSON có `"id": 5` (id của instance Grafana cũ) — phải đổi thành `null`
- File không phải JSON hợp lệ — thiếu dấu phẩy, dư dấu phẩy cuối
- Mount path sai — đảm bảo `./grafana/dashboards:/var/lib/grafana/dashboards` trong docker-compose

### 4.3 Panel "No data"

Theo thứ tự kiểm tra:
1. Mở Prometheus UI (http://localhost:9090) chạy thử cùng query — có data không?
2. Kiểm tra time range của Grafana (góc trên phải) — chọn `Last 15 minutes` để chắc chắn có data
3. Variables — nếu `$job` rỗng, query `{job=~"$job"}` sẽ match hết hoặc không match gì

### 4.4 Quên password admin

```bash
docker compose exec grafana grafana-cli admin reset-admin-password newpassword
```

### 4.5 Dashboard bị reset sau restart

User chỉnh dashboard qua UI → chỉnh sửa lưu vào DB Grafana (`grafana_data` volume). Nhưng provisioning đọc file JSON gốc và **ghi đè** mỗi khi reload nếu `disableDeletion: false`.

→ Workflow đúng: chỉnh trong UI → **Export JSON** → ghi đè file trong [grafana/dashboards/](../grafana/dashboards/) → commit git.

## 5. Best practices đang được áp dụng

- **Provisioning > UI clicks** — toàn bộ dashboard và datasource lưu dưới dạng code, có thể version control
- **uid cố định cho datasource** — `prometheus` là id ổn định để dashboard reference
- **Folder logic** — gom dashboard theo domain (`E-commerce`) thay vì để "General"
- **Variable `$job`** — 1 dashboard tái sử dụng cho mọi service thay vì copy n lần
- **Tag dashboard** — `["ecommerce", "saga"]` giúp tìm kiếm và filter sau này
