// Browser-side API client — calls BFF proxy /api/proxy/*
// Token & session ID are handled by the proxy route handler automatically.

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: unknown
  ) {
    super(`API ${status}`);
    this.name = "ApiError";
  }
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {}
): Promise<T> {
  // Use BFF proxy so cookies (access_token, guest_session_id) are forwarded
  const baseUrl = "/api/proxy";
  const url = path.startsWith("http") ? path : `${baseUrl}${path}`;

  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }

  const res = await fetch(url, {
    ...init,
    headers,
    credentials: "include",
  });

  if (!res.ok) {
    let body: unknown = null;
    try {
      body = await res.json();
    } catch {
      // ignore
    }
    throw new ApiError(res.status, body);
  }

  // 204 No Content
  if (res.status === 204) return undefined as T;

  return res.json() as Promise<T>;
}
