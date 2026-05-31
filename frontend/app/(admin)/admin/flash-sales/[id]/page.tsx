import { notFound } from "next/navigation";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { FlashSaleForm } from "@/components/admin/forms/FlashSaleForm";
import { adminFetchSafe } from "@/lib/admin/api";
import type { FlashSaleAdmin } from "@/lib/admin/types";
import { updateFlashSaleAction } from "../actions";

export default async function EditFlashSalePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const flashSale = await adminFetchSafe<FlashSaleAdmin | null>(`/flash-sales/${id}`, null);
  if (!flashSale) notFound();

  return (
    <>
      <AdminPageHeader title="Sửa flash-sale" description={flashSale.productName} />
      <FlashSaleForm flashSale={flashSale} action={updateFlashSaleAction.bind(null, id)} />
    </>
  );
}
