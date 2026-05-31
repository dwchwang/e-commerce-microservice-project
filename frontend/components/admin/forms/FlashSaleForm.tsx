import { Save } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/admin/forms/Field";
import { AdminForm } from "@/components/admin/AdminForm";
import { SubmitButton } from "@/components/admin/SubmitButton";
import { toDateTimeLocal } from "@/lib/admin/api";
import type { FormState } from "@/lib/admin/form-state";
import type { FlashSaleAdmin } from "@/lib/admin/types";

export function FlashSaleForm({
  flashSale,
  action,
}: {
  flashSale?: FlashSaleAdmin | null;
  action: (state: FormState, formData: FormData) => Promise<FormState>;
}) {
  return (
    <AdminForm action={action}>
      <Card className="rounded-lg">
        <CardContent className="grid gap-5 p-5 lg:grid-cols-2">
          <Field label="Product ID">
            <Input name="productId" defaultValue={flashSale?.productId ?? ""} required />
          </Field>
          <Field label="SKU">
            <Input name="sku" defaultValue={flashSale?.sku ?? ""} required />
          </Field>
          <Field label="Tên sản phẩm">
            <Input name="productName" defaultValue={flashSale?.productName ?? ""} required />
          </Field>
          <Field label="Giá gốc">
            <Input name="originalPrice" type="number" min="1" step="1000" defaultValue={String(flashSale?.originalPrice ?? "")} required />
          </Field>
          <Field label="Giá flash-sale">
            <Input name="salePrice" type="number" min="1" step="1000" defaultValue={String(flashSale?.salePrice ?? "")} required />
          </Field>
          <Field label="Số lượng">
            <Input name="quantity" type="number" min="1" defaultValue={String(flashSale?.quantity ?? "")} required />
          </Field>
          <Field label="Bắt đầu">
            <Input name="startTime" type="datetime-local" defaultValue={toDateTimeLocal(flashSale?.startTime)} required />
          </Field>
          <Field label="Kết thúc">
            <Input name="endTime" type="datetime-local" defaultValue={toDateTimeLocal(flashSale?.endTime)} required />
          </Field>
          <div className="lg:col-span-2">
            <SubmitButton pendingText="Đang lưu...">
              <Save className="size-4" />
              Lưu flash-sale
            </SubmitButton>
          </div>
        </CardContent>
      </Card>
    </AdminForm>
  );
}
