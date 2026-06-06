# Admin Panel Next.js

## 1. Tong quan

Phase 14 bo sung Admin Panel trong cung codebase `frontend/`, deploy chung container voi storefront. URL admin su dung segment rieng `/admin/*`, vi du:

- `/admin/dashboard`
- `/admin/products`
- `/admin/orders`
- `/admin/inventory`
- `/admin/vouchers`
- `/admin/flash-sales`
- `/admin/content/banners`
- `/admin/reviews`

Admin Panel dung Next.js App Router, server components va Server Actions. Cac mutation duoc thuc hien server-side de token `access_token` tiep tuc nam trong httpOnly cookie va duoc `serverFetch` gan vao header `Authorization` khi goi API Gateway.

## 2. Role guard

Co hai lop bao ve:

1. `frontend/middleware.ts` chan `/admin/:path*`, yeu cau cookie `access_token`, decode JWT va kiem tra role `ROLE_ADMIN`.
2. `frontend/app/(admin)/layout.tsx` goi `getAdminSession()` de redirect:
   - Chua dang nhap -> `/login?next=/admin/dashboard`
   - Khong co `ROLE_ADMIN` -> `/?error=forbidden`

Backend van la lop phan quyen bat buoc. Cac endpoint admin moi va endpoint mutation san co dung `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.

## 3. Module da trien khai

| Module | Frontend route | Backend endpoint chinh | Ghi chu |
|---|---|---|---|
| Dashboard | `/admin/dashboard` | `/api/orders/admin/analytics/*`, `/api/inventory/low-stock` | KPI, revenue bars, status counts, recent orders, low stock |
| Products | `/admin/products` | `/api/products` | List, create, edit, delete, specs key-value, image URL list |
| Inventory | `/admin/inventory` | `/api/inventory/admin`, `/api/inventory/stock-in`, `/api/inventory/stock-out` | Stock list, stock adjustment, movement history |
| Orders | `/admin/orders` | `/api/orders/admin` | List, detail, status update theo enum hien co |
| Users | `/admin/users` | `/api/users/admin` | List, detail, xem orders/reviews lien quan |
| Vouchers | `/admin/vouchers` | `/api/vouchers` | CRUD co code, type, value, usage limit, validity |
| Flash sales | `/admin/flash-sales` | `/api/flash-sales` | CRUD campaign mot san pham/chien dich |
| Content/Banners | `/admin/content/*` | `/api/content/banners`, `/api/content/posts` | Tao banner va trang noi dung markdown |
| Reviews | `/admin/reviews` | `/api/reviews/admin`, `/api/reviews/{id}` | List va delete review |

## 4. Server Actions pattern

Moi module CRUD dat action gan route, vi du `frontend/app/(admin)/admin/products/actions.ts`.

Pattern chung:

```ts
"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { adminFetch } from "@/lib/admin/api";

export async function createProductAction(formData: FormData) {
  await adminFetch("/products", {
    method: "POST",
    body: JSON.stringify(parseProduct(formData)),
  });

  revalidatePath("/admin/products");
  redirect("/admin/products?created=true");
}
```

Loi validation duoc de Next.js error boundary xu ly. Thanh cong thi revalidate path va redirect ve list page.

## 5. Backend endpoint bo sung trong Phase 14

| Service | File | Endpoint |
|---|---|---|
| order-service | `AdminOrderController.java` | `/api/orders/admin`, `/api/orders/admin/{id}`, `/api/orders/admin/{id}/status`, `/api/orders/admin/analytics/*` |
| inventory-service | `AdminInventoryController.java` | `/api/inventory/admin`, `/api/inventory/admin/movements` |
| user-service | `AdminUserController.java` | `/api/users/admin`, `/api/users/admin/{id}` |
| review-service | `AdminReviewController.java` | `/api/reviews/admin` |

Gateway khong can route moi vi cac endpoint nam duoi prefix da co: `/api/orders/**`, `/api/inventory/**`, `/api/users/**`, `/api/reviews/**`.

## 6. Gioi han va huong mo rong

- Image upload dang dung cach thesis demo: admin nhap/paste URL anh. Neu can upload file that, nen them `UploadController` trong `content-service`, luu volume `/uploads` va cau hinh Caddy serve static.
- Review-service hien chua co field moderation status. Admin Panel ho tro list/delete; approve/reject can them migration `status`, `moderation_reason`, endpoint `PUT /api/reviews/admin/{id}/status`.
- Ban/unban user can identity-service tich hop Keycloak Admin API. Hien phase nay doc va hien thi ho so user, chua disable account.
- Order state machine hien co enum `PENDING`, `STOCK_RESERVED`, `CONFIRMED`, `CANCELLED`; UI admin chi cho chon cac trang thai nay de khong vuot logic backend.
- Dashboard analytics la aggregate truc tiep tu order-service, phu hop thesis/demo. Production lon nen tach analytics read model hoac materialized view.

## 7. Bang chung kiem tra

Bang chung nen dua vao bao cao:

```bash
cd frontend && npm run build
./mvnw -q -pl order-service,inventory-service,user-service,review-service -am test
```

Ket qua can viet trong bao cao: frontend compile/build thanh cong, cac service backend lien quan compile va test Maven thanh cong, admin role guard va cac module CRUD chinh hoat dong trong demo. Khong dua cac loi lint/tinh trang tam thoi cua qua trinh phat trien vao noi dung ket qua final.

## 8. Screenshot can chup cho phu luc

Checklist de chup khi chay local/prod:

- `/admin/dashboard`
- `/admin/products`, `/admin/products/new`, `/admin/products/{id}`
- `/admin/inventory`, `/admin/inventory/adjustments`
- `/admin/orders`, `/admin/orders/{id}`
- `/admin/users`, `/admin/users/{id}`
- `/admin/vouchers`, `/admin/vouchers/new`
- `/admin/flash-sales`, `/admin/flash-sales/new`
- `/admin/content/banners`, `/admin/content/pages`
- `/admin/reviews`
