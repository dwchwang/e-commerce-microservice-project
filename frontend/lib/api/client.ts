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

let isRefreshing = false;
let refreshPromise: Promise<boolean> | null = null;

async function refreshToken(): Promise<boolean> {
  if (isRefreshing) return refreshPromise!;
  isRefreshing = true;
  refreshPromise = fetch("/api/auth/refresh", { method: "POST" })
    .then((res) => res.ok)
    .catch(() => false)
    .finally(() => {
      isRefreshing = false;
      refreshPromise = null;
    });
  return refreshPromise;
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

  const doFetch = () =>
    fetch(url, {
      ...init,
      headers,
      credentials: "include",
    });

  let res = await doFetch();

  // Auto-refresh on 401
  if (res.status === 401 && !path.startsWith("/api/auth")) {
    const refreshed = await refreshToken();
    if (refreshed) {
      res = await doFetch();
    }
  }

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
