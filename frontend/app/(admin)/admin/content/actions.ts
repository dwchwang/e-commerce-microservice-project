"use server";

import { revalidatePath } from "next/cache";
import { z } from "zod";
import { adminMutate } from "@/lib/admin/api";
import type { FormState } from "@/lib/admin/form-state";

const BannerSchema = z.object({
  title: z.string().min(1).max(255),
  imageUrl: z.string().url(),
  linkUrl: z.string().optional(),
  displayOrder: z.coerce.number().int().optional(),
  isActive: z.string().optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
});

export async function createBannerAction(_prev: FormState, formData: FormData): Promise<FormState> {
  let raw;
  try {
    raw = BannerSchema.parse(Object.fromEntries(formData.entries()));
  } catch {
    return { error: "Dữ liệu banner không hợp lệ. Image URL phải là URL hợp lệ." };
  }

  const result = await adminMutate("/content/banners", {
    method: "POST",
    body: JSON.stringify({
      ...raw,
      isActive: raw.isActive === "on",
      startDate: raw.startDate ? new Date(raw.startDate).toISOString() : null,
      endDate: raw.endDate ? new Date(raw.endDate).toISOString() : null,
    }),
  });
  if (!result.success) return { error: result.error ?? "Tạo banner thất bại." };

  revalidatePath("/admin/content/banners");
  return { success: true };
}

export async function deleteBannerAction(formData: FormData): Promise<FormState> {
  const id = String(formData.get("id") ?? "");
  if (!id) return { error: "Thiếu mã banner." };

  const result = await adminMutate(`/content/banners/${id}`, { method: "DELETE" });
  if (!result.success) return { error: result.error ?? "Xóa banner thất bại." };

  revalidatePath("/admin/content/banners");
  return { success: true };
}

const PageSchema = z.object({
  title: z.string().min(1).max(500),
  slug: z.string().min(1).max(500),
  content: z.string().min(1),
  thumbnailUrl: z.string().optional(),
  author: z.string().optional(),
  isPublished: z.string().optional(),
});

export async function createContentPageAction(_prev: FormState, formData: FormData): Promise<FormState> {
  let raw;
  try {
    raw = PageSchema.parse(Object.fromEntries(formData.entries()));
  } catch {
    return { error: "Dữ liệu trang không hợp lệ. Tiêu đề, slug và nội dung là bắt buộc." };
  }

  const result = await adminMutate("/content/posts", {
    method: "POST",
    body: JSON.stringify({ ...raw, isPublished: raw.isPublished === "on" }),
  });
  if (!result.success) return { error: result.error ?? "Tạo trang nội dung thất bại." };

  revalidatePath("/admin/content/pages");
  return { success: true };
}

export async function deleteContentPageAction(formData: FormData): Promise<FormState> {
  const id = String(formData.get("id") ?? "");
  if (!id) return { error: "Thiếu mã trang." };

  const result = await adminMutate(`/content/posts/${id}`, { method: "DELETE" });
  if (!result.success) return { error: result.error ?? "Xóa trang nội dung thất bại." };

  revalidatePath("/admin/content/pages");
  return { success: true };
}
