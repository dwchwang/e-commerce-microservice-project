# Chạy End-to-End (Backend + Frontend + Admin Panel)

> **Mục tiêu:** Chạy toàn bộ hệ thống thật (13 microservice + infrastructure + frontend Next.js) và kiểm tra từng luồng nghiệp vụ từ giao diện người dùng đến database, đảm bảo storefront và admin panel (Phase 12 + Phase 14) hoạt động đúng.
>
> **Đối tượng:** Người chạy lần đầu, cần làm tuần tự từ trên xuống.
>
> **Thời gian dự kiến:** 20–40 phút (lần đầu, gồm build).

---

## 0. Tổng Quan Kiến Trúc Khi Chạy E2E

```
Trình duyệt (localhost:3000)
        │
        ▼
Frontend Next.js (npm run dev)   ← chạy trên HOST
   ├─ BFF proxy  /api/proxy/*  ─┐
   └─ Auth routes /api/auth/*  ─┤  (server-side gắn token vào cookie httpOnly)
                                 ▼
              API Gateway (localhost:8080)  ← chạy trong Docker
                                 │
        ┌────────────────────────┼────────────────────────┐
        ▼                        ▼                         ▼
  identity/user          product/cart/order        flash-sale/review/...
        │                        │                         │
        └──── Postgres / Redis / Kafka / Keycloak / Elasticsearch ────┘
```

- **Backend + infrastructure:** chạy bằng Docker Compose.
- **Frontend:** chạy local bằng `npm run dev` (file `docker-compose.yml` mặc định KHÔNG có service frontend — frontend chỉ nằm trong `docker-compose.prod.yml`).
- **Trình duyệt không bao giờ thấy access_token** — token nằm trong cookie httpOnly, frontend proxy tự gắn vào request.

---

## 1. Yêu Cầu Trước Khi Chạy

| Công cụ | Phiên bản | Kiểm tra |
|---|---|---|
| Docker + Docker Compose | mới | `docker --version` && `docker compose version` |
| JDK | 21 | `java -version` |
| Node.js | 22+ | `node -v` |
| RAM trống | ≥ 8 GB | Hệ thống có ~20 container |

> Nếu máy yếu, đóng bớt ứng dụng. Elasticsearch + Kafka + Keycloak khá nặng.

---

## 2. Chuẩn Bị Cấu Hình

### 2.1 File `.env` cho backend (thư mục gốc)

Nếu chưa có `.env`, tạo từ mẫu:

```bash
cp .env.example .env
```

Mở `.env` và điền theo hướng dẫn ở **`.guide/02-lay-credentials.md`**. Tối thiểu cần:

- Mật khẩu Postgres / Keycloak / Eureka / Grafana (tự đặt).
- `KEYCLOAK_CLIENT_SECRET`, `KEYCLOAK_ADMIN_CLIENT_SECRET` — copy từ `keycloak/realm-export.json`.
- VNPAY: nếu chưa test thanh toán thật, để giá trị demo:
  ```env
  VNPAY_TMN_CODE=DEMO
  VNPAY_HASH_SECRET=DEMOSECRET123
  VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  VNPAY_RETURN_URL=http://localhost:8080/api/payments/vnpay/return
  FRONTEND_ORDER_RESULT_URL=http://localhost:3000/order/result
  ```

> **Quan trọng cho luồng VNPAY:** `FRONTEND_ORDER_RESULT_URL` phải trỏ `http://localhost:3000/order/result` để sau khi thanh toán, VNPAY redirect về đúng trang kết quả của frontend.

Xác nhận không còn placeholder:
```bash
grep "change-me" .env    # không có output là OK
```

### 2.2 File `.env.local` cho frontend

```bash
cp frontend/.env.local.example frontend/.env.local
```

Sửa lại cho **chạy local trỏ vào backend Docker** (đây là điểm dễ sai nhất):

