import { Badge } from "@/components/ui/badge";

const SUCCESS = new Set(["ACTIVE", "APPROVED", "COMPLETED", "CONFIRMED", "PUBLISHED", "SHIPPED"]);
const WARNING = new Set(["PENDING", "CREATED", "STOCK_RESERVED", "UPCOMING", "SCHEDULED", "DRAFT"]);
const DANGER = new Set(["INACTIVE", "REJECTED", "CANCELLED", "FAILED", "ENDED", "OUT_OF_STOCK"]);

export function StatusBadge({ status }: { status?: string | boolean | null }) {
  const label =
    typeof status === "boolean" ? (status ? "ACTIVE" : "INACTIVE") : status ? String(status).toUpperCase() : "UNKNOWN";
  const variant = DANGER.has(label) ? "destructive" : SUCCESS.has(label) ? "default" : WARNING.has(label) ? "secondary" : "outline";

  return <Badge variant={variant}>{label.replaceAll("_", " ")}</Badge>;
}
