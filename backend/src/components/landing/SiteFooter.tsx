export function SiteFooter() {
  return (
    <footer className="border-t border-kai-border/60">
      <div className="mx-auto flex max-w-6xl flex-col gap-2 px-6 py-10 text-sm font-bold text-kai-muted sm:flex-row sm:items-center sm:justify-between">
        <p>© {new Date().getFullYear()} KaiOS</p>
        <p className="text-xs uppercase tracking-[0.08em]">
          Android launcher · work in progress
        </p>
      </div>
    </footer>
  );
}
