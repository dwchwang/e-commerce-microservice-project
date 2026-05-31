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
import { Textarea } from "@/components/ui/textarea";
import { adminFetchSafe, formatDateTime, toAdminPage, type PagePayload } from "@/lib/admin/api";
import type { ContentAdmin } from "@/lib/admin/types";
import { createContentPageAction, deleteContentPageAction } from "../actions";

export default async function AdminContentPagesPage() {
  const payload = await adminFetchSafe<PagePayload<ContentAdmin>>("/content/posts?size=100", { content: [] });
  const pages = toAdminPage(payload, 100);

  return (
    <>
      <AdminPageHeader title="Nội dung tĩnh" description="FAQ, terms, privacy, about và các bài viết nội dung." />
      <div className="grid gap-4 xl:grid-cols-[1fr_420px]">
        <AdminTableShell footer={`Tổng ${pages.totalElements} trang`}>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tiêu đề</TableHead>
                <TableHead>Slug</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead>Published</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {pages.items.map((page) => (
                <TableRow key={page.id}>
                  <TableCell className="font-medium">{page.title}</TableCell>
                  <TableCell>{page.slug}</TableCell>
                  <TableCell>
                    <StatusBadge status={page.isPublished ? "PUBLISHED" : "DRAFT"} />
                  </TableCell>
                  <TableCell>{formatDateTime(page.publishedAt)}</TableCell>
                  <TableCell className="text-right">
                    <DeleteButton
                      id={page.id}
                      action={deleteContentPageAction}
                      title="Xóa trang nội dung"
                      description={`Xóa trang "${page.title}"?`}
                      label="Xóa trang"
                      successMessage="Đã xóa trang nội dung"
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </AdminTableShell>

        <AdminForm action={createContentPageAction} successMessage="Đã thêm trang" className="rounded-lg border bg-card p-4">
          <h2 className="mb-4 text-base font-semibold">Thêm trang</h2>
          <div className="space-y-4">
            <Field label="Tiêu đề">
              <Input name="title" required />
            </Field>
            <Field label="Slug">
              <Input name="slug" required />
            </Field>
            <Field label="Thumbnail URL">
              <Input name="thumbnailUrl" />
            </Field>
            <Field label="Author">
              <Input name="author" defaultValue="Admin" />
            </Field>
            <Field label="Nội dung markdown">
              <Textarea name="content" rows={10} required />
            </Field>
            <label className="flex items-center gap-2 text-sm">
              <Checkbox name="isPublished" defaultChecked />
              Publish
            </label>
            <SubmitButton className="w-full" pendingText="Đang lưu...">
              Lưu trang
            </SubmitButton>
          </div>
        </AdminForm>
      </div>
    </>
  );
}
