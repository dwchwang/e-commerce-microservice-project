"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BarChart3,
  FileText,
  Images,
  Package,
  ShoppingCart,
  Star,
  Tags,
  TicketPercent,
  Users,
  Warehouse,
  Zap,
} from "lucide-react";
import { cn } from "@/lib/utils";

const ITEMS = [
  { href: "/admin/dashboard", label: "Dashboard", icon: BarChart3 },
  { href: "/admin/products", label: "Sản phẩm", icon: Package },
  { href: "/admin/inventory", label: "Kho", icon: Warehouse },
  { href: "/admin/orders", label: "Đơn hàng", icon: ShoppingCart },
  { href: "/admin/users", label: "Khách hàng", icon: Users },
  { href: "/admin/vouchers", label: "Voucher", icon: TicketPercent },
  { href: "/admin/flash-sales", label: "Flash sale", icon: Zap },
  { href: "/admin/content/banners", label: "Banner", icon: Images },
  { href: "/admin/content/pages", label: "Nội dung", icon: FileText },
  { href: "/admin/reviews", label: "Đánh giá", icon: Star },
];

export function AdminSidebar() {
  const pathname = usePathname();

  return (
    <nav className="flex h-full flex-col bg-zinc-950 text-zinc-100">
      <Link href="/admin/dashboard" className="flex h-14 items-center gap-2 border-b border-white/10 px-4">
        <Tags className="size-5" />
        <span className="text-sm font-semibold tracking-wide">TechStore Admin</span>
      </Link>

      <div className="flex-1 space-y-1 overflow-y-auto p-3">
        {ITEMS.map((item) => {
          const Icon = item.icon;
          const active = pathname === item.href || pathname.startsWith(`${item.href}/`);

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex h-9 items-center gap-3 rounded-md px-3 text-sm text-zinc-300 transition-colors hover:bg-white/10 hover:text-white",
                active && "bg-white text-zinc-950 hover:bg-white hover:text-zinc-950"
              )}
            >
              <Icon className="size-4" />
              <span className="truncate">{item.label}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
