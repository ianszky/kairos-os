import { PhoneMock } from "./PhoneMock";
import { WaitlistForm } from "./WaitlistForm";

export function Hero() {
  return (
    <section className="relative overflow-hidden">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-[520px] kai-device-glow opacity-40" />
      <div className="relative mx-auto grid max-w-6xl gap-12 px-6 pb-20 pt-16 lg:grid-cols-[minmax(0,1fr)_390px] lg:items-center lg:gap-16 lg:pb-28 lg:pt-24">
        <div className="max-w-xl">
          <p className="text-sm font-bold uppercase tracking-[0.08em] text-kai-accent">
            Text-first Android launcher
          </p>
          <h1 className="mt-4 text-balance text-[clamp(2.5rem,6vw,4.5rem)] font-bold leading-[1.02] tracking-[-0.03em] text-kai-fg">
            Your phone, blank until you mean it.
          </h1>
          <p className="mt-5 max-w-lg text-pretty text-base font-bold leading-relaxed text-kai-muted">
            KaiOS replaces the app grid with one command line. Connect your
            tools, route intent through MCP, and open trap apps only when you
            can name why.
          </p>
          <div id="waitlist" className="mt-8">
            <WaitlistForm source="hero" />
          </div>
        </div>
        <PhoneMock />
      </div>
    </section>
  );
}
