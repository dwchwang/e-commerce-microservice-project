"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Loader2 } from "lucide-react";

const schema = z.object({
  username: z.string().min(3, "Tên đăng nhập ít nhất 3 ký tự"),
  email: z.string().email("Email không hợp lệ"),
  password: z.string().min(6, "Mật khẩu ít nhất 6 ký tự"),
  firstName: z.string().optional(),
  lastName: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export default function RegisterPage() {
  const router = useRouter();
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormValues) => {
    setLoading(true);
    setError("");
    try {
      const res = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json();
        setError(err.error || "Đăng ký thất bại");
        return;
      }
      setSuccess(true);
      setTimeout(() => router.push("/login"), 2000);
    } catch {
      setError("Lỗi kết nối, vui lòng thử lại");
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <Card>
        <CardContent className="text-center py-8">
          <p className="text-lg font-semibold text-green-600 mb-2">Đăng ký thành công!</p>
          <p className="text-muted-foreground">Đang chuyển đến trang đăng nhập...</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle className="text-2xl">Đăng ký</CardTitle>
        <CardDescription>Tạo tài khoản để bắt đầu mua sắm</CardDescription>
      </CardHeader>
      <CardContent>
        {error && (
          <Alert variant="destructive" className="mb-4">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="firstName">Họ</Label>
              <Input id="firstName" {...register("firstName")} placeholder="Nguyễn" />
            </div>
            <div>
              <Label htmlFor="lastName">Tên</Label>
              <Input id="lastName" {...register("lastName")} placeholder="Văn A" />
            </div>
          </div>
          <div>
            <Label htmlFor="username">Tên đăng nhập</Label>
            <Input id="username" {...register("username")} placeholder="Nhập tên đăng nhập" />
            {errors.username && (
              <p className="text-sm text-destructive mt-1">{errors.username.message}</p>
            )}
          </div>
          <div>
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" {...register("email")} placeholder="example@email.com" />
            {errors.email && (
              <p className="text-sm text-destructive mt-1">{errors.email.message}</p>
            )}
          </div>
          <div>
            <Label htmlFor="password">Mật khẩu</Label>
            <Input id="password" type="password" {...register("password")} placeholder="Ít nhất 6 ký tự" />
            {errors.password && (
              <p className="text-sm text-destructive mt-1">{errors.password.message}</p>
            )}
          </div>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
            Đăng ký
          </Button>
        </form>
        <p className="text-center text-sm text-muted-foreground mt-4">
          Đã có tài khoản?{" "}
          <Link href="/login" className="text-primary hover:underline font-medium">
            Đăng nhập
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
