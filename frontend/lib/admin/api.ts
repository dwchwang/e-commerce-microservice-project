import { serverFetch } from "@/lib/api/server-client";

export type ApiEnvelope<T> = {
  success?: boolean;
  message?: string;
  data?: T;
  timestamp?: string;
};

export type PagePayload<T> = {
  content?: T[];
  data?: T[];
  page?: number;
  number?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
};

export type AdminPage<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

function isApiEnvelope<T>(obj: unknown): obj is ApiEnvelope<T> {
  return (
    typeof obj === "object" &&
    obj !== null &&
    "data" in obj &&
    ("success" in obj || "message" in obj || "timestamp" in obj)
  );
}

export async function adminFetch<T>(
  path: string,
  init?: RequestInit,
  options?: { revalidate?: number }
): Promise<T> {
  const response = await serverFetch<T | ApiEnvelope<T>>(path, init ?? {}, {
    revalidate: options?.revalidate ?? 0,
  });

  if (isApiEnvelope<T>(response)) {
    return response.data as T;
  }

  return response as T;
}

export async function adminFetchSafe<T>(path: string, fallback: T, options?: { revalidate?: number }): Promise<T> {
  try {
    return await adminFetch<T>(path, {}, options);
  } catch {
    return fallback;
  }
}

export function toAdminPage<T>(payload: PagePayload<T> | T[] | null | undefined, size = 20): AdminPage<T> {
  if (Array.isArray(payload)) {
    return {
      items: payload,
      page: 0,
      size,
      totalElements: payload.length,
      totalPages: payload.length > 0 ? 1 : 0,
    };
  }

  const items = payload?.content ?? payload?.data ?? [];
  return {
    items,
    page: payload?.number ?? payload?.page ?? 0,
    size: payload?.size ?? size,
    totalElements: payload?.totalElements ?? items.length,
    totalPages: payload?.totalPages ?? (items.length > 0 ? 1 : 0),
  };
}

export function formatCurrency(value: unknown): string {
  const amount = typeof value === "number" ? value : Number(value ?? 0);
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number.isFinite(amount) ? amount : 0);
}

export type MutateResult = { success: boolean; error?: string };

/**
 * Safe wrapper for admin mutations. Catches errors and returns a result
 * object so Server Actions can pass error messages to the UI.
 */
export async function adminMutate(
  path: string,
  init: RequestInit,
): Promise<MutateResult> {
  try {
    await adminFetch(path, init);
    return { success: true };
  } catch (e) {
    const errorMsg = e instanceof Error ? e.message : "Unknown error";
    console.error(`adminMutate ${init.method ?? "GET"} ${path}:`, errorMsg);
    return { success: false, error: errorMsg };
  }
}

export function formatDateTime(value: unknown): string {
  if (!value) return "-";
  const date = new Date(String(value));
  if (Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

export function toDateTimeLocal(value: unknown): string {
  if (!value) return "";
  const date = new Date(String(value));
  if (Number.isNaN(date.getTime())) return "";
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
}
