// Server-side API client for RSC (React Server Components).
// Calls BE directly via Docker network (or localhost in dev).

import { cookies } from "next/headers";
import { unwrapEnvelope } from "@/lib/api/envelope";

const BE_URL = process.env.API_BASE_URL || "http://localhost:8080";

export async function serverFetch<T>(
  path: string,
  init: RequestInit = {},
  options?: { cache?: RequestCache; revalidate?: number }
): Promise<T> {
  const c = await cookies();
  const headers = new Headers(init.headers);

  const accessToken = c.get("access_token")?.value;
  const guestSessionId = c.get("guest_session_id")?.value;

  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  if (guestSessionId) headers.set("X-Session-Id", guestSessionId);
  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }

  const url = `${BE_URL}/api${path}`;

  const res = await fetch(url, {
    ...init,
    headers,
    // Default: no cache for dynamic data. Use { revalidate: N } for ISR.
    next: options?.revalidate
      ? { revalidate: options.revalidate }
      : { revalidate: 0 },
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`API ${res.status}: ${text}`);
  }

  if (res.status === 204) return undefined as T;

  const json = await res.json();
  return unwrapEnvelope<T>(json);
}
