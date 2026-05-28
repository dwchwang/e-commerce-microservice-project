import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const BE_URL = process.env.API_BASE_URL || "http://localhost:8080";

export async function POST(_req: NextRequest) {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get("refresh_token")?.value;

  if (!refreshToken) {
    return NextResponse.json({ error: "No refresh token" }, { status: 401 });
  }

  const res = await fetch(`${BE_URL}/api/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) {
    return NextResponse.json({ error: "Refresh failed" }, { status: 401 });
  }

  const data = (await res.json()) as {
    accessToken?: string;
    refreshToken?: string;
    expiresIn?: number;
    token?: string;
  };

  const accessToken = data.accessToken || data.token;
  if (!accessToken) {
    return NextResponse.json({ error: "No token in response" }, { status: 502 });
  }

  const r = NextResponse.json({ ok: true });
  r.cookies.set("access_token", accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: data.expiresIn ?? 900,
  });

  if (data.refreshToken) {
    r.cookies.set("refresh_token", data.refreshToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: 7 * 24 * 3600,
    });
  }

  return r;
}
