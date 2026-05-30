"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Checkbox } from "@/components/ui/checkbox";
import { Slider } from "@/components/ui/slider";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { Button } from "@/components/ui/button";
import { useState, useEffect, useCallback, useMemo } from "react";
import type { Facets } from "@/lib/api/types";

const DEFAULT_MAX_PRICE = 50_000_000;

export function ProductFilter({ facets }: { facets: Facets | null }) {
  const router = useRouter();
  const params = useSearchParams();

  const maxPrice = useMemo(
    () => facets?.priceRange?.max || DEFAULT_MAX_PRICE,
    [facets]
  );

  const buildParams = useCallback(
    (overrides: Record<string, string | string[]>) => {
      const sp = new URLSearchParams(params);
      Object.entries(overrides).forEach(([k, v]) => {
        sp.delete(k);
        (Array.isArray(v) ? v : [v]).forEach((x) => sp.append(k, x));
      });
      return sp.toString();
    },
    [params]
  );

  const toggleFilter = (key: string, value: string) => {
    const current = params.getAll(key);
    const next = current.includes(value)
      ? current.filter((v) => v !== value)
      : [...current, value];
    router.push(`/products?${buildParams({ [key]: next })}`);
  };

  const clearAll = () => router.push("/products");

  // Price range filter
  const [priceRange, setPriceRange] = useState<[number, number]>([
    Number(params.get("minPrice")) || 0,
    Number(params.get("maxPrice")) || maxPrice,
  ]);

  useEffect(() => {
    setPriceRange([
      Number(params.get("minPrice")) || 0,
      Number(params.get("maxPrice")) || maxPrice,
    ]);
  }, [params, maxPrice]);

  const applyPrice = () => {
    const sp = new URLSearchParams(params);
    if (priceRange[0] > 0) sp.set("minPrice", String(priceRange[0]));
    else sp.delete("minPrice");
    if (priceRange[1] < maxPrice) sp.set("maxPrice", String(priceRange[1]));
    else sp.delete("maxPrice");
    router.push(`/products?${sp.toString()}`);
  };

  const hasFilters = Array.from(params.keys()).some(
    (k) => k !== "page" && k !== "size" && k !== "sort"
  );

  if (!facets) {
    return (
      <div className="space-y-4">
        <h3 className="font-semibold">Bộ lọc</h3>
        <p className="text-sm text-muted-foreground">Đang tải bộ lọc...</p>
      </div>
    );
  }

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
        {/* Categories */}
        {facets.categories && facets.categories.length > 0 && (
          <AccordionItem value="category">
            <AccordionTrigger>Danh mục</AccordionTrigger>
            <AccordionContent className="space-y-2">
              {facets.categories.map((c) => (
                <label key={c.value} className="flex items-center gap-2 text-sm cursor-pointer">
                  <Checkbox
                    checked={params.getAll("category").includes(c.value)}
                    onCheckedChange={() => toggleFilter("category", c.value)}
                  />
                  <span>{c.value}</span>
                  <span className="text-muted-foreground ml-auto">({c.count})</span>
                </label>
              ))}
            </AccordionContent>
          </AccordionItem>
        )}

        {/* Brands */}
        {facets.brands && facets.brands.length > 0 && (
          <AccordionItem value="brand">
            <AccordionTrigger>Thương hiệu</AccordionTrigger>
            <AccordionContent className="space-y-2">
              {facets.brands.map((b) => (
                <label key={b.value} className="flex items-center gap-2 text-sm cursor-pointer">
                  <Checkbox
                    checked={params.getAll("brand").includes(b.value)}
                    onCheckedChange={() => toggleFilter("brand", b.value)}
                  />
                  <span>{b.value}</span>
                  <span className="text-muted-foreground ml-auto">({b.count})</span>
                </label>
              ))}
            </AccordionContent>
          </AccordionItem>
        )}

        {/* Price Range */}
        {facets.priceRange && (
          <AccordionItem value="price">
            <AccordionTrigger>Khoảng giá</AccordionTrigger>
            <AccordionContent className="space-y-3 px-1">
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
            </AccordionContent>
          </AccordionItem>
        )}
      </Accordion>
    </div>
  );
}
