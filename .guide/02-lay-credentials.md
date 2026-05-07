# Lấy Credentials & API Keys

## Tổng Quan — Cái Gì Cần Tạo, Cái Gì Có Sẵn

| Credential | Trạng thái | Cách lấy |
|------------|-----------|---------|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | **Tự đặt** | Bạn tự đặt bất kỳ |
| `KEYCLOAK_DB_USER` / `KEYCLOAK_DB_PASSWORD` | **Tự đặt** | Bạn tự đặt bất kỳ |
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` | **Tự đặt** | Bạn tự đặt bất kỳ |
| `KEYCLOAK_CLIENT_SECRET` | **Có sẵn** | Copy từ `realm-export.json` (xem Mục 1) |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | **Có sẵn** | Copy từ `realm-export.json` (xem Mục 1) |
| `EUREKA_USER` / `EUREKA_PASSWORD` | **Tự đặt** | Bạn tự đặt bất kỳ |
| `GF_SECURITY_ADMIN_PASSWORD` | **Tự đặt** | Bạn tự đặt bất kỳ |
| `VNPAY_TMN_CODE` | **Cần đăng ký** | Đăng ký tại VNPAY Sandbox (xem Mục 2) |
| `VNPAY_HASH_SECRET` | **Cần đăng ký** | Đăng ký tại VNPAY Sandbox (xem Mục 2) |
| `SMTP_HOST` / `SMTP_PORT` | **Có sẵn** | Dùng Mailpit local (không cần thay đổi) |

---

## Mục 1: Keycloak Secrets (Có Sẵn — Chỉ Cần Copy)

Dự án đã có file `keycloak/realm-export.json` được import tự động khi Docker khởi động.
Secrets đã được đặt sẵn trong file này. Bạn chỉ cần copy vào `.env`.

### Giá trị cần copy:

```
KEYCLOAK_CLIENT_ID=ecommerce-client
KEYCLOAK_CLIENT_SECRET=local-dev-ecommerce-secret

KEYCLOAK_ADMIN_CLIENT_ID=identity-service-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=local-dev-identity-admin-secret
```

### Xác nhận lại bằng lệnh:
```bash
cat keycloak/realm-export.json | python3 -c "
import json, sys
data = json.load(sys.stdin)
for c in data.get('clients', []):
    cid = c.get('clientId','')
    if cid in ['ecommerce-client', 'identity-service-admin']:
        print(f'  {cid}  =>  secret = {c.get(\"secret\", \"N/A\")}')
"
```

> **Lưu ý bảo mật**: Các secret này chỉ dùng cho môi trường **local/dev**.
> Khi deploy production, cần đổi sang secret ngẫu nhiên mạnh hơn.

---

## Mục 2: VNPAY Sandbox (Bắt Buộc Đăng Ký)

VNPAY là cổng thanh toán Việt Nam. Để test tính năng thanh toán, bạn cần đăng ký tài khoản **Sandbox** (miễn phí, không dùng tiền thật).

> **Nếu bạn chưa cần test thanh toán**, có thể để giá trị giả:
> ```
> VNPAY_TMN_CODE=DEMO
> VNPAY_HASH_SECRET=DEMOSECRET123
> ```
> Hệ thống vẫn chạy nhưng tính năng thanh toán sẽ không thực sự hoạt động.

---

### Bước 2.1: Truy Cập Trang Đăng Ký Developer VNPAY

Mở trình duyệt, vào địa chỉ:
```
https://sandbox.vnpayment.vn/devreg/
```

Bạn sẽ thấy trang "Đăng ký tích hợp VNPAY Sandbox".

---

### Bước 2.2: Điền Form Đăng Ký

Điền đầy đủ thông tin:

| Trường | Gợi ý điền |
|--------|-----------|
| **Họ và tên** | Tên thật của bạn |
| **Email** | Email đang dùng (sẽ nhận thông tin đăng nhập) |
| **Số điện thoại** | Số điện thoại thật |
| **Tên website/ứng dụng** | `E-Commerce DATN` hoặc bất kỳ tên nào |
| **Website URL** | `http://localhost:8080` (URL local dev) |
| **Mô tả** | `Đồ án tốt nghiệp - Hệ thống e-commerce` |

Sau khi điền xong, click **"Đăng ký"**.

---

### Bước 2.3: Kiểm Tra Email Xác Nhận

1. Vào hòm thư email bạn vừa đăng ký
2. Tìm email từ VNPAY với tiêu đề dạng **"Thông tin tài khoản Sandbox VNPAY"** hoặc tương tự
3. Email sẽ chứa:
   - **Username** và **Password** để đăng nhập trang quản lý
   - Hoặc link kích hoạt tài khoản

> Nếu không thấy email sau 5 phút, kiểm tra thư mục **Spam/Junk**.

---

### Bước 2.4: Đăng Nhập Trang Quản Lý Merchant

