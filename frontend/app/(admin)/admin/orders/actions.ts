"use server";

import { revalidatePath } from "next/cache";
import { z } from "zod";
import { adminMutate } from "@/lib/admin/api";
import type { FormState } from "@/lib/admin/form-state";

const StatusSchema = z.object({
  orderId: z.string().min(1),
  status: z.enum(["PENDING", "STOCK_RESERVED", "CONFIRMED", "CANCELLED"]),
  note: z.string().optional(),
});

export async function updateOrderStatusAction(_prev: FormState, formData: FormData): Promise<FormState> {
  let data;
  try {
    data = StatusSchema.parse(Object.fromEntries(formData.entries()));
  } catch {
    return { error: "Trạng thái không hợp lệ." };
  }

  const result = await adminMutate(`/orders/admin/${data.orderId}/status`, {
    method: "PUT",
    body: JSON.stringify({ status: data.status, note: data.note }),
  });
  if (!result.success) return { error: result.error ?? "Cập nhật trạng thái thất bại." };

  revalidatePath("/admin/orders");
  revalidatePath(`/admin/orders/${data.orderId}`);
  return { success: true };
}
