"use client";

export default function ErrorPage({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <h2 className="text-2xl font-bold mb-2">Đã xảy ra lỗi</h2>
        <p className="text-muted-foreground mb-4">
          {error.message || "Có lỗi không mong muốn xảy ra."}
        </p>
        <button
          onClick={reset}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:opacity-90"
        >
          Thử lại
        </button>
      </div>
    </div>
  );
}
