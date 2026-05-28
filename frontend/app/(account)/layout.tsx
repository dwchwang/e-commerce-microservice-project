import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";

export default function AccountLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <Header />
      <main className="flex-1 container mx-auto px-4 py-8">{children}</main>
      <Footer />
    </>
  );
}
