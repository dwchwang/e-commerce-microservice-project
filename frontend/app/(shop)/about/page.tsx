import type { Metadata } from "next";
import Link from "next/link";
import Image from "next/image";
import { HERO_IMAGES } from "@/lib/product-image";
import { Truck, ShieldCheck, RotateCcw, Headphones, ArrowRight } from "lucide-react";

export const metadata: Metadata = {
  title: "Giới thiệu — TechStore",
  description: "TechStore — cửa hàng thiết bị điện tử chính hãng, xây dựng trên kiến trúc microservices.",
};

const stats = [
  { value: "13+", label: "Microservice" },
  { value: "10K+", label: "Sản phẩm" },
  { value: "99.9%", label: "Thời gian hoạt động" },
  { value: "24/7", label: "Hỗ trợ khách hàng" },
];

const values = [
  { icon: ShieldCheck, title: "Chính hãng 100%", desc: "Cam kết sản phẩm chính hãng, đầy đủ hóa đơn và bảo hành nhà sản xuất." },
  { icon: Truck, title: "Giao hàng toàn quốc", desc: "Vận chuyển nhanh chóng, miễn phí cho đơn hàng từ 5 triệu đồng." },
  { icon: RotateCcw, title: "Đổi trả linh hoạt", desc: "Đổi trả trong 7 ngày nếu sản phẩm gặp lỗi từ nhà sản xuất." },
  { icon: Headphones, title: "Hỗ trợ tận tâm", desc: "Đội ngũ tư vấn đồng hành cùng bạn trước và sau khi mua hàng." },
];

export default function AboutPage() {
  return (
    <div>
      {/* Hero */}
      <section className="bg-secondary">
        <div className="container mx-auto px-4 py-20 text-center md:py-28">
          <p className="mb-3 text-sm font-medium uppercase tracking-widest text-primary">Về chúng tôi</p>
          <h1 className="headline-tight mx-auto max-w-3xl text-4xl md:text-6xl">
            Công nghệ chính hãng,<br />trải nghiệm trọn vẹn.
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-lg font-light text-muted-foreground">
            TechStore là cửa hàng bán lẻ thiết bị điện tử được xây dựng trên nền tảng
            microservices hiện đại — mang đến trải nghiệm mua sắm nhanh, ổn định và đáng tin cậy.
          </p>
        </div>
      </section>

      {/* Stats */}
      <section className="border-b border-border bg-white">
        <div className="container mx-auto grid grid-cols-2 gap-6 px-4 py-12 md:grid-cols-4">
          {stats.map((s) => (
            <div key={s.label} className="text-center">
              <p className="text-3xl font-semibold tracking-tight text-primary md:text-4xl">{s.value}</p>
              <p className="mt-1 text-sm text-muted-foreground">{s.label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Story */}
      <section className="container mx-auto grid items-center gap-10 px-4 py-20 md:grid-cols-2">
        <div>
          <h2 className="headline-tight text-3xl md:text-4xl">Câu chuyện của chúng tôi</h2>
          <div className="mt-5 space-y-4 text-[17px] leading-relaxed text-muted-foreground">
            <p>
              TechStore ra đời với mong muốn đưa các thiết bị công nghệ chính hãng đến gần hơn
              với người dùng Việt Nam, đi kèm dịch vụ minh bạch và giá hợp lý.
            </p>
            <p>
              Hệ thống được phát triển theo kiến trúc microservices với Spring Boot, Spring Cloud,
              Kafka và Elasticsearch — đảm bảo khả năng mở rộng, chịu tải cao và vận hành liên tục.
            </p>
          </div>
          <Link
            href="/products"
            className="mt-6 inline-flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-[15px] font-medium text-primary-foreground transition active:scale-95"
          >
            Khám phá sản phẩm <ArrowRight className="size-4" />
          </Link>
        </div>
        <div className="relative aspect-[4/3] overflow-hidden rounded-3xl bg-secondary">
          <Image
            src={`${HERO_IMAGES.workspace}?w=900&q=75&auto=format&fit=crop`}
            alt="Không gian làm việc công nghệ"
            fill
            className="object-cover"
            sizes="(max-width: 768px) 100vw, 50vw"
          />
        </div>
      </section>

      {/* Values */}
      <section className="bg-secondary">
        <div className="container mx-auto px-4 py-20">
          <h2 className="headline-tight mb-10 text-center text-3xl md:text-4xl">Giá trị cốt lõi</h2>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {values.map((v) => (
              <div key={v.title} className="rounded-2xl bg-card p-6 text-center">
                <div className="mx-auto mb-4 flex size-12 items-center justify-center rounded-full bg-primary/10">
                  <v.icon className="size-6 text-primary" />
                </div>
                <h3 className="text-lg font-semibold tracking-tight">{v.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground">{v.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
