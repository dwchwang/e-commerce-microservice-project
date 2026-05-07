# 06. Bảo Mật — Keycloak, OAuth 2.0, OIDC, JWT

## 1. Mục Tiêu Nghiên Cứu

- Hiểu OAuth 2.0 framework, các grant type (flow)
- Hiểu OpenID Connect (OIDC) — lớp authentication
- Hiểu JWT (JWS) — cấu trúc, ký số, claim
- Hiểu Keycloak — Identity Provider, Realm, Client, Role
- Phân biệt Authentication vs Authorization

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Authentication vs Authorization

- **Authentication (AuthN)**: "Bạn là ai?" — verify identity (đăng nhập)
- **Authorization (AuthZ)**: "Bạn được làm gì?" — verify permission (RBAC)

→ JWT có thể chứa cả AuthN (`sub`, `email`) và AuthZ (`roles`, `scope`).

### 2.2. OAuth 2.0 (RFC 6749, 2012)

**Vai trò**:
- **Resource Owner**: User thật
- **Client**: App muốn truy cập tài nguyên (frontend React, mobile)
- **Authorization Server**: Cấp token (Keycloak)
- **Resource Server**: API có tài nguyên (backend services)

**Mục đích gốc**: AUTHORIZATION (delegate quyền), không phải authentication. Dùng OAuth làm login là sai mục đích — đây là lý do OIDC ra đời.

**Grant types (flows)**:

| Grant | Use case | Status |
|-------|---------|--------|
| **Authorization Code** | Web app server-side | Recommended |
| **Authorization Code + PKCE** | SPA, Mobile | Recommended |
| Implicit | SPA (cũ) | **Deprecated** (OAuth 2.1) |
| Resource Owner Password | Trusted apps | **Deprecated** |
| Client Credentials | Service-to-service | OK |
| Device Code | TV, IoT | OK |
| Refresh Token | Renew access token | OK |

### 2.3. OpenID Connect (OIDC, 2014)

OIDC = OAuth 2.0 + **ID Token** (JWT chứa thông tin user).

```
Access Token (OAuth)  → "Cho phép truy cập API X"  → audience là Resource Server
ID Token (OIDC)        → "Đây là user ABC, tên..."  → audience là Client app
Refresh Token          → "Dùng để xin access token mới"
```

Standard claims (OIDC): `sub`, `iss`, `aud`, `exp`, `iat`, `name`, `email`, `email_verified`, `picture`, ...

### 2.4. JWT (RFC 7519) — JSON Web Token

Cấu trúc 3 phần `header.payload.signature` (Base64Url):

```
Header:    {"alg":"RS256","typ":"JWT","kid":"abc123"}
Payload:   {"sub":"user-id","iss":"keycloak","exp":1700000,"realm_access":{"roles":["USER"]}}
Signature: RSA-SHA256(base64(header) + "." + base64(payload), private_key)
```

- **JWS**: JWT có signature
- **JWE**: JWT mã hóa (ít dùng)
- **Algorithm**: RS256 (asymmetric, public key verify) — **dùng trong dự án**, hoặc HS256 (symmetric)

**Tại sao asymmetric**:
- Keycloak ký bằng private key (giữ kín)
- Mọi service verify bằng public key (lấy từ JWK Set endpoint)
- Không service nào phải biết secret → an toàn hơn HS256

### 2.5. JWK & JWKS

**JWK (JSON Web Key)**: format JSON cho public key
**JWKS (JWK Set)**: endpoint trả nhiều key (key rotation)

Keycloak expose:
```
GET /realms/ecommerce/protocol/openid-connect/certs
→ { "keys": [ {"kty":"RSA", "kid":"...", "n":"...", "e":"AQAB"} ] }
```

Spring Security OAuth2 Resource Server:
- Cache JWKS local
- Khi nhận JWT → lấy `kid` từ header → tìm key tương ứng → verify

### 2.6. Keycloak

**Open-source Identity Provider** (RedHat). Hỗ trợ:
- OAuth 2.0, OIDC, SAML 2.0
- LDAP/AD federation
- Social login (Google, Facebook, ...)
- 2FA, OTP, WebAuthn
- Admin Console UI

**Khái niệm Keycloak**:

