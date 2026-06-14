import Link from "next/link";
import { LogOut, Store } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { AdminSession } from "@/lib/auth/admin-session";

export function AdminTopbar({ user }: { user: AdminSession }) {
  return (
    <div className="flex h-14 items-center justify-between gap-4 px-5">
      <div>
        <p className="text-sm font-medium leading-none">{user.name}</p>
        <p className="mt-1 text-xs text-muted-foreground">{user.email || "ROLE_ADMIN"}</p>
      </div>

      <div className="flex items-center gap-2">
        <Link
          href="/"
          className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-border px-2.5 text-sm font-medium hover:bg-muted"
        >
          <Store className="size-4" />
          Quay lại cửa hàng
        </Link>
        <form action="/api/auth/logout" method="post">
          <Button type="submit" variant="ghost" size="sm">
            <LogOut className="size-4" />
            Đăng xuất
          </Button>
        </form>
      </div>
    </div>
  );
}
