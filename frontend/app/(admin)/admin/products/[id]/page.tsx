import { notFound } from "next/navigation";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { ProductForm } from "@/components/admin/forms/ProductForm";
import { adminFetchSafe } from "@/lib/admin/api";
import type { ProductAdmin } from "@/lib/admin/types";
import { updateProductAction } from "../actions";

type Option = { id: string; name: string };

export default async function EditProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [product, categories, brands] = await Promise.all([
    adminFetchSafe<ProductAdmin | null>(`/products/${id}`, null),
    adminFetchSafe<Option[]>("/products/categories", []),
    adminFetchSafe<Option[]>("/products/brands", []),
  ]);

  if (!product) notFound();

  const action = updateProductAction.bind(null, id);

  return (
    <>
      <AdminPageHeader title="Sửa sản phẩm" description={product.name} />
      <ProductForm product={product} categories={categories} brands={brands} action={action} />
    </>
  );
}
