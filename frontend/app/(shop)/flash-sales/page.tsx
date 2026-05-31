import Link from "next/link";
import { serverFetch } from "@/lib/api/server-client";
import type { FlashSale } from "@/lib/api/types";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Zap, Clock } from "lucide-react";

function remainingOf(sale: FlashSale): number {
  if (typeof sale.remainingStock === "number") return sale.remainingStock;
  return Math.max(sale.quantity - (sale.soldCount ?? 0), 0);
}

export default async function FlashSalesPage() {
  let sales: FlashSale[] = [];
  try {
    sales = await serverFetch<FlashSale[]>("/flash-sales", {}, { revalidate: 30 });
  } catch {
    // empty
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center gap-3 mb-6">
        <Zap className="h-8 w-8 text-orange-500" />
        <h1 className="text-3xl font-bold">Flash Sale</h1>
      </div>

      {sales.length === 0 ? (
        <div className="text-center py-16 text-muted-foreground">
          <Zap className="h-12 w-12 mx-auto mb-4 opacity-30" />
          <p>Hiện không có chương trình Flash Sale nào đang diễn ra.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {sales.map((sale) => {
            const remaining = remainingOf(sale);
            const sold = sale.soldCount ?? 0;
            const progress = sale.quantity > 0 ? Math.round((sold / sale.quantity) * 100) : 0;
            const discount = sale.originalPrice
              ? Math.round((1 - sale.salePrice / sale.originalPrice) * 100)
              : 0;

            return (
              <Link key={sale.id} href={`/flash-sales/${sale.id}`}>
                <Card className="hover:shadow-lg transition-shadow overflow-hidden">
                  <CardContent className="p-4">
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <h3 className="font-semibold text-lg">{sale.productName}</h3>
                        {discount > 0 && <Badge className="mt-1 bg-orange-500">-{discount}%</Badge>}
                      </div>
                      <div className="text-right">
                        <p className="text-xl font-bold text-primary">
                          {sale.salePrice.toLocaleString("vi-VN")}₫
                        </p>
                        {sale.originalPrice && (
                          <p className="text-sm line-through text-muted-foreground">
                            {sale.originalPrice.toLocaleString("vi-VN")}₫
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="mt-3">
                      <div className="flex justify-between text-xs text-muted-foreground mb-1">
                        <span>Đã bán: {sold}</span>
                        <span>Còn: {remaining}</span>
                      </div>
                      <div className="h-2 bg-muted rounded-full overflow-hidden">
                        <div
                          className="h-full bg-orange-500 rounded-full transition-all"
                          style={{ width: `${Math.min(progress, 100)}%` }}
                        />
                      </div>
                    </div>

                    <div className="flex items-center gap-1 mt-3 text-xs text-muted-foreground">
                      <Clock className="h-3 w-3" />
                      <span>{new Date(sale.endTime).toLocaleString("vi-VN")}</span>
                    </div>
                  </CardContent>
                </Card>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
