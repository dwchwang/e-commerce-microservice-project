import { notFound } from "next/navigation";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { VoucherForm } from "@/components/admin/forms/VoucherForm";
import { adminFetchSafe } from "@/lib/admin/api";
import type { VoucherAdmin } from "@/lib/admin/types";
import { updateVoucherAction } from "../actions";

export default async function EditVoucherPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const voucher = await adminFetchSafe<VoucherAdmin | null>(`/vouchers/${id}`, null);
  if (!voucher) notFound();

  return (
    <>
      <AdminPageHeader title="Sửa voucher" description={voucher.code} />
      <VoucherForm voucher={voucher} action={updateVoucherAction.bind(null, id)} />
    </>
  );
}
