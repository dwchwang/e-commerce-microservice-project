import Link from "next/link";
import { CheckCircle2, XCircle, Clock } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * VNPAY return target (FRONTEND_ORDER_RESULT_URL = /order/result).
 * Backend redirects here with ?orderId&paymentId&status=success|failed.
 *
 * This page renders the result inline instead of redirecting to the protected
 * /orders/[id] route — the VNPAY round-trip is a cross-site navigation and the
 * auth cookie may not accompany it, which previously bounced users to /login.
 */
export default async function OrderResultPage({
  searchParams,
}: {
  searchParams: Promise<{ orderId?: string; paymentId?: string; status?: string }>;
}) {
  const { orderId, status } = await searchParams;
  const hasOrder = !!orderId && orderId !== "unknown";
  const shortId = hasOrder ? orderId!.slice(0, 8) : null;

  const state =
    status === "success"
      ? {
          icon: <CheckCircle2 className="size-16 text-emerald-500" />,
          title: "Thanh toán thành công",
          desc: "Cảm ơn bạn! Giao dịch VNPAY đã hoàn tất và đơn hàng đang được xử lý.",
        }
      : status === "failed"
        ? {
            icon: <XCircle className="size-16 text-destructive" />,
            title: "Thanh toán thất bại",
            desc: "Giao dịch chưa hoàn tất. Bạn có thể thử lại hoặc chọn thanh toán khi nhận hàng (COD).",
          }
        : {
            icon: <Clock className="size-16 text-amber-500" />,
            title: "Đang xác nhận thanh toán",
            desc: "Chúng tôi đang xác nhận kết quả giao dịch. Vui lòng kiểm tra lại đơn hàng sau giây lát.",
          };

  return (
    <div className="mx-auto flex max-w-md flex-col items-center py-16 text-center">
      {state.icon}
      <h1 className="mt-6 text-2xl font-semibold tracking-tight">{state.title}</h1>
      {shortId && (
        <p className="mt-2 text-sm text-muted-foreground">
          Mã đơn hàng: <span className="font-medium text-foreground">#{shortId}</span>
        </p>
      )}
      <p className="mt-3 text-[15px] text-muted-foreground">{state.desc}</p>

      <div className="mt-8 flex w-full flex-col gap-3 sm:flex-row sm:justify-center">
        {hasOrder && (
          <Link
            href={`/orders/${orderId}?status=${status ?? "unknown"}`}
            className={cn(buttonVariants({ size: "lg" }), "rounded-full")}
          >
            Xem đơn hàng
          </Link>
        )}
        <Link
          href="/products"
          className={cn(buttonVariants({ variant: "outline", size: "lg" }), "rounded-full")}
        >
          Tiếp tục mua sắm
        </Link>
      </div>
    </div>
  );
}
