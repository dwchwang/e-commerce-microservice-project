import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

const schema = z.object({
  username: z.string().min(1),
  email: z.string().email(),
  password: z.string().min(6),
  firstName: z.string().optional(),
  lastName: z.string().optional(),
});

const BE_URL = process.env.API_BASE_URL || "http://localhost:8080";

export async function POST(req: NextRequest) {
  try {
    const body = schema.parse(await req.json());
    const res = await fetch(`${BE_URL}/api/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: (err as Record<string, unknown>).message || "Registration failed" },
        { status: res.status }
      );
    }

    return NextResponse.json({ ok: true });
  } catch (err) {
    if (err instanceof z.ZodError) {
      return NextResponse.json({ error: err.issues[0]?.message || "Validation error" }, { status: 400 });
    }
    return NextResponse.json({ error: "Internal server error" }, { status: 500 });
  }
}
