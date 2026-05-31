import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * Server-rendered pagination. Builds page links by merging the target page into
 * the current query string so existing filters (q, status...) are preserved.
 */
export function AdminPagination({
  basePath,
  page,
  totalPages,
  totalElements,
  searchParams,
}: {
  basePath: string;
  page: number;
  totalPages: number;
  totalElements: number;
  searchParams?: Record<string, string | string[] | undefined>;
}) {
  if (totalPages <= 1) {
    return <span className="text-sm text-muted-foreground">Tổng {totalElements} mục</span>;
  }

  function hrefFor(targetPage: number) {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(searchParams ?? {})) {
      if (key === "page" || value === undefined) continue;
      params.set(key, Array.isArray(value) ? value[0] : value);
    }
    params.set("page", String(targetPage));
    return `${basePath}?${params.toString()}`;
  }

  const prevDisabled = page <= 0;
  const nextDisabled = page >= totalPages - 1;

  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-sm text-muted-foreground">
        Trang {page + 1}/{totalPages} · {totalElements} mục
      </span>
      <div className="flex items-center gap-2">
        {prevDisabled ? (
          <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "pointer-events-none opacity-50")}>
            <ChevronLeft className="size-4" />
            Trước
          </span>
        ) : (
          <Link href={hrefFor(page - 1)} className={buttonVariants({ variant: "outline", size: "sm" })}>
            <ChevronLeft className="size-4" />
            Trước
          </Link>
        )}
        {nextDisabled ? (
          <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "pointer-events-none opacity-50")}>
            Sau
            <ChevronRight className="size-4" />
          </span>
        ) : (
          <Link href={hrefFor(page + 1)} className={buttonVariants({ variant: "outline", size: "sm" })}>
            Sau
            <ChevronRight className="size-4" />
          </Link>
        )}
      </div>
    </div>
  );
}
