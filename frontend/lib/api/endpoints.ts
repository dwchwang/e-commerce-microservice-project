// API endpoint constants.
// All paths are relative to API base (without /api prefix — added by client/proxy).
// These mirror the real backend controllers.

export const endpoints = {
  // Auth (handled by FE BFF routes, not the proxy)
  auth: {
    login: "/auth/login",
    register: "/auth/register",
    refresh: "/auth/refresh",
    logout: "/auth/logout",
  },

  // Products
  products: {
    list: "/products",
    detail: (id: string) => `/products/${id}`,
    categories: "/products/categories",
    brands: "/products/brands",
  },

  // Search
  search: "/search",

  // Cart (items keyed by productId)
  cart: {
    get: "/cart",
    addItem: "/cart/items",
    updateItem: (productId: string) => `/cart/items/${productId}`,
    removeItem: (productId: string) => `/cart/items/${productId}`,
    clear: "/cart",
  },

  // Orders
  orders: {
    list: "/orders",
    create: "/orders",
    detail: (id: string) => `/orders/${id}`,
  },

  // Payments
  payments: {
    vnpayCreate: (orderId: string) => `/payments/vnpay/create?orderId=${orderId}`,
  },

  // Addresses
  addresses: {
    list: "/users/me/addresses",
    create: "/users/me/addresses",
    update: (id: string) => `/users/me/addresses/${id}`,
    delete: (id: string) => `/users/me/addresses/${id}`,
  },

  // Reviews
  reviews: {
    byProduct: (productId: string) => `/reviews/product/${productId}`,
    rating: (productId: string) => `/reviews/product/${productId}/rating`,
    create: "/reviews",
  },

  // Flash Sales
  flashSales: {
    list: "/flash-sales",
    detail: (id: string) => `/flash-sales/${id}`,
    purchase: (id: string) => `/flash-sales/${id}/purchase`,
  },

  // Content
  content: {
    bySlug: (slug: string) => `/content/posts/${slug}`,
  },

  // Users
  users: {
    profile: "/users/me",
  },
} as const;
