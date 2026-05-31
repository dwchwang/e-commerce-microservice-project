"use server";

import { revalidatePath } from "next/cache";
import { z } from "zod";
import { adminMutate } from "@/lib/admin/api";
import type { FormState } from "@/lib/admin/form-state";

const AdjustmentSchema = z.object({
  sku: z.string().min(1),
  productName: z.string().optional(),
  type: z.enum(["IN", "OUT"]),
  quantity: z.coerce.number().int().positive(),
  note: z.string().optional(),
});

export async function adjustStockAction(_prev: FormState, formData: FormData): Promise<FormState> {
  let data;
  try {
    data = AdjustmentSchema.parse(Object.fromEntries(formData.entries()));
  } catch {
    return { error: "Dữ liệu điều chỉnh không hợp lệ. SKU và số lượng (> 0) là bắt buộc." };
  }

  const path = data.type === "IN" ? "/inventory/stock-in" : "/inventory/stock-out";
  const body =
    data.type === "IN"
      ? { sku: data.sku, productName: data.productName, quantity: data.quantity, note: data.note }
      : { sku: data.sku, quantity: data.quantity, note: data.note };

  const result = await adminMutate(path, { method: "POST", body: JSON.stringify(body) });
  if (!result.success) return { error: result.error ?? "Điều chỉnh tồn kho thất bại." };

  revalidatePath("/admin/inventory");
  revalidatePath("/admin/inventory/adjustments");
  return { success: true };
}
