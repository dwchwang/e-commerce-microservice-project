"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Separator } from "@/components/ui/separator";
import { Card, CardContent } from "@/components/ui/card";
import { Loader2, ArrowLeft } from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api/client";
import { useCart } from "@/lib/cart/hooks";
import { qk } from "@/lib/query/keys";
import type { Address, Order } from "@/lib/api/types";
import Link from "next/link";
import { toast } from "sonner";

type VnPayCreateResponse = { paymentId: string; orderId: string; paymentUrl: string };

// VNPAY payment can only be created once the order saga has reserved stock
// (order status STOCK_RESERVED). The order is created as PENDING and moves to
// STOCK_RESERVED asynchronously, so we poll briefly before requesting the URL.
async function waitForStockReserved(orderId: string, attempts = 20, delayMs = 700): Promise<void> {
  for (let i = 0; i < attempts; i++) {
    const o = await apiFetch<Order>(`/orders/${orderId}`);
    if (o.status === "STOCK_RESERVED") return;
    if (o.status === "CANCELLED") {
      throw new ApiError(409, { message: o.cancelReason || "Đơn hàng đã bị hủy (hết hàng hoặc lỗi kho)." });
    }
    await new Promise((r) => setTimeout(r, delayMs));
  }
  throw new ApiError(408, { message: "Hệ thống đang xử lý đơn hàng, vui lòng thử lại sau giây lát." });
}

