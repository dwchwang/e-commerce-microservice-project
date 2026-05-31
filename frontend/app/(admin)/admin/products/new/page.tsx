import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { ProductForm } from "@/components/admin/forms/ProductForm";
import { adminFetchSafe } from "@/lib/admin/api";
import { createProductAction } from "../actions";

type Option = { id: string; name: string };

export default async function NewProductPage() {
  const [categories, brands] = await Promise.all([
    adminFetchSafe<Option[]>("/products/categories", []),
    adminFetchSafe<Option[]>("/products/brands", []),
  ]);

  return (
    <>
      <AdminPageHeader title="Thêm sản phẩm" description="Tạo sản phẩm mới và đồng bộ ngay với storefront." />
      <ProductForm categories={categories} brands={brands} action={createProductAction} />
    </>
  );
}
