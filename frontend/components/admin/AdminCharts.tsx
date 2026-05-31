import { formatCurrency } from "@/lib/admin/api";

export type ChartPoint = {
  label: string;
  value: number;
};

export function LineBars({ data }: { data: ChartPoint[] }) {
  const max = Math.max(...data.map((item) => item.value), 1);

  return (
    <div className="flex h-56 items-end gap-2">
      {data.map((item) => (
        <div key={item.label} className="flex min-w-0 flex-1 flex-col items-center gap-2">
          <div
            className="w-full rounded-t bg-zinc-900 transition-all"
            style={{ height: `${Math.max(6, (item.value / max) * 190)}px` }}
            title={`${item.label}: ${formatCurrency(item.value)}`}
          />
          <span className="w-full truncate text-center text-[11px] text-muted-foreground">{item.label}</span>
        </div>
      ))}
    </div>
  );
}

export function DonutLegend({ data }: { data: ChartPoint[] }) {
  const total = data.reduce((sum, item) => sum + item.value, 0);

  return (
    <div className="space-y-3">
      {data.map((item, index) => {
        const percent = total > 0 ? Math.round((item.value / total) * 100) : 0;
        return (
          <div key={item.label} className="space-y-1">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium">{item.label}</span>
              <span className="text-muted-foreground">
                {item.value} ({percent}%)
              </span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-muted">
              <div className="h-full bg-zinc-900" style={{ width: `${percent}%`, opacity: 1 - index * 0.08 }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}
