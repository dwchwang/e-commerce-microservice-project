import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

const schema = z.object({
  username: z.string().min(1, "Username is required"),
  password: z.string().min(1, "Password is required"),
});

const BE_URL = process.env.API_BASE_URL || "http://localhost:8080";

export async function POST(req: NextRequest) {
  try {
    const body = schema.parse(await req.json());

    const res = await fetch(`${BE_URL}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: (err as Record<string, unknown>).message || "Invalid credentials" },
        { status: res.status }
      );
    }

    const data = (await res.json()) as {
      accessToken?: string;
      refreshToken?: string;
      expiresIn?: number;
      token?: string; // some BE use "token"
    };

    const accessToken = data.accessToken || data.token;
    const refreshToken = data.refreshToken;

    if (!accessToken) {
      return NextResponse.json({ error: "No token in response" }, { status: 502 });
    }

    const r = NextResponse.json({ ok: true });

    // Set access token cookie (short-lived)
    r.cookies.set("access_token", accessToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: data.expiresIn ?? 900,
    });

    // Set refresh token cookie (long-lived)
    if (refreshToken) {
      r.cookies.set("refresh_token", refreshToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: "lax",
        path: "/",
        maxAge: 7 * 24 * 3600,
      });
    }

    // Merge guest cart to user on login
    const cookieStore = req.cookies;
    const guestId = cookieStore.get("guest_session_id")?.value;
    if (guestId) {
      try {
        await fetch(`${BE_URL}/api/cart/merge`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "X-Session-Id": guestId,
          },
        });
        // Only clear guest cookie if merge succeeded
        r.cookies.set("guest_session_id", "", { maxAge: 0, path: "/" });
      } catch {
        // merge is best-effort; keep guest cookie so cart isn't lost
      }
    }

    return r;
  } catch (err) {
    if (err instanceof z.ZodError) {
      return NextResponse.json({ error: err.issues[0]?.message || "Validation error" }, { status: 400 });
    }
    return NextResponse.json({ error: "Internal server error" }, { status: 500 });
  }
}
