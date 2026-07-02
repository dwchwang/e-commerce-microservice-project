"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { ArrowUpDown } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const SORT_OPTIONS = [
  { value: "featured", label: "Nổi bật" },
  { value: "price,asc", label: "Giá: Thấp → Cao" },
  { value: "price,desc", label: "Giá: Cao → Thấp" },
  { value: "name,asc", label: "Tên: A → Z" },
  { value: "createdAt,desc", label: "Mới nhất" },
] as const;

export function ProductSort() {
  const router = useRouter();
  const params = useSearchParams();
  const current = params.get("sort") ?? "featured";

  const onChange = (value: string | null) => {
    const next = value ?? "featured";
    // Guard: base-ui Select can emit the current value on mount/sync — never
    // navigate when nothing actually changed, otherwise we loop.
    if (next === current) return;
    const sp = new URLSearchParams(params);
    if (next === "featured") sp.delete("sort");
    else sp.set("sort", next);
    sp.delete("page");
    router.push(`/products?${sp.toString()}`);
  };

  return (
    <div className="flex items-center gap-2">
      <ArrowUpDown className="size-4 text-muted-foreground" />
      <Select defaultValue={current} onValueChange={onChange}>
        <SelectTrigger className="h-9 min-w-[168px]">
          <SelectValue>
            {(value: string) => SORT_OPTIONS.find((o) => o.value === value)?.label ?? "Nổi bật"}
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          {SORT_OPTIONS.map((o) => (
            <SelectItem key={o.value} value={o.value}>
              {o.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
