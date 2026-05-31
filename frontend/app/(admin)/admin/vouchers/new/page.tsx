import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { VoucherForm } from "@/components/admin/forms/VoucherForm";
import { createVoucherAction } from "../actions";

export default function NewVoucherPage() {
  return (
    <>
      <AdminPageHeader title="Thêm voucher" description="Tạo mã khuyến mãi mới cho checkout." />
      <VoucherForm action={createVoucherAction} />
    </>
  );
}
