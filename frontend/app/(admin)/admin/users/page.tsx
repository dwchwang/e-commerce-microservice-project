import Link from "next/link";
import { Search } from "lucide-react";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminNotice } from "@/components/admin/AdminNotice";
import { AdminTableShell } from "@/components/admin/AdminTableShell";
import { AdminPagination } from "@/components/admin/AdminPagination";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatDateTime, toAdminPage, type PagePayload } from "@/lib/admin/api";
import type { UserAdmin } from "@/lib/admin/types";

export default async function AdminUsersPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const sp = await searchParams;
  const q = String(sp.q ?? "");
  const page = String(sp.page ?? "0");
  const query = new URLSearchParams({ size: "20", page });
  if (q) query.set("q", q);
  const payload = await adminFetchSafe<PagePayload<UserAdmin>>(`/users/admin?${query.toString()}`, { content: [] });
  const users = toAdminPage(payload, 20);

  return (
    <>
      <AdminPageHeader title="Khách hàng" description="Danh sách hồ sơ khách hàng đã đồng bộ từ identity/user-service." />
      <AdminNotice title="Keycloak account actions">
        Phase 14 hiển thị hồ sơ người dùng; thao tác ban/unban cần identity-service tích hợp Keycloak Admin API và được ghi vào tài liệu.
      </AdminNotice>
      <AdminTableShell
        filters={
          <form className="flex max-w-lg gap-2">
            <Input name="q" defaultValue={q} placeholder="Email, tên hoặc số điện thoại" />
            <Button type="submit" variant="outline">
              <Search className="size-4" />
              Tìm
            </Button>
          </form>
        }
        footer={
          <AdminPagination
            basePath="/admin/users"
            page={users.page}
            totalPages={users.totalPages}
            totalElements={users.totalElements}
            searchParams={sp}
          />
        }
      >
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Email</TableHead>
              <TableHead>Họ tên</TableHead>
              <TableHead>Điện thoại</TableHead>
              <TableHead>Điểm</TableHead>
              <TableHead>Ngày tạo</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.items.map((user) => (
              <TableRow key={user.id}>
                <TableCell>
                  <Link href={`/admin/users/${user.id}`} className="font-medium hover:underline">
                    {user.email}
                  </Link>
                </TableCell>
                <TableCell>{user.fullName ?? "-"}</TableCell>
                <TableCell>{user.phoneNumber ?? "-"}</TableCell>
                <TableCell>{user.loyaltyPoints ?? 0}</TableCell>
                <TableCell>{formatDateTime(user.createdAt)}</TableCell>
              </TableRow>
            ))}
            {users.items.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="h-24 text-center text-muted-foreground">
                  Chưa có khách hàng phù hợp.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </AdminTableShell>
    </>
  );
}
