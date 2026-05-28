"use client";

import Link from "next/link";
import { BarChart3, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useCompare } from "@/lib/compare/store";

export function CompareDrawer() {
  const { productIds, clear } = useCompare();

  if (productIds.length === 0) return null;

  return (
    <div className="fixed bottom-4 left-1/2 -translate-x-1/2 z-40 bg-background border shadow-lg rounded-lg px-4 py-3 flex items-center gap-3">
      <BarChart3 className="h-4 w-4 text-primary shrink-0" />
      <span className="text-sm font-medium">
        {productIds.length} sản phẩm được chọn
      </span>
      <Link
        href={`/compare?ids=${productIds.join(",")}`}
        className="text-sm text-primary hover:underline font-medium"
      >
        So sánh ngay
      </Link>
      <Button variant="ghost" size="icon" className="h-6 w-6" onClick={clear}>
        <X className="h-3 w-3" />
      </Button>
    </div>
  );
}