```env
# frontend/.env.local
# Server-side (RSC + route handlers) gọi gateway qua cổng host
API_BASE_URL=http://localhost:8080

# Browser dùng BFF proxy nên giá trị này không bắt buộc cho fetch,
# nhưng cứ đặt cho nhất quán
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

> Khác biệt với production: trong `docker-compose.prod.yml`, frontend chạy trong Docker nên `API_BASE_URL=http://api-gateway:8080`. Khi chạy `npm run dev` trên host phải dùng `http://localhost:8080`.

---

## 3. Build & Khởi Động Backend

### 3.1 Build JAR tất cả service

```bash
./mvnw clean package -DskipTests
```

### 3.2 Khởi động toàn bộ backend + infrastructure

```bash
docker compose up -d --build
```

> Lần đầu mất 5–10 phút. `flash-sale-service` có endpoint admin mới (`/api/flash-sales/admin`) — `--build` đảm bảo image được build lại.

### 3.3 Chờ và kiểm tra trạng thái

```bash
docker compose ps
```

Đợi đến khi các service `(healthy)` / `Up`. Kiểm tra nhanh gateway:

```bash
curl http://localhost:8080/actuator/health/readiness
# {"status":"UP"}
```

Kiểm tra Eureka đã đăng ký đủ 13 service + gateway:
```bash
curl -u eureka:YOUR_EUREKA_PASSWORD http://localhost:8761/eureka/apps | grep -o '<app>[^<]*' | sort -u
```

> Nếu service nào chưa `UP`, xem log: `docker compose logs -f <tên-service>`. Tham khảo `.guide/10-xu-ly-loi.md`.

---

## 4. Cấp Quyền ADMIN cho Tài Khoản Test

Đăng ký mặc định chỉ cấp `ROLE_USER`. Để test admin panel cần một tài khoản có `ROLE_ADMIN`.

