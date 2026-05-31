"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Loader2, Zap } from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api/client";
import { useSession } from "@/lib/auth/hooks";
import { toast } from "sonner";
import type { FlashSalePurchaseResult } from "@/lib/api/types";

export function FlashSaleBuyButton({
  saleId,
  active,
  soldOut,
}: {
  saleId: string;
  active: boolean;
  soldOut: boolean;
}) {
  const router = useRouter();
  const { data: session } = useSession();
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<"COD" | "VNPAY">("COD");
  const [form, setForm] = useState({ shippingName: "", shippingPhone: "", shippingAddress: "" });

  const handleBuy = async () => {
    if (!session?.user) {
      toast.error("Vui lòng đăng nhập để mua flash sale");
      router.push(`/login?next=/flash-sales/${saleId}`);
      return;
    }
    if (!form.shippingName || !form.shippingPhone || !form.shippingAddress) {
      toast.error("Vui lòng nhập đầy đủ thông tin giao hàng");
      return;
    }

    setLoading(true);
    try {
      const result = await apiFetch<FlashSalePurchaseResult>(`/flash-sales/${saleId}/purchase`, {
        method: "POST",
        body: JSON.stringify({ paymentMethod, ...form }),
      });

      if (result.success) {
        toast.success("Đặt mua thành công! Đơn hàng đang được xử lý.");
        setOpen(false);
        router.push("/orders");
      } else {
        toast.error(result.message || "Không thể mua, vui lòng thử lại");
      }
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 429) {
          toast.warning("Quá nhanh! Vui lòng thử lại sau giây lát");
        } else {
          const msg =
            err.body && typeof err.body === "object" && "message" in err.body
              ? String((err.body as Record<string, unknown>).message)
              : "Không thể mua sản phẩm này";
          toast.error(msg);
        }
      } else {
        toast.error("Lỗi kết nối, vui lòng thử lại");
      }
    } finally {
      setLoading(false);
    }
  };

  if (soldOut) {
    return (
      <Button className="w-full h-12 text-lg" disabled>
        Hết hàng
      </Button>
    );
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger
        render={
          <Button className="w-full h-12 text-lg gap-2" disabled={!active}>
            <Zap className="h-5 w-5" />
            {active ? "Mua ngay" : "Chưa mở bán"}
          </Button>
        }
      />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Thông tin giao hàng</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div>
            <Label>Họ tên người nhận</Label>
            <Input
              value={form.shippingName}
              onChange={(e) => setForm({ ...form, shippingName: e.target.value })}
              placeholder="Nguyễn Văn A"
            />
          </div>
          <div>
            <Label>Số điện thoại</Label>
            <Input
              value={form.shippingPhone}
              onChange={(e) => setForm({ ...form, shippingPhone: e.target.value })}
              placeholder="0912345678"
            />
          </div>
          <div>
            <Label>Địa chỉ đầy đủ</Label>
            <Input
              value={form.shippingAddress}
              onChange={(e) => setForm({ ...form, shippingAddress: e.target.value })}
              placeholder="Số 1, Đường ABC, Quận X, TP. HCM"
            />
          </div>
          <div>
            <Label className="mb-2 block">Thanh toán</Label>
            <RadioGroup
              value={paymentMethod}
              onValueChange={(v) => setPaymentMethod(v as "COD" | "VNPAY")}
              className="space-y-2"
            >
              <label className="flex items-center gap-2 text-sm">
                <RadioGroupItem value="COD" /> COD (thanh toán khi nhận)
              </label>
              <label className="flex items-center gap-2 text-sm">
                <RadioGroupItem value="VNPAY" /> VNPAY
              </label>
            </RadioGroup>
          </div>
          <Button className="w-full" onClick={handleBuy} disabled={loading}>
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Zap className="h-4 w-4" />}
            Xác nhận mua
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
