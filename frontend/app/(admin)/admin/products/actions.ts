"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { z } from "zod";
import { adminMutate } from "@/lib/admin/api";
import type { FormState } from "@/lib/admin/form-state";

const ProductSchema = z.object({
  sku: z.string().min(1).max(50),
  name: z.string().min(3).max(255),
  description: z.string().optional(),
  price: z.coerce.number().nonnegative(),
  categoryId: z.string().optional(),
  brandId: z.string().optional(),
  imageUrls: z.string().optional(),
  specs: z.string().optional(),
});

function parseProduct(formData: FormData) {
  const raw = ProductSchema.parse(Object.fromEntries(formData.entries()));
  const specs = (raw.specs ?? "")
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [name, ...rest] = line.split(":");
      return { specName: name.trim(), specValue: rest.join(":").trim() };
    })
    .filter((spec) => spec.specName && spec.specValue);

  return {
    sku: raw.sku,
    name: raw.name,
    description: raw.description ?? "",
    price: raw.price,
    categoryId: raw.categoryId || null,
    brandId: raw.brandId || null,
    imageUrls: (raw.imageUrls ?? "")
      .split("\n")
      .map((url) => url.trim())
      .filter(Boolean),
    specs,
  };
}

export async function createProductAction(_prev: FormState, formData: FormData): Promise<FormState> {
  let payload;
  try {
    payload = parseProduct(formData);
  } catch {
    return { error: "Dữ liệu sản phẩm không hợp lệ. Vui lòng kiểm tra lại các trường." };
  }

  const result = await adminMutate("/products", { method: "POST", body: JSON.stringify(payload) });
  if (!result.success) return { error: result.error ?? "Tạo sản phẩm thất bại." };

  revalidatePath("/admin/products");
  redirect("/admin/products?created=true");
}

export async function updateProductAction(id: string, _prev: FormState, formData: FormData): Promise<FormState> {
  let payload;
  try {
    payload = parseProduct(formData);
  } catch {
    return { error: "Dữ liệu sản phẩm không hợp lệ. Vui lòng kiểm tra lại các trường." };
  }

  const result = await adminMutate(`/products/${id}`, { method: "PUT", body: JSON.stringify(payload) });
  if (!result.success) return { error: result.error ?? "Cập nhật sản phẩm thất bại." };

  revalidatePath("/admin/products");
  revalidatePath(`/admin/products/${id}`);
  redirect("/admin/products?updated=true");
}

export async function deleteProductAction(formData: FormData): Promise<FormState> {
  const id = String(formData.get("id") ?? "");
  if (!id) return { error: "Thiếu mã sản phẩm." };

  const result = await adminMutate(`/products/${id}`, { method: "DELETE" });
  if (!result.success) return { error: result.error ?? "Xóa sản phẩm thất bại." };

  revalidatePath("/admin/products");
  return { success: true };
}
