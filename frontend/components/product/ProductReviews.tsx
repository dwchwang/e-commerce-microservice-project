"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch, ApiError } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";
import { useSession } from "@/lib/auth/hooks";
import type { Review, SpringPage } from "@/lib/api/types";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Loader2, Star } from "lucide-react";
import { toast } from "sonner";

export function ProductReviews({ productId }: { productId: string }) {
  const qc = useQueryClient();
  const { data: session } = useSession();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: qk.reviews.byProduct(productId),
    queryFn: () => apiFetch<SpringPage<Review>>(`/reviews/product/${productId}?size=20`),
  });

  const reviews = data?.content ?? [];

  const submit = useMutation({
    mutationFn: () =>
      apiFetch("/reviews", {
        method: "POST",
        body: JSON.stringify({ productId, rating, comment: comment.trim() || undefined }),
      }),
    onSuccess: () => {
      toast.success("Đã gửi đánh giá");
      setComment("");
      setRating(5);
      qc.invalidateQueries({ queryKey: qk.reviews.byProduct(productId) });
    },
    onError: (err) => {
      if (err instanceof ApiError && err.status === 403) {
        toast.error("Bạn cần mua sản phẩm này trước khi đánh giá");
      } else {
        toast.error("Không thể gửi đánh giá, vui lòng thử lại");
      }
    },
  });

  return (
    <div className="space-y-6">
      {session?.user ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            submit.mutate();
          }}
          className="space-y-3 rounded-lg border p-4"
        >
          <p className="text-sm font-medium">Viết đánh giá</p>
          <div className="flex items-center gap-1">
            {[1, 2, 3, 4, 5].map((n) => (
              <button
                key={n}
                type="button"
                onClick={() => setRating(n)}
                aria-label={`${n} sao`}
                className="p-0.5"
              >
                <Star className={`h-5 w-5 ${n <= rating ? "fill-yellow-500 text-yellow-500" : "text-muted-foreground"}`} />
              </button>
            ))}
          </div>
          <Textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Chia sẻ cảm nhận của bạn về sản phẩm..."
            rows={3}
            maxLength={5000}
          />
          <Button type="submit" size="sm" disabled={submit.isPending}>
            {submit.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            Gửi đánh giá
          </Button>
        </form>
      ) : (
        <p className="text-sm text-muted-foreground">Đăng nhập để viết đánh giá.</p>
      )}

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Đang tải đánh giá...</p>
      ) : reviews.length === 0 ? (
        <p className="text-sm text-muted-foreground">Chưa có đánh giá nào cho sản phẩm này.</p>
      ) : (
        <ul className="space-y-4">
          {reviews.map((review) => (
            <li key={review.id} className="border-b pb-3 last:border-b-0">
              <div className="flex items-center gap-1 mb-1">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Star
                    key={i}
                    className={`h-4 w-4 ${i < review.rating ? "fill-yellow-500 text-yellow-500" : "text-muted-foreground"}`}
                  />
                ))}
                <span className="ml-2 text-xs text-muted-foreground">
                  {new Date(review.createdAt).toLocaleDateString("vi-VN")}
                </span>
              </div>
              {review.comment && <p className="text-sm text-muted-foreground">{review.comment}</p>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
