"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";
import { getOrCreateGuestSessionId } from "@/lib/cart/guest-session";
import type { CartResponse } from "@/lib/api/types";

export function useCart() {
  return useQuery({
    queryKey: qk.cart,
    queryFn: async () => {
      getOrCreateGuestSessionId();
      try {
        return await apiFetch<CartResponse>("/cart");
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
    mutationFn: ({ itemId, quantity }: { itemId: string; quantity: number }) =>
      apiFetch(`/cart/items/${itemId}`, {
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
    mutationFn: (itemId: string) =>
      apiFetch(`/cart/items/${itemId}`, { method: "DELETE" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.cart });
    },
  });
}
