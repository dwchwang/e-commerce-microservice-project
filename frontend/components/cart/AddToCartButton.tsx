"use client";

import { Button } from "@/components/ui/button";
import { ShoppingCart, Loader2 } from "lucide-react";
import { useAddToCart } from "@/lib/cart/hooks";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

export function AddToCartButton({
  productId,
  className,
}: {
  productId: string;
  className?: string;
}) {
  const addToCart = useAddToCart();

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    addToCart.mutate(
      { productId, quantity: 1 },
      {
        onSuccess: () => toast.success("Đã thêm vào giỏ hàng"),
        onError: () => toast.error("Không thể thêm vào giỏ hàng"),
      }
    );
  };

  return (
    <Button
      size="sm"
      className={cn("gap-1", className)}
      onClick={handleClick}
      disabled={addToCart.isPending}
    >
      {addToCart.isPending ? (
        <Loader2 className="h-4 w-4 animate-spin" />
      ) : (
        <ShoppingCart className="h-4 w-4" />
      )}
      Thêm
    </Button>
  );
}
