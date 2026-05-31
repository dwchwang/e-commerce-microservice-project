import Link from "next/link";
import { serverFetch } from "@/lib/api/server-client";
import type { FlashSale } from "@/lib/api/types";
import { notFound } from "next/navigation";
import { Badge } from "@/components/ui/badge";
import { Zap } from "lucide-react";
import { FlashSaleBuyButton } from "@/components/flash-sale/FlashSaleBuyButton";
import { CountdownTimer } from "@/components/flash-sale/CountdownTimer";

export default async function FlashSaleDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  let sale: FlashSale | null = null;
  try {
    sale = await serverFetch<FlashSale>(`/flash-sales/${id}`, {}, { revalidate: 10 });
  } catch {
    notFound();
  }

  if (!sale) notFound();

  const sold = sale.soldCount ?? 0;
  const remaining = typeof sale.remainingStock === "number" ? sale.remainingStock : Math.max(sale.quantity - sold, 0);
  const progress = sale.quantity > 0 ? Math.round((sold / sale.quantity) * 100) : 0;

  const isUpcoming = sale.status === "SCHEDULED";
  const isActive = sale.status === "ACTIVE";
  const isEnded = sale.status === "ENDED" || sale.status === "CANCELLED" || remaining <= 0;

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl">
      <Link href="/flash-sales" className="text-sm text-muted-foreground hover:text-primary mb-4 inline-block">
        ← Quay lại Flash Sale
      </Link>

      <div className="flex items-center gap-3 mb-6">
        <Zap className="h-8 w-8 text-orange-500" />
        <h1 className="text-3xl font-bold">{sale.productName}</h1>
      </div>

      {/* Countdown */}
      <div className="mb-6">
        {isUpcoming && (
          <div>
            <p className="text-sm text-muted-foreground mb-2">Bắt đầu sau:</p>
            <CountdownTimer targetDate={sale.startTime} />
          </div>
        )}
        {isActive && (
          <div>
            <p className="text-sm text-muted-foreground mb-2">Kết thúc sau:</p>
            <CountdownTimer targetDate={sale.endTime} />
          </div>
        )}
        {isEnded && <Badge variant="secondary" className="text-lg px-4 py-2">Đã kết thúc</Badge>}
      </div>

      {/* Price */}
      <div className="bg-muted/50 rounded-lg p-6 mb-6">
        <div className="flex items-end gap-3">
          <span className="text-3xl font-bold text-primary">
            {sale.salePrice.toLocaleString("vi-VN")}₫
          </span>
          {sale.originalPrice && (
            <span className="text-lg line-through text-muted-foreground">
              {sale.originalPrice.toLocaleString("vi-VN")}₫
            </span>
          )}
          {sale.originalPrice && (
            <Badge className="bg-orange-500 mb-1">
              -{Math.round((1 - sale.salePrice / sale.originalPrice) * 100)}%
            </Badge>
          )}
        </div>
      </div>

      {/* Progress */}
      <div className="mb-6">
        <div className="flex justify-between text-sm mb-1">
          <span>Đã bán: {sold}</span>
          <span>Còn: {remaining}</span>
        </div>
        <div className="h-3 bg-muted rounded-full overflow-hidden">
          <div
            className="h-full bg-orange-500 rounded-full transition-all duration-500"
            style={{ width: `${Math.min(progress, 100)}%` }}
          />
        </div>
      </div>

      <FlashSaleBuyButton saleId={sale.id} active={isActive} soldOut={remaining <= 0} />
    </div>
  );
}
