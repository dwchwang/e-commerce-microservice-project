import Link from "next/link";
import { Plus, Search } from "lucide-react";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminTableShell } from "@/components/admin/AdminTableShell";
import { AdminPagination } from "@/components/admin/AdminPagination";
import { DeleteButton } from "@/components/admin/DeleteButton";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatCurrency, toAdminPage, type PagePayload } from "@/lib/admin/api";
import type { ProductAdmin } from "@/lib/admin/types";
import { deleteProductAction } from "./actions";

export default async function AdminProductsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const sp = await searchParams;
  const q = String(sp.q ?? "");
  const page = String(sp.page ?? "0");
  const payload = await adminFetchSafe<PagePayload<ProductAdmin>>(
    `/products?size=20&page=${page}${q ? `&keyword=${encodeURIComponent(q)}` : ""}`,
    { content: [] }
  );
  const products = toAdminPage(payload, 20);

  return (
    <>
      <AdminPageHeader
        title="Sản phẩm"
        description="Quản lý catalog thiết bị điện tử, giá bán, ảnh và thông số kỹ thuật."
        action={
          <Link href="/admin/products/new" className="inline-flex h-8 items-center gap-1.5 rounded-lg bg-primary px-2.5 text-sm font-medium text-primary-foreground">
            <Plus className="size-4" />
            Thêm sản phẩm
          </Link>
        }
      />

      <AdminTableShell
        filters={
          <form className="flex max-w-lg items-center gap-2">
            <Input name="q" defaultValue={q} placeholder="Tìm theo tên hoặc SKU" />
            <Button type="submit" variant="outline">
              <Search className="size-4" />
              Tìm
            </Button>
          </form>
        }
        footer={
          <AdminPagination
            basePath="/admin/products"
            page={products.page}
            totalPages={products.totalPages}
            totalElements={products.totalElements}
            searchParams={sp}
          />
        }
      >
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>SKU</TableHead>
              <TableHead>Sản phẩm</TableHead>
              <TableHead>Danh mục</TableHead>
              <TableHead>Thương hiệu</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Giá</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {products.items.map((product) => (
              <TableRow key={product.id}>
                <TableCell className="font-medium">{product.sku}</TableCell>
                <TableCell>
                  <Link href={`/admin/products/${product.id}`} className="font-medium hover:underline">
                    {product.name}
                  </Link>
                </TableCell>
                <TableCell>{product.categoryName ?? "-"}</TableCell>
                <TableCell>{product.brandName ?? "-"}</TableCell>
                <TableCell>
                  <StatusBadge status={product.isActive ?? true} />
                </TableCell>
                <TableCell className="text-right">{formatCurrency(product.price)}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Link href={`/admin/products/${product.id}`} className="inline-flex h-7 items-center rounded-md border px-2 text-xs hover:bg-muted">
                      Sửa
                    </Link>
                    <DeleteButton
                      id={product.id}
                      action={deleteProductAction}
                      title="Xóa sản phẩm"
                      description={`Xóa sản phẩm "${product.name}"? Sản phẩm sẽ bị ẩn khỏi storefront.`}
                      label="Xóa sản phẩm"
                      successMessage="Đã xóa sản phẩm"
                    />
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {products.items.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="h-24 text-center text-muted-foreground">
                  Chưa có sản phẩm phù hợp.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </AdminTableShell>
    </>
  );
}
