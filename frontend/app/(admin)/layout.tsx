import type { ReactNode } from "react";
import { Suspense } from "react";
import { redirect } from "next/navigation";
import { AdminSidebar } from "@/components/admin/AdminSidebar";
import { AdminTopbar } from "@/components/admin/AdminTopbar";
import { ToastOnParam } from "@/components/admin/ToastOnParam";
import { getAdminSession } from "@/lib/auth/admin-session";

export default async function AdminLayout({ children }: { children: ReactNode }) {
  const session = await getAdminSession();
  if (!session) redirect("/login?next=/admin/dashboard");
  if (!session.roles.includes("ROLE_ADMIN")) redirect("/?error=forbidden");

  return (
    <div className="grid min-h-screen grid-cols-1 bg-muted/20 md:grid-cols-[240px_minmax(0,1fr)]">
      <aside className="hidden min-h-screen md:block">
        <AdminSidebar />
      </aside>
      <div className="min-w-0">
        <header className="sticky top-0 z-30 border-b bg-background/95 backdrop-blur">
          <AdminTopbar user={session} />
        </header>
        <main className="mx-auto w-full max-w-[1440px] p-4 md:p-6">{children}</main>
      </div>
      <Suspense fallback={null}>
        <ToastOnParam />
      </Suspense>
    </div>
  );
}
