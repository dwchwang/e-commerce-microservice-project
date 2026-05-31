"use client";

import { useEffect, useRef } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";

const MESSAGES: Record<string, string> = {
  created: "Tạo mới thành công",
  updated: "Cập nhật thành công",
  deleted: "Đã xóa thành công",
};

/**
 * Reads a one-shot result flag from the URL (?created=true, ?updated=true, ...)
 * set by a Server Action redirect, shows a toast, then strips the param so the
 * toast does not re-fire on refresh/back navigation.
 */
export function ToastOnParam() {
  const params = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;

    for (const key of Object.keys(MESSAGES)) {
      if (params.get(key) === "true") {
        handled.current = true;
        toast.success(MESSAGES[key]);

        const next = new URLSearchParams(params.toString());
        next.delete(key);
        const qs = next.toString();
        router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
        break;
      }
    }
  }, [params, pathname, router]);

  return null;
}
