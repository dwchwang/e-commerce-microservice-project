"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";
import type { UserSession } from "@/lib/api/types";

export function useSession() {
  return useQuery({
    queryKey: qk.session,
    queryFn: () => apiFetch<UserSession>("/api/auth/session"),
    staleTime: 5 * 60 * 1000,
  });
}

export function useLogout() {
  const router = useRouter();
  const queryClient = useQueryClient();

  return async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    queryClient.clear();
    router.push("/");
    router.refresh();
  };
}
