/**
 * KAIROS OS Application Document — text-first editable DOCX
 * Prose-heavy; screenshots for evidence; tables only where they clarify.
 */
const fs = require("fs");
const path = require("path");
const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  ImageRun,
  Header,
  Footer,
  AlignmentType,
  HeadingLevel,
  BorderStyle,
  WidthType,
  ShadingType,
  PageNumber,
  LevelFormat,
  Table,
  TableRow,
  TableCell,
  TabStopType,
} = require("docx");

const ROOT = "C:\\Dev\\kairos-os";
const SHOTS = path.join(ROOT, "screenshots", "integrations");
const TMP = path.join(ROOT, "tmp", "pdfs");
const OUT = path.join(
  ROOT,
  "output",
  "pdf",
  "KAIROS_OS_Application_Document_Text.docx"
);

const INK = "1A1A1A";
const MUTED = "555555";
const CONTENT_W = 9360; // Letter, 1" margins

function run(text, opts = {}) {
  return new TextRun({
    text,
    bold: !!opts.bold,
    italics: !!opts.italics,
    size: opts.size || 22, // 11pt
    font: opts.font || "Times New Roman",
    color: opts.color || INK,
  });
}

function para(text, opts = {}) {
  return new Paragraph({
    alignment: opts.align || AlignmentType.JUSTIFIED,
    spacing: {
      before: opts.before ?? 0,
      after: opts.after ?? 200,
      line: 360,
      lineRule: "auto",
    },
    indent: opts.indent,
    children: [run(text, opts)],
  });
}

function paraRuns(runs, opts = {}) {
  return new Paragraph({
    alignment: opts.align || AlignmentType.JUSTIFIED,
    spacing: {
      before: opts.before ?? 0,
      after: opts.after ?? 200,
      line: 360,
      lineRule: "auto",
    },
    children: runs.map((r) => run(r.text, r)),
  });
}

function h1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 360, after: 200 },
    children: [
      run(text, { bold: true, size: 28, font: "Arial" }),
    ],
  });
}

function h2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 280, after: 160 },
    children: [
      run(text, { bold: true, size: 24, font: "Arial" }),
    ],
  });
}

function bullet(text, ref = "bullets") {
  return new Paragraph({
    numbering: { reference: ref, level: 0 },
    alignment: AlignmentType.LEFT,
    spacing: { before: 60, after: 60, line: 320 },
    children: [run(text, { size: 22 })],
  });
}

function caption(text) {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 80, after: 280 },
    children: [run(text, { italics: true, size: 18, color: MUTED })],
  });
}

function screenshot(file, title, width = 240, height = 520) {
  const data = fs.readFileSync(file);
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 200, after: 80 },
      children: [
        new ImageRun({
          type: "png",
          data,
          transformation: { width, height },
          altText: { title, description: title, name: title },
        }),
      ],
    }),
    caption(title),
  ];
}

function simpleTable(headers, rows) {
  const colCount = headers.length;
  const colW = Math.floor(CONTENT_W / colCount);
  const border = { style: BorderStyle.SINGLE, size: 4, color: "999999" };
  const borders = { top: border, bottom: border, left: border, right: border };

  const headerRow = new TableRow({
    children: headers.map(
      (h) =>
        new TableCell({
          borders,
          width: { size: colW, type: WidthType.DXA },
          shading: { fill: "F0F0F0", type: ShadingType.CLEAR },
          margins: { top: 60, bottom: 60, left: 80, right: 80 },
          children: [
            new Paragraph({
              children: [run(h, { bold: true, size: 18, font: "Arial" })],
            }),
          ],
        })
    ),
  });

  const bodyRows = rows.map(
    (row) =>
      new TableRow({
        children: row.map(
          (cell) =>
            new TableCell({
              borders,
              width: { size: colW, type: WidthType.DXA },
              margins: { top: 60, bottom: 60, left: 80, right: 80 },
              children: [
                new Paragraph({
                  spacing: { after: 40 },
                  children: [run(cell, { size: 18 })],
                }),
              ],
            })
        ),
      })
  );

  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: Array(colCount).fill(colW),
    rows: [headerRow, ...bodyRows],
  });
}

