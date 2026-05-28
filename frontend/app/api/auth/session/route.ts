import { cookies } from "next/headers";
import { NextResponse } from "next/server";

export async function GET() {
  const cookieStore = await cookies();
  const token = cookieStore.get("access_token")?.value;

  if (!token) {
    return NextResponse.json({ user: null });
  }

  try {
    // Decode JWT payload without verification (for UI display only)
    const payload = JSON.parse(
      Buffer.from(token.split(".")[1], "base64url").toString()
    );
    return NextResponse.json({
      user: {
        id: payload.sub,
        email: payload.email,
        name: payload.name || payload.preferred_username,
        roles: payload.realm_access?.roles ?? [],
      },
    });
  } catch {
    return NextResponse.json({ user: null });
  }
}
