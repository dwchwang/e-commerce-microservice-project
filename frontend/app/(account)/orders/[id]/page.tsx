"use client";

import { use, useEffect, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";
import type { Order } from "@/lib/api/types";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { PriceTag } from "@/components/shared/PriceTag";
import { ArrowLeft } from "lucide-react";
import { toast } from "sonner";

const statusLabels: Record<string, { label: string; variant: "default" | "secondary" | "destructive" | "outline" }> = {
  CREATED: { label: "Đã tạo", variant: "secondary" },
  STOCK_RESERVED: { label: "Đã giữ hàng", variant: "secondary" },
  CONFIRMED: { label: "Đã xác nhận", variant: "default" },
  SHIPPED: { label: "Đang giao", variant: "default" },
  COMPLETED: { label: "Hoàn thành", variant: "default" },
  CANCELLED: { label: "Đã hủy", variant: "destructive" },
  FAILED: { label: "Thất bại", variant: "destructive" },
};

const terminalStatuses = ["CONFIRMED", "COMPLETED", "CANCELLED", "FAILED"];

export default function OrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <Suspense fallback={<div className="text-center py-16">Đang tải...</div>}>
      <OrderDetailContent params={params} />
    </Suspense>
  );
}

function OrderDetailContent({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const searchParams = useSearchParams();

  // Show VNPAY return status toast
  useEffect(() => {
    const vnpStatus = searchParams.get("vnp_TransactionStatus");
    const status = searchParams.get("status");
    if (vnpStatus === "00" || status === "success") {
      toast.success("Thanh toán thành công! Đơn hàng đang được xử lý.");
    } else if (status === "failed") {
      toast.error("Thanh toán thất bại. Vui lòng thử lại.");
    }
  }, [searchParams]);

  const { data: order, isLoading } = useQuery({
    queryKey: qk.orders.detail(id),
    queryFn: () => apiFetch<Order>(`/orders/${id}`),
    refetchInterval: (query) => {
      const data = query.state.data;
      if (!data) return 2000;
      return terminalStatuses.includes(data.status) ? false : 2000;
    },
  });

  if (isLoading) return <div className="text-center py-16">Đang tải...</div>;
  if (!order) return <div className="text-center py-16">Không tìm thấy đơn hàng</div>;

  const st = statusLabels[order.status] || { label: order.status, variant: "outline" as const };

  return (
    <div className="max-w-3xl mx-auto">
      <Link href="/orders" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-primary mb-4">
        <ArrowLeft className="h-4 w-4" /> Quay lại đơn hàng
      </Link>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">
          Đơn hàng #{order.orderNumber || order.id.slice(0, 8)}
        </h1>
        <Badge variant={st.variant} className="text-sm px-3 py-1">{st.label}</Badge>
      </div>

      <p className="text-sm text-muted-foreground mb-6">
        Ngày đặt: {new Date(order.createdAt).toLocaleString("vi-VN")}
      </p>

      {/* Items */}
      <Card className="mb-4">
        <CardContent className="p-4">
          <h3 className="font-semibold mb-3">Sản phẩm</h3>
          <div className="space-y-3">
            {order.items?.map((item, idx) => (
              <div key={idx} className="flex justify-between text-sm">
                <span>{item.productName} x{item.quantity}</span>
                <PriceTag price={item.price} size="sm" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Address & Payment */}
      {order.address && (
        <Card className="mb-4">
          <CardContent className="p-4">
            <h3 className="font-semibold mb-2">Địa chỉ giao hàng</h3>
            <p className="text-sm">{order.address.fullName} — {order.address.phone}</p>
            <p className="text-sm text-muted-foreground">
              {order.address.addressLine}, {order.address.city}
            </p>
          </CardContent>
        </Card>
      )}

      <Card className="mb-4">
        <CardContent className="p-4">
          <div className="flex justify-between text-sm">
            <span>Phương thức thanh toán</span>
            <span className="font-medium">{order.paymentMethod}</span>
          </div>
          {order.paymentStatus && (
            <div className="flex justify-between text-sm mt-1">
              <span>Trạng thái thanh toán</span>
              <span>{order.paymentStatus}</span>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Total */}
      <Card>
        <CardContent className="p-4">
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span>Tạm tính</span>
              <span>{order.totalAmount?.toLocaleString("vi-VN")}₫</span>
            </div>
            {order.discountAmount && order.discountAmount > 0 && (
              <div className="flex justify-between text-green-600">
                <span>Giảm giá</span>
                <span>-{order.discountAmount.toLocaleString("vi-VN")}₫</span>
              </div>
            )}
            <Separator className="my-2" />
            <div className="flex justify-between font-bold text-lg">
              <span>Tổng cộng</span>
              <span className="text-primary">
                {((order.totalAmount || 0) - (order.discountAmount || 0)).toLocaleString("vi-VN")}₫
              </span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
