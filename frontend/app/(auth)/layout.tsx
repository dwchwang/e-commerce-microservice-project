import Link from "next/link";
import { ShieldCheck, Truck, Zap } from "lucide-react";

const perks = [
  { icon: ShieldCheck, text: "Sản phẩm chính hãng, bảo hành đầy đủ" },
  { icon: Truck, text: "Giao hàng nhanh toàn quốc" },
  { icon: Zap, text: "Flash Sale & ưu đãi mỗi ngày" },
];

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Brand panel */}
      <div className="relative hidden flex-col justify-between bg-foreground p-12 text-white lg:flex">
        <Link href="/" className="text-xl font-semibold tracking-tight">
          <span className="text-[var(--primary-on-dark)]">Tech</span>Store
        </Link>
        <div>
          <h2 className="headline-tight max-w-sm text-4xl text-white">
            Công nghệ chính hãng, trải nghiệm trọn vẹn.
          </h2>
          <ul className="mt-8 space-y-4">
            {perks.map((p) => (
              <li key={p.text} className="flex items-center gap-3 text-[var(--on-dark-muted)]">
                <p.icon className="size-5 text-[var(--primary-on-dark)]" />
                <span className="text-[15px]">{p.text}</span>
              </li>
            ))}
          </ul>
        </div>
        <p className="text-xs text-[var(--on-dark-muted)]">
          © {new Date().getFullYear()} TechStore. Đồ án tốt nghiệp.
        </p>
      </div>

      {/* Form area */}
      <div className="flex flex-col items-center justify-center bg-background px-4 py-12">
        <div className="w-full max-w-sm">
          <Link href="/" className="mb-8 block text-center text-lg font-semibold tracking-tight lg:hidden">
            <span className="text-primary">Tech</span>Store
          </Link>
          {children}
          <p className="mt-8 text-center text-xs text-muted-foreground">
            <Link href="/" className="hover:text-foreground">← Quay lại cửa hàng</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
