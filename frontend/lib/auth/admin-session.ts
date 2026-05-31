import { cookies } from "next/headers";

export type AdminSession = {
  id: string;
  email: string;
  name: string;
  roles: string[];
};

type JwtPayload = {
  sub?: string;
  email?: string;
  name?: string;
  preferred_username?: string;
  realm_access?: { roles?: string[] };
  resource_access?: Record<string, { roles?: string[] }>;
};

function decodePayload(token: string): JwtPayload | null {
  const [, payload] = token.split(".");
  if (!payload) return null;

  try {
    return JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as JwtPayload;
  } catch {
    return null;
  }
}

function extractRoles(payload: JwtPayload): string[] {
  const roles = new Set(payload.realm_access?.roles ?? []);

  Object.values(payload.resource_access ?? {}).forEach((client) => {
    client.roles?.forEach((role) => roles.add(role));
  });

  return Array.from(roles).map(normalizeRole);
}

function normalizeRole(role: string): string {
  const normalized = role.trim().toUpperCase();
  return normalized.startsWith("ROLE_") ? normalized : `ROLE_${normalized}`;
}

export async function getAdminSession(): Promise<AdminSession | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("access_token")?.value;
  if (!token) return null;

  const payload = decodePayload(token);
  if (!payload?.sub) return null;

  return {
    id: payload.sub,
    email: payload.email ?? "",
    name: payload.name ?? payload.preferred_username ?? payload.email ?? "Admin",
    roles: extractRoles(payload),
  };
}
