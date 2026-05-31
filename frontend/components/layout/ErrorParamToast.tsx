"use client";

import { useEffect, useRef } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";

const ERROR_MESSAGES: Record<string, string> = {
  forbidden: "Bạn không có quyền truy cập trang quản trị.",
  session_expired: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
};

/**
 * Surfaces `?error=<code>` flags (e.g. from the admin guard redirect) as a toast,
 * then strips the param so it does not re-fire on refresh.
 */
export function ErrorParamToast() {
  const params = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;
    const code = params.get("error");
    if (!code) return;

    handled.current = true;
    toast.error(ERROR_MESSAGES[code] ?? "Đã xảy ra lỗi.");

    const next = new URLSearchParams(params.toString());
    next.delete("error");
    const qs = next.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }, [params, pathname, router]);

  return null;
}
