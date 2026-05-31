import { serverFetch } from "@/lib/api/server-client";
import { notFound } from "next/navigation";

type BlogPost = {
  title?: string;
  content?: string;
  author?: string;
  publishedAt?: string;
};

export default async function ContentPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;

  let post: BlogPost | null = null;
  try {
    post = await serverFetch<BlogPost>(`/content/posts/${slug}`, {}, { revalidate: 300 });
  } catch {
    notFound();
  }

  if (!post) notFound();

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <h1 className="text-3xl font-bold mb-2">{post.title || slug}</h1>
      {post.author && (
        <p className="text-sm text-muted-foreground mb-6">
          {post.author}
          {post.publishedAt && ` · ${new Date(post.publishedAt).toLocaleDateString("vi-VN")}`}
        </p>
      )}
      <div className="prose dark:prose-invert max-w-none whitespace-pre-line">
        {post.content}
      </div>
    </div>
  );
}