### 4.1 Đăng ký tài khoản sẽ làm admin

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123","fullName":"Quản Trị Viên"}'
```

### 4.2 Gán ROLE_ADMIN qua Keycloak Admin Console

1. Mở http://localhost:8180 → đăng nhập `admin` / `KEYCLOAK_ADMIN_PASSWORD` (giá trị trong `.env`).
2. Chọn realm **`ecommerce`** (góc trên bên trái).
3. Vào **Users** → tìm `admin@example.com` → mở user.
4. Tab **Role mapping** → **Assign role** → lọc theo realm roles → chọn **`ROLE_ADMIN`** → **Assign**.

> Đăng ký thêm 1 tài khoản thường (ví dụ `user@example.com` / `User@123`) để test luồng khách hàng và kiểm tra chặn quyền admin.

---

## 5. Seed Dữ Liệu Tối Thiểu (Bắt Buộc)

Database khởi tạo trống. Storefront sẽ không có gì để hiển thị nếu chưa có sản phẩm. Cách nhanh nhất: dùng chính **Admin Panel** sau khi frontend chạy (Mục 6). Nếu muốn seed bằng API trước, làm như dưới.

### 5.1 Lấy access token của admin

```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
echo "$ADMIN_TOKEN" | cut -c1-20
```

### 5.2 Tạo category + brand + product

```bash
# Category
CAT_ID=$(curl -s -X POST http://localhost:8080/api/products/categories \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"Máy tính xách tay"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

# Brand
BRAND_ID=$(curl -s -X POST http://localhost:8080/api/products/brands \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Dell"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

# Product
curl -s -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "{\"sku\":\"DELL-XPS-13\",\"name\":\"Dell XPS 13\",\"description\":\"Laptop cao cấp\",\"price\":29990000,\"categoryId\":\"$CAT_ID\",\"brandId\":\"$BRAND_ID\",\"imageUrls\":[],\"specs\":[{\"specName\":\"CPU\",\"specValue\":\"Intel Core i7\"},{\"specName\":\"RAM\",\"specValue\":\"16GB\"}]}"
```

### 5.3 Nhập tồn kho cho sản phẩm (để mua được)

```bash
curl -s -X POST http://localhost:8080/api/inventory/stock-in \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"sku":"DELL-XPS-13","productName":"Dell XPS 13","quantity":50,"note":"seed"}'
```

> Có thể bỏ qua bước 5.2/5.3 và tạo sản phẩm + nhập kho trực tiếp trong Admin Panel ở Mục 6 — trực quan hơn.

---

## 6. Chạy Frontend (Local Dev)

Mở terminal MỚI (giữ backend đang chạy):

```bash
cd frontend
npm install        # lần đầu
npm run dev
```

Mặc định chạy tại **http://localhost:3000**.

> Nếu đổi code và muốn kiểm tra production build: `npm run build && npm run start`.

---

## 7. Kịch Bản E2E Theo Từng Luồng

Mỗi luồng có **Bước làm** và **Kết quả mong đợi**. Làm tuần tự.

### 7.1 Đăng ký & Đăng nhập (lưu ý: dùng EMAIL)

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Vào http://localhost:3000/register | Form có **Họ tên / Email / Mật khẩu** |
| 2 | Đăng ký `user@example.com` / `User@123` | Báo "Đăng ký thành công", chuyển sang `/login` |
| 3 | Đăng nhập bằng **email** vừa tạo | Về trang chủ, góc phải hiện menu user |
| 4 | Mở DevTools → Application → Cookies | Có `access_token`, `refresh_token` (httpOnly) |

> Realm bật `registrationEmailAsUsername` → **đăng nhập bằng email**, không phải username.

### 7.2 Duyệt sản phẩm & Tìm kiếm

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Trang chủ | Thấy sản phẩm đã seed ở "Sản phẩm nổi bật" |
| 2 | Vào `/products` | Danh sách + bộ lọc Danh mục/Thương hiệu/Giá; phân trang nếu > 20 |
| 3 | Chọn lọc 1 danh mục | URL có `?categoryId=...`, danh sách lọc đúng |
| 4 | Mở 1 sản phẩm | Trang chi tiết: giá, mô tả, tab Thông số (CPU/RAM), tab Đánh giá |
| 5 | Gõ từ khóa ở ô tìm kiếm | Vào `/search?q=...`, ra kết quả (cần search-service đã index — xem ghi chú) |

> **Search/Elasticsearch:** product-service publish event khi tạo sản phẩm; search-service consume để index. Nếu kết quả tìm kiếm trống ngay sau khi tạo, đợi vài giây hoặc kiểm tra log `search-service`.

### 7.3 Giỏ hàng (Guest → User merge)

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | **Đăng xuất**, thêm sản phẩm vào giỏ (khách) | Toast "Đã thêm vào giỏ hàng"; badge giỏ tăng |
| 2 | Mở `/cart` | Thấy item, số lượng, tổng tiền (`totalPrice`) |
| 3 | Tăng/giảm số lượng, xóa item | Cập nhật ngay |
| 4 | **Đăng nhập** | Giỏ khách được merge vào giỏ user (cookie `guest_session_id` mất đi) |

> Cookie `guest_session_id` là **UUID v4** (gateway `GuestSessionFilter` validate). Nếu thấy lỗi 400 ở `/api/cart`, kiểm tra cookie này.

### 7.4 Đặt hàng COD

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Có hàng trong giỏ → `/checkout` | Tóm tắt đơn, chọn/nhập địa chỉ, chọn thanh toán |
| 2 | Nhập địa chỉ mới hoặc chọn địa chỉ đã lưu | Hợp lệ mới cho đặt |
| 3 | Chọn **COD** → "Đặt hàng" | Toast thành công, chuyển `/orders/{id}` |
| 4 | Trang chi tiết đơn | Trạng thái ban đầu `PENDING` rồi tự động chuyển sang `STOCK_RESERVED` → `CONFIRMED` (saga) |
| 5 | Mở Mailpit http://localhost:8025 | Có email xác nhận khi đơn `CONFIRMED` |

> Trang chi tiết đơn tự poll mỗi 2s đến khi trạng thái terminal (`CONFIRMED`/`CANCELLED`).

### 7.5 Đặt hàng VNPAY (sandbox)

> Chỉ chạy được nếu đã cấu hình `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` thật từ sandbox. Với `DEMO`, bước redirect sẽ lỗi chữ ký — bỏ qua luồng này.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | `/checkout`, chọn **VNPAY** → "Thanh toán qua VNPAY" | Redirect sang cổng VNPAY sandbox |
| 2 | Thanh toán bằng thẻ test NCB (xem `.guide/02-lay-credentials.md` mục 2.7) | VNPAY xử lý xong redirect về `/order/result` |
| 3 | Trang `/order/result` | Tự chuyển về `/orders/{id}?status=success`, toast "Thanh toán thành công" |
| 4 | Trạng thái đơn | Chuyển `CONFIRMED` sau khi IPN xử lý |

### 7.6 Flash Sale

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | (Admin) Tạo campaign ở `/admin/flash-sales/new`, thời gian bắt đầu trong tương lai gần | Campaign `SCHEDULED` hiện trong danh sách admin |
| 2 | Đợi đến giờ bắt đầu (scheduler chuyển sang `ACTIVE`) | Storefront `/flash-sales` hiện campaign |
| 3 | (User) Vào chi tiết flash sale → "Mua ngay" | Dialog nhập địa chỉ + chọn thanh toán |
| 4 | Xác nhận mua | Toast "Đặt mua thành công", chuyển `/orders`; đơn flash sale xuất hiện |

> Mua flash sale yêu cầu đăng nhập. Endpoint purchase có rate limit — bấm quá nhanh trả 429 (toast cảnh báo).

### 7.7 Đánh giá sản phẩm

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Đăng nhập, mở sản phẩm **đã mua và CONFIRMED** | Tab Đánh giá có form viết đánh giá |
| 2 | Chọn số sao + nội dung → Gửi | Toast "Đã gửi đánh giá", review hiện trong danh sách |
| 3 | Sản phẩm chưa mua | Gửi đánh giá trả lỗi quyền (review-service kiểm tra đã mua) |

### 7.8 Admin Panel (Phase 14)

Đăng nhập bằng `admin@example.com`, vào http://localhost:3000/admin

| Module | Kiểm tra | Kết quả mong đợi |
|---|---|---|
| Guard | User thường vào `/admin` | Bị đẩy về `/` kèm toast "không có quyền" |
| Dashboard | `/admin/dashboard` | KPI doanh thu/đơn/khách, chart, đơn gần đây, tồn kho thấp |
| Sản phẩm | Tạo / sửa (có thông số) / xóa | Lưu được; thông số hiển thị lại đúng khi sửa; storefront cập nhật |
| Tồn kho | Nhập/xuất kho + lịch sử | Số lượng đổi, có dòng audit trong "Adjustment history" |
| Đơn hàng | Lọc + đổi trạng thái | Cập nhật trạng thái, toast thành công |
| Khách hàng | Mở chi tiết user | Thấy đơn hàng + đánh giá của user đó |
| Voucher | Tạo (percent ≤ 100, endDate > startDate) | Validate đúng; tạo được; áp được ở checkout |
| Flash sale | Tạo campaign mới | Campaign `SCHEDULED` hiện ngay trong danh sách |
| Banner / Nội dung | Thêm banner / trang | Tạo được, có toast |
| Đánh giá | Xóa review | Có dialog xác nhận, xóa được |

> Mọi thao tác CRUD admin có: nút submit hiện spinner khi đang lưu, lỗi hiện banner đỏ + toast, xóa có dialog xác nhận, tạo/sửa xong toast thành công.

---

## 8. Kiểm Tra Phụ Trợ (Tùy Chọn Nhưng Nên Làm)

| Công cụ | URL | Kiểm tra |
|---|---|---|
| Mailpit | http://localhost:8025 | Email đăng ký / xác nhận đơn |
| Zipkin | http://localhost:9411 | Trace một request đặt hàng đi qua nhiều service |
| Prometheus | http://localhost:9090/targets | Tất cả target `UP` |
| Grafana | http://localhost:3000 *(xung đột cổng FE — xem ghi chú)* | Dashboard Saga/JVM |
| Swagger | http://localhost:8080/swagger-ui.html | Thử API trực tiếp |

> **Xung đột cổng 3000:** Frontend dev và Grafana đều mặc định 3000. Khi chạy E2E local, frontend chiếm 3000. Grafana vẫn ở 3000 trong `docker-compose.yml` → sẽ KHÔNG vào được Grafana qua 3000 khi FE đang chạy. Nếu cần Grafana, dừng FE hoặc đổi cổng FE: `npm run dev -- -p 3005` (và cập nhật `NEXT_PUBLIC_APP_URL`, `FRONTEND_ORDER_RESULT_URL` tương ứng).

---

## 9. Kiểm Tra Tầng Dữ Liệu (Khi Nghi Ngờ)

```bash
# Đơn hàng trong DB
docker compose exec postgres psql -U postgres -d order_db -c "SELECT id, status, total_amount, payment_method FROM orders ORDER BY created_at DESC LIMIT 5;"

# Tồn kho
docker compose exec postgres psql -U postgres -d inventory_db -c "SELECT sku, quantity, reserved_quantity FROM inventory;"

# Giỏ hàng trong Redis
docker compose exec redis redis-cli KEYS "cart:*"

# Kafka topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

---

## 10. Lỗi Thường Gặp Khi Chạy E2E

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Đăng nhập luôn báo sai | Đăng nhập bằng username thay vì **email** | Dùng email; realm dùng email làm username |
| Storefront trống trơn | Chưa seed sản phẩm | Làm Mục 5 hoặc tạo sản phẩm trong Admin |
| `/api/cart` trả 400 | Thiếu/không hợp lệ `guest_session_id` | Xóa cookie cũ, reload; cookie phải là UUID v4 |
| FE gọi API lỗi `ECONNREFUSED` | `API_BASE_URL` sai | Local dev phải là `http://localhost:8080` |
| Mua được nhưng đơn kẹt `PENDING` | Saga/inventory chưa xử lý hoặc hết hàng | Xem log `order-service`, `inventory-service`; kiểm tra đã stock-in |
| VNPAY báo sai chữ ký | Đang dùng `DEMO` secret | Đăng ký sandbox thật hoặc bỏ qua luồng VNPAY |
| Flash sale `/admin/flash-sales` trống sau khi tạo | `flash-sale-service` chưa rebuild image mới | `docker compose up -d --build flash-sale-service` |
| Vào được `/admin` dù là user thường | Chưa gán/đã gán nhầm role | Kiểm tra Role mapping trong Keycloak; xóa cookie, đăng nhập lại |
| Admin sửa sản phẩm mất thông số | (Đã fix) contract specName/specValue | Đảm bảo đang chạy code mới nhất |

Tham khảo thêm: `.guide/10-xu-ly-loi.md`.

---

## 11. Checklist Hoàn Tất E2E

- [ ] `docker compose ps` — tất cả service `Up/healthy`
- [ ] Eureka có đủ 13 service + gateway
- [ ] Đăng ký + đăng nhập bằng email OK
- [ ] Duyệt sản phẩm + chi tiết + tìm kiếm OK
- [ ] Giỏ hàng guest → merge khi đăng nhập OK
- [ ] Đặt hàng COD → đơn `CONFIRMED` + email Mailpit
- [ ] (Tùy chọn) Đặt hàng VNPAY sandbox → `CONFIRMED`
- [ ] Flash sale: tạo (admin) → mua (user) OK
- [ ] Viết đánh giá sản phẩm đã mua OK
- [ ] Admin: guard chặn user thường
- [ ] Admin: CRUD products / inventory / orders / vouchers / flash-sales / content / reviews OK
- [ ] Không có lỗi đỏ trong console trình duyệt / log service

---

## 12. Dừng Hệ Thống

```bash
# Dừng frontend: Ctrl+C ở terminal npm

# Dừng backend, giữ data
docker compose down

# Hoặc reset sạch toàn bộ data
docker compose down -v
```

> `down -v` xóa hết database/kafka/elasticsearch — chỉ dùng khi muốn chạy lại từ đầu hoàn toàn.
