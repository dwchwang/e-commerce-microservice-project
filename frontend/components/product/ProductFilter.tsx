"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Checkbox } from "@/components/ui/checkbox";
import { Slider } from "@/components/ui/slider";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { Button } from "@/components/ui/button";
import { useState, useMemo } from "react";

const DEFAULT_MAX_PRICE = 50_000_000;

export type FilterFacets = {
  categories: { id: string; name: string }[];
  brands: { id: string; name: string }[];
};

export function ProductFilter({ facets }: { facets: FilterFacets }) {
  const router = useRouter();
  const params = useSearchParams();

  // BE getProducts filters by a single categoryId / brandId (UUID).
  const setSingle = (key: string, value: string) => {
    const sp = new URLSearchParams(params);
    if (sp.get(key) === value) {
      sp.delete(key);
    } else {
      sp.set(key, value);
    }
    sp.delete("page");
    router.push(`/products?${sp.toString()}`);
  };

  const clearAll = () => router.push("/products");

  const initialPriceRange = useMemo<[number, number]>(
    () => [Number(params.get("minPrice")) || 0, Number(params.get("maxPrice")) || DEFAULT_MAX_PRICE],
    [params]
  );
  const priceRangeKey = `${initialPriceRange[0]}:${initialPriceRange[1]}`;

  const hasFilters = ["categoryId", "brandId", "minPrice", "maxPrice", "keyword"].some((k) => params.has(k));

  const hasCategories = facets.categories.length > 0;
  const hasBrands = facets.brands.length > 0;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold">Bộ lọc</h3>
        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={clearAll}>
            Xóa tất cả
          </Button>
        )}
      </div>

      <Accordion multiple>
        {hasCategories && (
          <AccordionItem value="category">
            <AccordionTrigger>Danh mục</AccordionTrigger>
            <AccordionContent className="space-y-2">
              {facets.categories.map((c) => (
                <label key={c.id} className="flex items-center gap-2 text-sm cursor-pointer">
                  <Checkbox
                    checked={params.get("categoryId") === c.id}
                    onCheckedChange={() => setSingle("categoryId", c.id)}
                  />
                  <span>{c.name}</span>
                </label>
              ))}
            </AccordionContent>
          </AccordionItem>
        )}

        {hasBrands && (
          <AccordionItem value="brand">
            <AccordionTrigger>Thương hiệu</AccordionTrigger>
            <AccordionContent className="space-y-2">
              {facets.brands.map((b) => (
                <label key={b.id} className="flex items-center gap-2 text-sm cursor-pointer">
                  <Checkbox
                    checked={params.get("brandId") === b.id}
                    onCheckedChange={() => setSingle("brandId", b.id)}
                  />
                  <span>{b.name}</span>
                </label>
              ))}
            </AccordionContent>
          </AccordionItem>
        )}

        <AccordionItem value="price">
          <AccordionTrigger>Khoảng giá</AccordionTrigger>
          <AccordionContent className="space-y-3 px-1">
            <PriceRangeFilter key={priceRangeKey} initialPriceRange={initialPriceRange} maxPrice={DEFAULT_MAX_PRICE} />
          </AccordionContent>
        </AccordionItem>
      </Accordion>

      {!hasCategories && !hasBrands && (
        <p className="text-sm text-muted-foreground">Chưa có dữ liệu danh mục/thương hiệu.</p>
      )}
    </div>
  );
}

function PriceRangeFilter({
  initialPriceRange,
  maxPrice,
}: {
  initialPriceRange: [number, number];
  maxPrice: number;
}) {
  const router = useRouter();
  const params = useSearchParams();
  const [priceRange, setPriceRange] = useState<[number, number]>(initialPriceRange);

  const applyPrice = () => {
    const sp = new URLSearchParams(params);
    if (priceRange[0] > 0) sp.set("minPrice", String(priceRange[0]));
    else sp.delete("minPrice");
    if (priceRange[1] < maxPrice) sp.set("maxPrice", String(priceRange[1]));
    else sp.delete("maxPrice");
    sp.delete("page");
    router.push(`/products?${sp.toString()}`);
  };

  return (
    <>
      <Slider
        min={0}
        max={maxPrice}
        step={500_000}
        value={priceRange}
        onValueChange={(v) => setPriceRange(v as [number, number])}
      />
      <div className="flex justify-between text-xs text-muted-foreground">
        <span>{priceRange[0].toLocaleString("vi-VN")}₫</span>
        <span>{priceRange[1].toLocaleString("vi-VN")}₫</span>
      </div>
      <Button size="sm" variant="outline" className="w-full" onClick={applyPrice}>
        Áp dụng
      </Button>
    </>
  );
}
