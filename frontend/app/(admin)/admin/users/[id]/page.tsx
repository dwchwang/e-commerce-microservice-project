import { notFound } from "next/navigation";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatCurrency, formatDateTime } from "@/lib/admin/api";
import type { OrderAdmin, ReviewAdmin, UserAdmin } from "@/lib/admin/types";

export default async function AdminUserDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const user = await adminFetchSafe<UserAdmin | null>(`/users/admin/${id}`, null);

  if (!user) notFound();

  // Orders and reviews are keyed by the Keycloak user id (JWT subject), not the
  // user-service profile id. Fall back to the profile id only if missing.
  const ownerId = user.keycloakUserId ?? user.id;
  const [orders, reviews] = await Promise.all([
    adminFetchSafe<OrderAdmin[]>(`/orders/admin?userId=${encodeURIComponent(ownerId)}`, []),
    adminFetchSafe<ReviewAdmin[]>(`/reviews/admin?userId=${encodeURIComponent(ownerId)}`, []),
  ]);

  return (
    <>
      <AdminPageHeader title={user.email} description={user.fullName ?? "Hồ sơ khách hàng"} />
      <div className="grid gap-4 xl:grid-cols-[360px_1fr]">
        <Card className="rounded-lg">
          <CardHeader>
            <CardTitle>Profile</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <Info label="Email" value={user.email} />
            <Info label="Họ tên" value={user.fullName} />
            <Info label="Điện thoại" value={user.phoneNumber} />
            <Info label="Keycloak ID" value={user.keycloakUserId} />
            <Info label="Loyalty" value={String(user.loyaltyPoints ?? 0)} />
            <Info label="Ngày tạo" value={formatDateTime(user.createdAt)} />
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card className="rounded-lg">
            <CardHeader>
              <CardTitle>Đơn hàng</CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Mã đơn</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead>Ngày tạo</TableHead>
                    <TableHead className="text-right">Tổng</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {orders.map((order) => (
                    <TableRow key={order.id}>
                      <TableCell>{order.id.slice(0, 8)}</TableCell>
                      <TableCell>
                        <StatusBadge status={order.status} />
                      </TableCell>
                      <TableCell>{formatDateTime(order.createdAt)}</TableCell>
                      <TableCell className="text-right">{formatCurrency(order.totalAmount)}</TableCell>
                    </TableRow>
                  ))}
                  {orders.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4} className="h-16 text-center text-muted-foreground">
                        Chưa có đơn hàng.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card className="rounded-lg">
            <CardHeader>
              <CardTitle>Đánh giá</CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Product ID</TableHead>
                    <TableHead>Rating</TableHead>
                    <TableHead>Nội dung</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {reviews.map((review) => (
                    <TableRow key={review.id}>
                      <TableCell>{review.productId}</TableCell>
                      <TableCell>{review.rating}/5</TableCell>
                      <TableCell className="max-w-md truncate">{review.comment ?? "-"}</TableCell>
                    </TableRow>
                  ))}
                  {reviews.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={3} className="h-16 text-center text-muted-foreground">
                        Chưa có đánh giá.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      </div>
    </>
  );
}

function Info({ label, value }: { label: string; value?: string | null }) {
  return (
    <div className="grid grid-cols-[110px_1fr] gap-2">
      <span className="text-muted-foreground">{label}</span>
      <span className="break-all">{value || "-"}</span>
    </div>
  );
}
