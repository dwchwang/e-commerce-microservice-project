import { NextRequest, NextResponse } from "next/server";

// Protected paths — require login
const protectedPaths = ["/checkout", "/orders", "/profile", "/addresses"];

export function middleware(req: NextRequest) {
  const path = req.nextUrl.pathname;
  const isProtected = protectedPaths.some((p) => path.startsWith(p));

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

export const config = {
  matcher: ["/checkout/:path*", "/orders/:path*", "/profile/:path*", "/addresses/:path*"],
};
