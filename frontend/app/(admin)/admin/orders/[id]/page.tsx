import type { ReactNode } from "react";
import { notFound } from "next/navigation";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminForm } from "@/components/admin/AdminForm";
import { SubmitButton } from "@/components/admin/SubmitButton";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatCurrency, formatDateTime } from "@/lib/admin/api";
import type { OrderAdmin } from "@/lib/admin/types";
import { updateOrderStatusAction } from "../actions";

export default async function AdminOrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const order = await adminFetchSafe<OrderAdmin | null>(`/orders/admin/${id}`, null);
  if (!order) notFound();

  return (
    <>
      <AdminPageHeader title={`Đơn hàng ${order.id.slice(0, 8)}`} description={`Tạo lúc ${formatDateTime(order.createdAt)}`} />
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card className="rounded-lg">
          <CardHeader>
            <CardTitle>Sản phẩm</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>SKU</TableHead>
                  <TableHead>Sản phẩm</TableHead>
                  <TableHead className="text-right">SL</TableHead>
                  <TableHead className="text-right">Đơn giá</TableHead>
                  <TableHead className="text-right">Tạm tính</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(order.items ?? []).map((item) => (
                  <TableRow key={item.id ?? item.sku}>
                    <TableCell>{item.sku ?? "-"}</TableCell>
                    <TableCell>{item.productName}</TableCell>
                    <TableCell className="text-right">{item.quantity}</TableCell>
                    <TableCell className="text-right">{formatCurrency(item.price)}</TableCell>
                    <TableCell className="text-right">{formatCurrency(item.subtotal)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <div className="mt-4 flex justify-end">
              <div className="w-full max-w-sm space-y-2 text-sm">
                <TotalRow label="Subtotal" value={formatCurrency(order.subtotal)} />
                <TotalRow label="Discount" value={formatCurrency(order.discountAmount)} />
                <TotalRow label="Total" value={formatCurrency(order.totalAmount)} strong />
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card className="rounded-lg">
            <CardHeader>
              <CardTitle>Thông tin đơn</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <Info label="Khách hàng" value={order.userEmail ?? order.userId} />
              <Info label="Trạng thái" value={<StatusBadge status={order.status} />} />
              <Info label="Thanh toán" value={order.paymentMethod} />
              <Info label="Người nhận" value={order.shippingName} />
              <Info label="SĐT" value={order.shippingPhone} />
              <Info label="Địa chỉ" value={order.shippingAddress} />
            </CardContent>
          </Card>

          <AdminForm action={updateOrderStatusAction} successMessage="Đã cập nhật trạng thái đơn" className="rounded-lg border bg-card p-4">
            <input type="hidden" name="orderId" value={order.id} />
            <h2 className="mb-4 text-base font-semibold">Cập nhật trạng thái</h2>
            <Label className="mb-2 block">Trạng thái mới</Label>
            <select name="status" defaultValue={order.status} className="mb-3 h-8 w-full rounded-lg border border-input bg-background px-2 text-sm">
              {["PENDING", "STOCK_RESERVED", "CONFIRMED", "CANCELLED"].map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
            <Label className="mb-2 block">Ghi chú nội bộ</Label>
            <Input name="note" placeholder="tracking number, lý do hủy..." />
            <SubmitButton className="mt-4 w-full" pendingText="Đang lưu...">
              Lưu trạng thái
            </SubmitButton>
          </AdminForm>
        </div>
      </div>
    </>
  );
}

function TotalRow({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className={strong ? "flex justify-between border-t pt-2 font-semibold" : "flex justify-between"}>
      <span>{label}</span>
      <span>{value}</span>
    </div>
  );
}

function Info({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="grid grid-cols-[110px_1fr] gap-2">
      <span className="text-muted-foreground">{label}</span>
      <span>{value ?? "-"}</span>
    </div>
  );
}
