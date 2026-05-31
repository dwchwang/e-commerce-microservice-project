// Backend wraps every response in ApiResponse<T> = { success, message, data, timestamp }.
// This helper transparently unwraps that envelope so callers work with the inner payload.

type ApiEnvelope<T> = {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
};

function isApiEnvelope(value: unknown): value is ApiEnvelope<unknown> {
  if (typeof value !== "object" || value === null || Array.isArray(value)) return false;
  const obj = value as Record<string, unknown>;
  // The BE envelope always carries success + timestamp + a data key.
  return "success" in obj && "data" in obj && "timestamp" in obj;
}

/** Returns `response.data` when the value is an ApiResponse envelope, otherwise the value as-is. */
export function unwrapEnvelope<T>(value: unknown): T {
  if (isApiEnvelope(value)) {
    return value.data as T;
  }
  return value as T;
}
