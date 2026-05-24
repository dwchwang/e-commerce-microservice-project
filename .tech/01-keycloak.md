# 01. Keycloak — Identity Provider & OAuth2/OIDC

> Keycloak là **identity broker** cho toàn hệ thống. Mọi xác thực (login, refresh, logout) đều đi qua Keycloak; mọi API protected ở các service đều validate JWT do Keycloak phát hành.

## 1. Khái niệm cốt lõi

| Khái niệm | Vai trò trong hệ thống |
|-----------|------------------------|
| **Realm** | Một tenant độc lập. Hệ thống dùng realm `ecommerce` |
| **Client** | App nói chuyện với Keycloak. Có 2 client: `ecommerce-client` (login/refresh) và `identity-service-admin` (admin API) |
| **User** | Tài khoản người dùng. Mỗi user có `sub` (UUID) — đây chính là `userId` ở các service business |
| **Realm Role** | `ROLE_USER` (mặc định khi đăng ký), `ROLE_ADMIN` (admin) |
| **Access Token** | JWT (RS256) chứa `sub`, `email`, `realm_access.roles`, `exp` |
| **Refresh Token** | JWT để lấy access token mới mà không cần đăng nhập lại |
| **JWKS** | Public keys để các resource server verify chữ ký JWT |

## 2. Hệ thống đang dùng Keycloak ra sao

### 2.1 Cấu hình thực tế

- **Container**: `quay.io/keycloak/keycloak:26.6.1` — port host `8180`, port nội bộ `8080`
- **Mode**: `start-dev --import-realm` (auto-import realm khi khởi động)
- **DB riêng**: `keycloak-db` (PostgreSQL 17), schema riêng biệt với DB business
- **Realm import**: [keycloak/realm-export.json](../keycloak/realm-export.json)

### 2.2 Hai client đã được cấu hình

| Client ID | Mục đích | Auth flow | Secret |
|-----------|----------|-----------|--------|
| `ecommerce-client` | App frontend + identity-service login | `password`, `refresh_token`, `authorization_code` | `local-dev-ecommerce-secret` |
| `identity-service-admin` | identity-service gọi Admin REST API | `client_credentials` | `local-dev-identity-admin-secret` |

`identity-service-admin` được gán các realm-management roles: `manage-users`, `view-realm`, `view-users`, `query-users` — đủ để identity-service tạo user / gán role qua Keycloak Admin Client.

### 2.3 Hai vai trò của Keycloak trong code

**Vai trò A — Token Provider (identity-service)**

[identity-service/.../AuthServiceImpl.java](../identity-service/src/main/java/com/ecommerce/identity/service/impl/AuthServiceImpl.java) gọi:
- `POST /realms/ecommerce/protocol/openid-connect/token` để login/refresh
- `POST /realms/ecommerce/protocol/openid-connect/logout` để logout
- Keycloak Admin Java client (`org.keycloak:keycloak-admin-client`) để register user + assign role

**Vai trò B — JWT Validator (api-gateway)**

[api-gateway/.../SecurityConfig.java](../api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java) — `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`. Spring Security dùng JWKS endpoint:
```
http://keycloak:8080/realms/ecommerce/protocol/openid-connect/certs
```
Để verify chữ ký JWT mà không cần gọi Keycloak mỗi request.

Sau khi token hợp lệ, [AuthHeaderFilter.java](../api-gateway/src/main/java/com/ecommerce/gateway/filter/AuthHeaderFilter.java) **strip** các header identity do client gửi (`X-User-Id`, `X-User-Roles`, `X-User-Email`) rồi **inject lại từ JWT claims** — đây là pattern quan trọng chống spoofing: downstream service tin tưởng tuyệt đối các header này vì chúng đến từ gateway đã verify token.

## 3. Workflow vận hành

### 3.1 Khởi động và truy cập Admin Console

```bash
docker compose up -d keycloak keycloak-db
docker compose logs -f keycloak | grep -i "started\|imported"
```

Truy cập: http://localhost:8180 — login bằng tài khoản trong `.env` (mặc định `admin` / `admin`).

Vào realm `ecommerce` (góc trên bên trái). 4 menu cần biết:

| Menu | Dùng để |
|------|---------|
| **Users** | Xem/tạo/disable user, reset password, gán role |
| **Clients** | Xem 2 client đã import, lấy lại client secret nếu cần |
| **Realm roles** | Tạo role mới, xem composite role |
| **Sessions** | Xem ai đang login, force-logout |

