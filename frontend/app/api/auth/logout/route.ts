import { NextResponse } from "next/server";

export async function POST() {
  const r = NextResponse.json({ ok: true });
  r.cookies.set("access_token", "", { maxAge: 0, path: "/" });
  r.cookies.set("refresh_token", "", { maxAge: 0, path: "/" });
  return r;
}
