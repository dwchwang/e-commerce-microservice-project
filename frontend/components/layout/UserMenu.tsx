"use client";

import { useRouter } from "next/navigation";
import { User, LogOut, Package, MapPin, ShoppingBag } from "lucide-react";
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
  const { data: session } = useSession();
  const logout = useLogout();

  if (!session?.user) {
    return (
      <Button variant="ghost" size="icon" onClick={() => router.push("/login")}>
        <User className="h-5 w-5" />
      </Button>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger>
        <Button variant="ghost" size="icon" type="button">
          <User className="h-5 w-5" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <div className="px-2 py-1.5 text-sm font-medium truncate">
          {session.user.email}
        </div>
        <DropdownMenuSeparator />
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
