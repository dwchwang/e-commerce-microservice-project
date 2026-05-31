export type ProductAdmin = {
  id: string;
  sku: string;
  name: string;
  description?: string;
  price: number | string;
  categoryId?: string;
  categoryName?: string;
  brandId?: string;
  brandName?: string;
  isActive?: boolean;
  primaryImageUrl?: string;
  imageUrls?: string[];
  specs?: { specName?: string; specValue?: string }[];
  createdAt?: string;
  updatedAt?: string;
};

export type InventoryAdmin = {
  id?: string;
  sku: string;
  productName?: string;
  quantity: number;
  reservedQuantity?: number;
  availableQuantity?: number;
  updatedAt?: string;
};

export type OrderAdmin = {
  id: string;
  userId?: string;
  userEmail?: string;
  status: string;
  paymentMethod?: string;
  subtotal?: number | string;
  discountAmount?: number | string;
  totalAmount: number | string;
  voucherCode?: string;
  shippingName?: string;
  shippingPhone?: string;
  shippingAddress?: string;
  cancelReason?: string;
  isFlashSale?: boolean;
  createdAt?: string;
  updatedAt?: string;
  items?: {
    id?: string;
    productId?: string;
    sku?: string;
    productName: string;
    price: number | string;
    quantity: number;
    subtotal: number | string;
  }[];
};

export type UserAdmin = {
  id: string;
  keycloakUserId?: string;
  email: string;
  fullName?: string;
  phoneNumber?: string;
  avatarUrl?: string;
  loyaltyPoints?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type VoucherAdmin = {
  id: string;
  code: string;
  discountType: "PERCENTAGE" | "FIXED_AMOUNT" | string;
  discountValue: number | string;
  minOrderValue?: number | string;
  maxDiscount?: number | string;
  usageLimit?: number;
  usedCount?: number;
  startDate: string;
  endDate: string;
  isActive?: boolean;
};

export type FlashSaleAdmin = {
  id: string;
  productId: string;
  sku: string;
  productName: string;
  originalPrice: number | string;
  salePrice: number | string;
  quantity: number;
  soldCount?: number;
  remainingStock?: number;
  startTime: string;
  endTime: string;
  status: "SCHEDULED" | "UPCOMING" | "ACTIVE" | "ENDED" | "CANCELLED" | string;
};

export type BannerAdmin = {
  id: string;
  title: string;
  imageUrl: string;
  linkUrl?: string;
  displayOrder?: number;
  isActive?: boolean;
  startDate?: string;
  endDate?: string;
};

export type ContentAdmin = {
  id: string;
  title: string;
  slug: string;
  content: string;
  thumbnailUrl?: string;
  author?: string;
  isPublished?: boolean;
  publishedAt?: string;
};

export type ReviewAdmin = {
  id: string;
  userId: string;
  productId: string;
  rating: number;
  comment?: string;
  status?: "APPROVED" | "PENDING" | "REJECTED" | string;
  createdAt?: string;
  updatedAt?: string;
};
