"use client";

import { Button } from "@/components/ui/button";
import { ShoppingCart, Loader2, Plus, Check } from "lucide-react";
import { useAddToCart } from "@/lib/cart/hooks";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import { useState } from "react";

type Variant = "default" | "outline" | "secondary" | "ghost";

export function AddToCartButton({
  productId,
  className,
  variant = "default",
  size = "sm",
  iconOnly = false,
  label = "Thêm",
}: {
  productId: string;
  className?: string;
  variant?: Variant;
  size?: "sm" | "default" | "lg" | "icon";
  iconOnly?: boolean;
  label?: string;
}) {
  const addToCart = useAddToCart();
  const [justAdded, setJustAdded] = useState(false);

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    addToCart.mutate(
      { productId, quantity: 1 },
      {
        onSuccess: () => {
          toast.success("Đã thêm vào giỏ hàng");
          setJustAdded(true);
          setTimeout(() => setJustAdded(false), 1500);
        },
        onError: () => toast.error("Không thể thêm vào giỏ hàng"),
      }
    );
  };

  const icon = addToCart.isPending ? (
    <Loader2 className="h-4 w-4 animate-spin" />
  ) : justAdded ? (
    <Check className="h-4 w-4" />
  ) : iconOnly ? (
    <Plus className="h-4 w-4" />
  ) : (
    <ShoppingCart className="h-4 w-4" />
  );

  return (
    <Button
      size={iconOnly ? "icon" : size}
      variant={variant}
      aria-label={iconOnly ? "Thêm vào giỏ hàng" : undefined}
      className={cn("gap-1.5", className)}
      onClick={handleClick}
      disabled={addToCart.isPending}
    >
      {icon}
      {!iconOnly && (justAdded ? "Đã thêm" : label)}
    </Button>
  );
}
