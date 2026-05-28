import Image from "next/image";
import { notFound } from "next/navigation";
import { serverFetch } from "@/lib/api/server-client";
import type { Product, Review, PaginatedResponse } from "@/lib/api/types";
import { PriceTag } from "@/components/shared/PriceTag";
import { AddToCartButton } from "@/components/cart/AddToCartButton";
import { CompareButton } from "@/components/product/CompareButton";
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
    product = await serverFetch<Product>(`/products/${slug}`, {}, { revalidate: 60 });
  } catch {
    notFound();
  }

  if (!product) notFound();

  const hasDiscount = product.originalPrice && product.originalPrice > product.price;

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Image Gallery */}
        <div className="aspect-square relative bg-muted rounded-lg overflow-hidden">
          {product.imageUrl ? (
            <Image
              src={product.imageUrl}
              alt={product.name}
              fill
              className="object-cover"
              sizes="(max-width: 768px) 100vw, 50vw"
              priority
            />
          ) : (
            <div className="flex items-center justify-center h-full text-6xl">📦</div>
          )}
          {hasDiscount && (
            <Badge className="absolute top-3 left-3 text-lg px-3 py-1 bg-destructive">
              -{Math.round((1 - product.price / product.originalPrice!) * 100)}%
            </Badge>
          )}
        </div>

        {/* Product Info */}
        <div>
          {product.brand && (
            <p className="text-sm text-muted-foreground mb-1">{product.brand}</p>
          )}
          <h1 className="text-2xl md:text-3xl font-bold mb-4">{product.name}</h1>

          {product.rating && (
            <div className="flex items-center gap-1 mb-4">
              <span className="text-yellow-500 text-lg">★</span>
              <span className="font-medium">{product.rating.toFixed(1)}</span>
              {product.reviewCount && (
                <span className="text-muted-foreground">({product.reviewCount} đánh giá)</span>
              )}
            </div>
          )}

          <PriceTag
            price={product.price}
            originalPrice={product.originalPrice}
            size="lg"
            className="mb-6"
          />

          <div className="flex gap-3 mb-6">
            <AddToCartButton productId={product.id} className="h-10 px-8" />
            <CompareButton productId={product.id} className="h-10 w-10" />
          </div>

          <Separator className="my-6" />

          {/* Tabs: Description, Specs, Reviews */}
          <Tabs defaultValue="description">
            <TabsList>
              <TabsTrigger value="description">Mô tả</TabsTrigger>
              <TabsTrigger value="specs">Thông số</TabsTrigger>
              <TabsTrigger value="reviews">Đánh giá</TabsTrigger>
            </TabsList>
            <TabsContent value="description" className="mt-4 text-sm text-muted-foreground leading-relaxed">
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
              <p className="text-sm text-muted-foreground">
                Tính năng đánh giá sẽ có trong bản cập nhật tiếp theo.
              </p>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}