| Term | Ý nghĩa |
|------|---------|
| **Realm** | Tenant cô lập (users, clients, roles riêng) |
| **Client** | App đăng ký với Keycloak (vd: `ecommerce-client`) |
| **User** | Account của người dùng |
| **Role** | Permission group (Realm role, Client role) |
| **Group** | Tập user với role chung |
| **Client Scope** | Bộ claim/protocol mapper tái dùng |
| **Identity Provider** | External IdP cho federation |

**Token endpoint**:
```
POST /realms/ecommerce/protocol/openid-connect/token
Body: grant_type=password & username=... & password=... & client_id=...
Response: { access_token, refresh_token, id_token, expires_in }
```

### 2.7. Authorization Code + PKCE (chuẩn cho SPA)

```
1. SPA tạo code_verifier (random), tính code_challenge = SHA256(code_verifier)
2. Redirect → Keycloak login UI với code_challenge
3. User login → Keycloak redirect về SPA với ?code=...
4. SPA POST /token với code + code_verifier
5. Keycloak verify hash → trả token
```

→ PKCE chống Authorization Code Interception attack (mobile/SPA không thể giữ secret).

### 2.8. Token lifecycle & Refresh

- Access token: thời hạn ngắn (5 phút) → giảm thiệt hại nếu lộ
- Refresh token: thời hạn dài (30 phút–nhiều giờ) → renew silently
- Khi access token expired: SPA dùng refresh token POST `/token` với `grant_type=refresh_token`

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. Keycloak setup
- Container `keycloak:26.6.1` cổng `8180`
- DB riêng (`keycloak-db`)
- Auto-import realm từ `keycloak/realm-export.json` lúc khởi động
- Realm: `ecommerce`
- Client: `ecommerce-client` (Confidential, Direct Access Grants ON cho dev)
- Roles: `USER`, `ADMIN`

### 3.2. identity-service module

Vai trò: **Facade** trước Keycloak Admin REST API. Lý do:
- Giấu Keycloak internal URL
- Validate input + log audit
- Publish Kafka event `user-registered` sau khi tạo user trong Keycloak

```
POST /api/auth/register
  identity-service:
    1. Validate input
    2. POST /admin/realms/ecommerce/users (Keycloak Admin API)
    3. Set password
    4. Assign role USER
    5. Publish Kafka: user-registered → user-service tạo profile, notification gửi welcome email
```

### 3.3. Flow đăng nhập
```
Frontend → POST /realms/ecommerce/protocol/openid-connect/token
                grant_type=password (test) hoặc authorization_code (production)
       ← access_token + refresh_token + id_token
Frontend → API request: Authorization: Bearer <access_token>
       → Gateway verify JWT
       → forward + inject X-User-Id header
       → backend service xử lý
```

### 3.4. Spring Security OAuth2 Resource Server (Gateway)

```yaml
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: http://keycloak:8080/realms/ecommerce
  jwk-set-uri: http://keycloak:8080/realms/ecommerce/protocol/openid-connect/certs
```

- `issuer-uri` cho phép tự discover JWKS (nhưng dự án explicit để tránh DNS issue)
- Tự verify: signature, exp, iss, aud

### 3.5. Authorization trong Gateway

`SecurityWebFilterChain`:
```java
http.authorizeExchange(exchange -> exchange
  .pathMatchers("/api/auth/**", "/api/products/**", "/api/search/**").permitAll()
  .pathMatchers("/api/payments/vnpay/**").permitAll()  // VNPAY callback không có JWT
  .anyExchange().authenticated()
);
```

### 3.6. Role-based Authorization
Trong service backend: dùng header `X-User-Roles` hoặc Annotation `@PreAuthorize`. Nhưng dự án dùng Trusted Subsystem → kiểm tra `X-User-Roles` ở controller.

---

## 4. Mô Hình Bảo Mật Tổng Quan

```
┌─────────┐                    ┌──────────┐              ┌──────────┐
│ Browser │ ── /token ──────► │ Keycloak │              │ Backend  │
│ (React) │ ◄── tokens ────── │  :8180   │              │ Service  │
└────┬────┘                    └─────▲────┘              └────▲─────┘
     │                                │                        │
     │ Authorization: Bearer X         │                        │
     ▼                                 │ JWKS                   │ X-User-Id
┌──────────┐                            │                        │
│ Gateway  │ ──── verify JWT ───────────┘                        │
│  :8080   │ ──────── forward + identity headers ─────────────► (trust)
└──────────┘
```

