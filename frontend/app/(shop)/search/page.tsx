import Link from "next/link";
import { serverFetch } from "@/lib/api/server-client";
import type { Product, SpringPage } from "@/lib/api/types";
import { mapSearchResult, type SearchResultItem } from "@/lib/api/mappers";
import { ProductGrid } from "@/components/product/ProductGrid";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { ChevronLeft, ChevronRight } from "lucide-react";

export default async function SearchPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string; page?: string }>;
}) {
  const sp = await searchParams;
  const query = sp.q || "";
  const page = Math.max(0, Number(sp.page ?? 0) || 0);

  let products: Product[] = [];
  let totalPages = 0;
  let totalElements = 0;

  if (query) {
    try {
      const res = await serverFetch<SpringPage<SearchResultItem>>(
        `/search?q=${encodeURIComponent(query)}&size=20&page=${page}`,
        {},
        { revalidate: 30 }
      );
      products = (res.content ?? []).map(mapSearchResult);
      totalPages = res.totalPages ?? 0;
      totalElements = res.totalElements ?? products.length;
    } catch {
      // empty
    }
  }

  function pageHref(target: number) {
    return `/search?q=${encodeURIComponent(query)}&page=${target}`;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="mb-2 text-3xl font-semibold tracking-tight md:text-4xl">Tìm kiếm</h1>
      {query && (
        <p className="mb-6 text-muted-foreground">
          Kết quả cho: <span className="font-medium text-foreground">&quot;{query}&quot;</span>
          {totalElements > 0 && ` — ${totalElements} sản phẩm`}
        </p>
      )}

      {!query ? (
        <p className="py-12 text-center text-muted-foreground">Nhập từ khóa để tìm kiếm sản phẩm.</p>
      ) : products.length === 0 ? (
        <p className="py-12 text-center text-muted-foreground">
          Không tìm thấy sản phẩm nào cho &quot;{query}&quot;.
        </p>
      ) : (
        <>
          <ProductGrid products={products} />

          {totalPages > 1 && (
            <nav className="mt-8 flex items-center justify-center gap-2" aria-label="Phân trang kết quả">
              {page > 0 ? (
                <Link href={pageHref(page - 1)} className={buttonVariants({ variant: "outline", size: "sm" })}>
                  <ChevronLeft className="h-4 w-4" /> Trước
                </Link>
              ) : (
                <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "pointer-events-none opacity-50")}>
                  <ChevronLeft className="h-4 w-4" /> Trước
                </span>
              )}
              <span className="px-3 text-sm text-muted-foreground">Trang {page + 1}/{totalPages}</span>
              {page < totalPages - 1 ? (
                <Link href={pageHref(page + 1)} className={buttonVariants({ variant: "outline", size: "sm" })}>
                  Sau <ChevronRight className="h-4 w-4" />
                </Link>
              ) : (
                <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "pointer-events-none opacity-50")}>
                  Sau <ChevronRight className="h-4 w-4" />
                </span>
              )}
            </nav>
          )}
        </>
      )}
    </div>
  );
}
