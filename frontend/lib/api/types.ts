// Shared API types used across the frontend.

export interface Product {
  id: string;
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
  stock?: number;
  rating?: number;
  reviewCount?: number;
  createdAt?: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CartItem {
  id: string;
  productId: string;
  productName: string;
  productImage?: string;
  price: number;
  quantity: number;
  subtotal: number;
}

export interface CartResponse {
  id: string;
  items: CartItem[];
  totalAmount: number;
  discountAmount?: number;
  voucherCode?: string;
}

export interface OrderItem {
  productId: string;
  productName: string;
  productImage?: string;
  price: number;
  quantity: number;
  subtotal: number;
}

export interface Order {
  id: string;
  orderNumber?: string;
  items: OrderItem[];
  totalAmount: number;
  discountAmount?: number;
  status: OrderStatus;
  paymentMethod: "COD" | "VNPAY";
  paymentStatus?: string;
  paymentUrl?: string;
  address?: Address;
  voucherCode?: string;
  createdAt: string;
  updatedAt?: string;
}

export type OrderStatus =
  | "CREATED"
  | "STOCK_RESERVED"
  | "CONFIRMED"
  | "SHIPPED"
  | "COMPLETED"
  | "CANCELLED"
  | "FAILED";

export interface Address {
  id?: string;
  fullName: string;
  phone: string;
  addressLine: string;
  city: string;
  district?: string;
  ward?: string;
  isDefault?: boolean;
}

export interface Review {
  id: string;
  productId: string;
  userId: string;
  userName?: string;
  rating: number;
  title: string;
  content: string;
  createdAt: string;
}

export interface FlashSale {
  id: string;
  name: string;
  productId: string;
  productName?: string;
  productImage?: string;
  flashSalePrice: number;
  originalPrice?: number;
  totalStock: number;
  soldCount: number;
  startAt: string;
  endAt: string;
  status: "UPCOMING" | "ACTIVE" | "ENDED";
}

export interface FlashSalePurchaseResult {
  success: boolean;
  orderId?: string;
  code: number; // 0=success, -1=sold out, -2=duplicate, -3=not started
  message?: string;
}

export interface Facets {
  brands?: { value: string; count: number }[];
  categories?: { value: string; count: number }[];
  priceRange?: { min: number; max: number };
}

export interface UserSession {
  user: {
    id: string;
    email: string;
    roles: string[];
    name?: string;
  } | null;
}
