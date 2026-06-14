"use client";

import Link from "next/link";
import Image from "next/image";
import { resolveProductImage } from "@/lib/product-image";
import { useCart, useUpdateCartItem, useRemoveCartItem } from "@/lib/cart/hooks";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { EmptyState } from "@/components/shared/EmptyState";
import { PriceTag } from "@/components/shared/PriceTag";
import { Loader2, ShoppingCart, Trash2, Minus, Plus, ArrowRight } from "lucide-react";

export default function CartPage() {
  const { data: cart, isLoading } = useCart();
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
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
          <Card key={item.productId}>
            <CardContent className="flex items-center gap-4 p-4">
              <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-secondary">
                <Image
                  src={resolveProductImage({ id: item.productId, name: item.productName }, item.productImage, 160)}
                  alt={item.productName}
                  fill
                  className="object-cover"
                  sizes="64px"
                />
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
                  disabled={item.quantity <= 1 || updateItem.isPending}
                  onClick={() =>
                    updateItem.mutate({ productId: item.productId, quantity: item.quantity - 1 })
                  }
                >
                  <Minus className="h-3 w-3" />
                </Button>
                <span className="w-8 text-center text-sm">{item.quantity}</span>
                <Button
                  variant="outline"
                  size="icon"
                  className="h-8 w-8"
                  disabled={updateItem.isPending}
                  onClick={() =>
                    updateItem.mutate({ productId: item.productId, quantity: item.quantity + 1 })
                  }
                >
                  <Plus className="h-3 w-3" />
                </Button>
              </div>
              <div className="w-24 text-right text-sm font-medium">
                {item.subtotal.toLocaleString("vi-VN")}₫
              </div>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 text-destructive"
                disabled={removeItem.isPending}
                onClick={() => removeItem.mutate(item.productId)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>

      <Separator className="my-4" />

      <div className="space-y-2">
        <div className="flex justify-between font-bold text-lg">
          <span>Tổng cộng</span>
          <span className="text-primary">{cart.totalPrice.toLocaleString("vi-VN")}₫</span>
        </div>
        <p className="text-sm text-muted-foreground">
          Mã giảm giá có thể được áp dụng ở bước thanh toán.
        </p>
      </div>

      <Link href="/checkout" className="block mt-6">
        <Button className="w-full h-12 text-lg gap-2">
          Tiến hành đặt hàng <ArrowRight className="h-5 w-5" />
        </Button>
      </Link>
    </div>
  );
}
