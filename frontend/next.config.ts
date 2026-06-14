import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "*.nip.io" },
      { protocol: "http", hostname: "localhost" },
      // Demo product / banner imagery (seed data ships without images).
      { protocol: "https", hostname: "images.unsplash.com" },
      { protocol: "https", hostname: "picsum.photos" },
    ],
  },
  // BFF proxy is handled by app/api/proxy/[...path]/route.ts
  // No rewrites needed — the route handler attaches tokens automatically.
};

export default nextConfig;
