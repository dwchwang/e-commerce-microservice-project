// Shared state contract for admin Server Actions used with `useActionState`.
// `null` is the initial (idle) state. On failure return `{ error }`. On success
// the action either redirects (throws) or returns `{ success: true }`.
export type FormState = { error?: string; success?: boolean } | null;
