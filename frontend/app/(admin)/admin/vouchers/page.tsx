import Link from "next/link";
import { Plus } from "lucide-react";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminTableShell } from "@/components/admin/AdminTableShell";
import { DeleteButton } from "@/components/admin/DeleteButton";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatCurrency, formatDateTime, toAdminPage, type PagePayload } from "@/lib/admin/api";
import type { VoucherAdmin } from "@/lib/admin/types";
import { deleteVoucherAction } from "./actions";

export default async function AdminVouchersPage() {
  const payload = await adminFetchSafe<PagePayload<VoucherAdmin>>("/vouchers?size=100", { content: [] });
  const vouchers = toAdminPage(payload, 100);

  return (
    <>
      <AdminPageHeader
        title="Voucher"
        description="CRUD mã khuyến mãi, thời hạn, giới hạn lượt dùng và trạng thái."
        action={
          <Link href="/admin/vouchers/new" className="inline-flex h-8 items-center gap-1.5 rounded-lg bg-primary px-2.5 text-sm font-medium text-primary-foreground">
            <Plus className="size-4" />
            Thêm voucher
          </Link>
        }
      />
      <AdminTableShell footer={`Tổng ${vouchers.totalElements} voucher`}>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Code</TableHead>
              <TableHead>Loại</TableHead>
              <TableHead className="text-right">Giá trị</TableHead>
              <TableHead>Lượt dùng</TableHead>
              <TableHead>Hiệu lực</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {vouchers.items.map((voucher) => (
              <TableRow key={voucher.id}>
                <TableCell>
                  <Link href={`/admin/vouchers/${voucher.id}`} className="font-medium hover:underline">
                    {voucher.code}
                  </Link>
                </TableCell>
                <TableCell>{voucher.discountType}</TableCell>
                <TableCell className="text-right">{formatCurrency(voucher.discountValue)}</TableCell>
                <TableCell>
                  {voucher.usedCount ?? 0}/{voucher.usageLimit ?? "-"}
                </TableCell>
                <TableCell>
                  {formatDateTime(voucher.startDate)} - {formatDateTime(voucher.endDate)}
                </TableCell>
                <TableCell>
                  <StatusBadge status={voucher.isActive ?? false} />
                </TableCell>
                <TableCell className="text-right">
                  <DeleteButton
                    id={voucher.id}
                    action={deleteVoucherAction}
                    title="Xóa voucher"
                    description={`Xóa voucher "${voucher.code}"? Mã sẽ ngừng áp dụng tại checkout.`}
                    label="Xóa voucher"
                    successMessage="Đã xóa voucher"
                  />
                </TableCell>
              </TableRow>
            ))}
            {vouchers.items.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="h-24 text-center text-muted-foreground">
                  Chưa có voucher.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </AdminTableShell>
    </>
  );
}
