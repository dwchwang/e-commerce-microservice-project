import Link from "next/link";
import { Plus } from "lucide-react";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminTableShell } from "@/components/admin/AdminTableShell";
import { DeleteButton } from "@/components/admin/DeleteButton";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatCurrency, formatDateTime } from "@/lib/admin/api";
import type { FlashSaleAdmin } from "@/lib/admin/types";
import { deleteFlashSaleAction } from "./actions";

export default async function AdminFlashSalesPage() {
  const flashSales = await adminFetchSafe<FlashSaleAdmin[]>("/flash-sales/admin", []);

  return (
    <>
      <AdminPageHeader
        title="Flash sale"
        description="Tạo lịch bán nhanh, giá sale và tồn kho chiến dịch."
        action={
          <Link href="/admin/flash-sales/new" className="inline-flex h-8 items-center gap-1.5 rounded-lg bg-primary px-2.5 text-sm font-medium text-primary-foreground">
            <Plus className="size-4" />
            Thêm flash-sale
          </Link>
        }
      />
      <AdminTableShell footer={`Tổng ${flashSales.length} chiến dịch`}>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Sản phẩm</TableHead>
              <TableHead>SKU</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Giá sale</TableHead>
              <TableHead className="text-right">Còn lại</TableHead>
              <TableHead>Thời gian</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {flashSales.map((item) => (
              <TableRow key={item.id}>
                <TableCell>
                  <Link href={`/admin/flash-sales/${item.id}`} className="font-medium hover:underline">
                    {item.productName}
                  </Link>
                </TableCell>
                <TableCell>{item.sku}</TableCell>
                <TableCell>
                  <StatusBadge status={item.status} />
                </TableCell>
                <TableCell className="text-right">{formatCurrency(item.salePrice)}</TableCell>
                <TableCell className="text-right">{item.remainingStock ?? item.quantity}</TableCell>
                <TableCell>
                  {formatDateTime(item.startTime)} - {formatDateTime(item.endTime)}
                </TableCell>
                <TableCell className="text-right">
                  <DeleteButton
                    id={item.id}
                    action={deleteFlashSaleAction}
                    title="Hủy flash-sale"
                    description={`Hủy chiến dịch "${item.productName}"? Chiến dịch sẽ chuyển sang trạng thái CANCELLED.`}
                    label="Hủy flash-sale"
                    successMessage="Đã hủy flash-sale"
                  />
                </TableCell>
              </TableRow>
            ))}
            {flashSales.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="h-24 text-center text-muted-foreground">
                  Chưa có flash-sale.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </AdminTableShell>
    </>
  );
}
