# Cấu Hình Keycloak

## Tổng Quan

Keycloak là **Identity Provider** của hệ thống — quản lý đăng nhập, đăng ký, JWT token.

- **URL Admin UI**: http://localhost:8180
- **Realm**: `ecommerce`
- **Tự động import**: File `keycloak/realm-export.json` được import khi Keycloak khởi động lần đầu

---

## Realm Đã Được Cấu Hình Sẵn

Khi Docker khởi động, Keycloak tự import realm `ecommerce` với:

### Clients (Ứng dụng kết nối Keycloak)

| Client ID | Mục đích |
|-----------|---------|
| `ecommerce-client` | Client chính để login, logout, lấy JWT token |
| `identity-service-admin` | Service account cho identity-service quản lý users qua Admin API |

### Roles (Quyền)
| Role | Mô tả |
|------|-------|
| `ROLE_USER` | Người dùng thông thường |
| `ROLE_ADMIN` | Quản trị viên |

---

## Lấy Client Secret

### Cách 1: Đọc từ realm-export.json
```bash
# Secret của ecommerce-client
cat keycloak/realm-export.json | python3 -c "
import json, sys
data = json.load(sys.stdin)
for c in data.get('clients', []):
    if c.get('clientId') in ['ecommerce-client', 'identity-service-admin']:
        print(c['clientId'], ':', c.get('secret','N/A'))
"
```

### Cách 2: Từ Keycloak Admin UI
1. Vào http://localhost:8180
2. Đăng nhập `admin` / mật khẩu trong `.env`
3. Chọn realm **ecommerce** (dropdown trên cùng bên trái)
4. Vào **Clients** → chọn `ecommerce-client`
5. Tab **Credentials** → Copy **Client secret**
6. Lặp lại với `identity-service-admin`

---

## Đăng Ký User Mới (qua API)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123",
    "fullName": "Test User"
  }'
```

---

## Đăng Nhập và Lấy JWT Token

```bash
# Thay YOUR_CLIENT_SECRET bằng secret thực từ Keycloak
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=ecommerce-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=test@example.com" \
  -d "password=Test@123" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

echo "Token: $TOKEN"
```

### Sử dụng Token để gọi API
```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## Thêm User Thủ Công Trong Keycloak Admin UI

1. Vào http://localhost:8180 → realm `ecommerce`
2. **Users** → **Add user**
3. Điền: Username, Email, First name, Last name
4. **Credentials** tab → Set password (tắt "Temporary")
5. **Role mapping** tab → Assign role `ROLE_USER`

---

## Cấu Hình Token

Mặc định từ realm-export:

| Setting | Giá trị |
|---------|---------|
| Access Token Lifespan | 5 phút |
| Refresh Token Lifespan | 30 phút |
| Algorithm | RS256 |

---

## Logout

```bash
# Thay REFRESH_TOKEN bằng refresh token nhận được khi đăng nhập
curl -X POST \
  http://localhost:8180/realms/ecommerce/protocol/openid-connect/logout \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=ecommerce-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "refresh_token=YOUR_REFRESH_TOKEN"
```

---

## Xuất Realm (Sau Khi Thay Đổi)

Nếu bạn thay đổi cấu hình Keycloak và muốn lưu lại:

```bash
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh export \
  --dir /opt/keycloak/data/import \
  --realm ecommerce \
  --users realm_file
```

File sẽ được lưu vào `keycloak/realm-export.json` (mounted volume).
