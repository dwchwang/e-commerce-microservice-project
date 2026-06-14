import Link from "next/link";
import Image from "next/image";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { PriceTag } from "@/components/shared/PriceTag";
import { AddToCartButton } from "@/components/cart/AddToCartButton";
import { resolveProductImage } from "@/lib/product-image";
import type { Product } from "@/lib/api/types";

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const hasDiscount = product.originalPrice && product.originalPrice > product.price;
  const href = `/products/${product.slug || product.id}`;
  const imageUrl = resolveProductImage(product, product.imageUrl, 600);

  return (
    <Card className="group flex flex-col overflow-hidden rounded-2xl border border-border bg-card p-0 transition-transform duration-300 hover:-translate-y-1">
      <Link href={href}>
        <div className="relative aspect-square overflow-hidden bg-secondary">
          <Image
            src={imageUrl}
            alt={product.name}
            fill
            className="object-cover transition-transform duration-500 group-hover:scale-105"
            sizes="(max-width: 768px) 50vw, 25vw"
          />
          {hasDiscount && (
            <Badge className="absolute left-3 top-3 rounded-full bg-destructive px-2.5 py-0.5 text-destructive-foreground">
              -{Math.round((1 - product.price / product.originalPrice!) * 100)}%
            </Badge>
          )}
        </div>
      </Link>

      <div className="flex flex-1 flex-col p-4">
        {product.brand && (
          <p className="mb-1 text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
            {product.brand}
          </p>
        )}
        <Link href={href}>
          <h3 className="line-clamp-2 text-[15px] font-semibold leading-snug tracking-tight transition-colors hover:text-primary">
            {product.name}
          </h3>
        </Link>

        {product.rating ? (
          <div className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
            <span className="text-amber-500">★</span>
            <span className="font-medium text-foreground">{product.rating.toFixed(1)}</span>
            {product.reviewCount ? <span>({product.reviewCount})</span> : null}
          </div>
        ) : null}

        <PriceTag
          price={product.price}
          originalPrice={product.originalPrice}
          size="md"
          className="mt-3"
        />

        <div className="mt-auto pt-4">
          <AddToCartButton productId={product.id} className="w-full rounded-full" />
        </div>
      </div>
    </Card>
  );
}
