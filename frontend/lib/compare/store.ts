// Zustand store for product comparison — persists in localStorage.

import { create } from "zustand";
import { persist } from "zustand/middleware";

interface CompareState {
  productIds: string[];
  add: (id: string) => void;
  remove: (id: string) => void;
  clear: () => void;
  isInList: (id: string) => boolean;
}

export const useCompare = create<CompareState>()(
  persist(
    (set, get) => ({
      productIds: [],
      add: (id) => {
        const { productIds } = get();
        if (productIds.length >= 4 || productIds.includes(id)) return;
        set({ productIds: [...productIds, id] });
      },
      remove: (id) =>
        set({ productIds: get().productIds.filter((p) => p !== id) }),
      clear: () => set({ productIds: [] }),
      isInList: (id) => get().productIds.includes(id),
    }),
    { name: "compare-store" }
  )
);
