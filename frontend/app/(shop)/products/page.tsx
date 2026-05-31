import { Suspense } from "react";
import Link from "next/link";
import { serverFetch } from "@/lib/api/server-client";
import type { Product, ProductSummaryResponse, SpringPage } from "@/lib/api/types";
import { mapProductSummary } from "@/lib/api/mappers";
import { ProductGrid } from "@/components/product/ProductGrid";
import { ProductFilter, type FilterFacets } from "@/components/product/ProductFilter";
import { Skeleton } from "@/components/ui/skeleton";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { ChevronLeft, ChevronRight } from "lucide-react";

type CategoryOption = { id: string; name: string };
type BrandOption = { id: string; name: string };

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
  let totalPages = 0;
  let currentPage = 0;
  let facets: FilterFacets = { categories: [], brands: [] };

  try {
    const [productsRes, categories, brands] = await Promise.all([
      serverFetch<SpringPage<ProductSummaryResponse>>(`/products?${qs.toString()}`, {}, { revalidate: 60 }),
      serverFetch<CategoryOption[]>("/products/categories", {}, { revalidate: 300 }).catch(() => []),
      serverFetch<BrandOption[]>("/products/brands", {}, { revalidate: 300 }).catch(() => []),
    ]);
    products = (productsRes.content ?? []).map(mapProductSummary);
    totalPages = productsRes.totalPages ?? 0;
    currentPage = productsRes.number ?? 0;
    facets = {
      categories: (categories ?? []).map((c) => ({ id: c.id, name: c.name })),
      brands: (brands ?? []).map((b) => ({ id: b.id, name: b.name })),
    };
  } catch {
    // Ignore — show empty
  }

  function pageHref(targetPage: number) {
    const params = new URLSearchParams(qs.toString());
    params.set("page", String(targetPage));
    return `/products?${params.toString()}`;
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

          {totalPages > 1 && (
            <nav className="mt-8 flex items-center justify-center gap-2" aria-label="Phân trang sản phẩm">
              {currentPage > 0 ? (
                <Link href={pageHref(currentPage - 1)} className={buttonVariants({ variant: "outline", size: "sm" })}>
                  <ChevronLeft className="h-4 w-4" />
                  Trước
                </Link>
              ) : (
                <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "pointer-events-none opacity-50")}>
                  <ChevronLeft className="h-4 w-4" />
                  Trước
                </span>
              )}
              <span className="px-3 text-sm text-muted-foreground">
                Trang {currentPage + 1}/{totalPages}
              </span>
              {currentPage < totalPages - 1 ? (
                <Link href={pageHref(currentPage + 1)} className={buttonVariants({ variant: "outline", size: "sm" })}>
                  Sau
                  <ChevronRight className="h-4 w-4" />
                </Link>
              ) : (
                <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "pointer-events-none opacity-50")}>
                  Sau
                  <ChevronRight className="h-4 w-4" />
                </span>
              )}
            </nav>
          )}
        </main>
      </div>
    </div>
  );
}
