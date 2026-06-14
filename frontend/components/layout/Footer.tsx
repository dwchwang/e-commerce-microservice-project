import Link from "next/link";

const columns = [
  {
    heading: "Mua sắm",
    links: [
      { href: "/products", label: "Tất cả sản phẩm" },
      { href: "/flash-sales", label: "Flash Sale" },
      { href: "/search", label: "Tìm kiếm" },
    ],
  },
  {
    heading: "Công ty",
    links: [
      { href: "/about", label: "Về chúng tôi" },
      { href: "/contact", label: "Liên hệ" },
    ],
  },
  {
    heading: "Tài khoản",
    links: [
      { href: "/profile", label: "Hồ sơ của tôi" },
      { href: "/orders", label: "Đơn hàng" },
      { href: "/addresses", label: "Sổ địa chỉ" },
      { href: "/cart", label: "Giỏ hàng" },
    ],
  },
  {
    heading: "Hỗ trợ",
    links: [
      { href: "/content/faq", label: "Câu hỏi thường gặp" },
      { href: "/content/terms", label: "Điều khoản dịch vụ" },
      { href: "/content/privacy", label: "Chính sách bảo mật" },
      { href: "/content/shipping", label: "Vận chuyển & đổi trả" },
    ],
  },
];

export function Footer() {
  return (
    <footer className="mt-auto bg-secondary text-foreground">
      <div className="container mx-auto px-4 py-14">
        <div className="grid grid-cols-2 gap-x-8 gap-y-10 md:grid-cols-5">
          {/* Brand */}
          <div className="col-span-2 md:col-span-1">
            <h3 className="text-lg font-semibold tracking-tight mb-3">
              <span className="text-primary">Tech</span>Store
            </h3>
            <p className="text-[13px] leading-relaxed text-muted-foreground max-w-xs">
              Cửa hàng bán lẻ thiết bị điện tử chính hãng — laptop, điện thoại,
              phụ kiện. Giao hàng toàn quốc, bảo hành chính hãng.
            </p>
          </div>

          {columns.map((col) => (
            <div key={col.heading}>
              <h4 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground mb-4">
                {col.heading}
              </h4>
              <ul className="space-y-2.5">
                {col.links.map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-[13px] text-foreground/80 transition-colors hover:text-primary"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-12 border-t border-border pt-6 text-[12px] text-muted-foreground">
          © {new Date().getFullYear()} TechStore. Đồ án tốt nghiệp — Hệ thống
          thương mại điện tử Microservices.
        </div>
      </div>
    </footer>
  );
}
