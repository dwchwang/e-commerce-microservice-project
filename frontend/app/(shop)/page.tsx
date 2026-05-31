import Link from "next/link";
import { serverFetch } from "@/lib/api/server-client";
import type { Product, FlashSale, ProductSummaryResponse, SpringPage } from "@/lib/api/types";
import { mapProductSummary } from "@/lib/api/mappers";
import { ProductCard } from "@/components/product/ProductCard";
import { Button } from "@/components/ui/button";
import { ArrowRight, Zap } from "lucide-react";

export default async function HomePage() {
  let featured: Product[] = [];
  let flashSales: FlashSale[] = [];

  try {
    const [productsRes, flashRes] = await Promise.all([
      serverFetch<SpringPage<ProductSummaryResponse>>("/products?size=8&sort=createdAt,desc", {}, { revalidate: 60 }),
      serverFetch<FlashSale[]>("/flash-sales", {}, { revalidate: 30 }),
    ]);
    featured = (productsRes.content ?? []).map(mapProductSummary);
    flashSales = Array.isArray(flashRes) ? flashRes : [];
  } catch {
    // BE might not be running — show empty state
  }

  return (
    <div>
      {/* Hero Banner */}
      <section className="bg-linear-to-r from-primary/10 via-primary/5 to-background py-16 md:py-24">
        <div className="container mx-auto px-4 text-center">
          <h1 className="text-4xl md:text-5xl font-bold tracking-tight mb-4">
            Thiết bị điện tử <span className="text-primary">chính hãng</span>
          </h1>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto mb-8">
            Laptop, điện thoại, máy tính bảng và phụ kiện — giá tốt, bảo hành chính hãng, giao hàng toàn quốc.
          </p>
          <div className="flex gap-4 justify-center">
            <Link href="/products">
              <Button size="lg">
                Xem sản phẩm <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </Link>
            <Link href="/flash-sales">
              <Button size="lg" variant="outline">
                <Zap className="mr-2 h-4 w-4" /> Flash Sale
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Flash Sales */}
      {flashSales.length > 0 && (
        <section className="container mx-auto px-4 py-12">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-bold flex items-center gap-2">
              <Zap className="h-6 w-6 text-orange-500" /> Flash Sale
            </h2>
            <Link href="/flash-sales">
              <Button variant="ghost" size="sm">
                Xem tất cả <ArrowRight className="ml-1 h-4 w-4" />
              </Button>
            </Link>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {flashSales.slice(0, 4).map((fs) => (
              <ProductCard
                key={fs.id}
                product={{
                  id: fs.productId,
                  name: fs.productName,
                  slug: fs.productId,
                  price: fs.salePrice,
                  originalPrice: fs.originalPrice,
                }}
              />
            ))}
          </div>
        </section>
      )}

      {/* Featured Products */}
      <section className="container mx-auto px-4 py-12">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold">Sản phẩm nổi bật</h2>
          <Link href="/products">
            <Button variant="ghost" size="sm">
              Xem tất cả <ArrowRight className="ml-1 h-4 w-4" />
            </Button>
          </Link>
        </div>
        {featured.length > 0 ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {featured.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-muted-foreground">
            <p>Chưa có sản phẩm nào. Hãy khởi động backend để xem dữ liệu.</p>
          </div>
        )}
      </section>
    </div>
  );
}
