"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Loader2, Zap } from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api/client";
import { toast } from "sonner";
import type { FlashSalePurchaseResult } from "@/lib/api/types";

export function FlashSaleBuyButton({
  saleId,
  disabled,
  soldOut,
}: {
  saleId: string;
  disabled: boolean;
  soldOut: boolean;
}) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  const handleBuy = async () => {
    setLoading(true);
    try {
      const result = await apiFetch<FlashSalePurchaseResult>(
        `/flash-sales/${saleId}/purchase`,
        { method: "POST" }
      );

      if (result.success && result.orderId) {
        toast.success("Đặt hàng thành công!");
        router.push(`/orders/${result.orderId}`);
      } else if (result.code === -1) {
        toast.error("Sản phẩm đã hết hàng");
      } else if (result.code === -2) {
        toast.warning("Bạn đã mua sản phẩm này rồi");
      } else if (result.code === -3) {
        toast.info("Flash sale chưa bắt đầu");
      } else {
        toast.error(result.message || "Lỗi hệ thống, thử lại sau");
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 429) {
        toast.warning("Quá nhanh! Vui lòng thử lại sau");
      } else {
        toast.error("Lỗi kết nối, vui lòng thử lại");
      }
    } finally {
      setLoading(false);
    }
  };

  if (soldOut) {
    return (
      <Button className="w-full h-12 text-lg" disabled>
        Hết hàng
      </Button>
    );
  }

  return (
    <Button
      className="w-full h-12 text-lg gap-2"
      onClick={handleBuy}
      disabled={disabled || loading}
    >
      {loading ? (
        <Loader2 className="h-5 w-5 animate-spin" />
      ) : (
        <Zap className="h-5 w-5" />
      )}
      Mua ngay
    </Button>
  );
}
