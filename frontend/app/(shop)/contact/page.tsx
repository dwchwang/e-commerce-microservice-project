"use client";

import { useState } from "react";
import { MapPin, Phone, Mail, Clock, Loader2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";

const info = [
  { icon: MapPin, label: "Địa chỉ", value: "Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội" },
  { icon: Phone, label: "Hotline", value: "1900 1234" },
  { icon: Mail, label: "Email", value: "support@techstore.vn" },
  { icon: Clock, label: "Giờ làm việc", value: "08:00 – 21:00 (T2 – CN)" },
];

export default function ContactPage() {
  const [form, setForm] = useState({ name: "", email: "", message: "" });
  const [sending, setSending] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim() || !form.email.trim() || !form.message.trim()) {
      toast.error("Vui lòng điền đầy đủ thông tin");
      return;
    }
    setSending(true);
    // Demo: no contact backend — simulate sending.
    setTimeout(() => {
      setSending(false);
      setForm({ name: "", email: "", message: "" });
      toast.success("Đã gửi! Chúng tôi sẽ phản hồi sớm nhất.");
    }, 700);
  };

  return (
    <div>
      <section className="bg-secondary">
        <div className="container mx-auto px-4 py-16 text-center md:py-20">
          <p className="mb-3 text-sm font-medium uppercase tracking-widest text-primary">Liên hệ</p>
          <h1 className="headline-tight mx-auto max-w-2xl text-4xl md:text-5xl">Chúng tôi luôn sẵn sàng hỗ trợ</h1>
          <p className="mx-auto mt-4 max-w-xl text-lg font-light text-muted-foreground">
            Có câu hỏi về sản phẩm, đơn hàng hay bảo hành? Gửi tin nhắn cho chúng tôi.
          </p>
        </div>
      </section>

      <section className="container mx-auto grid gap-10 px-4 py-16 md:grid-cols-2">
        {/* Info */}
        <div>
          <h2 className="text-2xl font-semibold tracking-tight">Thông tin liên hệ</h2>
          <div className="mt-6 space-y-5">
            {info.map((i) => (
              <div key={i.label} className="flex items-start gap-4">
                <div className="flex size-11 shrink-0 items-center justify-center rounded-full bg-primary/10">
                  <i.icon className="size-5 text-primary" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">{i.label}</p>
                  <p className="text-[15px] font-medium">{i.value}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Form */}
        <div className="rounded-3xl border border-border bg-card p-6 md:p-8">
          <h2 className="text-2xl font-semibold tracking-tight">Gửi tin nhắn</h2>
          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div>
              <Label htmlFor="name">Họ tên</Label>
              <Input id="name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Nguyễn Văn A" className="mt-1.5" />
            </div>
            <div>
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="email@example.com" className="mt-1.5" />
            </div>
            <div>
              <Label htmlFor="message">Nội dung</Label>
              <Textarea id="message" rows={5} value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })} placeholder="Nội dung cần hỗ trợ..." className="mt-1.5" />
            </div>
            <Button type="submit" className="w-full rounded-full" disabled={sending}>
              {sending ? <Loader2 className="mr-2 size-4 animate-spin" /> : null}
              Gửi tin nhắn
            </Button>
          </form>
        </div>
      </section>
    </div>
  );
}
