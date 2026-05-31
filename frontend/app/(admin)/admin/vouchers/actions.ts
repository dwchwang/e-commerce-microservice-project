"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { z } from "zod";
import { adminMutate } from "@/lib/admin/api";
import type { FormState } from "@/lib/admin/form-state";

const VoucherSchema = z.object({
  code: z.string().min(2).max(50),
  discountType: z.string().min(1),
  discountValue: z.coerce.number().positive(),
  minOrderValue: z.coerce.number().nonnegative().optional(),
  maxDiscount: z.coerce.number().positive().optional(),
  usageLimit: z.coerce.number().int().positive().optional(),
  startDate: z.string().min(1),
  endDate: z.string().min(1),
  isActive: z.string().optional(),
});

function parseVoucher(formData: FormData) {
  const raw = VoucherSchema.parse(Object.fromEntries(formData.entries()));
  if (new Date(raw.endDate) <= new Date(raw.startDate)) {
    throw new Error("Ngày kết thúc phải sau ngày bắt đầu");
  }
  if (raw.discountType === "PERCENTAGE" && raw.discountValue > 100) {
    throw new Error("Giảm theo phần trăm không được vượt quá 100%");
  }
  return {
    ...raw,
    startDate: new Date(raw.startDate).toISOString().slice(0, 19),
    endDate: new Date(raw.endDate).toISOString().slice(0, 19),
    isActive: raw.isActive === "on",
  };
}

export async function createVoucherAction(_prev: FormState, formData: FormData): Promise<FormState> {
  let payload;
  try {
    payload = parseVoucher(formData);
  } catch (e) {
    return { error: e instanceof Error ? e.message : "Dữ liệu voucher không hợp lệ." };
  }

  const result = await adminMutate("/vouchers", { method: "POST", body: JSON.stringify(payload) });
  if (!result.success) return { error: result.error ?? "Tạo voucher thất bại." };

  revalidatePath("/admin/vouchers");
  redirect("/admin/vouchers?created=true");
}

export async function updateVoucherAction(id: string, _prev: FormState, formData: FormData): Promise<FormState> {
  let payload;
  try {
    payload = parseVoucher(formData);
  } catch (e) {
    return { error: e instanceof Error ? e.message : "Dữ liệu voucher không hợp lệ." };
  }

  const result = await adminMutate(`/vouchers/${id}`, { method: "PUT", body: JSON.stringify(payload) });
  if (!result.success) return { error: result.error ?? "Cập nhật voucher thất bại." };

  revalidatePath("/admin/vouchers");
  redirect("/admin/vouchers?updated=true");
}

export async function deleteVoucherAction(formData: FormData): Promise<FormState> {
  const id = String(formData.get("id") ?? "");
  if (!id) return { error: "Thiếu mã voucher." };

  const result = await adminMutate(`/vouchers/${id}`, { method: "DELETE" });
  if (!result.success) return { error: result.error ?? "Xóa voucher thất bại." };

  revalidatePath("/admin/vouchers");
  return { success: true };
}
