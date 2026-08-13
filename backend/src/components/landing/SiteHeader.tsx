import Image from "next/image";
import Link from "next/link";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-50 border-b border-kai-border/60 bg-kai-bg/90 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link href="/" className="flex items-center gap-3">
          <Image
            src="/brand/logomark-for-dark.svg"
            alt=""
            width={28}
            height={28}
            priority
          />
          <Image
            src="/brand/wordmark-for-dark.svg"
            alt="KaiOS"
            width={88}
            height={20}
            priority
          />
        </Link>
        <a
          href="#waitlist"
          className="rounded-lg border border-kai-border px-4 py-2 text-xs font-bold uppercase tracking-[0.08em] text-kai-fg transition hover:border-kai-accent hover:text-kai-accent focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-kai-accent"
        >
          Early access
        </a>
      </div>
    </header>
  );
}
