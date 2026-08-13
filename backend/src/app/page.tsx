import { SiteFooter } from "@/components/landing/SiteFooter";
import { SiteHeader } from "@/components/landing/SiteHeader";
import { Hero } from "@/components/landing/Hero";
import { FeatureIntent } from "@/components/landing/FeatureIntent";
import { WaitlistForm } from "@/components/landing/WaitlistForm";

export default function Home() {
  return (
    <>
      <SiteHeader />
      <main>
        <Hero />
        <FeatureIntent />
        <section className="border-t border-kai-border/60 bg-kai-surface/30">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <div className="max-w-xl">
              <h2 className="text-balance text-3xl font-bold tracking-[-0.02em] text-kai-fg">
                Get early access
              </h2>
              <p className="mt-4 text-pretty text-base font-bold leading-relaxed text-kai-muted">
                KaiOS is in active development for Android. Join the waitlist
                and we&apos;ll notify you when installs open.
              </p>
              <div className="mt-8">
                <WaitlistForm source="footer" />
              </div>
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </>
  );
}
