# 5.x. Frontend — Next.js 15 Storefront

Tài liệu này mô tả kiến trúc và triển khai frontend e-commerce storefront sử dụng Next.js 15 App Router.

---

## 5.x.1. Tổng quan kiến trúc

```
Browser (HTTPS)
       │
       ▼
┌──────────────────────────────────────┐
│  Caddy Reverse Proxy (:443)          │
│  app.<ip>.nip.io → localhost:3000    │
└──────────┬───────────────────────────┘
           │
           ▼
┌──────────────────────────────────────┐
│  Next.js 15 (standalone)             │
│  ┌────────────────────────────────┐  │
│  │  App Router                    │  │
│  │  • RSC (server) → BE directly │  │
│  │  • Client Components           │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │  BFF Proxy /api/proxy/*        │  │
│  │  • Attaches token from cookie │  │
│  │  • Attaches X-Session-Id      │  │
│  │  • Forwards to API Gateway    │  │
│  └────────────────────────────────┘  │
└──────────┬───────────────────────────┘
           │ (Docker network)
           ▼
┌──────────────────────────────────────┐
│  API Gateway :8080                   │
│  → 13 Business Services             │
└──────────────────────────────────────┘
```

---

## 5.x.2. Tech Stack

| Layer            | Technology                               |
| ---------------- | ---------------------------------------- |
| Framework        | Next.js 15 (App Router)                  |
| Language         | TypeScript                               |
| Styling          | Tailwind CSS v4                          |
| UI Components    | shadcn/ui (Radix primitives)             |
| Data Fetching    | TanStack Query (React Query v5)          |
| Form Validation  | react-hook-form + zod                    |
| State Management | Zustand (compare), React Context (query) |
| Auth             | BFF pattern — httpOnly cookies           |
| Icons            | Lucide React                             |
| Notifications    | Sonner (toast)                           |

---

## 5.x.3. Auth Flow (BFF Pattern)

1. User submits login form → `POST /api/auth/login` (Next.js Route Handler)
2. Handler forwards to BE `POST /api/auth/login`, receives access token + refresh token
3. Handler sets `access_token` and `refresh_token` as **httpOnly cookies** (never exposed to browser JS)
4. Middleware protects `/checkout`, `/orders`, `/profile`, `/addresses` — redirects to `/login` if no token
5. BFF proxy `/api/proxy/*` reads cookie, attaches `Authorization: Bearer` header, forwards to BE
6. Session endpoint `/api/auth/session` decodes JWT payload (no verify) for UI display

**Security benefits:**

- Token never in localStorage (no XSS risk)
- `SameSite=Lax` prevents CSRF
- `Secure` flag in production

---

## 5.x.4. Cart: Guest → User Merge

1. Guest browses, adds items → `X-Session-Id` header with UUID v4
2. Guest session ID stored in cookie (30 days)
3. On login success, route handler calls `POST /api/cart/merge` with both `Authorization: Bearer` and `X-Session-Id`
4. Cart service merges guest items into user cart (Redis), deletes guest cart
5. Client invalidates cart query → refreshed UI

---

## 5.x.5. Flash Sale UX

1. `app/(shop)/flash-sales/[id]/page.tsx` — RSC fetches flash sale detail
2. `<CountdownTimer>` — client component, realtime countdown via `setInterval`
3. `<FlashSaleBuyButton>` — disabled until `startAt`, calls `POST /api/flash-sales/{id}/purchase`
4. Status mapping from Lua script response:
   - Code 0: Success → redirect `/orders/{id}`
   - Code -1: Sold out → disable button
   - Code -2: Duplicate buyer → warning toast
   - Code -3: Not started → info toast
   - HTTP 429: Rate limited → warning toast

---

## 5.x.6. Advanced Filter

- **URL is source of truth**: filter params in query string (`?brand=Apple&ram=16GB`)
- **Facets from BE**: search-service Elasticsearch aggregations → brands, categories, price range
- **Client component**: `<ProductFilter>` with accordion checkboxes + price slider
- **Server component**: `products/page.tsx` RSC reads searchParams, fetches filtered products
- **Deep linkable**: share URL with filters → same results

