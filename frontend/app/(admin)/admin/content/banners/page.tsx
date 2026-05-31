import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminTableShell } from "@/components/admin/AdminTableShell";
import { AdminForm } from "@/components/admin/AdminForm";
import { SubmitButton } from "@/components/admin/SubmitButton";
import { DeleteButton } from "@/components/admin/DeleteButton";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Field } from "@/components/admin/forms/Field";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatDateTime } from "@/lib/admin/api";
import type { BannerAdmin } from "@/lib/admin/types";
import { createBannerAction, deleteBannerAction } from "../actions";

export default async function AdminBannersPage() {
  const banners = await adminFetchSafe<BannerAdmin[]>("/content/banners/active", []);

  return (
    <>
      <AdminPageHeader title="Banner" description="Quản lý hero/banner hiển thị trên storefront." />
      <div className="grid gap-4 xl:grid-cols-[1fr_380px]">
        <AdminTableShell footer={`Tổng ${banners.length} banner active`}>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tiêu đề</TableHead>
                <TableHead>Link</TableHead>
                <TableHead>Thứ tự</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead>Thời hạn</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {banners.map((banner) => (
                <TableRow key={banner.id}>
                  <TableCell className="font-medium">{banner.title}</TableCell>
                  <TableCell className="max-w-xs truncate">{banner.linkUrl ?? banner.imageUrl}</TableCell>
                  <TableCell>{banner.displayOrder ?? 0}</TableCell>
                  <TableCell>
                    <StatusBadge status={banner.isActive ?? false} />
                  </TableCell>
                  <TableCell>
                    {formatDateTime(banner.startDate)} - {formatDateTime(banner.endDate)}
                  </TableCell>
                  <TableCell className="text-right">
                    <DeleteButton
                      id={banner.id}
                      action={deleteBannerAction}
                      title="Xóa banner"
                      description={`Xóa banner "${banner.title}"?`}
                      label="Xóa banner"
                      successMessage="Đã xóa banner"
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </AdminTableShell>

        <AdminForm action={createBannerAction} successMessage="Đã thêm banner" className="rounded-lg border bg-card p-4">
          <h2 className="mb-4 text-base font-semibold">Thêm banner</h2>
          <div className="space-y-4">
            <Field label="Tiêu đề">
              <Input name="title" required />
            </Field>
            <Field label="Image URL">
              <Input name="imageUrl" type="url" required />
            </Field>
            <Field label="Link URL">
              <Input name="linkUrl" />
            </Field>
            <Field label="Thứ tự">
              <Input name="displayOrder" type="number" defaultValue="0" />
            </Field>
            <Field label="Bắt đầu">
              <Input name="startDate" type="datetime-local" />
            </Field>
            <Field label="Kết thúc">
              <Input name="endDate" type="datetime-local" />
            </Field>
            <label className="flex items-center gap-2 text-sm">
              <Checkbox name="isActive" defaultChecked />
              Active
            </label>
            <SubmitButton className="w-full" pendingText="Đang lưu...">
              Lưu banner
            </SubmitButton>
          </div>
        </AdminForm>
      </div>
    </>
  );
}
