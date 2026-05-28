import { serverFetch } from "@/lib/api/server-client";
import type { Product, PaginatedResponse } from "@/lib/api/types";
import { ProductGrid } from "@/components/product/ProductGrid";

export default async function SearchPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const sp = await searchParams;
  const query = sp.q || "";

  let products: Product[] = [];

  if (query) {
    try {
      const res = await serverFetch<PaginatedResponse<Product>>(
        `/search?q=${encodeURIComponent(query)}&size=20`,
        {},
        { revalidate: 30 }
      );
      products = res.data ?? [];
    } catch {
      // empty
    }
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-2">Tìm kiếm</h1>
      {query && (
        <p className="text-muted-foreground mb-6">
          Kết quả cho: <span className="font-medium text-foreground">&quot;{query}&quot;</span>
          {products.length > 0 && ` (${products.length} sản phẩm)`}
        </p>
      )}
      {query && products.length === 0 ? (
        <p className="text-center py-12 text-muted-foreground">
          Không tìm thấy sản phẩm nào cho &quot;{query}&quot;.
        </p>
      ) : query ? (
        <ProductGrid products={products} />
      ) : (
        <p className="text-center py-12 text-muted-foreground">
          Nhập từ khóa để tìm kiếm sản phẩm.
        </p>
      )}
    </div>
  );
}
