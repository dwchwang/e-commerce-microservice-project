import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { FlashSaleForm } from "@/components/admin/forms/FlashSaleForm";
import { createFlashSaleAction } from "../actions";

export default function NewFlashSalePage() {
  return (
    <>
      <AdminPageHeader title="Thêm flash-sale" description="Tạo chiến dịch bán nhanh cho một sản phẩm." />
      <FlashSaleForm action={createFlashSaleAction} />
    </>
  );
}
