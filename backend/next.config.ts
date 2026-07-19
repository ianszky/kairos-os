import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  experimental: {
    allowedDevOrigins: ["192.168.100.132", "192.168.100.132:3001"]
  }
};

export default nextConfig;
