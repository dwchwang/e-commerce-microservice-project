"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";
import type { Order } from "@/lib/api/types";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/EmptyState";
import { Package } from "lucide-react";

const statusLabels: Record<string, { label: string; variant: "default" | "secondary" | "destructive" | "outline" }> = {
  PENDING: { label: "Chờ xử lý", variant: "secondary" },
  STOCK_RESERVED: { label: "Đã giữ hàng", variant: "secondary" },
  CONFIRMED: { label: "Đã xác nhận", variant: "default" },
  CANCELLED: { label: "Đã hủy", variant: "destructive" },
};

export default function OrdersPage() {
  const { data, isLoading } = useQuery({
    queryKey: qk.orders.all,
    queryFn: () => apiFetch<Order[]>("/orders"),
  });

  const orders = data ?? [];

  if (isLoading) return <div className="text-center py-16">Đang tải...</div>;

  if (orders.length === 0) {
    return (
      <EmptyState
        icon={<Package className="h-12 w-12" />}
        title="Chưa có đơn hàng"
        description="Bạn chưa có đơn hàng nào. Hãy mua sắm ngay!"
        action={
          <Link href="/products">
            <Button>Xem sản phẩm</Button>
          </Link>
        }
      />
    );
  }

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Đơn hàng của tôi</h1>
      <div className="space-y-3">
        {orders.map((order) => {
          const st = statusLabels[order.status] || { label: order.status, variant: "outline" as const };
          return (
            <Link key={order.id} href={`/orders/${order.id}`}>
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="p-4">
                  <div className="flex items-center justify-between mb-2">
                    <div>
                      <p className="font-medium">Đơn hàng #{order.id.slice(0, 8)}</p>
                      <p className="text-xs text-muted-foreground">
                        {new Date(order.createdAt).toLocaleString("vi-VN")}
                      </p>
                    </div>
                    <Badge variant={st.variant}>{st.label}</Badge>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>{order.items?.length || 0} sản phẩm</span>
                    <span className="font-semibold">{order.totalAmount?.toLocaleString("vi-VN")}₫</span>
                  </div>
                </CardContent>
              </Card>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