Truy cập:
```
https://sandbox.vnpayment.vn/merchantv2/
```

Đăng nhập bằng tài khoản vừa nhận qua email.

---

### Bước 2.5: Lấy Terminal ID (TMN Code) và Secret Key

Sau khi đăng nhập vào trang merchant:

1. Nhìn vào menu bên trái, tìm mục **"Quản lý website"** hoặc **"Thông tin tích hợp"**

2. Click vào website/ứng dụng bạn vừa đăng ký

3. Tìm thông tin:

   ```
   Terminal ID (mã TMN): XXXXXXXX   ← đây là VNPAY_TMN_CODE
   Secret Key (Checksum Key): XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX   ← đây là VNPAY_HASH_SECRET
   ```

4. Copy cả hai giá trị này

---

### Bước 2.6: Điền Vào File .env

Mở file `.env` và thay thế:
```env
VNPAY_TMN_CODE=XXXXXXXX          # Terminal ID từ bước 2.5
VNPAY_HASH_SECRET=XXXXXXXXX...   # Secret Key từ bước 2.5
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/api/payments/vnpay/return
FRONTEND_ORDER_RESULT_URL=http://localhost:3000/order/result
```

> `VNPAY_PAY_URL` đã đúng cho môi trường sandbox — **không cần thay đổi**.

---

### Bước 2.7: Test Thanh Toán Với Thẻ Giả

VNPAY Sandbox cung cấp thẻ test để thử nghiệm:

**Thẻ nội địa (ATM):**
| Thông tin | Giá trị |
|-----------|---------|
| Ngân hàng | NCB |
| Số thẻ | `9704198526191432198` |
| Tên chủ thẻ | `NGUYEN VAN A` |
| Ngày phát hành | `07/15` |
| OTP | `123456` |

**Thẻ quốc tế (Visa test):**
| Thông tin | Giá trị |
|-----------|---------|
| Số thẻ | `4524680741032172` |
| Ngày hết hạn | `01/28` |
| CVV | `645` |
| Tên | Bất kỳ |

> Nguồn: https://sandbox.vnpayment.vn/apis/vnpay-demo/

---

## Mục 3: File .env Hoàn Chỉnh Sau Khi Điền Đủ

Đây là file `.env` mẫu hoàn chỉnh sau khi điền tất cả thông tin:

```env
# ============================================================
# PostgreSQL — Tự đặt mật khẩu
# ============================================================
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123

# ============================================================
# Keycloak DB — Tự đặt mật khẩu
# ============================================================
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=keycloak123

# ============================================================
# Keycloak Admin — Tự đặt mật khẩu đăng nhập Admin UI
# ============================================================
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123

# ============================================================
# Keycloak Clients — Copy từ realm-export.json (KHÔNG thay đổi)
# ============================================================
KEYCLOAK_CLIENT_ID=ecommerce-client
KEYCLOAK_CLIENT_SECRET=local-dev-ecommerce-secret

KEYCLOAK_ADMIN_CLIENT_ID=identity-service-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=local-dev-identity-admin-secret

# ============================================================
# Eureka — Tự đặt mật khẩu, nhớ update EUREKA_DEFAULT_ZONE khớp
# ============================================================
EUREKA_USER=eureka
EUREKA_PASSWORD=eureka123
EUREKA_DEFAULT_ZONE=http://eureka:eureka123@discovery-server:8761/eureka/

# ============================================================
# Grafana — Tự đặt mật khẩu đăng nhập dashboard
# ============================================================
GF_SECURITY_ADMIN_PASSWORD=grafana123

# ============================================================
# VNPAY — Lấy từ https://sandbox.vnpayment.vn/devreg/
# ============================================================
VNPAY_TMN_CODE=YOUR_TMN_CODE_HERE
VNPAY_HASH_SECRET=YOUR_HASH_SECRET_HERE
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/api/payments/vnpay/return
FRONTEND_ORDER_RESULT_URL=http://localhost:3000/order/result

# ============================================================
# Email — Dùng Mailpit local, không cần thay đổi
# ============================================================
SMTP_HOST=mailpit
SMTP_PORT=1025
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_AUTH=false
SMTP_STARTTLS=false
```

---

## Mục 4: Xác Nhận Tất Cả Đã Đúng

Chạy lệnh này để kiểm tra file `.env` không còn giá trị `change-me`:

```bash
grep "change-me" .env
# Nếu không có output → đã điền đầy đủ

# Kiểm tra EUREKA_DEFAULT_ZONE khớp với EUREKA_PASSWORD
EUREKA_PWD=$(grep "^EUREKA_PASSWORD=" .env | cut -d= -f2)
EUREKA_ZONE=$(grep "^EUREKA_DEFAULT_ZONE=" .env | cut -d= -f2)
echo "Password: $EUREKA_PWD"
echo "Zone URL: $EUREKA_ZONE"
# Phải thấy password xuất hiện trong Zone URL
```