### 3.2 Test login flow bằng curl

```bash
# Bước 1 — Đăng ký user mới qua identity-service
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@ecommerce.local",
    "password": "Password123!",
    "fullName": "Demo User"
  }'

# Bước 2 — Login để lấy access token
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@ecommerce.local","password":"Password123!"}' \
  | jq -r .accessToken)

echo "Token: $ACCESS_TOKEN"

# Bước 3 — Gọi API protected
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/users/me
```

### 3.3 Decode JWT để xem claims

```bash
# Lấy phần payload (giữa 2 dấu chấm) rồi base64 decode
echo "$ACCESS_TOKEN" | cut -d '.' -f2 | base64 -d 2>/dev/null | jq
```

Bạn sẽ thấy:
```json
{
  "sub": "9c1f...-uuid",
  "email": "demo@ecommerce.local",
  "preferred_username": "demo@ecommerce.local",
  "realm_access": { "roles": ["ROLE_USER", "default-roles-ecommerce"] },
  "exp": 1716549...,
  "iss": "http://keycloak:8080/realms/ecommerce"
}
```

`sub` chính là `userId` mà order-service, cart-service... lưu trong DB.

### 3.4 Gán role ADMIN cho 1 user

**Cách 1 — Qua Admin Console**: Users → chọn user → tab `Role mapping` → Assign role → chọn `ROLE_ADMIN`.

**Cách 2 — Qua API của identity-service** (yêu cầu token đã có ROLE_ADMIN):
```bash
curl -X POST http://localhost:8080/api/auth/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId":"<keycloak-sub>","roleName":"ROLE_ADMIN"}'
```

User cần **logout-login lại** để token mới chứa `ROLE_ADMIN`.

### 3.5 Refresh token

Access token mặc định sống ngắn (5 phút). Frontend nên dùng refresh token để gia hạn:
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

### 3.6 Đổi token lifespan (cho dev/demo)

Realm Settings → **Tokens**:
- `Access Token Lifespan` — mặc định 5 phút, tăng lên 1h cho dev đỡ phải refresh
- `SSO Session Idle` — bao lâu không hoạt động thì invalidate session
- `Client Session Max` — giới hạn cứng tổng thời gian session

## 4. Troubleshooting

### 4.1 Gateway trả về 401 Unauthorized

```bash
# Kiểm tra issuer-uri có khớp không
docker compose exec api-gateway env | grep KEYCLOAK
# JWT có claim "iss" phải BẰNG ĐÚNG issuer-uri trong gateway config
```

Nguyên nhân thường gặp:
- Token issued bởi `localhost:8180` nhưng gateway expect `keycloak:8080` (hoặc ngược lại) → mismatch issuer
- Token đã hết hạn (`exp` < now) → cần refresh
- JWKS chưa load được (Keycloak chưa healthy khi gateway start) → restart gateway

### 4.2 "invalid_grant" khi login

- Sai password — Keycloak luôn trả lỗi mơ hồ này dù sai email hay password (anti-enumeration)
- User chưa enabled hoặc chưa email-verified — check Users → Details
- Client secret sai — đối chiếu với `realm-export.json`

### 4.3 Reset toàn bộ realm về trạng thái import

```bash
docker compose down
docker volume rm ecommerce-microservice-project_keycloak_db_data
docker compose up -d keycloak-db keycloak
```

### 4.4 Backup realm sau khi đã chỉnh sửa

```bash
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh export \
  --dir /tmp/export --realm ecommerce --users realm_file
docker compose cp keycloak:/tmp/export/ecommerce-realm.json ./keycloak/
```

### 4.5 Token không có `realm_access.roles`

Vào Clients → `ecommerce-client` → tab `Client scopes` → `ecommerce-client-dedicated` → Add mapper → **By configuration** → `User Realm Role` → đảm bảo `Add to access token` = ON.

## 5. Best practices đang được áp dụng

- **Resource server không validate qua introspection** (chậm) mà dùng **JWKS** (nhanh, không cần roundtrip)
- **Identity headers do gateway set, không tin client** — strip rồi mới inject
- **Service account riêng cho Admin API** (`identity-service-admin`) — tách biệt với client login (`ecommerce-client`)
- **DB riêng cho Keycloak** — không dùng chung với business DB, tránh blast radius khi backup/restore
- **`KC_HEALTH_ENABLED=true`** — cho phép healthcheck container đi qua, để gateway có thể `depends_on: condition: service_healthy`
