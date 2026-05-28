"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Separator } from "@/components/ui/separator";
import { Card, CardContent } from "@/components/ui/card";
import { Loader2, ArrowLeft } from "lucide-react";
import { apiFetch } from "@/lib/api/client";
import { useCart } from "@/lib/cart/hooks";
import { qk } from "@/lib/query/keys";
import type { Address, Order } from "@/lib/api/types";
import Link from "next/link";
import { toast } from "sonner";

export default function CheckoutPage() {
  const router = useRouter();
  const { data: cart } = useCart();
  const [paymentMethod, setPaymentMethod] = useState<"COD" | "VNPAY">("COD");
  const [submitting, setSubmitting] = useState(false);

  // Fetch saved addresses
  const { data: addresses, isLoading: loadingAddresses } = useQuery({
    queryKey: qk.addresses,
    queryFn: () => apiFetch<Address[]>("/users/addresses"),
  });

  // New address form
  const [newAddress, setNewAddress] = useState<Address>({
    fullName: "",
    phone: "",
    addressLine: "",
    city: "",
    district: "",
    ward: "",
  });
  const [useNewAddress, setUseNewAddress] = useState(false);
  const [selectedAddressId, setSelectedAddressId] = useState<string>("");

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const payload: Record<string, unknown> = {
        paymentMethod,
      };

      if (useNewAddress) {
        payload.address = newAddress;
      } else if (selectedAddressId) {
        payload.addressId = selectedAddressId;
      } else {
        toast.error("Vui lòng chọn địa chỉ giao hàng");
        setSubmitting(false);
        return;
      }

      const order = await apiFetch<Order>("/orders", {
        method: "POST",
        body: JSON.stringify(payload),
      });

      if (order.paymentMethod === "VNPAY" && order.paymentUrl) {
        window.location.href = order.paymentUrl;
      } else {
        toast.success("Đặt hàng thành công!");
        router.push(`/orders/${order.id}`);
      }
    } catch {
      toast.error("Đặt hàng thất bại, vui lòng thử lại");
    } finally {
      setSubmitting(false);
    }
  };

  if (!cart || !cart.items || cart.items.length === 0) {
    return (
      <div className="text-center py-16">
        <p className="text-lg">Giỏ hàng trống</p>
        <Link href="/products">
          <Button className="mt-4">Xem sản phẩm</Button>
        </Link>
      </div>
    );
  }

  const total = (cart.totalAmount || 0) - (cart.discountAmount || 0);

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
              <div key={item.id} className="flex justify-between">
                <span className="truncate flex-1 mr-2">{item.productName} x{item.quantity}</span>
                <span>{item.subtotal.toLocaleString("vi-VN")}₫</span>
              </div>
            ))}
          </div>
          <Separator className="my-2" />
          <div className="flex justify-between font-bold">
            <span>Tổng cộng</span>
            <span className="text-primary">{total.toLocaleString("vi-VN")}₫</span>
          </div>
        </CardContent>
      </Card>

      {/* Address */}
      <div className="mb-6">
        <h3 className="font-semibold mb-3">Địa chỉ giao hàng</h3>

        {!loadingAddresses && addresses && addresses.length > 0 && !useNewAddress && (
          <RadioGroup
            value={selectedAddressId}
            onValueChange={setSelectedAddressId}
            className="space-y-3 mb-4"
          >
            {addresses.map((addr) => (
              <label
                key={addr.id}
                className="flex items-start gap-3 p-3 border rounded-lg cursor-pointer hover:bg-muted/50"
              >
                <RadioGroupItem value={addr.id!} className="mt-1" />
                <div className="text-sm">
                  <p className="font-medium">{addr.fullName} — {addr.phone}</p>
                  <p className="text-muted-foreground">{addr.addressLine}, {addr.ward}, {addr.district}, {addr.city}</p>
                </div>
              </label>
            ))}
          </RadioGroup>
        )}

        <Button
          variant="outline"
          size="sm"
          onClick={() => setUseNewAddress(!useNewAddress)}
        >
          {useNewAddress ? "Chọn địa chỉ đã lưu" : "Dùng địa chỉ mới"}
        </Button>

        {useNewAddress && (
          <div className="grid grid-cols-2 gap-3 mt-3">
            <div className="col-span-2">
              <Label>Họ tên</Label>
              <Input
                value={newAddress.fullName}
                onChange={(e) => setNewAddress({ ...newAddress, fullName: e.target.value })}
                placeholder="Nguyễn Văn A"
              />
            </div>
            <div className="col-span-2">
              <Label>Số điện thoại</Label>
              <Input
                value={newAddress.phone}
                onChange={(e) => setNewAddress({ ...newAddress, phone: e.target.value })}
                placeholder="0912345678"
              />
            </div>
            <div className="col-span-2">
              <Label>Địa chỉ</Label>
              <Input
                value={newAddress.addressLine}
                onChange={(e) => setNewAddress({ ...newAddress, addressLine: e.target.value })}
                placeholder="Số 1, Đường ABC"
              />
            </div>
            <div>
              <Label>Phường/Xã</Label>
              <Input
                value={newAddress.ward || ""}
                onChange={(e) => setNewAddress({ ...newAddress, ward: e.target.value })}
              />
            </div>
            <div>
              <Label>Quận/Huyện</Label>
              <Input
                value={newAddress.district || ""}
                onChange={(e) => setNewAddress({ ...newAddress, district: e.target.value })}
              />
            </div>
            <div>
              <Label>Tỉnh/Thành phố</Label>
              <Input
                value={newAddress.city}
                onChange={(e) => setNewAddress({ ...newAddress, city: e.target.value })}
                placeholder="TP. Hồ Chí Minh"
              />
            </div>
          </div>
        )}
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
              <p className="text-sm text-muted-foreground">Thanh toán qua cổng VNPAY</p>
            </div>
          </label>
        </RadioGroup>
      </div>

      <Button
        className="w-full h-12 text-lg"
        onClick={handleSubmit}
        disabled={submitting}
      >
        {submitting ? <Loader2 className="h-5 w-5 animate-spin mr-2" /> : null}
        {paymentMethod === "VNPAY" ? "Thanh toán qua VNPAY" : "Đặt hàng"}
      </Button>
    </div>
  );
}
