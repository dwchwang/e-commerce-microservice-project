"use client";

import Link from "next/link";
import Image from "next/image";
import { resolveProductImage } from "@/lib/product-image";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { ShoppingCart, Trash2, Plus, Minus, ArrowRight } from "lucide-react";
import { useCart, useUpdateCartItem, useRemoveCartItem } from "@/lib/cart/hooks";
import { PriceTag } from "@/components/shared/PriceTag";
import { EmptyState } from "@/components/shared/EmptyState";

export function CartDrawer() {
  const { data: cart } = useCart();
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();

  const itemCount = cart?.items?.length ?? 0;
  const total = cart?.totalPrice ?? 0;

  return (
    <Sheet>
      <SheetTrigger>
        <Button variant="ghost" size="icon" className="relative">
          <ShoppingCart className="h-5 w-5" />
          {itemCount > 0 && (
            <Badge className="absolute -top-1 -right-1 h-5 w-5 flex items-center justify-center p-0 text-xs">
              {itemCount}
            </Badge>
          )}
        </Button>
      </SheetTrigger>
      <SheetContent className="w-95 flex flex-col">
        <SheetHeader>
          <SheetTitle>Giỏ hàng ({itemCount})</SheetTitle>
        </SheetHeader>

        {!cart || itemCount === 0 ? (
          <EmptyState
            icon={<ShoppingCart className="h-10 w-10" />}
            title="Giỏ hàng trống"
            className="flex-1"
          />
        ) : (
          <>
            <div className="flex-1 overflow-y-auto space-y-3 py-4">
              {cart.items.map((item) => (
                <div key={item.productId} className="flex items-center gap-3">
                  <div className="relative h-12 w-12 shrink-0 overflow-hidden rounded-lg bg-secondary">
                    <Image
                      src={resolveProductImage({ id: item.productId, name: item.productName }, item.productImage, 120)}
                      alt={item.productName}
                      fill
                      className="object-cover"
                      sizes="48px"
                    />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">{item.productName}</p>
                    <PriceTag price={item.price} size="sm" />
                  </div>
                  <div className="flex items-center gap-1">
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-7 w-7"
                      disabled={item.quantity <= 1}
                      onClick={() => updateItem.mutate({ productId: item.productId, quantity: item.quantity - 1 })}
                    >
                      <Minus className="h-3 w-3" />
                    </Button>
                    <span className="w-6 text-center text-xs">{item.quantity}</span>
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-7 w-7"
                      onClick={() => updateItem.mutate({ productId: item.productId, quantity: item.quantity + 1 })}
                    >
                      <Plus className="h-3 w-3" />
                    </Button>
                  </div>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7 text-destructive"
                    onClick={() => removeItem.mutate(item.productId)}
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>

            <Separator />
            <div className="py-3">
              <div className="flex justify-between font-bold mb-3">
                <span>Tổng cộng</span>
                <span className="text-primary">{total.toLocaleString("vi-VN")}₫</span>
              </div>
              <Link href="/cart" className="block">
                <Button className="w-full gap-2">
                  Xem giỏ hàng <ArrowRight className="h-4 w-4" />
                </Button>
              </Link>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}
