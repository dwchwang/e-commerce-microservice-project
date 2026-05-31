import { Save } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Field } from "@/components/admin/forms/Field";
import { AdminForm } from "@/components/admin/AdminForm";
import { SubmitButton } from "@/components/admin/SubmitButton";
import type { FormState } from "@/lib/admin/form-state";
import type { ProductAdmin } from "@/lib/admin/types";

type Option = { id: string; name: string };

export function ProductForm({
  product,
  categories,
  brands,
  action,
}: {
  product?: ProductAdmin | null;
  categories: Option[];
  brands: Option[];
  action: (state: FormState, formData: FormData) => Promise<FormState>;
}) {
  const specs = product?.specs?.map((spec) => `${spec.specName}: ${spec.specValue}`).join("\n") ?? "";

  return (
    <AdminForm action={action}>
      <Card className="rounded-lg">
        <CardContent className="grid gap-5 p-5 lg:grid-cols-2">
          <Field label="SKU">
            <Input name="sku" defaultValue={product?.sku ?? ""} required maxLength={50} />
          </Field>
          <Field label="Tên sản phẩm">
            <Input name="name" defaultValue={product?.name ?? ""} required maxLength={255} />
          </Field>
          <Field label="Giá bán">
            <Input name="price" type="number" min="0" step="1000" defaultValue={String(product?.price ?? "")} required />
          </Field>
          <Field label="Danh mục">
            <select
              name="categoryId"
              defaultValue={product?.categoryId ?? ""}
              className="h-8 rounded-lg border border-input bg-background px-2 text-sm"
            >
              <option value="">Không chọn</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Thương hiệu">
            <select
              name="brandId"
              defaultValue={product?.brandId ?? ""}
              className="h-8 rounded-lg border border-input bg-background px-2 text-sm"
            >
              <option value="">Không chọn</option>
              {brands.map((brand) => (
                <option key={brand.id} value={brand.id}>
                  {brand.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Ảnh sản phẩm (mỗi URL một dòng)" className="lg:col-span-2">
            <Textarea name="imageUrls" rows={4} defaultValue={product?.imageUrls?.join("\n") ?? product?.primaryImageUrl ?? ""} />
          </Field>
          <Field label="Mô tả" className="lg:col-span-2">
            <Textarea name="description" rows={6} defaultValue={product?.description ?? ""} />
          </Field>
          <Field label="Thông số kỹ thuật (key: value, mỗi dòng một thông số)" className="lg:col-span-2">
            <Textarea name="specs" rows={6} defaultValue={specs} />
          </Field>
          <div className="lg:col-span-2">
            <SubmitButton pendingText="Đang lưu...">
              <Save className="size-4" />
              Lưu sản phẩm
            </SubmitButton>
          </div>
        </CardContent>
      </Card>
    </AdminForm>
  );
}
