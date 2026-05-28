// BFF Proxy — forwards browser requests to backend, attaching auth tokens.
// Browser never sees the access_token; it stays in httpOnly cookies.

import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const BE_URL = process.env.API_BASE_URL || "http://localhost:8080";

async function proxy(
  req: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path } = await params;
  const pathStr = path.join("/");
  const url = new URL(`${BE_URL}/api/${pathStr}`);
  url.search = req.nextUrl.search;

  const cookieStore = await cookies();
  const accessToken = cookieStore.get("access_token")?.value;
  const guestSessionId = cookieStore.get("guest_session_id")?.value;

  const headers = new Headers(req.headers);
  headers.delete("host");
  headers.delete("cookie");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  if (guestSessionId) headers.set("X-Session-Id", guestSessionId);

  const body =
    req.method !== "GET" && req.method !== "HEAD"
      ? await req.text().catch(() => undefined)
      : undefined;

  const res = await fetch(url.toString(), {
    method: req.method,
    headers,
    body,
    redirect: "manual",
  });

  const responseHeaders = new Headers(res.headers);
  responseHeaders.delete("transfer-encoding");

  return new NextResponse(res.body, {
    status: res.status,
    headers: responseHeaders,
  });
}

export { proxy as GET, proxy as POST, proxy as PUT, proxy as DELETE, proxy as PATCH };
