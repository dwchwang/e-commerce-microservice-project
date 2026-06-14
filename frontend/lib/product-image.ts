// Deterministic real-image resolver for demo data.
//
// Backend seed products are created with empty `imageUrls`, so the storefront
// would otherwise render blank tiles. This module maps a product to a stable,
// real Unsplash photo based on its category / brand / name. The same product
// always resolves to the same image (hash of its id), and several products may
// share an image — which is acceptable for a demo catalog.
//
// All URLs below were verified to return HTTP 200 from the Unsplash CDN.

const UNSPLASH = "https://images.unsplash.com/photo-";

function u(id: string): string {
  return `${UNSPLASH}${id}`;
}

// Curated pools of real tech product photos, keyed by category keyword.
const POOLS: Record<string, string[]> = {
  laptop: [u("1496181133206-80ce9b88a853"), u("1517336714731-489689fd1ca8"), u("1526170375885-4d8ecf77b99f")],
  smartphone: [u("1511707171634-5f897ff02aa9"), u("1510557880182-3d4d3cba35a5"), u("1491933382434-500287f9b54b")],
  phone: [u("1511707171634-5f897ff02aa9"), u("1510557880182-3d4d3cba35a5")],
  tablet: [u("1542751371-adc38448a05e"), u("1544244015-0df4b3ffc6b0")],
  monitor: [u("1527443224154-c4a3942d3acf"), u("1527864550417-7fd91fc51a46"), u("1547082299-de196ea013d6")],
  keyboard: [u("1587829741301-dc798b83add3"), u("1618384887929-16ec33fab9ef"), u("1541140532154-b024d705b90a")],
  mouse: [u("1527814050087-3793815479db"), u("1615663245857-ac93bb7c39e7")],
  headphone: [u("1505740420928-5e560c06d30e"), u("1545454675-3531b543be5d"), u("1484704849700-f032a568e944")],
  earbuds: [u("1583394838336-acd977736f90"), u("1484704849700-f032a568e944")],
  speaker: [u("1546054454-aa26e2b734c7"), u("1572569511254-d8f925fe2cbb")],
  camera: [u("1516035069371-29a1b244cc32"), u("1502920917128-1aa500764cbd")],
  printer: [u("1612810806695-30f7a8258391")],
  watch: [u("1558618666-fcd25c85cd64"), u("1551446591-142875a901a1")],
};

// General fallback pool for anything that doesn't match a category keyword.
const DEFAULT_POOL = [
  u("1593642632823-8f785ba67e45"),
  u("1550009158-9ebf69173e03"),
  u("1468495244123-6c6c332eeece"),
  u("1505740420928-5e560c06d30e"),
];

// Hero / banner-friendly wide shots.
export const HERO_IMAGES = {
  workspace: u("1541140532154-b024d705b90a"),
  gadgets: u("1550009158-9ebf69173e03"),
  desktop: u("1527864550417-7fd91fc51a46"),
};

/** Stable 32-bit hash of a string (FNV-1a) for deterministic picks. */
function hash(input: string): number {
  let h = 0x811c9dc5;
  for (let i = 0; i < input.length; i++) {
    h ^= input.charCodeAt(i);
    h = Math.imul(h, 0x01000193);
  }
  return h >>> 0;
}

function poolFor(text: string): string[] {
  const t = text.toLowerCase();
  for (const [key, pool] of Object.entries(POOLS)) {
    if (t.includes(key)) return pool;
  }
  // Light Vietnamese keyword coverage.
  if (t.includes("điện thoại") || t.includes("dien thoai")) return POOLS.smartphone;
  if (t.includes("tai nghe")) return POOLS.headphone;
  if (t.includes("máy ảnh") || t.includes("may anh")) return POOLS.camera;
  if (t.includes("đồng hồ") || t.includes("dong ho")) return POOLS.watch;
  return DEFAULT_POOL;
}

export interface ImageSeed {
  id: string;
  name?: string;
  category?: string;
  brand?: string;
}

/**
 * Resolve a product to a real image URL. Returns the product's own image if it
 * has one; otherwise picks a deterministic demo image based on category/name.
 */
export function resolveProductImage(
  product: ImageSeed,
  ownImageUrl?: string | null,
  size = 600
): string {
  if (ownImageUrl && ownImageUrl.trim().length > 0) return ownImageUrl;

  const keywordSource = `${product.category ?? ""} ${product.name ?? ""} ${product.brand ?? ""}`;
  const pool = poolFor(keywordSource);
  const picked = pool[hash(product.id) % pool.length];
  return `${picked}?w=${size}&q=70&auto=format&fit=crop`;
}
