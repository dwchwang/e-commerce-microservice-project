"use server";

import { revalidatePath } from "next/cache";
import { adminMutate } from "@/lib/admin/api";
import type { FormState } from "@/lib/admin/form-state";

export async function deleteReviewAction(formData: FormData): Promise<FormState> {
  const id = String(formData.get("id") ?? "");
  if (!id) return { error: "Thiếu mã đánh giá." };

  const result = await adminMutate(`/reviews/${id}`, { method: "DELETE" });
  if (!result.success) return { error: result.error ?? "Xóa đánh giá thất bại." };

  revalidatePath("/admin/reviews");
  return { success: true };
}
