import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminNotice } from "@/components/admin/AdminNotice";
import { AdminTableShell } from "@/components/admin/AdminTableShell";
import { DeleteButton } from "@/components/admin/DeleteButton";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatDateTime } from "@/lib/admin/api";
import type { ReviewAdmin } from "@/lib/admin/types";
import { deleteReviewAction } from "./actions";

export default async function AdminReviewsPage() {
  const reviews = await adminFetchSafe<ReviewAdmin[]>("/reviews/admin?size=100", []);

  return (
    <>
      <AdminPageHeader title="Đánh giá" description="Kiểm duyệt và xóa đánh giá không phù hợp." />
      <AdminNotice title="Moderation scope">
        Review-service hiện lưu review đã xác thực mua hàng nhưng chưa có cột trạng thái PENDING/APPROVED/REJECTED; Admin Panel hỗ trợ danh sách và xóa vĩnh viễn, phần approve/reject được ghi trong docs như endpoint cần mở rộng.
      </AdminNotice>
      <AdminTableShell footer={`Tổng ${reviews.length} đánh giá`}>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Product ID</TableHead>
              <TableHead>User ID</TableHead>
              <TableHead>Rating</TableHead>
              <TableHead>Nội dung</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Ngày tạo</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {reviews.map((review) => (
              <TableRow key={review.id}>
                <TableCell>{review.productId}</TableCell>
                <TableCell>{review.userId}</TableCell>
                <TableCell>{review.rating}/5</TableCell>
                <TableCell className="max-w-md truncate">{review.comment ?? "-"}</TableCell>
                <TableCell>
                  <StatusBadge status={review.status ?? "APPROVED"} />
                </TableCell>
                <TableCell>{formatDateTime(review.createdAt)}</TableCell>
                <TableCell className="text-right">
                  <DeleteButton
                    id={review.id}
                    action={deleteReviewAction}
                    title="Xóa đánh giá"
                    description="Xóa vĩnh viễn đánh giá này? Hành động không thể hoàn tác."
                    label="Xóa đánh giá"
                    successMessage="Đã xóa đánh giá"
                  />
                </TableCell>
              </TableRow>
            ))}
            {reviews.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="h-24 text-center text-muted-foreground">
                  Chưa có đánh giá.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </AdminTableShell>
    </>
  );
}
