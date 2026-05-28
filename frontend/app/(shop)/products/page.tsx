import { Suspense } from "react";
import { serverFetch } from "@/lib/api/server-client";
import type { Product, PaginatedResponse, Facets } from "@/lib/api/types";
import { ProductGrid } from "@/components/product/ProductGrid";
import { ProductFilter } from "@/components/product/ProductFilter";
import { Skeleton } from "@/components/ui/skeleton";

export default async function ProductsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[]>>;
}) {
  const sp = await searchParams;
  const qs = new URLSearchParams();
  Object.entries(sp).forEach(([k, v]) => {
    (Array.isArray(v) ? v : [v]).forEach((x) => qs.append(k, x));
  });
  if (!qs.has("size")) qs.set("size", "20");

  let products: Product[] = [];
  let facets: Facets | null = null;
  let totalPages = 0;
  let currentPage = 0;

  try {
    const [productsRes, facetsRes] = await Promise.all([
      serverFetch<PaginatedResponse<Product>>(`/products?${qs.toString()}`, {}, { revalidate: 60 }),
      serverFetch<Facets>("/products/facets", {}, { revalidate: 300 }).catch(() => null),
    ]);
    products = productsRes.data ?? [];
    totalPages = productsRes.totalPages ?? 0;
    currentPage = productsRes.page ?? 0;
    facets = facetsRes;
  } catch {
    // Ignore — show empty
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">Tất cả sản phẩm</h1>
      <div className="grid grid-cols-1 md:grid-cols-[260px_1fr] gap-6">
        <aside className="hidden md:block">
          <Suspense fallback={<Skeleton className="h-96" />}>
            <ProductFilter facets={facets} />
          </Suspense>
        </aside>
        <main>
          <Suspense fallback={<Skeleton className="h-96" />}>
            <ProductGrid products={products} />
          </Suspense>
        </main>
      </div>
    </div>
  );
}