Đặc điểm:
- **Stateless**: Server không lưu session — mỗi request tự chứa identity qua JWT
- **Single sign-on**: 1 lần login, nhiều dịch vụ
- **Token rotation**: Refresh token có thể rotate (single use) cho bảo mật cao

---

## 5. Tấn Công Phổ Biến & Phòng Chống

| Attack | Phòng |
|--------|-------|
| **Token theft** (XSS) | HttpOnly cookie, hoặc lưu trong memory; CSP header |
| **Token replay** | Short expiry (5 phút), revoke list |
| **Cross-Site Request Forgery (CSRF)** | Dùng Authorization header thay cookie session → tự miễn nhiễm |
| **JWT confusion** (alg=none) | Spring Security mặc định reject `alg: none` |
| **JWK kid spoofing** | Spring chỉ accept kid trong JWKS tin cậy |
| **Brute force login** | Rate limit `/api/auth/**` (10/s/IP), Keycloak có lockout |

---

## 6. Từ Khóa Nghiên Cứu

```
- oauth 2.0 rfc 6749
- openid connect spec
- jwt rfc 7519 jws jwe
- keycloak realm client role
- pkce rfc 7636 single page app
- jwk set endpoint key rotation
- access token vs refresh token vs id token
- spring security oauth2 resource server
- stateless authentication microservices
- BCP for OAuth 2.0 (rfc 8252, oauth 2.1 draft)
```

---

## 7. Câu Hỏi Phản Biện

**Q1: Tại sao chọn Keycloak mà không tự code login?**
→ Identity là phần cực kỳ phức tạp (password hashing, OTP, social login, audit). Keycloak là production-grade open source, do RedHat maintain. Tự code dễ vulnerable.

**Q2: Tại sao JWT mà không phải session?**
→ Microservice cần stateless để scale ngang. Session cần shared store (Redis), thêm latency. JWT tự chứa, mỗi service verify cục bộ.

**Q3: Nhược điểm của JWT?**
→ (1) Khó revoke ngay (token vẫn valid đến exp) — em giải bằng short expiry. (2) Token size lớn (~1KB). (3) Nếu lộ private key → toàn hệ thống compromise.

**Q4: Khác biệt giữa access token và ID token?**
→ Access token cho Resource Server (API). ID token cho Client app (chứa thông tin user để render UI). ID token KHÔNG nên gửi đến API.

**Q5: Em dùng grant type gì?**
→ Đồ án demo dùng Resource Owner Password (đơn giản cho test). Production phải dùng Authorization Code + PKCE.

**Q6: Backend tin tưởng header X-User-Id không an toàn?**
→ Đây là Trusted Subsystem pattern. An toàn miễn là backend không expose ra Internet. Defense-in-depth: backend cũng có thể validate JWT (đánh đổi latency).

**Q7: Tại sao Keycloak ký bằng RS256 thay vì HS256?**
→ RS256 (asymmetric): Keycloak giữ private key, service verify bằng public key. Không service nào biết secret. HS256 (symmetric) yêu cầu mỗi service biết shared secret — rủi ro lộ.

**Q8: Refresh token bị steal thì sao?**
→ Có thể dùng **refresh token rotation** (single-use): mỗi lần refresh trả refresh token mới. Nếu phát hiện refresh cũ → revoke cả family.

---

## 8. Tài Liệu Tham Khảo

### RFCs (BẮT BUỘC trích trong báo cáo)
- **RFC 6749** — The OAuth 2.0 Authorization Framework
- **RFC 7519** — JSON Web Token (JWT)
- **RFC 7515** — JSON Web Signature (JWS)
- **RFC 7636** — Proof Key for Code Exchange (PKCE)
- **RFC 8252** — OAuth 2.0 for Native Apps
- OpenID Connect Core 1.0 spec — openid.net/connect/

### Sách
- Justin Richer, Antonio Sanso, *OAuth 2 in Action*, Manning, 2017
- Prabath Siriwardena, *Microservices Security in Action*, Manning, 2020
- Stian Thorgersen, *Keycloak — Identity and Access Management*, Packt, 2023

### Tài liệu chính thức
- https://www.keycloak.org/documentation
- Spring Security Reference — OAuth2 Resource Server
- OWASP "Authentication Cheat Sheet"
- OAuth 2.0 Security Best Current Practice (draft-ietf-oauth-security-topics)
