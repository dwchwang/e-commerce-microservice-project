"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { z } from "zod";
import { adminMutate } from "@/lib/admin/api";
import type { FormState } from "@/lib/admin/form-state";

const FlashSaleSchema = z.object({
  productId: z.string().uuid(),
  sku: z.string().min(1),
  productName: z.string().min(1),
  originalPrice: z.coerce.number().positive(),
  salePrice: z.coerce.number().positive(),
  quantity: z.coerce.number().int().positive(),
  startTime: z.string().min(1),
  endTime: z.string().min(1),
});

function parseFlashSale(formData: FormData) {
  const raw = FlashSaleSchema.parse(Object.fromEntries(formData.entries()));
  const start = new Date(raw.startTime);
  const end = new Date(raw.endTime);
  if (end <= start) {
    throw new Error("Thời gian kết thúc phải sau thời gian bắt đầu");
  }
  if (start <= new Date()) {
    throw new Error("Thời gian bắt đầu phải ở tương lai");
  }
  if (raw.salePrice >= raw.originalPrice) {
    throw new Error("Giá flash-sale phải nhỏ hơn giá gốc");
  }
  return {
    ...raw,
    startTime: start.toISOString().slice(0, 19),
    endTime: end.toISOString().slice(0, 19),
  };
}

export async function createFlashSaleAction(_prev: FormState, formData: FormData): Promise<FormState> {
  let payload;
  try {
    payload = parseFlashSale(formData);
  } catch (e) {
    return { error: e instanceof Error ? e.message : "Dữ liệu flash-sale không hợp lệ." };
  }

  const result = await adminMutate("/flash-sales", { method: "POST", body: JSON.stringify(payload) });
  if (!result.success) return { error: result.error ?? "Tạo flash-sale thất bại." };

  revalidatePath("/admin/flash-sales");
  redirect("/admin/flash-sales?created=true");
}

export async function updateFlashSaleAction(id: string, _prev: FormState, formData: FormData): Promise<FormState> {
  let payload;
  try {
    payload = parseFlashSale(formData);
  } catch (e) {
    return { error: e instanceof Error ? e.message : "Dữ liệu flash-sale không hợp lệ." };
  }

  const result = await adminMutate(`/flash-sales/${id}`, { method: "PUT", body: JSON.stringify(payload) });
  if (!result.success) return { error: result.error ?? "Cập nhật flash-sale thất bại." };

  revalidatePath("/admin/flash-sales");
  redirect("/admin/flash-sales?updated=true");
}

export async function deleteFlashSaleAction(formData: FormData): Promise<FormState> {
  const id = String(formData.get("id") ?? "");
  if (!id) return { error: "Thiếu mã flash-sale." };

  const result = await adminMutate(`/flash-sales/${id}`, { method: "DELETE" });
  if (!result.success) return { error: result.error ?? "Xóa flash-sale thất bại." };

  revalidatePath("/admin/flash-sales");
  return { success: true };
}
