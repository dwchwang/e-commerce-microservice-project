"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";
import type { Address } from "@/lib/api/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { EmptyState } from "@/components/shared/EmptyState";
import { MapPin, Plus, Pencil, Trash2, Loader2 } from "lucide-react";
import { toast } from "sonner";

export default function AddressesPage() {
  const qc = useQueryClient();
  const { data: addresses, isLoading } = useQuery({
    queryKey: qk.addresses,
    queryFn: () => apiFetch<Address[]>("/users/addresses"),
  });

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<Address>({
    fullName: "", phone: "", addressLine: "", city: "", district: "", ward: "",
  });

  const resetForm = () => {
    setForm({ fullName: "", phone: "", addressLine: "", city: "", district: "", ward: "" });
    setEditingId(null);
  };

  const openEdit = (addr: Address) => {
    setForm({ ...addr });
    setEditingId(addr.id || null);
    setDialogOpen(true);
  };

  const saveMutation = useMutation({
    mutationFn: (data: Address) =>
      editingId
        ? apiFetch(`/users/addresses/${editingId}`, { method: "PUT", body: JSON.stringify(data) })
        : apiFetch("/users/addresses", { method: "POST", body: JSON.stringify(data) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.addresses });
      setDialogOpen(false);
      resetForm();
      toast.success(editingId ? "Đã cập nhật địa chỉ" : "Đã thêm địa chỉ");
    },
    onError: () => toast.error("Lỗi, vui lòng thử lại"),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => apiFetch(`/users/addresses/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.addresses });
      toast.success("Đã xóa địa chỉ");
    },
  });

  if (isLoading) return <div className="text-center py-16">Đang tải...</div>;

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Sổ địa chỉ</h1>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger>
            <Button onClick={resetForm} type="button">
              <Plus className="h-4 w-4 mr-1" /> Thêm địa chỉ
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{editingId ? "Sửa địa chỉ" : "Thêm địa chỉ mới"}</DialogTitle>
            </DialogHeader>
            <div className="grid grid-cols-2 gap-3 mt-3">
              <div className="col-span-2">
                <Label>Họ tên</Label>
                <Input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
              </div>
              <div className="col-span-2">
                <Label>SĐT</Label>
                <Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              </div>
              <div className="col-span-2">
                <Label>Địa chỉ</Label>
                <Input value={form.addressLine} onChange={(e) => setForm({ ...form, addressLine: e.target.value })} />
              </div>
              <div><Label>Phường/Xã</Label><Input value={form.ward || ""} onChange={(e) => setForm({ ...form, ward: e.target.value })} /></div>
              <div><Label>Quận/Huyện</Label><Input value={form.district || ""} onChange={(e) => setForm({ ...form, district: e.target.value })} /></div>
              <div><Label>Tỉnh/TP</Label><Input value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} /></div>
            </div>
            <Button
              className="mt-4 w-full"
              onClick={() => saveMutation.mutate(form)}
              disabled={saveMutation.isPending}
            >
              {saveMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Lưu"}
            </Button>
          </DialogContent>
        </Dialog>
      </div>

      {(!addresses || addresses.length === 0) ? (
        <EmptyState
          icon={<MapPin className="h-12 w-12" />}
          title="Chưa có địa chỉ"
          description="Thêm địa chỉ để tiện cho việc giao hàng."
        />
      ) : (
        <div className="space-y-3">
          {addresses.map((addr) => (
            <Card key={addr.id}>
              <CardContent className="p-4 flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-medium">{addr.fullName}</span>
                    {addr.isDefault && <Badge variant="secondary" className="text-xs">Mặc định</Badge>}
                  </div>
                  <p className="text-sm text-muted-foreground">{addr.phone}</p>
                  <p className="text-sm text-muted-foreground">{addr.addressLine}, {addr.city}</p>
                </div>
                <div className="flex gap-1">
                  <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => openEdit(addr)}>
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive" onClick={() => deleteMutation.mutate(addr.id!)}>
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
