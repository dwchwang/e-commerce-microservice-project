import Link from "next/link";

export function Footer() {
  return (
    <footer className="border-t mt-auto bg-muted/30">
      <div className="container mx-auto px-4 py-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Brand */}
          <div>
            <h3 className="font-bold text-lg mb-3">
              <span className="text-primary">Tech</span>Store
            </h3>
            <p className="text-sm text-muted-foreground">
              Cửa hàng bán lẻ thiết bị điện tử chính hãng — Laptop, điện thoại, phụ kiện.
            </p>
          </div>

          {/* Links */}
          <div>
            <h4 className="font-semibold mb-3">Danh mục</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link href="/products" className="hover:text-primary transition-colors">Tất cả sản phẩm</Link></li>
              <li><Link href="/flash-sales" className="hover:text-primary transition-colors">Flash Sale</Link></li>
              <li><Link href="/search" className="hover:text-primary transition-colors">Tìm kiếm</Link></li>
            </ul>
          </div>

          {/* Support */}
          <div>
            <h4 className="font-semibold mb-3">Hỗ trợ</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link href="/content/faq" className="hover:text-primary transition-colors">FAQ</Link></li>
              <li><Link href="/content/terms" className="hover:text-primary transition-colors">Điều khoản</Link></li>
            </ul>
          </div>
        </div>
        <div className="border-t mt-8 pt-4 text-center text-sm text-muted-foreground">
          © {new Date().getFullYear()} TechStore. Đồ án tốt nghiệp — Microservice E-commerce.
        </div>
      </div>
    </footer>
  );
}
