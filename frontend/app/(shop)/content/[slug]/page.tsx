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
    <article className="container mx-auto max-w-3xl px-4 py-12 md:py-16">
      <h1 className="text-3xl font-semibold tracking-tight md:text-5xl">{post.title || slug}</h1>
      {post.author && (
        <p className="mt-4 text-sm text-muted-foreground">
          {post.author}
          {post.publishedAt && ` · ${new Date(post.publishedAt).toLocaleDateString("vi-VN")}`}
        </p>
      )}
      <div className="mt-8 whitespace-pre-line text-[17px] leading-relaxed text-foreground/90">
        {post.content}
      </div>
    </article>
  );
}
