import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

const schema = z.object({
  email: z.string().email("Email không hợp lệ"),
  password: z.string().min(1, "Vui lòng nhập mật khẩu"),
});

const BE_URL = process.env.API_BASE_URL || "http://localhost:8080";

type TokenPayload = {
  accessToken?: string;
  refreshToken?: string;
  expiresIn?: number;
  token?: string;
};

export async function POST(req: NextRequest) {
  try {
    const body = schema.parse(await req.json());

    const res = await fetch(`${BE_URL}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    const json = await res.json().catch(() => ({}));

    if (!res.ok) {
      return NextResponse.json(
        { error: (json as Record<string, unknown>).message || "Email hoặc mật khẩu không đúng" },
        { status: res.status }
      );
    }

    // BE wraps tokens in ApiResponse: { success, message, data: TokenResponse }
    const data = ((json as { data?: TokenPayload }).data ?? json) as TokenPayload;
    const accessToken = data.accessToken || data.token;
    const refreshToken = data.refreshToken;

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

    if (refreshToken) {
      r.cookies.set("refresh_token", refreshToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: "lax",
        path: "/",
        maxAge: 7 * 24 * 3600,
      });
    }

    // Merge guest cart into the user's cart on login (best-effort).
    const guestId = req.cookies.get("guest_session_id")?.value;
    if (guestId) {
      try {
        const mergeRes = await fetch(`${BE_URL}/api/cart/merge`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "X-Session-Id": guestId,
          },
        });
        if (mergeRes.ok) {
          r.cookies.set("guest_session_id", "", { maxAge: 0, path: "/" });
        }
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
