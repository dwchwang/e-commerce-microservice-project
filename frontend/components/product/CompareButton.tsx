"use client";

import { Button } from "@/components/ui/button";
import { BarChart3 } from "lucide-react";
import { useCompare } from "@/lib/compare/store";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

export function CompareButton({
  productId,
  className,
}: {
  productId: string;
  className?: string;
}) {
  const { productIds, add, remove, isInList } = useCompare();
  const inList = isInList(productId);

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (inList) {
      remove(productId);
    } else if (productIds.length >= 4) {
      toast.error("Chỉ so sánh tối đa 4 sản phẩm");
    } else {
      add(productId);
      toast.success("Đã thêm vào danh sách so sánh");
    }
  };

  return (
    <Button
      variant={inList ? "secondary" : "outline"}
      size="icon"
      className={cn("h-8 w-8 shrink-0", className)}
      onClick={handleClick}
      title={inList ? "Bỏ so sánh" : "So sánh"}
    >
      <BarChart3 className="h-4 w-4" />
    </Button>
  );
}
