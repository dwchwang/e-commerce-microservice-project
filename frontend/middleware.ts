import { NextRequest, NextResponse } from "next/server";

// Single source of truth for protected route prefixes
// ⚠️ config.matcher below must match these prefixes (Next.js requires static values)
const PROTECTED_PREFIXES = ["/checkout", "/orders", "/profile", "/addresses"];

export function middleware(req: NextRequest) {
  const path = req.nextUrl.pathname;
  const isProtected = PROTECTED_PREFIXES.some((p) => path.startsWith(p));

  if (!isProtected) return NextResponse.next();

  const token = req.cookies.get("access_token")?.value;
  if (!token) {
    const url = req.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("next", path);
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

// matcher must be static — keep in sync with PROTECTED_PREFIXES above
export const config = {
  matcher: ["/checkout/:path*", "/orders/:path*", "/profile/:path*", "/addresses/:path*"],
};