async function main() {
  const home = path.join(SHOTS, "extra-instagram-access-111307.png");
  const friction = path.join(SHOTS, "extra-friction-111139.png");
  const gmail = path.join(SHOTS, "01-gmail.png");
  const calendar = path.join(SHOTS, "02-googlecalendar.png");
  const slack = path.join(SHOTS, "03-slack.png");
  const logomark = path.join(TMP, "logomark-light.png");

  const children = [];

  // Title block
  if (fs.existsSync(logomark)) {
    children.push(
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { after: 120 },
        children: [
          new ImageRun({
            type: "png",
            data: fs.readFileSync(logomark),
            transformation: { width: 48, height: 48 },
            altText: {
              title: "KAIROS logomark",
              description: "KAIROS logomark",
              name: "logo",
            },
          }),
        ],
      })
    );
  }

  children.push(
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 80 },
      children: [
        run("KAIROS OS", { bold: true, size: 40, font: "Arial" }),
      ],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 80 },
      children: [
        run("Application Document", { size: 24, font: "Arial", color: MUTED }),
      ],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 360 },
      children: [
        run(
          "Core Functionality · Target Users · Practical Applications · Real-world Value",
          { size: 18, font: "Arial", color: MUTED }
        ),
      ],
    }),

    // Opening thesis
    para(
      "True connection requires the space to think. KAIROS OS embraces the \"Disconnected by Design\" philosophy by replacing the overwhelming grid of apps with a single line of intent, transforming the smartphone from a landscape of distraction into an instrument of narrow, deep, and uncompromised focus.",
      { italics: true, after: 240 }
    ),
    para(
      "In ancient Greek there are two words for time. Chronos is quantitative time: the ticking clock, the endless feed, the hours lost to doomscrolling. Kairos is qualitative time: the right moment, the opportune moment for deliberate action. Modern operating systems trap us in Chronos. KAIROS OS is built to rescue human attention from that loop, so every interaction with the device happens in a moment of Kairos - with intention and purpose."
    ),

    h1("1. What KAIROS OS Is"),
    para(
      "KAIROS OS is an agentic Android launcher. It replaces the traditional app-grid home screen with a minimalist, text-first command interface powered by AI. The phone becomes a thin client: it captures what the user wants, talks to a backend that classifies intent and calls the right tools, and returns either a structured widget in the command stream or a deep link into a native app."
    ),
    para(
      "The product is deliberately uncomfortable at the first unlock. There is no colorful grid doing the thinking for you. There is a blank canvas and a blinking cursor. That blankness is not a missing feature. It is the designed friction that breaks autopilot phone use. The launcher does not make the phone easier to use unintentionally. It makes unintentional use hard."
    ),
    para(
      "Architecturally, the Android app stays light. Complex routing, OAuth, and tool execution live on a Next.js backend connected to AI models and integrations (including MCP-style connectors for services such as Google Workspace). Simple on-device utilities can still be reached quickly. Secrets and tokens stay on the backend, not on the device."
    ),

    ...screenshot(
      home,
      "Figure 1. The KAIROS home screen: time, date, command input, and any running agents - not an icon grid."
    ),

    h1("2. The Problem"),
    para(
      "Modern smartphones are engineered to maximize engagement. Visual bombardment - colorful icons, badge counts, infinite scrolls - triggers dopamine loops and passive consumption. Push notifications shatter focus by pulling users out of whatever they were doing. Social platforms favor broad and shallow connections over narrow and deep ones. Zero-friction access means one tap opens any app, including the ones users later regret opening."
    ),
    para(
      "The result is familiar: hours on the phone without intention, digital fatigue, anxiety, and a gradual erosion of agency over attention. Convenience is treated as an absolute good. KAIROS OS challenges that assumption. Convenience without boundaries becomes capture."
    ),

    h1("3. Core Functionality"),
    para(
      "Everything in KAIROS OS exists to force articulation of intent before the device offers a path. The following capabilities form the core of the product."
    ),

    h2("3.1 Command interface"),
    para(
      "The home screen is a minimal text input with a blinking cursor as the primary interaction surface. Users direct work with an @app mention syntax, similar to attaching connectors in a desktop AI app. Available connections can be selected from a dropdown. Responses appear chronologically in a scrollable stream of interactions. For native utilities that do not need language understanding, a /open command launches the app directly."
    ),
    para(
      "Typical commands look like this:",
      { after: 100 }
    ),
    bullet("@alarm set an alarm for 6am tomorrow morning"),
    bullet("@google-calendar set the meeting for Ms. Tenorio tomorrow at 3pm"),
    bullet("@gmail display my most important emails"),
    bullet("@notes Shopping list for tomorrow: eggs, chicken, rice"),
    bullet("@spotify give me something random to play"),
    para(
      "The point is not that typing is always faster than tapping icons. The point is that typing forces the user to know what they want before the phone can respond.",
      { before: 160 }
    ),

    h2("3.2 AI-powered intent routing"),
    para(
      "Not every prompt should hit a heavy model. The system classifies incoming text into tiers. Structured commands such as /open can be handled by a fast path that bypasses the LLM entirely. Simple conversational intents (for example setting an alarm) go to a lightweight model. Complex tool-calling tasks - querying Gmail, operating Calendar, summarizing Slack - go to a more capable model. Active sessions keep conversation context so follow-ups like \"add that to my notes\" still make sense."
    ),
    para(
      "This tiering is how the product stays usable. Instant utilities should feel instant. Agentic work can take a few seconds. The user should never wait on a large model just to open the flashlight."
    ),

    h2("3.3 Integrations and widgets"),
    para(
      "KAIROS OS connects to Google Workspace apps (Gmail, Calendar, Sheets, Drive), native Android utilities (Clock, Calculator, Camera, Flashlight), and third-party services where APIs or MCP servers exist. Offline-critical tools without an API can be covered with lightweight in-house implementations."
    ),
    para(
      "When the backend answers a tool request, it does not always dump a wall of markdown. It can return structured layout definitions that the Android client renders as inline widgets: email lists, calendar schedules, digests, confirmations. Interactive widgets support actions such as dismiss, snooze, or open in the native app. The governing paradigm is: read via widgets, execute via deep links. Stay in the calm stream to understand; enter the noisy native app only when you choose to act there."
    ),

    ...screenshot(
      gmail,
      "Figure 2. A Gmail command returns a prioritized email widget inside the stream, with optional deep link into Gmail."
    ),
    ...screenshot(
      calendar,
      "Figure 3. A Calendar command surfaces today's schedule as a widget rather than dumping the user into a month grid."
    ),

    h2("3.4 Intent Gate (anti-doomscroll)"),
    para(
      "Supported apps fall into two tiers: Utility and Trap. Utilities (Camera, Calculator, Maps, and similar) open immediately. Trap apps - social feeds and anything the user marks as distracting - refuse a casual open. The system asks for a reason and a time limit before launching. Enforcement uses a foreground overlay or kill-switch so the timebox is real, not decorative."
    ),
    para(
      "Users configure which apps are Utility versus Trap during onboarding and in settings. That flexibility matters: a tool that feels like a prison gets uninstalled. But changes to guardrails are themselves subject to friction - for example a cooling-off period before Strict Mode becomes Free Mode - so a weak late-night impulse cannot instantly erase a clear-minded decision. Onboarding sets that psychological contract early."
    ),

    ...screenshot(
      friction,
      "Figure 4. Intent Gate for Instagram: choose a timebox, state a reason, see remaining leisure budget, then proceed with a mission."
    ),

    h2("3.5 Agentic notification interceptor"),
    para(
      "Outbound friction is not enough if inbound noise still owns the lock screen. KAIROS OS uses Android's Notification Listener Service to capture notifications silently. They are classified into Critical (instant passthrough) and Digest (batched and held). Critical covers phone calls, calendar alerts, and messages from user-defined VIP contacts. Everything else waits until the user asks - for example with a daily digest command. Users define these rules during onboarding. Notifications become a pull request, not a bombardment."
    ),

    h2("3.6 Persistent context anchor"),
    para(
      "When the launcher hands off to a native app, a floating overlay bubble can remain visible. For Trap sessions it can show a mission reminder and countdown. Tapping the bubble reopens the KAIROS command input as an overlay on top of the current app, so the user can issue the next command without navigating back through the home stack. The bubble is the tether: the agentic OS stays present even when the user briefly enters a chaotic native UI."
    ),

    h2("3.7 Onboarding and Deep-Work mode"),
    para(
      "First-time users walk through guided onboarding that explains intentional friction, then configure Trap versus Utility apps, notification tiers, and strict versus free preference. The contract is clear up front: removing guardrails later will take friction."
    ),
    para(
      "A later Enterprise / Deep-Work profile can prioritize business tools, aggregate Slack, Teams, and email into action-oriented briefs, and activate manually or during defined working hours. That mode is the business multiplier: the same command interface becomes a corporate focus layer on company-issued devices."
    ),

    h1("4. Target Users"),
    para(
      "KAIROS OS is not for someone who only wants a prettier launcher. It is for people ready to trade autopilot for agency."
    ),

    h2("4.1 Primary — The Overwhelmed Professional"),
    para(
      "Ages roughly 22 to 40. Knowledge workers, students, and creatives who already know their phone usage is excessive but lack tools that change the habit instead of merely tracking it. Their pain is doomscrolling, notification anxiety, inability to hold deep work, and guilt after unproductive sessions. Their goal is a phone experience that serves them rather than exploits them."
    ),

    h2("4.2 Secondary — The Enterprise User"),
    para(
      "Ages roughly 25 to 45. Employees on company-issued phones who need Workspace and team tools without social media as the default landscape. Their pain is corporate burnout, context-switching overhead, and meeting overload. Their goal is a streamlined command line for email, calendar, and sheets - without the visual noise - and eventually a Deep-Work profile for working hours."
    ),

    h2("4.3 Tertiary — The Digital Minimalist"),
    para(
      "Ages roughly 18 to 35. Privacy-conscious or philosophy-driven users who want dumbphone calm with smartphone capability. Their pain is discomfort with the attention economy. Their goal is a phone that is impossible to use unintentionally."
    ),
    para(
      "The shared thread across all three: they do not need more apps. They need a layer that restores the capacity to choose before the feed chooses for them."
    ),

    h1("5. Practical Applications"),
    para(
      "In daily life, KAIROS OS turns scattered app rituals into deliberate, bounded actions. The following scenarios show how the same command stream covers personal and professional use."
    ),

    h2("5.1 Morning triage"),
    para(
      "Instead of opening Gmail into an unread mountain, the user asks for important emails. The stream returns a prioritized widget (see Figure 2). The user reads what needs a decision, then opens Gmail only if a reply is required - not because a red badge demanded presence."
    ),

    h2("5.2 Protecting the calendar"),
    para(
      "The day appears as a sequence of commitments and focus blocks (see Figure 3). Meetings can be created or inspected by saying them. The user avoids wandering a month grid in search of a single afternoon."
    ),

    h2("5.3 Catching up without drowning"),
    para(
      "Team chat is summarized on request. Channel and DM highlights become an action brief. Re-entering Slack happens with a reason, not because a notification circle said so."
    ),
    ...screenshot(
      slack,
      "Figure 5. Slack highlights summarized into a digest card for intentional follow-up."
    ),

    h2("5.4 Bounded leisure"),
    para(
      "Opening Instagram requires a timebox and a reason - reply to a DM for five minutes, for example. Leisure budgets make the cost of distraction visible before it is spent. When the timer ends, the gate closes. The floating bubble keeps the mission visible while inside the app."
    ),

    h2("5.5 Capture and research without app hopping"),
    para(
      "Notes, browser answers, and background agents live in the same stream. A shopping list or launch plan can be captured without opening another app. Longer research can run as an agent while the user stays present. Search recovers prior conversations and context later."
    ),

    h2("5.6 Deep-work hours on company devices"),
    para(
      "During working hours, Deep-Work mode can silence social noise and prioritize Workspace and team tools. Status updates and CRM-style logging can happen from the command line without falling into an email black hole mid-task. That is the enterprise application of the same intentional interface."
    ),

    h1("6. Real-world Value"),
    para(
      "The value of KAIROS OS is not novelty for its own sake. It is measurable change in how attention is spent."
    ),

    h2("6.1 Reclaiming time from Chronos"),
    para(
      "Open-ended scrolling becomes timeboxed, reason-backed sessions. Leisure budgets turn distraction into a conscious spend instead of an ambient default. Users who want social media still get it - with boundaries they set while thinking clearly."
    ),

    h2("6.2 Narrow and deep work"),
    para(
      "Email, calendar, and chat stay in one cognitive lane. Widgets answer. Deep links open only when execution needs the full native app. Context-switching drops because the home screen no longer advertises fifty other destinations."
    ),

    h2("6.3 Business-ready focus"),
    para(
      "On corporate devices, Deep-Work mode turns the launcher into an executive command line for Workspace and team tools. That cuts burnout from feed-shaped interfaces on company time and supports the usefulness criterion for software that can be used for business."
    ),

    h2("6.4 Privacy-minded architecture"),
    para(
      "The phone remains a thin client for presentation and local utilities. API keys and OAuth tokens live on the backend. Simple tasks can stay on-device when possible. The user gets smartphone capability without putting every secret on the handset."
    ),

    para("The shift in practice can be summarized simply:", {
      before: 120,
      after: 120,
    }),

    simpleTable(
      ["Before (Chronos)", "With KAIROS OS (Kairos)"],
      [
        [
          "Icon grids that invite browsing",
          "A blank slate that demands intent",
        ],
        [
          "Badges and push as constant pull",
          "Digests you request on purpose",
        ],
        [
          "Social apps open in one tap",
          "Trap apps gated by reason and time",
        ],
        [
          "Hours lost without a decision",
          "Actions that feel chosen, not stolen",
        ],
      ]
    ),

    para("", { after: 200 }),

    h1("7. Closing"),
    para(
      "To be human is to choose. KAIROS OS restores that capacity by removing the autopilot of modern UI design. When there are no icons to tap mindlessly, no feeds to scroll by default, and no notifications to react to on impulse, what remains is the user - alone with intention, forced to decide what they truly want from their device."
    ),
    para(
      "Society is the sum of our attention. When that attention is harvested by algorithms optimized for engagement, society becomes shallow. KAIROS OS proposes a different contract with technology: the device serves human intent rather than exploiting human impulse."
    ),
    para(
      "Disconnected by Design. Intent over impulse. Depth over noise.",
      { italics: true, align: AlignmentType.CENTER, before: 200 }
    )
  );

  const doc = new Document({
    styles: {
      default: {
        document: {
          run: { font: "Times New Roman", size: 22 },
        },
      },
      paragraphStyles: [
        {
          id: "Heading1",
          name: "Heading 1",
          basedOn: "Normal",
          next: "Normal",
          quickFormat: true,
          run: { size: 28, bold: true, font: "Arial", color: INK },
          paragraph: {
            spacing: { before: 360, after: 200 },
            outlineLevel: 0,
          },
        },
        {
          id: "Heading2",
          name: "Heading 2",
          basedOn: "Normal",
          next: "Normal",
          quickFormat: true,
          run: { size: 24, bold: true, font: "Arial", color: INK },
          paragraph: {
            spacing: { before: 280, after: 160 },
            outlineLevel: 1,
          },
        },
      ],
    },
    numbering: {
      config: [
        {
          reference: "bullets",
          levels: [
            {
              level: 0,
              format: LevelFormat.BULLET,
              text: "-",
              alignment: AlignmentType.LEFT,
              style: {
                paragraph: { indent: { left: 720, hanging: 360 } },
              },
            },
          ],
        },
      ],
    },
    sections: [
      {
        properties: {
          page: {
            size: { width: 12240, height: 15840 },
            margin: {
              top: 1440,
              right: 1440,
              bottom: 1440,
              left: 1440,
            },
          },
        },
        headers: {
          default: new Header({
            children: [
              new Paragraph({
                tabStops: [
                  { type: TabStopType.RIGHT, position: CONTENT_W },
                ],
                border: {
                  bottom: {
                    style: BorderStyle.SINGLE,
                    size: 6,
                    color: "CCCCCC",
                    space: 8,
                  },
                },
                spacing: { after: 120 },
                children: [
                  run("KAIROS OS", {
                    bold: true,
                    size: 16,
                    font: "Arial",
                  }),
                  run("\t"),
                  run("Application Document", {
                    size: 16,
                    font: "Arial",
                    color: MUTED,
                  }),
                ],
              }),
            ],
          }),
        },
        footers: {
          default: new Footer({
            children: [
              new Paragraph({
                tabStops: [
                  { type: TabStopType.RIGHT, position: CONTENT_W },
                ],
                border: {
                  top: {
                    style: BorderStyle.SINGLE,
                    size: 4,
                    color: "CCCCCC",
                    space: 8,
                  },
                },
                spacing: { before: 80 },
                children: [
                  run("Disconnected by Design", {
                    size: 16,
                    font: "Arial",
                    color: MUTED,
                  }),
                  run("\t"),
                  run("Page ", { size: 16, font: "Arial", color: MUTED }),
                  new TextRun({
                    children: [PageNumber.CURRENT],
                    size: 16,
                    font: "Arial",
                    color: MUTED,
                  }),
                ],
              }),
            ],
          }),
        },
        children,
      },
    ],
  });

  const buffer = await Packer.toBuffer(doc);
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, buffer);
  console.log("Wrote", OUT);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
