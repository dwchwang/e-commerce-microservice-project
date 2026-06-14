import Image from "next/image";
import { notFound } from "next/navigation";
import { serverFetch } from "@/lib/api/server-client";
import type { Product, ProductResponse, ProductRating } from "@/lib/api/types";
import { mapProductDetail } from "@/lib/api/mappers";
import { resolveProductImage } from "@/lib/product-image";
import { PriceTag } from "@/components/shared/PriceTag";
import { AddToCartButton } from "@/components/cart/AddToCartButton";
import { ProductReviews } from "@/components/product/ProductReviews";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;

  let product: Product | null = null;
  try {
    const raw = await serverFetch<ProductResponse>(`/products/${slug}`, {}, { revalidate: 60 });
    product = mapProductDetail(raw);
  } catch {
    notFound();
  }

  if (!product) notFound();

  let rating: ProductRating | null = null;
  try {
    rating = await serverFetch<ProductRating>(`/reviews/product/${product.id}/rating`, {}, { revalidate: 60 });
  } catch {
    rating = null;
  }

  const hasDiscount = product.originalPrice && product.originalPrice > product.price;

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Image */}
        <div className="relative aspect-square overflow-hidden rounded-3xl bg-secondary">
          <Image
            src={resolveProductImage(product, product.imageUrl, 1000)}
            alt={product.name}
            fill
            className="object-cover"
            sizes="(max-width: 768px) 100vw, 50vw"
            priority
          />
          {hasDiscount && (
            <Badge className="absolute left-4 top-4 rounded-full bg-destructive px-3 py-1 text-base text-destructive-foreground">
              -{Math.round((1 - product.price / product.originalPrice!) * 100)}%
            </Badge>
          )}
        </div>

        {/* Info */}
        <div>
          {product.brand && (
            <p className="text-sm text-muted-foreground mb-1">{product.brand}</p>
          )}
          <h1 className="mb-4 text-2xl font-semibold tracking-tight md:text-4xl">{product.name}</h1>

          {rating && rating.reviewCount && rating.reviewCount > 0 && (
            <div className="flex items-center gap-1 mb-4">
              <span className="text-yellow-500 text-lg">★</span>
              <span className="font-medium">{(rating.averageRating ?? 0).toFixed(1)}</span>
              <span className="text-muted-foreground">({rating.reviewCount} đánh giá)</span>
            </div>
          )}

          <PriceTag price={product.price} originalPrice={product.originalPrice} size="lg" className="mb-6" />

          <div className="flex gap-3 mb-6">
            <AddToCartButton productId={product.id} className="h-11 rounded-full px-8" />
          </div>

          <Separator className="my-6" />

          <Tabs defaultValue="description">
            <TabsList>
              <TabsTrigger value="description">Mô tả</TabsTrigger>
              <TabsTrigger value="specs">Thông số</TabsTrigger>
              <TabsTrigger value="reviews">Đánh giá</TabsTrigger>
            </TabsList>
            <TabsContent value="description" className="mt-4 text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
              {product.description || "Chưa có mô tả cho sản phẩm này."}
            </TabsContent>
            <TabsContent value="specs" className="mt-4">
              {product.specs && Object.keys(product.specs).length > 0 ? (
                <table className="w-full text-sm">
                  <tbody>
                    {Object.entries(product.specs).map(([key, value]) => (
                      <tr key={key} className="border-b">
                        <td className="py-2 font-medium w-1/3">{key}</td>
                        <td className="py-2 text-muted-foreground">{value}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="text-sm text-muted-foreground">Chưa có thông số kỹ thuật.</p>
              )}
            </TabsContent>
            <TabsContent value="reviews" className="mt-4">
              <ProductReviews productId={product.id} />
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}
