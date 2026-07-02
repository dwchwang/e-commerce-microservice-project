"use client";

import { useRouter } from "next/navigation";
import { User, LogOut, Package, MapPin, ShoppingBag, LayoutDashboard, ChevronDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useSession, useLogout } from "@/lib/auth/hooks";

export function UserMenu() {
  const router = useRouter();
  const { data: session, isLoading } = useSession();
  const logout = useLogout();

  if (!session?.user) {
    return (
      <Button
        variant="ghost"
        size="sm"
        className="gap-1.5"
        onClick={() => router.push("/login")}
        disabled={isLoading}
      >
        <User className="h-5 w-5" />
        <span className="hidden sm:inline">Đăng nhập</span>
      </Button>
    );
  }

  const displayName = session.user.name?.trim() || session.user.email || "Tài khoản";
  const initial = displayName.charAt(0).toUpperCase();
  const isAdmin = session.user.roles?.includes("ROLE_ADMIN");

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="inline-flex h-9 items-center gap-2 rounded-full border border-border/70 py-0 pl-1 pr-2.5 text-foreground transition-colors hover:bg-muted">
        <span className="flex size-7 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
          {initial}
        </span>
        <span className="hidden max-w-[120px] truncate text-sm font-medium sm:block">
          {displayName}
        </span>
        <ChevronDown className="hidden size-4 text-muted-foreground sm:block" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-60">
        <div className="px-2 py-1.5">
          <p className="text-sm font-medium leading-tight">Chào, {displayName}</p>
          {session.user.email && (
            <p className="mt-0.5 truncate text-xs text-muted-foreground">{session.user.email}</p>
          )}
        </div>
        <DropdownMenuSeparator />
        {isAdmin && (
          <>
            <DropdownMenuItem onClick={() => router.push("/admin/dashboard")} className="cursor-pointer text-primary">
              <LayoutDashboard className="mr-2 h-4 w-4" />
              Trang quản trị
            </DropdownMenuItem>
            <DropdownMenuSeparator />
          </>
        )}
        <DropdownMenuItem onClick={() => router.push("/orders")} className="cursor-pointer">
          <Package className="mr-2 h-4 w-4" />
          Đơn hàng
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => router.push("/cart")} className="cursor-pointer">
          <ShoppingBag className="mr-2 h-4 w-4" />
          Giỏ hàng
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => router.push("/addresses")} className="cursor-pointer">
          <MapPin className="mr-2 h-4 w-4" />
          Địa chỉ
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => router.push("/profile")} className="cursor-pointer">
          <User className="mr-2 h-4 w-4" />
          Tài khoản
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={logout} className="cursor-pointer text-destructive">
          <LogOut className="mr-2 h-4 w-4" />
          Đăng xuất
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
