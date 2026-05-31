"use client";

import { useActionState, useEffect, type ReactNode } from "react";
import { AlertCircle } from "lucide-react";
import { toast } from "sonner";
import type { FormState } from "@/lib/admin/form-state";

type AdminFormProps = {
  action: (state: FormState, formData: FormData) => Promise<FormState>;
  children: ReactNode;
  className?: string;
  /** Toast shown when the action resolves with { success: true } (no redirect). */
  successMessage?: string;
};

/**
 * Client wrapper around a Server Action that surfaces validation/business errors
 * inline and shows a pending state via the nested <SubmitButton>. Successful
 * actions usually redirect; if they instead return { success: true } we toast.
 */
export function AdminForm({ action, children, className, successMessage }: AdminFormProps) {
  const [state, formAction] = useActionState<FormState, FormData>(action, null);

  useEffect(() => {
    if (state?.error) {
      toast.error(state.error);
    } else if (state?.success && successMessage) {
      toast.success(successMessage);
    }
  }, [state, successMessage]);

  return (
    <form action={formAction} className={className}>
      {state?.error && (
        <div className="mb-4 flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {children}
    </form>
  );
}
