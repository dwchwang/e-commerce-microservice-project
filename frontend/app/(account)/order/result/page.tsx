import { redirect } from "next/navigation";

/**
 * VNPAY return target (FRONTEND_ORDER_RESULT_URL = /order/result).
 * Backend redirects here with ?orderId&paymentId&status=success|failed.
 * We forward to the order detail page which shows the proper status + polls.
 */
export default async function OrderResultPage({
  searchParams,
}: {
  searchParams: Promise<{ orderId?: string; status?: string }>;
}) {
  const { orderId, status } = await searchParams;

  if (orderId && orderId !== "unknown") {
    redirect(`/orders/${orderId}?status=${status ?? "unknown"}`);
  }

  redirect("/orders");
}
