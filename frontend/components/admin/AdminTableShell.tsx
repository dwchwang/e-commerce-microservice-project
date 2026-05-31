import type { ReactNode } from "react";
import { Card, CardContent } from "@/components/ui/card";

export function AdminTableShell({
  filters,
  children,
  footer,
}: {
  filters?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <Card className="rounded-lg py-0">
      {filters && <div className="border-b p-4">{filters}</div>}
      <CardContent className="p-0">{children}</CardContent>
      {footer && <div className="border-t p-3 text-sm text-muted-foreground">{footer}</div>}
    </Card>
  );
}
