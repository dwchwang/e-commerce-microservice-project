import { NextRequest, NextResponse } from "next/server";

// Single source of truth for protected route prefixes
// ⚠️ config.matcher below must match these prefixes (Next.js requires static values)
const PROTECTED_PREFIXES = ["/checkout", "/orders", "/profile", "/addresses", "/admin"];

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const [, payload] = token.split(".");
  if (!payload) return null;

  try {
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    return JSON.parse(atob(padded)) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function hasAdminRole(token: string): boolean {
  const payload = decodeJwtPayload(token);
  const realmAccess = payload?.realm_access as { roles?: string[] } | undefined;
  const resourceAccess = payload?.resource_access as Record<string, { roles?: string[] }> | undefined;
  const roles = new Set<string>();

  realmAccess?.roles?.forEach((role) => roles.add(normalizeRole(role)));
  Object.values(resourceAccess ?? {}).forEach((client) => {
    client.roles?.forEach((role) => roles.add(normalizeRole(role)));
  });

  return roles.has("ROLE_ADMIN");
}

function normalizeRole(role: string): string {
  const normalized = role.trim().toUpperCase();
  return normalized.startsWith("ROLE_") ? normalized : `ROLE_${normalized}`;
}

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

  if (path.startsWith("/admin") && !hasAdminRole(token)) {
    return NextResponse.redirect(new URL("/?error=forbidden", req.url));
  }

  return NextResponse.next();
}

// matcher must be static — keep in sync with PROTECTED_PREFIXES above
export const config = {
  matcher: ["/checkout/:path*", "/orders/:path*", "/profile/:path*", "/addresses/:path*", "/admin/:path*"],
};
