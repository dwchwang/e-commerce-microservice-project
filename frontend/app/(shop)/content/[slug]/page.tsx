import { serverFetch } from "@/lib/api/server-client";
import { notFound } from "next/navigation";

export default async function ContentPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;

  let content: { title?: string; body?: string; content?: string } | null = null;
  try {
    content = await serverFetch(`/content/${slug}`, {}, { revalidate: 300 });
  } catch {
    notFound();
  }

  const body = content?.body || content?.content || "";

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <h1 className="text-3xl font-bold mb-6">{content?.title || slug}</h1>
      <div
        className="prose dark:prose-invert max-w-none"
        dangerouslySetInnerHTML={{ __html: body }}
      />
    </div>
  );
}
