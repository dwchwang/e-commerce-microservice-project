import { serverFetch } from "@/lib/api/server-client";
import type { Product } from "@/lib/api/types";
import { Card, CardContent } from "@/components/ui/card";
import { PriceTag } from "@/components/shared/PriceTag";
import { Button } from "@/components/ui/button";
import Link from "next/link";

export default async function ComparePage({
  searchParams,
}: {
  searchParams: Promise<{ ids?: string }>;
}) {
  const sp = await searchParams;
  const ids = (sp.ids ?? "").split(",").filter(Boolean);

  if (ids.length === 0) {
    return (
      <div className="container mx-auto px-4 py-16 text-center">
        <h1 className="text-2xl font-bold mb-4">So sánh sản phẩm</h1>
        <p className="text-muted-foreground mb-4">Chưa có sản phẩm nào được chọn để so sánh.</p>
        <Link href="/products">
          <Button>Xem sản phẩm</Button>
        </Link>
      </div>
    );
  }

  let products: Product[] = [];
  try {
    const results = await Promise.allSettled(
      ids.map((id) => serverFetch<Product>(`/products/${id}`, {}, { revalidate: 60 }))
    );
    products = results
      .filter((r) => r.status === "fulfilled")
      .map((r) => (r as PromiseFulfilledResult<Product>).value);
  } catch {
    // ignore
  }

  if (products.length === 0) {
    return (
      <div className="container mx-auto px-4 py-16 text-center">
        <h1 className="text-2xl font-bold mb-4">So sánh sản phẩm</h1>
        <p className="text-muted-foreground">Không thể tải thông tin sản phẩm.</p>
      </div>
    );
  }

  // Collect all spec keys
  const allSpecKeys = new Set<string>();
  products.forEach((p) => {
    if (p.specs) Object.keys(p.specs).forEach((k) => allSpecKeys.add(k));
  });

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">So sánh sản phẩm</h1>

      <div className="overflow-x-auto">
        <table className="w-full border-collapse">
          <thead>
            <tr>
              <th className="p-3 border bg-muted/50 text-left w-40">Thuộc tính</th>
              {products.map((p) => (
                <th key={p.id} className="p-3 border bg-muted/50 text-center min-w-50">
                  <Link href={`/products/${p.slug || p.id}`} className="hover:text-primary">
                    {p.name}
                  </Link>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            <tr>
              <td className="p-3 border font-medium">Giá</td>
              {products.map((p) => (
                <td key={p.id} className="p-3 border text-center">
                  <PriceTag price={p.price} originalPrice={p.originalPrice} />
                </td>
              ))}
            </tr>
            <tr>
              <td className="p-3 border font-medium">Thương hiệu</td>
              {products.map((p) => (
                <td key={p.id} className="p-3 border text-center">{p.brand || "—"}</td>
              ))}
            </tr>
            {Array.from(allSpecKeys).map((key) => (
              <tr key={key}>
                <td className="p-3 border font-medium">{key}</td>
                {products.map((p) => (
                  <td key={p.id} className="p-3 border text-center">
                    {p.specs?.[key] || "—"}
                  </td>
                ))}
              </tr>
            ))}
            <tr>
              <td className="p-3 border font-medium">Đánh giá</td>
              {products.map((p) => (
                <td key={p.id} className="p-3 border text-center">
                  {p.rating ? `★ ${p.rating.toFixed(1)}` : "—"}
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
