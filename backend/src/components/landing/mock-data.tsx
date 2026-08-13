export type ConnectorKind = "integration" | "app" | "trap";

export type Connector = {
  id: string;
  name: string;
  emoji: string;
  kind: ConnectorKind;
  tab: "integrations" | "app";
};

export const CONNECTORS: Connector[] = [
  { id: "gmail", name: "Gmail", emoji: "✉", kind: "integration", tab: "integrations" },
  { id: "google-calendar", name: "Calendar", emoji: "📅", kind: "integration", tab: "integrations" },
  { id: "notion", name: "Notion", emoji: "📝", kind: "integration", tab: "integrations" },
  { id: "slack", name: "Slack", emoji: "💬", kind: "integration", tab: "integrations" },
  { id: "github", name: "GitHub", emoji: "⌘", kind: "integration", tab: "integrations" },
  { id: "alarm", name: "Alarm", emoji: "⏰", kind: "app", tab: "app" },
  { id: "notes", name: "Notes", emoji: "📋", kind: "app", tab: "app" },
  { id: "clock", name: "Clock", emoji: "🕐", kind: "app", tab: "app" },
  { id: "scheduled", name: "Scheduled", emoji: "🔄", kind: "app", tab: "app" },
  { id: "instagram", name: "Instagram", emoji: "📷", kind: "trap", tab: "integrations" },
  { id: "tiktok", name: "TikTok", emoji: "🎵", kind: "trap", tab: "integrations" },
  { id: "messenger", name: "Messenger", emoji: "💭", kind: "trap", tab: "integrations" },
];

export const FRICTION_TIMES = ["5m", "10m", "15m", "30m", "45m", "1hr"] as const;

export const CANNED_RESPONSES: Record<string, string> = {
  gmail: "3 urgent threads: invoice from Acme, reply from Alex, calendar conflict tomorrow.",
  "google-calendar": "Tomorrow 3pm is open. Ms. Tenorio meeting scheduled.",
  alarm: "Alarm set for 6:00 AM tomorrow.",
  notes: "Added to your shopping list: eggs, chicken, rice.",
  instagram: "Intent logged. Opening Instagram for 10m — leisure budget: 42m left today.",
  default: "Done. Your command was routed and executed locally.",
};

export function getConnectorFromInput(value: string): Connector | undefined {
  const match = value.match(/@([\w-]+)/);
  if (!match) return undefined;
  return CONNECTORS.find((c) => c.id === match[1]);
}

export function renderMentionSegments(text: string) {
  const parts = text.split(/(@[\w-]+)/g);
  return parts.map((part, index) => {
    if (part.startsWith("@")) {
      return (
        <span key={`${part}-${index}`} className="kai-mention">
          {part}
        </span>
      );
    }
    return <span key={`${part}-${index}`}>{part}</span>;
  });
}
