import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Do NOT use "standalone" for Cloudflare - opennextjs-cloudflare handles the output
  typescript: {
    ignoreBuildErrors: true,
  },
  reactStrictMode: false,
  // Cloudflare doesn't support sharp (native image processing)
  images: {
    unoptimized: true,
  },
  // Mark server-only packages as external for the edge runtime
  serverExternalPackages: ["sharp"],
  // Ensure static optimization works
  experimental: {
    serverActions: {
      bodySizeLimit: "10mb",
    },
  },
};

export default nextConfig;
