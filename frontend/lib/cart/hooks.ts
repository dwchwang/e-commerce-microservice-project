"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";
import { getOrCreateGuestSessionId } from "@/lib/cart/guest-session";
import type { Cart, CartItem } from "@/lib/api/types";

/** Raw cart item from cart-service: { productId, productName, price, quantity, addedAt }. */
type RawCartItem = {
  productId: string;
  productName: string;
  price: number;
  quantity: number;
};

type RawCart = {
  items?: RawCartItem[];
  totalPrice?: number;
  totalItems?: number;
};

function normalizeCart(raw: RawCart | null): Cart | null {
  if (!raw) return null;
  const items: CartItem[] = (raw.items ?? []).map((it) => ({
    id: it.productId,
    productId: it.productId,
    productName: it.productName,
    price: it.price,
    quantity: it.quantity,
    subtotal: it.price * it.quantity,
  }));
  return {
    items,
    totalPrice: raw.totalPrice ?? items.reduce((sum, it) => sum + it.subtotal, 0),
    totalItems: raw.totalItems ?? items.reduce((sum, it) => sum + it.quantity, 0),
  };
}

export function useCart() {
  return useQuery({
    queryKey: qk.cart,
    queryFn: async () => {
      getOrCreateGuestSessionId();
      try {
        const raw = await apiFetch<RawCart>("/cart");
        return normalizeCart(raw);
      } catch {
        return null;
      }
    },
    staleTime: 30 * 1000,
  });
}

export function useAddToCart() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (item: { productId: string; quantity: number }) => {
      getOrCreateGuestSessionId();
      return apiFetch("/cart/items", {
        method: "POST",
        body: JSON.stringify(item),
      });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.cart });
    },
  });
}

export function useUpdateCartItem() {
  const qc = useQueryClient();
  return useMutation({
    // Backend keys cart items by productId.
    mutationFn: ({ productId, quantity }: { productId: string; quantity: number }) =>
      apiFetch(`/cart/items/${productId}`, {
        method: "PUT",
        body: JSON.stringify({ quantity }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.cart });
    },
  });
}

export function useRemoveCartItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (productId: string) =>
      apiFetch(`/cart/items/${productId}`, { method: "DELETE" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.cart });
    },
  });
}

export function useClearCart() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch("/cart", { method: "DELETE" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.cart });
    },
  });
}
