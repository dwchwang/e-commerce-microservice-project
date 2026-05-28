"use client";

import Link from "next/link";
import { useCart, useUpdateCartItem, useRemoveCartItem } from "@/lib/cart/hooks";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { EmptyState } from "@/components/shared/EmptyState";
import { PriceTag } from "@/components/shared/PriceTag";
import { Loader2, ShoppingCart, Trash2, Minus, Plus, ArrowRight } from "lucide-react";
import { useState } from "react";
import { apiFetch } from "@/lib/api/client";
import { toast } from "sonner";

export default function CartPage() {
  const { data: cart, isLoading } = useCart();
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();
  const [voucherCode, setVoucherCode] = useState("");
  const [applyingVoucher, setApplyingVoucher] = useState(false);

  const applyVoucher = async () => {
    if (!voucherCode.trim()) return;
    setApplyingVoucher(true);
    try {
      await apiFetch("/cart/voucher", {
        method: "POST",
        body: JSON.stringify({ code: voucherCode.trim() }),
      });
      toast.success("Đã áp dụng mã giảm giá");
      setVoucherCode("");
    } catch {
      toast.error("Mã giảm giá không hợp lệ");
    } finally {
      setApplyingVoucher(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  if (!cart || !cart.items || cart.items.length === 0) {
    return (
      <EmptyState
        icon={<ShoppingCart className="h-12 w-12" />}
        title="Giỏ hàng trống"
        description="Hãy thêm sản phẩm vào giỏ hàng để tiếp tục mua sắm."
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
      <h1 className="text-2xl font-bold mb-6">Giỏ hàng</h1>

      <div className="space-y-3">
        {cart.items.map((item) => (
          <Card key={item.id}>
            <CardContent className="flex items-center gap-4 p-4">
              <div className="w-16 h-16 bg-muted rounded shrink-0 flex items-center justify-center text-2xl">
                📦
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="font-medium text-sm truncate">{item.productName}</h3>
                <PriceTag price={item.price} size="sm" />
              </div>
              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="icon"
                  className="h-8 w-8"
                  disabled={item.quantity <= 1}
                  onClick={() =>
                    updateItem.mutate({ itemId: item.id, quantity: item.quantity - 1 })
                  }
                >
                  <Minus className="h-3 w-3" />
                </Button>
                <span className="w-8 text-center text-sm">{item.quantity}</span>
                <Button
                  variant="outline"
                  size="icon"
                  className="h-8 w-8"
                  onClick={() =>
                    updateItem.mutate({ itemId: item.id, quantity: item.quantity + 1 })
                  }
                >
                  <Plus className="h-3 w-3" />
                </Button>
              </div>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 text-destructive"
                onClick={() => removeItem.mutate(item.id)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Voucher */}
      <div className="flex gap-2 mt-4">
        <Input
          placeholder="Mã giảm giá"
          value={voucherCode}
          onChange={(e) => setVoucherCode(e.target.value)}
          className="max-w-50"
        />
        <Button variant="outline" onClick={applyVoucher} disabled={applyingVoucher}>
          {applyingVoucher ? <Loader2 className="h-4 w-4 animate-spin" /> : "Áp dụng"}
        </Button>
        {cart.voucherCode && (
          <span className="text-sm text-green-600 self-center">
            Đã áp dụng: {cart.voucherCode}
          </span>
        )}
      </div>

      <Separator className="my-4" />

      {/* Summary */}
      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <span>Tạm tính</span>
          <span>{cart.totalAmount?.toLocaleString("vi-VN")}₫</span>
        </div>
        {cart.discountAmount && cart.discountAmount > 0 && (
          <div className="flex justify-between text-sm text-green-600">
            <span>Giảm giá</span>
            <span>-{cart.discountAmount.toLocaleString("vi-VN")}₫</span>
          </div>
        )}
        <Separator />
        <div className="flex justify-between font-bold text-lg">
          <span>Tổng cộng</span>
          <span className="text-primary">
            {((cart.totalAmount || 0) - (cart.discountAmount || 0)).toLocaleString("vi-VN")}₫
          </span>
        </div>
      </div>

      <Link href="/checkout" className="block mt-6">
        <Button className="w-full h-12 text-lg gap-2">
          Tiến hành đặt hàng <ArrowRight className="h-5 w-5" />
        </Button>
      </Link>
    </div>
  );
}
