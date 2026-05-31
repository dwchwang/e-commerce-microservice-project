// Maps backend DTO shapes to the normalized UI types in types.ts.

import type {
  Product,
  ProductResponse,
  ProductSummaryResponse,
} from "@/lib/api/types";

/** Search-service SearchResponse shape (price as Double, imageUrls). */
export interface SearchResultItem {
  id: string;
  sku?: string;
  name: string;
  description?: string;
  price?: number;
  categoryName?: string;
  brandName?: string;
  imageUrls?: string[];
}

export function mapProductSummary(p: ProductSummaryResponse): Product {
  return {
    id: p.id,
    sku: p.sku,
    name: p.name,
    slug: p.id, // BE uses id-based product detail lookup
    price: p.price,
    brand: p.brandName,
    category: p.categoryName,
    imageUrl: p.primaryImageUrl,
  };
}

export function mapProductDetail(p: ProductResponse): Product {
  const specs = (p.specs ?? []).reduce<Record<string, string>>((acc, s) => {
    if (s.specName) acc[s.specName] = s.specValue ?? "";
    return acc;
  }, {});

  return {
    id: p.id,
    sku: p.sku,
    name: p.name,
    slug: p.id,
    description: p.description,
    price: p.price,
    brand: p.brandName,
    category: p.categoryName,
    imageUrl: p.imageUrls?.[0],
    images: p.imageUrls ?? [],
    specs: Object.keys(specs).length > 0 ? specs : undefined,
  };
}

export function mapSearchResult(s: SearchResultItem): Product {
  return {
    id: s.id,
    sku: s.sku,
    name: s.name,
    slug: s.id,
    description: s.description,
    price: s.price ?? 0,
    brand: s.brandName,
    category: s.categoryName,
    imageUrl: s.imageUrls?.[0],
    images: s.imageUrls ?? [],
  };
}
