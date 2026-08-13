import type { Metadata } from "next";
import { Doto } from "next/font/google";
import "./globals.css";

const doto = Doto({
  variable: "--font-doto",
  subsets: ["latin"],
  weight: ["400", "700"],
});

export const metadata: Metadata = {
  title: "KaiOS — Your phone, blank until you mean it",
  description:
    "A text-first Android launcher. One command line replaces the app grid. Connect your tools, set guardrails, and act with intent.",
  openGraph: {
    title: "KaiOS",
    description:
      "A text-first Android launcher. One command line replaces the app grid.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${doto.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col bg-kai-bg text-kai-fg">
        {children}
      </body>
    </html>
  );
}
