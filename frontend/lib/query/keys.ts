// TanStack Query key factory — ensures consistent cache keys.

export const qk = {
  // Products
  products: {
    all: ["products"] as const,
    list: (filters: Record<string, unknown>) => ["products", "list", filters] as const,
    detail: (slug: string) => ["products", "detail", slug] as const,
    facets: (filters: Record<string, unknown>) => ["products", "facets", filters] as const,
  },

  // Cart
  cart: ["cart"] as const,

  // Orders
  orders: {
    all: ["orders"] as const,
    detail: (id: string) => ["orders", id] as const,
  },

  // Addresses
  addresses: ["addresses"] as const,

  // Flash Sales
  flashSales: {
    active: ["flash-sales", "active"] as const,
    detail: (id: string) => ["flash-sales", id] as const,
  },

  // Reviews
  reviews: {
    byProduct: (productId: string) => ["reviews", "product", productId] as const,
  },

  // Search
  search: (query: string) => ["search", query] as const,

  // Session
  session: ["session"] as const,

  // Content
  content: (slug: string) => ["content", slug] as const,
} as const;