export default function CheckoutPage() {
  const router = useRouter();
  const { data: cart } = useCart();
  const [paymentMethod, setPaymentMethod] = useState<"COD" | "VNPAY">("COD");
  const [submitting, setSubmitting] = useState(false);
  const [voucherCode, setVoucherCode] = useState("");

  const { data: addresses, isLoading: loadingAddresses } = useQuery({
    queryKey: qk.addresses,
    queryFn: () => apiFetch<Address[]>("/users/me/addresses"),
  });

  const [shipping, setShipping] = useState({ name: "", phone: "", address: "" });
  const [useNewAddress, setUseNewAddress] = useState(false);
  const [selectedAddressId, setSelectedAddressId] = useState<string>("");

  function resolveShipping(): { name: string; phone: string; address: string } | null {
    if (useNewAddress) {
      if (!shipping.name || !shipping.phone || !shipping.address) return null;
      return shipping;
    }
    const addr = addresses?.find((a) => a.id === selectedAddressId);
    if (!addr) return null;
    const parts = [addr.addressLine, addr.ward, addr.district, addr.city].filter(Boolean);
    return { name: addr.recipientName, phone: addr.phoneNumber, address: parts.join(", ") };
  }

  const handleSubmit = async () => {
    if (!cart || cart.items.length === 0) return;

    const ship = resolveShipping();
    if (!ship) {
      toast.error("Vui lòng chọn hoặc nhập đầy đủ địa chỉ giao hàng");
      return;
    }

    setSubmitting(true);
    try {
      const order = await apiFetch<Order>("/orders", {
        method: "POST",
        body: JSON.stringify({
          items: cart.items.map((it) => ({ productId: it.productId, quantity: it.quantity })),
          paymentMethod,
          voucherCode: voucherCode.trim() || undefined,
          shippingName: ship.name,
          shippingPhone: ship.phone,
          shippingAddress: ship.address,
        }),
      });

      if (paymentMethod === "VNPAY") {
        // Order starts PENDING; wait until stock is reserved before requesting
        // a VNPAY URL, otherwise the backend rejects with "order not ready".
        await waitForStockReserved(order.id);
        const payment = await apiFetch<VnPayCreateResponse>(
          `/payments/vnpay/create?orderId=${order.id}`,
          { method: "POST" }
        );
        if (payment.paymentUrl) {
          window.location.href = payment.paymentUrl;
          return;
        }
        toast.error("Không tạo được liên kết thanh toán VNPAY");
        router.push(`/orders/${order.id}`);
        return;
      }

      toast.success("Đặt hàng thành công!");
      router.push(`/orders/${order.id}`);
    } catch (err) {
      const msg =
        err instanceof ApiError && err.body && typeof err.body === "object" && "message" in err.body
          ? String((err.body as Record<string, unknown>).message)
          : "Đặt hàng thất bại, vui lòng thử lại";
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (!cart || cart.items.length === 0) {
    return (
      <div className="text-center py-16">
        <p className="text-lg">Giỏ hàng trống</p>
        <Link href="/products">
          <Button className="mt-4">Xem sản phẩm</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <Link href="/cart" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-primary mb-4">
        <ArrowLeft className="h-4 w-4" /> Quay lại giỏ hàng
      </Link>
      <h1 className="text-2xl font-bold mb-6">Thanh toán</h1>

      {/* Order Summary */}
      <Card className="mb-6">
        <CardContent className="p-4">
          <h3 className="font-semibold mb-2">Đơn hàng ({cart.items.length} sản phẩm)</h3>
          <div className="space-y-2 text-sm">
            {cart.items.map((item) => (
              <div key={item.productId} className="flex justify-between">
                <span className="truncate flex-1 mr-2">{item.productName} x{item.quantity}</span>
                <span>{item.subtotal.toLocaleString("vi-VN")}₫</span>
              </div>
            ))}
          </div>
          <Separator className="my-2" />
          <div className="flex justify-between font-bold">
            <span>Tổng cộng</span>
            <span className="text-primary">{cart.totalPrice.toLocaleString("vi-VN")}₫</span>
          </div>
        </CardContent>
      </Card>

      {/* Address */}
      <div className="mb-6">
        <h3 className="font-semibold mb-3">Địa chỉ giao hàng</h3>

        {!loadingAddresses && addresses && addresses.length > 0 && !useNewAddress && (
          <RadioGroup value={selectedAddressId} onValueChange={setSelectedAddressId} className="space-y-3 mb-4">
            {addresses.map((addr) => (
              <label
                key={addr.id}
                className="flex items-start gap-3 p-3 border rounded-lg cursor-pointer hover:bg-muted/50"
              >
                <RadioGroupItem value={addr.id!} className="mt-1" />
                <div className="text-sm">
                  <p className="font-medium">{addr.recipientName} — {addr.phoneNumber}</p>
                  <p className="text-muted-foreground">
                    {[addr.addressLine, addr.ward, addr.district, addr.city].filter(Boolean).join(", ")}
                  </p>
                </div>
              </label>
            ))}
          </RadioGroup>
        )}

        <Button variant="outline" size="sm" onClick={() => setUseNewAddress(!useNewAddress)}>
          {useNewAddress ? "Chọn địa chỉ đã lưu" : "Nhập địa chỉ mới"}
        </Button>

        {useNewAddress && (
          <div className="grid grid-cols-1 gap-3 mt-3">
            <div>
              <Label>Họ tên người nhận</Label>
              <Input
                value={shipping.name}
                onChange={(e) => setShipping({ ...shipping, name: e.target.value })}
                placeholder="Nguyễn Văn A"
              />
            </div>
            <div>
              <Label>Số điện thoại</Label>
              <Input
                value={shipping.phone}
                onChange={(e) => setShipping({ ...shipping, phone: e.target.value })}
                placeholder="0912345678"
              />
            </div>
            <div>
              <Label>Địa chỉ đầy đủ</Label>
              <Input
                value={shipping.address}
                onChange={(e) => setShipping({ ...shipping, address: e.target.value })}
                placeholder="Số 1, Đường ABC, Phường X, Quận Y, TP. HCM"
              />
            </div>
          </div>
        )}
      </div>

      {/* Voucher */}
      <div className="mb-6">
        <h3 className="font-semibold mb-3">Mã giảm giá</h3>
        <Input
          value={voucherCode}
          onChange={(e) => setVoucherCode(e.target.value.toUpperCase())}
          placeholder="Nhập mã giảm giá (nếu có)"
          className="max-w-xs"
        />
      </div>

      {/* Payment Method */}
      <div className="mb-6">
        <h3 className="font-semibold mb-3">Phương thức thanh toán</h3>
        <RadioGroup
          value={paymentMethod}
          onValueChange={(v) => setPaymentMethod(v as "COD" | "VNPAY")}
          className="space-y-3"
        >
          <label className="flex items-center gap-3 p-3 border rounded-lg cursor-pointer hover:bg-muted/50">
            <RadioGroupItem value="COD" />
            <div>
              <p className="font-medium">Thanh toán khi nhận hàng (COD)</p>
              <p className="text-sm text-muted-foreground">Trả tiền mặt khi nhận hàng</p>
            </div>
          </label>
          <label className="flex items-center gap-3 p-3 border rounded-lg cursor-pointer hover:bg-muted/50">
            <RadioGroupItem value="VNPAY" />
            <div>
              <p className="font-medium">VNPAY</p>
              <p className="text-sm text-muted-foreground">Thanh toán qua cổng VNPAY (sandbox)</p>
            </div>
          </label>
        </RadioGroup>
      </div>

      <Button className="w-full h-12 text-lg" onClick={handleSubmit} disabled={submitting}>
        {submitting ? <Loader2 className="h-5 w-5 animate-spin mr-2" /> : null}
        {paymentMethod === "VNPAY" ? "Thanh toán qua VNPAY" : "Đặt hàng"}
      </Button>
    </div>
  );
}
