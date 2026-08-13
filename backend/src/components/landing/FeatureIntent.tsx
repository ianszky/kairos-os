type FeatureBlockProps = {
  title: string;
  body: string;
  examples: string[];
};

function FeatureBlock({ title, body, examples }: FeatureBlockProps) {
  return (
    <article className="grid gap-6 border-t border-kai-border/60 py-14 lg:grid-cols-[minmax(0,280px)_1fr] lg:gap-16">
      <div>
        <h2 className="text-balance text-2xl font-bold tracking-[-0.02em] text-kai-fg">
          {title}
        </h2>
      </div>
      <div>
        <p className="max-w-2xl text-pretty text-base font-bold leading-relaxed text-kai-muted">
          {body}
        </p>
        <ul className="mt-6 space-y-3">
          {examples.map((example) => (
            <li
              key={example}
              className="rounded-lg border border-kai-border bg-kai-surface px-4 py-3 text-sm font-bold text-kai-fg"
            >
              {example}
            </li>
          ))}
        </ul>
      </div>
    </article>
  );
}

export function FeatureIntent() {
  return (
    <section className="mx-auto max-w-6xl px-6 pb-10">
      <FeatureBlock
        title="Command, don't browse"
        body="The home screen is a clock and a cursor. You type what you want — alarms, email, notes, calendar — and KaiOS routes it. No icon grid. No infinite feeds waiting at launch."
        examples={[
          "@alarm set an alarm for 6am tomorrow morning",
          "@gmail show my most important emails",
          "@notes shopping list: eggs, chicken, rice",
        ]}
      />
      <FeatureBlock
        title="Connectors, not apps"
        body="Pick an integration from the drawer — Gmail, Calendar, Notion, Slack — or a built-in Kai app. Mentions render inline, Claude-style, so every command stays readable."
        examples={[
          "@google-calendar schedule Ms. Tenorio tomorrow at 3pm",
          "@slack summarize #project-updates from today",
          "@clock open",
        ]}
      />
      <FeatureBlock
        title="Friction for trap apps"
        body="Social apps don't open on autopilot. Name a duration and a reason first. You stay in control — strict by default, configurable when you're ready."
        examples={[
          "@instagram /open — reason: reply to DM — time: 10m",
          "Leisure budget tracks remaining scroll time for the day",
          "Utility apps (camera, maps, clock) stay one command away",
        ]}
      />
    </section>
  );
}
