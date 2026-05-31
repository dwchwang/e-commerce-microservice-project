import { Save } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/admin/forms/Field";
import { AdminForm } from "@/components/admin/AdminForm";
import { SubmitButton } from "@/components/admin/SubmitButton";
import { toDateTimeLocal } from "@/lib/admin/api";
import type { FormState } from "@/lib/admin/form-state";
import type { VoucherAdmin } from "@/lib/admin/types";

export function VoucherForm({
  voucher,
  action,
}: {
  voucher?: VoucherAdmin | null;
  action: (state: FormState, formData: FormData) => Promise<FormState>;
}) {
  return (
    <AdminForm action={action}>
      <Card className="rounded-lg">
        <CardContent className="grid gap-5 p-5 lg:grid-cols-2">
          <Field label="Code">
            <Input name="code" defaultValue={voucher?.code ?? ""} required />
          </Field>
          <Field label="Loại giảm">
            <select name="discountType" defaultValue={voucher?.discountType ?? "PERCENTAGE"} className="h-8 rounded-lg border border-input bg-background px-2 text-sm">
              <option value="PERCENTAGE">PERCENTAGE</option>
              <option value="FIXED_AMOUNT">FIXED_AMOUNT</option>
            </select>
          </Field>
          <Field label="Giá trị">
            <Input name="discountValue" type="number" min="0" step="1000" defaultValue={String(voucher?.discountValue ?? "")} required />
          </Field>
          <Field label="Đơn tối thiểu">
            <Input name="minOrderValue" type="number" min="0" step="1000" defaultValue={String(voucher?.minOrderValue ?? 0)} />
          </Field>
          <Field label="Giảm tối đa">
            <Input name="maxDiscount" type="number" min="1" step="1000" defaultValue={String(voucher?.maxDiscount ?? 1)} />
          </Field>
          <Field label="Giới hạn lượt dùng">
            <Input name="usageLimit" type="number" min="1" defaultValue={String(voucher?.usageLimit ?? 1)} />
          </Field>
          <Field label="Bắt đầu">
            <Input name="startDate" type="datetime-local" defaultValue={toDateTimeLocal(voucher?.startDate)} required />
          </Field>
          <Field label="Kết thúc">
            <Input name="endDate" type="datetime-local" defaultValue={toDateTimeLocal(voucher?.endDate)} required />
          </Field>
          <label className="flex items-center gap-2 text-sm lg:col-span-2">
            <Checkbox name="isActive" defaultChecked={voucher?.isActive ?? true} />
            Đang hoạt động
          </label>
          <div className="lg:col-span-2">
            <SubmitButton pendingText="Đang lưu...">
              <Save className="size-4" />
              Lưu voucher
            </SubmitButton>
          </div>
        </CardContent>
      </Card>
    </AdminForm>
  );
}
