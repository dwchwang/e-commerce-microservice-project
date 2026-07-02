"use client";

import { SlidersHorizontal } from "lucide-react";
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet";
import { ProductFilter, type FilterFacets } from "@/components/product/ProductFilter";

export function MobileFilterSheet({ facets }: { facets: FilterFacets }) {
  return (
    <Sheet>
      <SheetTrigger className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-border px-3 text-sm font-medium hover:bg-muted md:hidden">
        <SlidersHorizontal className="size-4" />
        Bộ lọc
      </SheetTrigger>
      <SheetContent side="left" className="w-80 overflow-y-auto p-6">
        <div className="mt-4">
          <ProductFilter facets={facets} />
        </div>
      </SheetContent>
    </Sheet>
  );
}
