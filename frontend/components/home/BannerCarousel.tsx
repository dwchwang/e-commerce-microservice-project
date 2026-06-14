"use client";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import Image from "next/image";
import { ChevronLeft, ChevronRight } from "lucide-react";

export interface Banner {
  id: string;
  title?: string;
  imageUrl?: string;
  linkUrl?: string;
}

export function BannerCarousel({ banners }: { banners: Banner[] }) {
  const [index, setIndex] = useState(0);
  const count = banners.length;

  const go = useCallback(
    (next: number) => setIndex((prev) => (next + count) % count),
    [count]
  );

  useEffect(() => {
    if (count <= 1) return;
    const t = setInterval(() => setIndex((p) => (p + 1) % count), 6000);
    return () => clearInterval(t);
  }, [count]);

  if (count === 0) return null;

  return (
    <section className="relative w-full overflow-hidden bg-black">
      <div className="relative mx-auto aspect-[21/9] max-h-[560px] w-full">
        {banners.map((b, i) => {
          const inner = (
            <Image
              src={b.imageUrl || ""}
              alt={b.title || "Banner"}
              fill
              priority={i === 0}
              className="object-cover"
              sizes="100vw"
            />
          );
          return (
            <div
              key={b.id}
              className={`absolute inset-0 transition-opacity duration-700 ${
                i === index ? "opacity-100" : "pointer-events-none opacity-0"
              }`}
            >
              {b.linkUrl ? <Link href={b.linkUrl}>{inner}</Link> : inner}
              {b.title && (
                <div className="absolute inset-0 flex items-end bg-gradient-to-t from-black/50 to-transparent p-8 md:p-14">
                  <h2 className="max-w-xl text-2xl font-semibold tracking-tight text-white md:text-4xl">
                    {b.title}
                  </h2>
                </div>
              )}
            </div>
          );
        })}

        {count > 1 && (
          <>
            <button
              aria-label="Trước"
              onClick={() => go(index - 1)}
              className="absolute left-4 top-1/2 grid size-10 -translate-y-1/2 place-items-center rounded-full bg-white/70 text-foreground backdrop-blur transition active:scale-95"
            >
              <ChevronLeft className="size-5" />
            </button>
            <button
              aria-label="Sau"
              onClick={() => go(index + 1)}
              className="absolute right-4 top-1/2 grid size-10 -translate-y-1/2 place-items-center rounded-full bg-white/70 text-foreground backdrop-blur transition active:scale-95"
            >
              <ChevronRight className="size-5" />
            </button>
            <div className="absolute bottom-4 left-1/2 flex -translate-x-1/2 gap-2">
              {banners.map((b, i) => (
                <button
                  key={b.id}
                  aria-label={`Banner ${i + 1}`}
                  onClick={() => setIndex(i)}
                  className={`h-2 rounded-full transition-all ${
                    i === index ? "w-6 bg-white" : "w-2 bg-white/50"
                  }`}
                />
              ))}
            </div>
          </>
        )}
      </div>
    </section>
  );
}
