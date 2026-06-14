import Link from "next/link";
import Image from "next/image";
import { serverFetch } from "@/lib/api/server-client";
import type { Product, FlashSale, ProductSummaryResponse, SpringPage } from "@/lib/api/types";
import { mapProductSummary } from "@/lib/api/mappers";
import { ProductCard } from "@/components/product/ProductCard";
import { BannerCarousel, type Banner } from "@/components/home/BannerCarousel";
import { resolveProductImage, HERO_IMAGES } from "@/lib/product-image";
import { ArrowRight, Zap, Truck, ShieldCheck, RotateCcw } from "lucide-react";

interface BlogPostSummary {
  id: string;
  title?: string;
  slug?: string;
  content?: string;
  thumbnailUrl?: string;
  publishedAt?: string;
}

export default async function HomePage() {
  let featured: Product[] = [];
  let flashSales: FlashSale[] = [];
  let banners: Banner[] = [];
  let posts: BlogPostSummary[] = [];

  const [productsRes, flashRes, bannerRes, postsRes] = await Promise.allSettled([
    serverFetch<SpringPage<ProductSummaryResponse>>("/products?size=8&sort=createdAt,desc", {}, { revalidate: 60 }),
    serverFetch<FlashSale[]>("/flash-sales", {}, { revalidate: 30 }),
    serverFetch<Banner[]>("/content/banners/active", {}, { revalidate: 60 }),
    serverFetch<SpringPage<BlogPostSummary>>("/content/posts?size=3&sort=publishedAt,desc", {}, { revalidate: 120 }),
  ]);

  if (productsRes.status === "fulfilled") featured = (productsRes.value.content ?? []).map(mapProductSummary);
  if (flashRes.status === "fulfilled") flashSales = Array.isArray(flashRes.value) ? flashRes.value : [];
  if (bannerRes.status === "fulfilled" && Array.isArray(bannerRes.value)) {
    banners = bannerRes.value.filter((b) => b.imageUrl);
  }
  if (postsRes.status === "fulfilled") posts = postsRes.value.content ?? [];

  const activeFlash = flashSales.filter((fs) => fs.status === "ACTIVE" || fs.status === "SCHEDULED").slice(0, 4);

  return (
    <div>
      {/* ---- Hero ---- */}
      {banners.length > 0 ? (
        <BannerCarousel banners={banners} />
      ) : (
        <section className="bg-secondary">
          <div className="container mx-auto flex flex-col items-center px-4 py-20 text-center md:py-28">
            <h1 className="headline-tight max-w-3xl text-4xl text-foreground md:text-6xl">
              Thiết bị điện tử <span className="text-primary">chính hãng</span>
            </h1>
            <p className="mt-5 max-w-xl text-lg font-light text-muted-foreground md:text-xl">
              Laptop, điện thoại, máy tính bảng và phụ kiện — giá tốt, bảo hành
              chính hãng, giao hàng toàn quốc.
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
              <Link
                href="/products"
                className="inline-flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-[15px] font-medium text-primary-foreground transition active:scale-95"
              >
                Xem sản phẩm <ArrowRight className="size-4" />
              </Link>
              <Link
                href="/flash-sales"
                className="inline-flex items-center gap-2 rounded-full border border-primary px-6 py-3 text-[15px] font-medium text-primary transition active:scale-95"
              >
                <Zap className="size-4" /> Flash Sale
              </Link>
            </div>
          </div>
        </section>
      )}

      {/* ---- Trust strip ---- */}
      <section className="border-b border-border bg-white">
        <div className="container mx-auto grid grid-cols-1 gap-4 px-4 py-6 sm:grid-cols-3">
          {[
            { icon: Truck, title: "Giao hàng toàn quốc", desc: "Miễn phí cho đơn từ 5 triệu" },
            { icon: ShieldCheck, title: "Bảo hành chính hãng", desc: "Cam kết 100% hàng thật" },
            { icon: RotateCcw, title: "Đổi trả 7 ngày", desc: "Hoàn tiền nếu lỗi nhà sản xuất" },
          ].map((f) => (
            <div key={f.title} className="flex items-center gap-3">
              <f.icon className="size-6 text-primary" />
              <div>
                <p className="text-sm font-semibold tracking-tight">{f.title}</p>
                <p className="text-xs text-muted-foreground">{f.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ---- Flash Sale (dark tile) ---- */}
      {activeFlash.length > 0 && (
        <section className="tile-dark">
          <div className="container mx-auto px-4 py-16">
            <div className="mb-8 flex items-end justify-between">
              <div>
                <p className="mb-1 inline-flex items-center gap-2 text-sm font-medium text-[var(--primary-on-dark)]">
                  <Zap className="size-4" /> Ưu đãi có hạn
                </p>
                <h2 className="headline-tight text-3xl text-white md:text-4xl">Flash Sale</h2>
              </div>
              <Link href="/flash-sales" className="text-[15px] font-medium text-[var(--primary-on-dark)] hover:underline">
                Xem tất cả <ArrowRight className="ml-1 inline size-4" />
              </Link>
            </div>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
              {activeFlash.map((fs) => (
                <Link
                  key={fs.id}
                  href={`/flash-sales/${fs.id}`}
                  className="group flex flex-col overflow-hidden rounded-2xl bg-[var(--tile-dark-2)] transition-transform hover:-translate-y-1"
                >
                  <div className="relative aspect-square overflow-hidden">
                    <Image
                      src={resolveProductImage({ id: fs.productId, name: fs.productName }, undefined, 500)}
                      alt={fs.productName}
                      fill
                      className="object-cover transition-transform duration-500 group-hover:scale-105"
                      sizes="(max-width: 768px) 50vw, 25vw"
                    />
                  </div>
                  <div className="flex flex-col p-4">
                    <h3 className="line-clamp-2 text-[14px] font-medium text-white">{fs.productName}</h3>
                    <div className="mt-2 flex items-baseline gap-2">
                      <span className="text-base font-semibold text-white">
                        {fs.salePrice.toLocaleString("vi-VN")}₫
                      </span>
                      {fs.originalPrice && fs.originalPrice > fs.salePrice && (
                        <span className="text-xs text-[var(--on-dark-muted)] line-through">
                          {fs.originalPrice.toLocaleString("vi-VN")}₫
                        </span>
                      )}
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ---- Featured products ---- */}
      <section className="container mx-auto px-4 py-16">
        <div className="mb-8 flex items-end justify-between">
          <h2 className="headline-tight text-3xl md:text-4xl">Sản phẩm nổi bật</h2>
          <Link href="/products" className="text-[15px] font-medium text-primary hover:underline">
            Xem tất cả <ArrowRight className="ml-1 inline size-4" />
          </Link>
        </div>
        {featured.length > 0 ? (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
            {featured.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        ) : (
          <div className="rounded-2xl bg-secondary py-16 text-center text-muted-foreground">
            <p>Chưa có sản phẩm nào. Hãy khởi động backend để xem dữ liệu.</p>
          </div>
        )}
      </section>

      {/* ---- Promo tile (dark, editorial) ---- */}
      <section className="tile-dark">
        <div className="container mx-auto grid items-center gap-8 px-4 py-20 md:grid-cols-2">
          <div>
            <h2 className="headline-tight text-3xl text-white md:text-5xl">
              Công nghệ cho mọi nhu cầu.
            </h2>
            <p className="mt-4 max-w-md text-lg font-light text-[var(--on-dark-muted)]">
              Từ laptop hiệu năng cao đến phụ kiện thiết yếu — khám phá bộ sưu
              tập đầy đủ với giá tốt nhất.
            </p>
            <Link
              href="/products"
              className="mt-6 inline-flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-[15px] font-medium text-primary-foreground transition active:scale-95"
            >
              Khám phá ngay <ArrowRight className="size-4" />
            </Link>
          </div>
          <div className="relative aspect-[4/3] overflow-hidden rounded-3xl">
            <Image
              src={`${HERO_IMAGES.gadgets}?w=900&q=75&auto=format&fit=crop`}
              alt="Bộ sưu tập thiết bị"
              fill
              className="object-cover"
              sizes="(max-width: 768px) 100vw, 50vw"
            />
          </div>
        </div>
      </section>

      {/* ---- Latest posts (content-service) ---- */}
      {posts.length > 0 && (
        <section className="container mx-auto px-4 py-16">
          <div className="mb-8 flex items-end justify-between">
            <h2 className="headline-tight text-3xl md:text-4xl">Bài viết mới nhất</h2>
          </div>
          <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
            {posts.map((post) => (
              <Link
                key={post.id}
                href={`/content/${post.slug}`}
                className="group flex flex-col overflow-hidden rounded-2xl border border-border bg-card transition-transform hover:-translate-y-1"
              >
                <div className="relative aspect-[16/9] overflow-hidden bg-secondary">
                  <Image
                    src={post.thumbnailUrl || `${HERO_IMAGES.workspace}?w=700&q=70&auto=format&fit=crop`}
                    alt={post.title || "Bài viết"}
                    fill
                    className="object-cover transition-transform duration-500 group-hover:scale-105"
                    sizes="(max-width: 768px) 100vw, 33vw"
                  />
                </div>
                <div className="flex flex-col p-5">
                  <h3 className="line-clamp-2 text-lg font-semibold tracking-tight group-hover:text-primary">
                    {post.title}
                  </h3>
                  {post.content && (
                    <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">
                      {post.content.replace(/<[^>]+>/g, "").slice(0, 120)}
                    </p>
                  )}
                  {post.publishedAt && (
                    <p className="mt-3 text-xs text-muted-foreground">
                      {new Date(post.publishedAt).toLocaleDateString("vi-VN")}
                    </p>
                  )}
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
