import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminTableShell } from "@/components/admin/AdminTableShell";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatDateTime } from "@/lib/admin/api";

type Movement = {
  id: string;
  sku: string;
  movementType: string;
  quantity: number;
  referenceId?: string;
  note?: string;
  createdAt?: string;
};

export default async function InventoryAdjustmentsPage() {
  const movements = await adminFetchSafe<Movement[]>("/inventory/admin/movements?size=100", []);

  return (
    <>
      <AdminPageHeader title="Adjustment history" description="Audit log nhập/xuất kho từ inventory-service." />
      <AdminTableShell footer={`Tổng ${movements.length} movement`}>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>SKU</TableHead>
              <TableHead>Loại</TableHead>
              <TableHead className="text-right">Số lượng</TableHead>
              <TableHead>Ghi chú</TableHead>
              <TableHead>Thời gian</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {movements.map((movement) => (
              <TableRow key={movement.id}>
                <TableCell className="font-medium">{movement.sku}</TableCell>
                <TableCell>
                  <StatusBadge status={movement.movementType} />
                </TableCell>
                <TableCell className="text-right">{movement.quantity}</TableCell>
                <TableCell>{movement.note ?? movement.referenceId ?? "-"}</TableCell>
                <TableCell>{formatDateTime(movement.createdAt)}</TableCell>
              </TableRow>
            ))}
            {movements.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="h-24 text-center text-muted-foreground">
                  Chưa có lịch sử điều chỉnh.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </AdminTableShell>
    </>
  );
}
