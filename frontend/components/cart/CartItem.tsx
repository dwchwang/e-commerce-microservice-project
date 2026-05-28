"use client";

import { Button } from "@/components/ui/button";
import { PriceTag } from "@/components/shared/PriceTag";
import { Trash2, Plus, Minus } from "lucide-react";
import type { CartItem as CartItemType } from "@/lib/api/types";

interface CartItemProps {
  item: CartItemType;
  onUpdate: (itemId: string, quantity: number) => void;
  onRemove: (itemId: string) => void;
}

export function CartItem({ item, onUpdate, onRemove }: CartItemProps) {
  return (
    <div className="flex items-center gap-3 py-2">
      <div className="w-14 h-14 bg-muted rounded shrink-0 flex items-center justify-center text-xl">
        📦
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium truncate">{item.productName}</p>
        <PriceTag price={item.price} size="sm" />
      </div>
      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="icon"
          className="h-7 w-7"
          disabled={item.quantity <= 1}
          onClick={() => onUpdate(item.id, item.quantity - 1)}
        >
          <Minus className="h-3 w-3" />
        </Button>
        <span className="w-6 text-center text-xs">{item.quantity}</span>
        <Button
          variant="outline"
          size="icon"
          className="h-7 w-7"
          onClick={() => onUpdate(item.id, item.quantity + 1)}
        >
          <Plus className="h-3 w-3" />
        </Button>
      </div>
      <Button
        variant="ghost"
        size="icon"
        className="h-7 w-7 text-destructive"
        onClick={() => onRemove(item.id)}
      >
        <Trash2 className="h-3 w-3" />
      </Button>
    </div>
  );
}
