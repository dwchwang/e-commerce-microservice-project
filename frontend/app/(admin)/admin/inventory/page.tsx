import { PackageCheck } from "lucide-react";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminNotice } from "@/components/admin/AdminNotice";
import { AdminTableShell } from "@/components/admin/AdminTableShell";
import { AdminForm } from "@/components/admin/AdminForm";
import { SubmitButton } from "@/components/admin/SubmitButton";
import { Field } from "@/components/admin/forms/Field";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatDateTime } from "@/lib/admin/api";
import type { InventoryAdmin } from "@/lib/admin/types";
import { adjustStockAction } from "./actions";

export default async function AdminInventoryPage() {
  const items = await adminFetchSafe<InventoryAdmin[]>("/inventory/admin", []);

  return (
    <>
      <AdminPageHeader
        title="Tồn kho"
        description="Theo dõi số lượng tổng, số lượng đã giữ và số lượng còn có thể bán."
      />
      <AdminNotice title="Stock adjustment">
        Điều chỉnh tồn kho dùng endpoint `stock-in/stock-out` hiện có; lịch sử chi tiết nằm ở tab Adjustment history.
      </AdminNotice>

      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <AdminTableShell footer={`Tổng ${items.length} SKU`}>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>SKU</TableHead>
                <TableHead>Sản phẩm</TableHead>
                <TableHead className="text-right">Tổng</TableHead>
                <TableHead className="text-right">Đã giữ</TableHead>
                <TableHead className="text-right">Còn bán</TableHead>
                <TableHead>Cập nhật</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((item) => (
                <TableRow key={item.id ?? item.sku}>
                  <TableCell className="font-medium">{item.sku}</TableCell>
                  <TableCell>{item.productName ?? "-"}</TableCell>
                  <TableCell className="text-right">{item.quantity}</TableCell>
                  <TableCell className="text-right">{item.reservedQuantity ?? 0}</TableCell>
                  <TableCell className="text-right">{item.availableQuantity ?? item.quantity}</TableCell>
                  <TableCell>{formatDateTime(item.updatedAt)}</TableCell>
                </TableRow>
              ))}
              {items.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="h-24 text-center text-muted-foreground">
                    Chưa có dữ liệu tồn kho.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </AdminTableShell>

        <AdminForm action={adjustStockAction} successMessage="Đã cập nhật tồn kho" className="rounded-lg border bg-card p-4">
          <h2 className="mb-4 flex items-center gap-2 text-base font-semibold">
            <PackageCheck className="size-4" />
            Điều chỉnh tồn kho
          </h2>
          <div className="space-y-4">
            <Field label="SKU">
              <Input name="sku" required />
            </Field>
            <Field label="Tên sản phẩm">
              <Input name="productName" />
            </Field>
            <Field label="Loại điều chỉnh">
              <select name="type" className="h-8 w-full rounded-lg border border-input bg-background px-2 text-sm">
                <option value="IN">Nhập kho</option>
                <option value="OUT">Xuất/giảm kho</option>
              </select>
            </Field>
            <Field label="Số lượng">
              <Input name="quantity" type="number" min="1" required />
            </Field>
            <Field label="Ghi chú">
              <Input name="note" placeholder="restock, damage, correction..." />
            </Field>
            <SubmitButton className="w-full" pendingText="Đang lưu...">
              Lưu điều chỉnh
            </SubmitButton>
          </div>
        </AdminForm>
      </div>
    </>
  );
}