---

## 5.x.7. Product Comparison

- **Zustand store**: persisted in localStorage, max 4 products
- **Compare button**: on product cards and detail page
- **Floating drawer**: appears when ≥1 product selected → "So sánh ngay" CTA
- **Compare page**: `/compare?ids=A,B,C,D` — RSC fetches all products, renders spec table
- **Spec table**: rows = attributes, columns = products, highlights differences

---

## 5.x.8. Folder Structure

```
frontend/
├── app/
│   ├── (shop)/           # Public storefront with Header/Footer
│   ├── (account)/         # Requires login (cart, orders, etc.)
│   ├── (auth)/            # Login/Register
│   ├── api/proxy/[...path]/route.ts  # BFF proxy
│   ├── api/auth/          # Auth route handlers
│   ├── providers.tsx      # QueryClient + Toaster + Tooltip
│   └── layout.tsx         # Root layout
├── components/
│   ├── ui/                # shadcn primitives
│   ├── product/           # ProductCard, ProductGrid, ProductFilter
│   ├── cart/              # CartDrawer, CartItem, AddToCartButton
│   ├── flash-sale/        # CountdownTimer, FlashSaleBuyButton
│   ├── compare/           # CompareDrawer
│   ├── layout/            # Header, Footer, UserMenu
│   └── shared/            # EmptyState, PriceTag
├── lib/
│   ├── api/               # client.ts, server-client.ts, endpoints.ts, types.ts
│   ├── auth/              # hooks.ts (useSession, useLogout)
│   ├── cart/              # guest-session.ts, hooks.ts
│   ├── compare/           # store.ts (Zustand)
│   └── query/             # keys.ts
├── Dockerfile             # Multi-stage: deps → builder → runner
├── next.config.ts         # standalone output, rewrites, image domains
└── .env.local.example     # NEXT_PUBLIC_API_BASE_URL, API_BASE_URL
```

---

## 5.x.9. Local Development

```bash
cd frontend
cp .env.local.example .env.local
# Edit .env.local with your BE URL
npm run dev
# → http://localhost:3000
```

With remote BE (AWS EC2 running):

```bash
NEXT_PUBLIC_API_BASE_URL=https://api.13-213-118-96.nip.io
API_BASE_URL=https://api.13-213-118-96.nip.io
```

With local BE (Docker Compose):

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
API_BASE_URL=http://localhost:8080
```

---

## 5.x.10. Containerization

Dockerfile 3-stage:

1. `deps` — npm ci for node_modules
2. `builder` — npm run build with build args for public env vars
3. `runner` — standalone output (~50MB), `node server.js`

Deploy via CI/CD: `.github/workflows/build-frontend.yml` builds and pushes to `ghcr.io/{owner}/frontend`.

---

## 5.x.11. Backend Changes for Phase 12

| Change                 | Service      | Description                                                  |
| ---------------------- | ------------ | ------------------------------------------------------------ |
| `POST /api/cart/merge` | cart-service | Merge guest cart to user cart on login                       |
| Address CRUD           | user-service | Already existed (`GET/POST/PUT/DELETE /api/users/addresses`) |

---

## 5.x.12. Screenshots (Phụ lục H)

Các màn hình chính cần chụp cho báo cáo:

1. Home page với featured products + flash sale banner
2. Catalog page với advanced filter sidebar
3. Product detail page với tabs (mô tả, thông số, đánh giá)
4. Search results page
5. Cart page (guest + user)
6. Login form
7. Register form
8. Checkout page với address picker + payment method
9. VNPAY redirect page
10. Order detail với status polling
11. Order history list
12. Flash sale page với countdown timer
13. Flash sale detail (đã hết hàng)
14. Compare page (2-4 sản phẩm)
15. Address book CRUD
16. Profile page
17. Mobile responsive view
