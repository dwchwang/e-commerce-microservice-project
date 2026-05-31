// Shared API types — aligned with the actual backend DTOs.

// --- Backend raw shapes (as returned by services, inside ApiResponse.data) ---

/** Spring Data Page<T> shape returned by paginated endpoints. */
export interface SpringPage<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first?: boolean;
  last?: boolean;
}

export interface ProductSpec {
  specName: string;
  specValue: string;
}

/** product-service ProductResponse (detail). */
export interface ProductResponse {
  id: string;
  sku: string;
  name: string;
  description?: string;
  price: number;
  categoryId?: string;
  categoryName?: string;
  brandId?: string;
  brandName?: string;
  isActive?: boolean;
  imageUrls?: string[];
  specs?: ProductSpec[];
  createdAt?: string;
  updatedAt?: string;
}

/** product-service ProductSummaryResponse (list items). */
export interface ProductSummaryResponse {
  id: string;
  sku: string;
  name: string;
  price: number;
  categoryId?: string;
  categoryName?: string;
  brandId?: string;
  brandName?: string;
  primaryImageUrl?: string;
}

// --- Normalized UI types (what components consume) ---

export interface Product {
  id: string;
  sku?: string;
  name: string;
  slug: string;
  description?: string;
  price: number;
  originalPrice?: number;
  brand?: string;
  category?: string;
  imageUrl?: string;
  images?: string[];
  specs?: Record<string, string>;
  rating?: number;
  reviewCount?: number;
}

export interface CartItem {
  /** Cart items are keyed by productId in the backend. */
  id: string;
  productId: string;
  productName: string;
  productImage?: string;
  price: number;
  quantity: number;
  subtotal: number;
}

export interface Cart {
  items: CartItem[];
  totalPrice: number;
  totalItems: number;
}

export interface OrderItem {
  id?: string;
  productId?: string;
  sku?: string;
  productName: string;
  price: number;
  quantity: number;
  subtotal: number;
}

export type OrderStatus = "PENDING" | "STOCK_RESERVED" | "CONFIRMED" | "CANCELLED";

export interface Order {
  id: string;
  userId?: string;
  userEmail?: string;
  status: OrderStatus | string;
  paymentMethod: "COD" | "VNPAY" | string;
  subtotal?: number;
  discountAmount?: number;
  totalAmount: number;
  voucherCode?: string;
  shippingName?: string;
  shippingPhone?: string;
  shippingAddress?: string;
  cancelReason?: string;
  isFlashSale?: boolean;
  createdAt: string;
  updatedAt?: string;
  items: OrderItem[];
}

/** user-service AddressResponse / AddressRequest shape. */
export interface Address {
  id?: string;
  recipientName: string;
  phoneNumber: string;
  addressLine: string;
  ward?: string;
  district?: string;
  city: string;
  defaultAddress?: boolean;
}

export interface Review {
  id: string;
  productId: string;
  userId: string;
  rating: number;
  comment?: string;
  createdAt: string;
}

export interface ProductRating {
  productId: string;
  averageRating?: number;
  reviewCount?: number;
}

/** flash-sale-service CampaignResponse. */
export interface FlashSale {
  id: string;
  productId: string;
  sku?: string;
  productName: string;
  originalPrice?: number;
  salePrice: number;
  quantity: number;
  soldCount?: number;
  remainingStock?: number;
  startTime: string;
  endTime: string;
  status: "SCHEDULED" | "ACTIVE" | "ENDED" | "CANCELLED" | string;
}

/** flash-sale-service PurchaseResponse. */
export interface FlashSalePurchaseResult {
  success: boolean;
  message?: string;
  campaignId?: string;
  remainingStock?: number;
}

export interface UserSession {
  user: {
    id: string;
    email: string;
    roles: string[];
    name?: string;
  } | null;
}
