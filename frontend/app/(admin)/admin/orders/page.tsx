import Link from "next/link";
import { Search } from "lucide-react";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminTableShell } from "@/components/admin/AdminTableShell";
import { AdminPagination } from "@/components/admin/AdminPagination";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatCurrency, formatDateTime, toAdminPage, type PagePayload } from "@/lib/admin/api";
import type { OrderAdmin } from "@/lib/admin/types";

export default async function AdminOrdersPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const sp = await searchParams;
  const q = String(sp.q ?? "");
  const status = String(sp.status ?? "");
  const page = String(sp.page ?? "0");
  const query = new URLSearchParams({ size: "20", page });
  if (q) query.set("q", q);
  if (status) query.set("status", status);

  const payload = await adminFetchSafe<PagePayload<OrderAdmin>>(`/orders/admin?${query.toString()}`, { content: [] });
  const orders = toAdminPage(payload, 20);

  return (
    <>
      <AdminPageHeader title="Đơn hàng" description="Theo dõi đơn hàng, thanh toán, trạng thái xử lý và flash-sale orders." />
      <AdminTableShell
        filters={
          <form className="flex flex-col gap-2 md:flex-row">
            <Input name="q" defaultValue={q} placeholder="Email khách hàng hoặc mã đơn" className="md:max-w-xs" />
            <select name="status" defaultValue={status} className="h-8 rounded-lg border border-input bg-background px-2 text-sm">
              <option value="">Tất cả trạng thái</option>
              {["PENDING", "STOCK_RESERVED", "CONFIRMED", "CANCELLED"].map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
            <Button type="submit" variant="outline">
              <Search className="size-4" />
              Lọc
            </Button>
          </form>
        }
        footer={
          <AdminPagination
            basePath="/admin/orders"
            page={orders.page}
            totalPages={orders.totalPages}
            totalElements={orders.totalElements}
            searchParams={sp}
          />
        }
      >
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mã đơn</TableHead>
              <TableHead>Khách hàng</TableHead>
              <TableHead>Thanh toán</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Ngày tạo</TableHead>
              <TableHead className="text-right">Tổng</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {orders.items.map((order) => (
              <TableRow key={order.id}>
                <TableCell>
                  <Link href={`/admin/orders/${order.id}`} className="font-medium hover:underline">
                    {order.id.slice(0, 8)}
                  </Link>
                </TableCell>
                <TableCell>{order.userEmail ?? order.userId ?? "-"}</TableCell>
                <TableCell>{order.paymentMethod ?? "-"}</TableCell>
                <TableCell>
                  <StatusBadge status={order.status} />
                </TableCell>
                <TableCell>{formatDateTime(order.createdAt)}</TableCell>
                <TableCell className="text-right">{formatCurrency(order.totalAmount)}</TableCell>
              </TableRow>
            ))}
            {orders.items.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="h-24 text-center text-muted-foreground">
                  Chưa có đơn hàng phù hợp.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </AdminTableShell>
    </>
  );
}
