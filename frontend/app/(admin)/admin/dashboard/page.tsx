import type { ReactNode } from "react";
import Link from "next/link";
import { AlertTriangle, Package, ShoppingCart, TrendingUp, Users } from "lucide-react";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { DonutLegend, LineBars } from "@/components/admin/AdminCharts";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminFetchSafe, formatCurrency, toAdminPage, type PagePayload } from "@/lib/admin/api";
import type { InventoryAdmin, OrderAdmin } from "@/lib/admin/types";
import { cn } from "@/lib/utils";

type Summary = {
  revenue: number;
  orders: number;
  customers: number;
  conversionRate: number;
};

type RevenuePoint = { date: string; revenue: number };
type TopProduct = { productName: string; quantity: number; revenue?: number };
type StatusCount = { status: string; count: number };

export default async function AdminDashboardPage() {
  const [summary, revenue, statusCounts, topProducts, recentOrdersPage, lowStock] = await Promise.all([
    adminFetchSafe<Summary>("/orders/admin/analytics/summary?period=7d", {
      revenue: 0,
      orders: 0,
      customers: 0,
      conversionRate: 0,
    }),
    adminFetchSafe<RevenuePoint[]>("/orders/admin/analytics/revenue?days=30", []),
    adminFetchSafe<StatusCount[]>("/orders/admin/analytics/status-counts", []),
    adminFetchSafe<TopProduct[]>("/orders/admin/analytics/top-products?limit=10", []),
    // /orders/admin returns a paginated Page payload, not a bare array.
    adminFetchSafe<PagePayload<OrderAdmin>>("/orders/admin?size=10", { content: [] }),
    adminFetchSafe<InventoryAdmin[]>("/inventory/low-stock?threshold=10", []),
  ]);

  const recentOrders = toAdminPage(recentOrdersPage, 10).items;

  const revenueChart = revenue.map((item) => ({
    label: new Date(item.date).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" }),
    value: Number(item.revenue ?? 0),
  }));
  const statusChart = statusCounts.map((item) => ({ label: item.status, value: item.count }));

  return (
    <>
      <AdminPageHeader
        title="Dashboard"
        description="Tổng quan doanh thu, đơn hàng, tồn kho và hoạt động gần đây."
      />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard accent="emerald" icon={<TrendingUp className="size-5" />} label="Doanh thu 7 ngày" value={formatCurrency(summary.revenue)} />
        <MetricCard accent="blue" icon={<ShoppingCart className="size-5" />} label="Đơn hàng" value={summary.orders.toString()} />
        <MetricCard accent="violet" icon={<Users className="size-5" />} label="Khách hàng mới" value={summary.customers.toString()} />
        <MetricCard accent="amber" icon={<Package className="size-5" />} label="Conversion" value={`${summary.conversionRate.toFixed(1)}%`} />
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-[1.4fr_0.8fr]">
        <Card className="rounded-lg">
          <CardHeader>
            <CardTitle>Doanh thu 30 ngày</CardTitle>
          </CardHeader>
          <CardContent>
            {revenueChart.length > 0 ? <LineBars data={revenueChart} /> : <EmptyPanel text="Chưa có dữ liệu doanh thu." />}
          </CardContent>
        </Card>

        <Card className="rounded-lg">
          <CardHeader>
            <CardTitle>Đơn theo trạng thái</CardTitle>
          </CardHeader>
          <CardContent>{statusChart.length > 0 ? <DonutLegend data={statusChart} /> : <EmptyPanel text="Chưa có đơn hàng." />}</CardContent>
        </Card>
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-2">
        <Card className="rounded-lg">
          <CardHeader>
            <CardTitle>Đơn hàng gần đây</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã đơn</TableHead>
                  <TableHead>Khách hàng</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Tổng</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {recentOrders.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell>
                      <Link href={`/admin/orders/${order.id}`} className="font-medium hover:underline">
                        {order.id.slice(0, 8)}
                      </Link>
                    </TableCell>
                    <TableCell>{order.userEmail ?? "-"}</TableCell>
                    <TableCell>
                      <StatusBadge status={order.status} />
                    </TableCell>
                    <TableCell className="text-right">{formatCurrency(order.totalAmount)}</TableCell>
                  </TableRow>
                ))}
                {recentOrders.length === 0 && <EmptyRow colSpan={4} text="Chưa có đơn hàng." />}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card className="rounded-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <AlertTriangle className="size-4" />
              Cảnh báo tồn kho thấp
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>SKU</TableHead>
                  <TableHead>Sản phẩm</TableHead>
                  <TableHead className="text-right">Còn bán</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {lowStock.map((item) => (
                  <TableRow key={item.id ?? item.sku}>
                    <TableCell className="font-medium">{item.sku}</TableCell>
                    <TableCell>{item.productName ?? "-"}</TableCell>
                    <TableCell className="text-right">{item.availableQuantity ?? item.quantity}</TableCell>
                  </TableRow>
                ))}
                {lowStock.length === 0 && <EmptyRow colSpan={3} text="Không có SKU dưới ngưỡng." />}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>

      <Card className="mt-4 rounded-lg">
        <CardHeader>
          <CardTitle>Top sản phẩm bán chạy</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Sản phẩm</TableHead>
                <TableHead className="text-right">Số lượng</TableHead>
                <TableHead className="text-right">Doanh thu</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {topProducts.map((item) => (
                <TableRow key={item.productName}>
                  <TableCell className="font-medium">{item.productName}</TableCell>
                  <TableCell className="text-right">{item.quantity}</TableCell>
                  <TableCell className="text-right">{formatCurrency(item.revenue)}</TableCell>
                </TableRow>
              ))}
              {topProducts.length === 0 && <EmptyRow colSpan={3} text="Chưa có dữ liệu bán chạy." />}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </>
  );
}

const ACCENTS = {
  emerald: "bg-emerald-50 text-emerald-600 ring-emerald-100",
  blue: "bg-blue-50 text-blue-600 ring-blue-100",
  violet: "bg-violet-50 text-violet-600 ring-violet-100",
  amber: "bg-amber-50 text-amber-600 ring-amber-100",
} as const;

function MetricCard({
  icon,
  label,
  value,
  accent = "blue",
}: {
  icon: ReactNode;
  label: string;
  value: string;
  accent?: keyof typeof ACCENTS;
}) {
  return (
    <Card className="rounded-xl transition-shadow hover:shadow-[0_8px_30px_-12px_rgba(0,0,0,0.12)]">
      <CardContent className="flex items-center justify-between p-4">
        <div className="min-w-0">
          <p className="text-sm text-muted-foreground">{label}</p>
          <p className="mt-2 truncate text-2xl font-semibold tracking-tight">{value}</p>
        </div>
        <div className={cn("shrink-0 rounded-xl p-2.5 ring-1 ring-inset", ACCENTS[accent])}>{icon}</div>
      </CardContent>
    </Card>
  );
}

function EmptyPanel({ text }: { text: string }) {
  return <div className="flex h-56 items-center justify-center rounded-md bg-muted/50 text-sm text-muted-foreground">{text}</div>;
}

function EmptyRow({ colSpan, text }: { colSpan: number; text: string }) {
  return (
    <TableRow>
      <TableCell colSpan={colSpan} className="h-20 text-center text-muted-foreground">
        {text}
      </TableCell>
    </TableRow>
  );
}
