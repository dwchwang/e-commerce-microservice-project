import { cn } from "@/lib/utils";

interface PriceTagProps {
  price: number;
  originalPrice?: number;
  className?: string;
  size?: "sm" | "md" | "lg";
}

export function PriceTag({ price, originalPrice, className, size = "md" }: PriceTagProps) {
  const sizeClasses = {
    sm: "text-sm",
    md: "text-base",
    lg: "text-xl",
  };

  const hasDiscount = originalPrice && originalPrice > price;

  return (
    <div className={cn("flex items-center gap-2", className)}>
      <span className={cn("font-semibold tracking-tight text-foreground", sizeClasses[size])}>
        {price.toLocaleString("vi-VN")}₫
      </span>
      {hasDiscount && (
        <>
          <span className={cn("line-through text-muted-foreground", size === "lg" ? "text-base" : "text-xs")}>
            {originalPrice.toLocaleString("vi-VN")}₫
          </span>
          <span className="text-xs text-destructive font-medium">
            -{Math.round((1 - price / originalPrice) * 100)}%
          </span>
        </>
      )}
    </div>
  );
}
