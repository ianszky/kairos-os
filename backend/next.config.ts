import type { NextConfig } from "next";
import path from "path";

const nextConfig: NextConfig = {
  allowedDevOrigins: ["192.168.100.132", "192.168.100.132:3001"],
  turbopack: {
    root: path.join(__dirname, ".."),
  },
};

export default nextConfig;
