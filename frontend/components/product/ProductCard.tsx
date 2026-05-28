import Link from "next/link";
import Image from "next/image";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { PriceTag } from "@/components/shared/PriceTag";
import { AddToCartButton } from "@/components/cart/AddToCartButton";
import { CompareButton } from "@/components/product/CompareButton";
import type { Product } from "@/lib/api/types";

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const hasDiscount = product.originalPrice && product.originalPrice > product.price;

  return (
    <Card className="group overflow-hidden hover:shadow-lg transition-shadow">
      <Link href={`/products/${product.slug || product.id}`}>
        <div className="aspect-square relative bg-muted">
          {product.imageUrl ? (
            <Image
              src={product.imageUrl}
              alt={product.name}
              fill
              className="object-cover group-hover:scale-105 transition-transform duration-300"
              sizes="(max-width: 768px) 50vw, 25vw"
            />
          ) : (
            <div className="flex items-center justify-center h-full text-muted-foreground">
              <span className="text-4xl">📦</span>
            </div>
          )}
          {hasDiscount && (
            <Badge className="absolute top-2 left-2 bg-destructive text-destructive-foreground">
              -{Math.round((1 - product.price / product.originalPrice!) * 100)}%
            </Badge>
          )}
        </div>
      </Link>
      <CardContent className="p-3">
        <Link href={`/products/${product.slug || product.id}`}>
          <h3 className="font-medium text-sm line-clamp-2 mb-1 hover:text-primary transition-colors">
            {product.name}
          </h3>
        </Link>
        {product.brand && (
          <p className="text-xs text-muted-foreground mb-1">{product.brand}</p>
        )}
        <PriceTag
          price={product.price}
          originalPrice={product.originalPrice}
          size="sm"
          className="mb-2"
        />
        {product.rating && (
          <div className="flex items-center gap-1 text-xs text-muted-foreground mb-2">
            <span className="text-yellow-500">★</span>
            <span>{product.rating.toFixed(1)}</span>
            {product.reviewCount && <span>({product.reviewCount})</span>}
          </div>
        )}
        <div className="flex items-center gap-1">
          <AddToCartButton productId={product.id} className="flex-1" />
          <CompareButton productId={product.id} />
        </div>
      </CardContent>
    </Card>
  );
}
