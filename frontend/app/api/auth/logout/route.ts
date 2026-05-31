import { cookies } from "next/headers";
import { NextResponse } from "next/server";

const BE_URL = process.env.API_BASE_URL || "http://localhost:8080";

export async function POST() {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get("refresh_token")?.value;

  // Best-effort: revoke the Keycloak session on the backend.
  if (refreshToken) {
    try {
      await fetch(`${BE_URL}/api/auth/logout`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
    } catch {
      // ignore — we still clear local cookies below
    }
  }

  const r = NextResponse.json({ ok: true });
  r.cookies.set("access_token", "", { maxAge: 0, path: "/" });
  r.cookies.set("refresh_token", "", { maxAge: 0, path: "/" });
  return r;
}
