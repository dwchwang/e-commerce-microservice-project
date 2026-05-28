// API endpoint constants.
// All paths are relative to API base (without /api prefix — added by client).

export const endpoints = {
  // Auth
  auth: {
    login: "/auth/login",
    register: "/auth/register",
    refresh: "/auth/refresh",
    logout: "/auth/logout",
  },

  // Products
  products: {
    list: "/products",
    detail: (slug: string) => `/products/${slug}`,
    facets: "/products/facets",
  },

  // Search
  search: "/search",

  // Cart
  cart: {
    get: "/cart",
    addItem: "/cart/items",
    updateItem: (itemId: string) => `/cart/items/${itemId}`,
    removeItem: (itemId: string) => `/cart/items/${itemId}`,
    applyVoucher: "/cart/voucher",
  },

  // Orders
  orders: {
    list: "/orders",
    create: "/orders",
    detail: (id: string) => `/orders/${id}`,
  },

  // Addresses
  addresses: {
    list: "/users/addresses",
    create: "/users/addresses",
    update: (id: string) => `/users/addresses/${id}`,
    delete: (id: string) => `/users/addresses/${id}`,
    setDefault: (id: string) => `/users/addresses/${id}/default`,
  },

  // Reviews
  reviews: {
    byProduct: (productId: string) => `/reviews/product/${productId}`,
    create: "/reviews",
  },

  // Flash Sales
  flashSales: {
    active: "/flash-sales/active",
    detail: (id: string) => `/flash-sales/${id}`,
    purchase: (id: string) => `/flash-sales/${id}/purchase`,
  },

  // Content
  content: {
    bySlug: (slug: string) => `/content/${slug}`,
  },

  // Users
  users: {
    profile: "/users/profile",
  },
} as const;
