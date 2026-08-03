/**
 * KAIROS OS Application Document (.docx)
 * Editable Word version of the 6-page application PDF.
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
  PageBreak,
  Table,
  TableRow,
  TableCell,
  VerticalAlign,
  LevelFormat,
  TabStopType,
  TabStopPosition,
} = require("docx");

const ROOT = "C:\\Dev\\kairos-os";
const TMP = path.join(ROOT, "tmp", "pdfs");
const SHOTS = path.join(ROOT, "screenshots", "integrations");
const OUT = path.join(ROOT, "output", "pdf", "KAIROS_OS_Application_Document.docx");

const ORANGE = "FF6B00";
const INK = "0A0A0A";
const MUTED = "6E6E6E";
const SURFACE = "F5F5F5";
const ACCENT_BG = "FFF4EC";
const WHITE = "FFFFFF";
const RULE = "DDDDDD";

const CONTENT_W = 10080; // Letter with 0.75" margins: 12240 - 2160
const MARGIN = 1080; // 0.75"

const thin = { style: BorderStyle.SINGLE, size: 4, color: RULE };
const borders = { top: thin, bottom: thin, left: thin, right: thin };
const noBorder = {
  top: { style: BorderStyle.NONE, size: 0, color: WHITE },
  bottom: { style: BorderStyle.NONE, size: 0, color: WHITE },
  left: { style: BorderStyle.NONE, size: 0, color: WHITE },
  right: { style: BorderStyle.NONE, size: 0, color: WHITE },
};
const orangeLeft = {
  top: thin,
  bottom: thin,
  right: thin,
  left: { style: BorderStyle.SINGLE, size: 24, color: ORANGE },
};

function loadPng(filePath) {
  return fs.readFileSync(filePath);
}

function img(filePath, width, height, title) {
  return new ImageRun({
    type: "png",
    data: loadPng(filePath),
    transformation: { width, height },
    altText: { title, description: title, name: title },
  });
}

function p(text, opts = {}) {
  const {
    bold = false,
    italics = false,
    size = 20,
    color = INK,
    font = "Arial",
    align = AlignmentType.LEFT,
    spacingBefore = 0,
    spacingAfter = 120,
    heading = undefined,
  } = opts;
  return new Paragraph({
    heading,
    alignment: align,
    spacing: { before: spacingBefore, after: spacingAfter, line: 276 },
    children: [
      new TextRun({
        text,
        bold,
        italics,
        size,
        color,
        font,
      }),
    ],
  });
}

function mixed(runs, opts = {}) {
  const {
    align = AlignmentType.LEFT,
    spacingBefore = 0,
    spacingAfter = 120,
    heading = undefined,
  } = opts;
  return new Paragraph({
    heading,
    alignment: align,
    spacing: { before: spacingBefore, after: spacingAfter, line: 276 },
    children: runs.map(
      (r) =>
        new TextRun({
          text: r.text,
          bold: !!r.bold,
          italics: !!r.italics,
          size: r.size || 20,
          color: r.color || INK,
          font: r.font || "Arial",
        })
    ),
  });
}

function sectionLabel(text) {
  return new Paragraph({
    spacing: { before: 0, after: 120 },
    border: {
      bottom: { style: BorderStyle.SINGLE, size: 12, color: ORANGE, space: 4 },
    },
    children: [
      new TextRun({
        text: text.toUpperCase(),
        bold: true,
        size: 16,
        color: ORANGE,
        font: "Arial",
      }),
    ],
  });
}

function heading1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 80, after: 160 },
    children: [
      new TextRun({
        text,
        bold: true,
        size: 36,
        color: INK,
        font: "Georgia",
      }),
    ],
  });
}

function heading2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 200, after: 100 },
    children: [
      new TextRun({
        text,
        bold: true,
        size: 26,
        color: INK,
        font: "Georgia",
      }),
    ],
  });
}

function cellParagraphs(paras) {
  return paras;
}

function cardCell(children, width, fill = SURFACE, cellBorders = borders) {
  return new TableCell({
    borders: cellBorders,
    width: { size: width, type: WidthType.DXA },
    shading: { fill, type: ShadingType.CLEAR },
    margins: { top: 100, bottom: 100, left: 140, right: 140 },
    children: children.length ? children : [new Paragraph({ children: [] })],
  });
}

function featureCard(num, title, body, width) {
  return cardCell(
    [
      p(String(num).padStart(2, "0"), {
        bold: true,
        size: 16,
        color: ORANGE,
        spacingAfter: 40,
      }),
      p(title, {
        bold: true,
        size: 22,
        font: "Georgia",
        spacingAfter: 80,
      }),
      p(body, { size: 17, color: MUTED, spacingAfter: 40 }),
    ],
    width
  );
}

function commandChip(text) {
  return new Paragraph({
    spacing: { before: 40, after: 80 },
    shading: { fill: INK, type: ShadingType.CLEAR },
    children: [
      new TextRun({
        text: `  ${text}  `,
        size: 16,
        color: "FF8A3D",
        font: "Consolas",
      }),
    ],
  });
}

function bullet(text, ref = "bullets") {
  return new Paragraph({
    numbering: { reference: ref, level: 0 },
    spacing: { before: 40, after: 40 },
    children: [
      new TextRun({ text, size: 18, color: INK, font: "Arial" }),
    ],
  });
}

function pageBreak() {
  return new Paragraph({ children: [new PageBreak()] });
}

function phoneImage(file, maxW = 220, maxH = 460, title = "Screenshot") {
  // Keep aspect; phone shots are ~1080x2358
  const aspect = 1080 / 2358;
  let w = maxW;
  let h = Math.round(w / aspect);
  if (h > maxH) {
    h = maxH;
    w = Math.round(h * aspect);
  }
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 80, after: 80 },
    children: [img(file, w, h, title)],
  });
}

async function main() {
  const logomark = path.join(TMP, "logomark-light.png");
  const phoneHome = path.join(TMP, "phone_home.png");
  const phoneFriction = path.join(TMP, "phone_friction.png");
  const phoneGmail = path.join(TMP, "phone_gmail.png");
  const phoneCalendar = path.join(TMP, "phone_calendar.png");
  const phoneSlack = path.join(TMP, "phone_slack.png");

  // Ensure phone assets exist (from PDF prep); fallback to raw shots
  const ensure = (prepared, raw) =>
    fs.existsSync(prepared) ? prepared : raw;

  const home = ensure(phoneHome, path.join(SHOTS, "extra-instagram-access-111307.png"));
  const friction = ensure(phoneFriction, path.join(SHOTS, "extra-friction-111139.png"));
  const gmail = ensure(phoneGmail, path.join(SHOTS, "01-gmail.png"));
  const calendar = ensure(phoneCalendar, path.join(SHOTS, "02-googlecalendar.png"));
  const slack = ensure(phoneSlack, path.join(SHOTS, "03-slack.png"));

  const half = Math.floor((CONTENT_W - 120) / 2);
  const third = Math.floor((CONTENT_W - 240) / 3);
  const quarter = Math.floor((CONTENT_W - 360) / 4);

  const children = [];

  // ========== PAGE 1: VISION ==========
  children.push(
    new Paragraph({
      spacing: { after: 200 },
      children: [
        img(logomark, 36, 36, "KAIROS logomark"),
        new TextRun({ text: "  " }),
        new TextRun({
          text: "KAIROS OS",
          bold: true,
          size: 36,
          font: "Georgia",
          color: INK,
        }),
        new TextRun({ text: "\t" }),
        new TextRun({
          text: "Application Document",
          size: 18,
          color: MUTED,
          font: "Arial",
        }),
      ],
      tabStops: [{ type: TabStopType.RIGHT, position: CONTENT_W }],
    }),
    sectionLabel("Vision"),
    heading1("An operating system for intentional action."),
    p(
      "True connection requires the space to think. KAIROS OS replaces the overwhelming grid of apps with a single line of intent - transforming the smartphone from a landscape of distraction into an instrument of narrow, deep, and uncompromised focus.",
      { italics: true, size: 21, color: MUTED, font: "Georgia", spacingAfter: 200 }
    )
  );

  // App / Solution / Vision cards + phone
  children.push(
    new Table({
      width: { size: CONTENT_W, type: WidthType.DXA },
      columnWidths: [Math.floor(CONTENT_W * 0.55), Math.floor(CONTENT_W * 0.45)],
      rows: [
        new TableRow({
          children: [
            new TableCell({
              borders: noBorder,
              width: { size: Math.floor(CONTENT_W * 0.55), type: WidthType.DXA },
              children: [
                new Table({
                  width: {
                    size: Math.floor(CONTENT_W * 0.55) - 40,
                    type: WidthType.DXA,
                  },
                  columnWidths: [Math.floor(CONTENT_W * 0.55) - 40],
                  rows: [
                    ["APP", "A text-first Android launcher. One cursor. No icon grid. No ambient noise."],
                    [
                      "SOLUTION",
                      "Intentional friction that makes the phone impossible to use unintentionally.",
                    ],
                    [
                      "VISION",
                      "Rescue attention from Chronos - endless scrolling - and restore Kairos: the right moment to act.",
                    ],
                  ].map(
                    ([label, body]) =>
                      new TableRow({
                        children: [
                          cardCell(
                            [
                              p(label, {
                                bold: true,
                                size: 16,
                                color: ORANGE,
                                spacingAfter: 40,
                              }),
                              p(body, { size: 17, color: MUTED, spacingAfter: 40 }),
                            ],
                            Math.floor(CONTENT_W * 0.55) - 40
                          ),
                        ],
                      })
                  ),
                }),
                new Paragraph({
                  spacing: { before: 160, after: 0 },
                  shading: { fill: ACCENT_BG, type: ShadingType.CLEAR },
                  children: [
                    new TextRun({
                      text: "  DISCONNECTED BY DESIGN  ",
                      bold: true,
                      size: 16,
                      color: ORANGE,
                      font: "Arial",
                    }),
                    new TextRun({
                      text: "  Intent over impulse  ·  Depth over noise",
                      size: 16,
                      color: MUTED,
                      font: "Arial",
                    }),
                  ],
                }),
              ],
            }),
            new TableCell({
              borders: noBorder,
              width: { size: Math.floor(CONTENT_W * 0.45), type: WidthType.DXA },
              verticalAlign: VerticalAlign.CENTER,
              children: [phoneImage(home, 200, 420, "KAIROS home screen")],
            }),
          ],
        }),
      ],
    })
  );

  // ========== PAGE 2: CORE FUNCTIONALITY 1 ==========
  children.push(
    pageBreak(),
    sectionLabel("01  ·  Core Functionality"),
    heading1("Command the phone. Don't browse it."),
    p(
      "KAIROS OS centers every interaction on a blinking cursor. You name the app, state the intent, and receive a focused result - as a widget, a deep link, or a quiet confirmation.",
      { size: 20, color: MUTED, spacingAfter: 200 }
    ),
    new Table({
      width: { size: CONTENT_W, type: WidthType.DXA },
      columnWidths: [half, half],
      rows: [
        new TableRow({
          children: [
            featureCard(
              1,
              "Text-first command interface",
              "A blank home screen with one input. Use @app mentions to route work to Gmail, Calendar, Slack, Notes, Spotify, and more - the same mental model as attaching connectors in a desktop AI app.",
              half
            ),
            featureCard(
              2,
              "Tiered AI intent routing",
              "Simple /open utilities dispatch in milliseconds. Lightweight on-device models handle routine intent. Cloud models take complex tool-calling - so the OS feels fast when it should, and smart when it must.",
              half
            ),
          ],
        }),
        new TableRow({
          children: [
            featureCard(
              3,
              "Server-driven widgets",
              "Responses render as interactive cards inside the stream: prioritized emails, today's schedule, digests. Read via widgets. Execute via deep links only when you choose to enter the native app.",
              half
            ),
            featureCard(
              4,
              "Background agents",
              "Delegate longer work - news digests, research, recurring jobs - then stay present. Running agents surface status without pulling you into another feed.",
              half
            ),
          ],
        }),
      ],
    }),
    heading2("How it sounds in practice"),
    commandChip("@gmail show my important emails"),
    commandChip("@googlecalendar show today's schedule"),
    commandChip("@slack summarize unread highlights"),
    commandChip("@alarm set an alarm for 6am tomorrow"),
    heading2("Widgets in the command stream"),
    new Table({
      width: { size: CONTENT_W, type: WidthType.DXA },
      columnWidths: [half, half],
      rows: [
        new TableRow({
          children: [
            new TableCell({
              borders: noBorder,
              width: { size: half, type: WidthType.DXA },
              children: [
                phoneImage(gmail, 210, 400, "Gmail widget"),
                p("Gmail - important emails widget", {
                  size: 15,
                  color: MUTED,
                  align: AlignmentType.CENTER,
                }),
              ],
            }),
            new TableCell({
              borders: noBorder,
              width: { size: half, type: WidthType.DXA },
              children: [
                phoneImage(calendar, 210, 400, "Calendar widget"),
                p("Calendar - today's schedule widget", {
                  size: 15,
                  color: MUTED,
                  align: AlignmentType.CENTER,
                }),
              ],
            }),
          ],
        }),
      ],
    })
  );

  // ========== PAGE 3: CORE FUNCTIONALITY 2 ==========
  children.push(
    pageBreak(),
    sectionLabel("01  ·  Core Functionality"),
    heading1("Guardrails that protect attention."),
    p(
      "Convenience without boundaries becomes capture. KAIROS OS adds deliberate friction where distraction thrives - and stays quiet where urgency is real.",
      { size: 20, color: MUTED, spacingAfter: 200 }
    ),
    new Table({
      width: { size: CONTENT_W, type: WidthType.DXA },
      columnWidths: [Math.floor(CONTENT_W * 0.52), Math.floor(CONTENT_W * 0.48)],
      rows: [
        new TableRow({
          children: [
            new TableCell({
              borders: noBorder,
              width: { size: Math.floor(CONTENT_W * 0.52), type: WidthType.DXA },
              children: [
                p("INTENT GATE", {
                  bold: true,
                  size: 16,
                  color: ORANGE,
                  spacingAfter: 60,
                }),
                p(
                  "Trap apps (Instagram, TikTok, and anything you mark) refuse a casual open. State a reason, pick a timebox, then go in with a mission. Leisure budgets make the cost of distraction visible before you spend it.",
                  { size: 18, color: MUTED, spacingAfter: 160 }
                ),
                p("NOTIFICATION INTERCEPTOR", {
                  bold: true,
                  size: 16,
                  color: ORANGE,
                  spacingAfter: 60,
                }),
                p(
                  "Push noise is captured silently. Critical signals - calls, calendar, VIP contacts - pass through. Everything else waits for an intentional pull: your daily digest, on your terms.",
                  { size: 18, color: MUTED, spacingAfter: 160 }
                ),
                p("PERSISTENT CONTEXT ANCHOR", {
                  bold: true,
                  size: 16,
                  color: ORANGE,
                  spacingAfter: 60,
                }),
                p(
                  "When a native app opens, a floating bubble keeps the mission visible - countdown included - and one tap returns you to the command line without losing the thread.",
                  { size: 18, color: MUTED, spacingAfter: 160 }
                ),
                p("CONFIGURABLE, NOT IMPULSIVE", {
                  bold: true,
                  size: 16,
                  color: ORANGE,
                  spacingAfter: 60,
                }),
                p(
                  "You set Utility vs Trap apps and strictness during onboarding. Softening guardrails later takes friction (cooling-off), so a weak moment can't erase a clear-minded decision.",
                  { size: 18, color: MUTED, spacingAfter: 60 }
                ),
              ],
            }),
            new TableCell({
              borders: noBorder,
              width: { size: Math.floor(CONTENT_W * 0.48), type: WidthType.DXA },
              children: [
                phoneImage(friction, 190, 340, "Intent Gate"),
                phoneImage(home, 190, 340, "Running Agents"),
              ],
            }),
          ],
        }),
      ],
    })
  );

  // ========== PAGE 4: TARGET USERS ==========
  const personas = [
    {
      tag: "PRIMARY",
      title: "The Overwhelmed Professional",
      age: "Ages 22-40",
      who: "Knowledge workers, students, and creatives who know their phone usage is excessive but lack tools that change the habit - not just track it.",
      pain: "Doomscrolling, notification anxiety, broken deep-work blocks, guilt after unproductive sessions.",
      goal: "A phone that serves intention instead of exploiting impulse.",
    },
    {
      tag: "SECONDARY",
      title: "The Enterprise User",
      age: "Ages 25-45",
      who: "Employees on company-issued devices who need Workspace, Slack, and calendars without social media as the default landscape.",
      pain: "Burnout from context-switching, meeting overload, inbox black holes mid-task.",
      goal: "A command line for business tools - and a Deep-Work profile for working hours.",
    },
    {
      tag: "TERTIARY",
      title: "The Digital Minimalist",
      age: "Ages 18-35",
      who: "Privacy-conscious or philosophy-driven users chasing dumbphone calm with smartphone capability.",
      pain: "Discomfort with the attention economy; apps designed to harvest, not help.",
      goal: "A device that is hard to use unintentionally - by design.",
    },
  ];

  children.push(
    pageBreak(),
    sectionLabel("02  ·  Target Users"),
    heading1("Built for people who want their phone back."),
    p(
      "KAIROS OS is not for everyone who wants a prettier launcher. It is for people ready to trade autopilot for agency - professionals, teams, and minimalists who feel the cost of shallow attention.",
      { size: 20, color: MUTED, spacingAfter: 200 }
    )
  );

  for (const persona of personas) {
    children.push(
      new Table({
        width: { size: CONTENT_W, type: WidthType.DXA },
        columnWidths: [CONTENT_W],
        rows: [
          new TableRow({
            children: [
              new TableCell({
                borders: orangeLeft,
                width: { size: CONTENT_W, type: WidthType.DXA },
                shading: { fill: SURFACE, type: ShadingType.CLEAR },
                margins: { top: 120, bottom: 120, left: 160, right: 160 },
                children: [
                  mixed(
                    [
                      {
                        text: persona.tag,
                        bold: true,
                        size: 15,
                        color: ORANGE,
                      },
                      { text: "\t", size: 15 },
                      { text: persona.age, size: 15, color: MUTED },
                    ],
                    {
                      spacingAfter: 60,
                    }
                  ),
                  p(persona.title, {
                    bold: true,
                    size: 26,
                    font: "Georgia",
                    spacingAfter: 100,
                  }),
                  new Table({
                    width: { size: CONTENT_W - 400, type: WidthType.DXA },
                    columnWidths: [
                      Math.floor((CONTENT_W - 400) / 2),
                      Math.floor((CONTENT_W - 400) / 2),
                    ],
                    rows: [
                      new TableRow({
                        children: [
                          cardCell(
                            [
                              p("WHO", {
                                bold: true,
                                size: 14,
                                color: MUTED,
                                spacingAfter: 40,
                              }),
                              p(persona.who, {
                                size: 17,
                                color: MUTED,
                                spacingAfter: 40,
                              }),
                            ],
                            Math.floor((CONTENT_W - 400) / 2),
                            SURFACE,
                            noBorder
                          ),
                          cardCell(
                            [
                              p("PAIN", {
                                bold: true,
                                size: 14,
                                color: MUTED,
                                spacingAfter: 40,
                              }),
                              p(persona.pain, {
                                size: 17,
                                color: MUTED,
                                spacingAfter: 40,
                              }),
                            ],
                            Math.floor((CONTENT_W - 400) / 2),
                            SURFACE,
                            noBorder
                          ),
                        ],
                      }),
                    ],
                  }),
                  mixed(
                    [
                      { text: "GOAL  ", bold: true, size: 14, color: MUTED },
                      {
                        text: persona.goal,
                        italics: true,
                        size: 17,
                        color: INK,
                      },
                    ],
                    { spacingBefore: 80, spacingAfter: 40 }
                  ),
                ],
              }),
            ],
          }),
        ],
      }),
      p("", { spacingAfter: 120 })
    );
  }

  children.push(
    new Paragraph({
      spacing: { before: 80, after: 80 },
      shading: { fill: ACCENT_BG, type: ShadingType.CLEAR },
      children: [
        new TextRun({
          text: "Shared thread across all three: they do not need more apps. They need a layer that restores the capacity to choose - before the feed chooses for them.",
          size: 18,
          color: MUTED,
          font: "Arial",
        }),
      ],
    })
  );

  // ========== PAGE 5: PRACTICAL APPLICATIONS ==========
  const scenarios = [
    {
      title: "Morning triage",
      cmd: "@gmail show my important emails",
      body: "Surface only what needs a decision. Open Gmail when you must reply - not when a badge demands your presence.",
      shot: gmail,
    },
    {
      title: "Protect the calendar",
      cmd: "@googlecalendar show today's schedule",
      body: "See the day as a sequence of commitments and focus blocks. Add meetings by saying them - without wandering the month grid.",
      shot: calendar,
    },
    {
      title: "Catch up without drowning",
      cmd: "@slack summarize unread highlights",
      body: "Get channel and DM highlights as an action brief. Re-enter Slack with a reason, not because the red circle said so.",
      shot: slack,
    },
  ];

  children.push(
    pageBreak(),
    sectionLabel("03  ·  Practical Applications"),
    heading1("A day run by intent, not habit."),
    p(
      "From morning triage to evening wind-down, KAIROS OS turns scattered app rituals into deliberate, bounded actions.",
      { size: 20, color: MUTED, spacingAfter: 200 }
    )
  );

  for (const s of scenarios) {
    children.push(
      new Table({
        width: { size: CONTENT_W, type: WidthType.DXA },
        columnWidths: [Math.floor(CONTENT_W * 0.6), Math.floor(CONTENT_W * 0.4)],
        rows: [
          new TableRow({
            children: [
              new TableCell({
                borders,
                width: { size: Math.floor(CONTENT_W * 0.6), type: WidthType.DXA },
                shading: { fill: SURFACE, type: ShadingType.CLEAR },
                margins: { top: 120, bottom: 120, left: 140, right: 140 },
                children: [
                  p(s.title, {
                    bold: true,
                    size: 24,
                    font: "Georgia",
                    spacingAfter: 80,
                  }),
                  commandChip(s.cmd),
                  p(s.body, { size: 18, color: MUTED, spacingAfter: 40 }),
                ],
              }),
              new TableCell({
                borders,
                width: { size: Math.floor(CONTENT_W * 0.4), type: WidthType.DXA },
                shading: { fill: SURFACE, type: ShadingType.CLEAR },
                verticalAlign: VerticalAlign.CENTER,
                children: [phoneImage(s.shot, 160, 300, s.title)],
              }),
            ],
          }),
        ],
      }),
      p("", { spacingAfter: 100 })
    );
  }

  children.push(
    heading2("Also in the field"),
    new Table({
      width: { size: CONTENT_W, type: WidthType.DXA },
      columnWidths: [quarter, quarter, quarter, quarter],
      rows: [
        new TableRow({
          children: [
            [
              "Bounded leisure",
              "Open Instagram for 5 minutes to reply to a DM - then the gate closes.",
            ],
            [
              "Capture without hop",
              "Drop a shopping list or launch plan into Kai Notes from the same stream.",
            ],
            [
              "Deep-work hours",
              "Enterprise profile prioritizes Workspace and silences social noise on company devices.",
            ],
            [
              "On-demand digest",
              "Pull a daily summary when you are ready - never when an algorithm is.",
            ],
          ].map(([t, b]) =>
            new TableCell({
              borders: noBorder,
              width: { size: quarter, type: WidthType.DXA },
              shading: { fill: INK, type: ShadingType.CLEAR },
              margins: { top: 120, bottom: 120, left: 100, right: 100 },
              children: [
                p(t, {
                  bold: true,
                  size: 16,
                  color: WHITE,
                  font: "Georgia",
                  spacingAfter: 60,
                }),
                p(b, { size: 14, color: "C9C4BC", spacingAfter: 40 }),
              ],
            })
          ),
        }),
      ],
    })
  );

  // ========== PAGE 6: REAL-WORLD VALUE ==========
  const values = [
    [
      "Reclaim time from Chronos",
      "Replace open-ended scrolling with timeboxed, reason-backed sessions. Leisure budgets make distraction a conscious spend - not an ambient default.",
    ],
    [
      "Narrow and deep work",
      "One command stream keeps email, calendar, and chat in a single cognitive lane. Widgets answer; deep links open only when execution requires the full app.",
    ],
    [
      "Business-ready focus",
      "On corporate devices, Deep-Work mode turns the launcher into an executive command line for Workspace and team tools - cutting burnout from feed-shaped interfaces on company time.",
    ],
    [
      "Privacy-minded architecture",
      "The phone stays a thin client for presentation and local utilities. Secrets and OAuth live on the backend. Simple tasks can stay on-device when possible.",
    ],
  ];

  children.push(
    pageBreak(),
    sectionLabel("04  ·  Real-world Value"),
    heading1("Less noise. More agency. Measurable calm."),
    p(
      "KAIROS OS creates value where attention leaks today: fewer context switches, fewer accidental doomscrolls, and faster paths to the one next action that matters.",
      { size: 20, color: MUTED, spacingAfter: 200 }
    ),
    new Table({
      width: { size: CONTENT_W, type: WidthType.DXA },
      columnWidths: [half, half],
      rows: [
        new TableRow({
          children: [
            cardCell(
              [
                p(values[0][0], {
                  bold: true,
                  size: 20,
                  font: "Georgia",
                  spacingAfter: 60,
                }),
                p(values[0][1], { size: 17, color: MUTED, spacingAfter: 40 }),
              ],
              half
            ),
            cardCell(
              [
                p(values[1][0], {
                  bold: true,
                  size: 20,
                  font: "Georgia",
                  spacingAfter: 60,
                }),
                p(values[1][1], { size: 17, color: MUTED, spacingAfter: 40 }),
              ],
              half
            ),
          ],
        }),
        new TableRow({
          children: [
            cardCell(
              [
                p(values[2][0], {
                  bold: true,
                  size: 20,
                  font: "Georgia",
                  spacingAfter: 60,
                }),
                p(values[2][1], { size: 17, color: MUTED, spacingAfter: 40 }),
              ],
              half
            ),
            cardCell(
              [
                p(values[3][0], {
                  bold: true,
                  size: 20,
                  font: "Georgia",
                  spacingAfter: 60,
                }),
                p(values[3][1], { size: 17, color: MUTED, spacingAfter: 40 }),
              ],
              half
            ),
          ],
        }),
      ],
    }),
    heading2("The shift in practice"),
    new Table({
      width: { size: CONTENT_W, type: WidthType.DXA },
      columnWidths: [half, half],
      rows: [
        new TableRow({
          children: [
            cardCell(
              [
                p("BEFORE  ·  CHRONOS", {
                  bold: true,
                  size: 15,
                  color: MUTED,
                  spacingAfter: 80,
                }),
                bullet("Icon grids that invite browsing", "chronos"),
                bullet("Badges and push as constant pull", "chronos"),
                bullet("Social apps open in one tap", "chronos"),
                bullet("Hours lost without a decision", "chronos"),
              ],
              half,
              SURFACE
            ),
            new TableCell({
              borders: {
                top: { style: BorderStyle.SINGLE, size: 12, color: ORANGE },
                bottom: { style: BorderStyle.SINGLE, size: 12, color: ORANGE },
                left: { style: BorderStyle.SINGLE, size: 12, color: ORANGE },
                right: { style: BorderStyle.SINGLE, size: 12, color: ORANGE },
              },
              width: { size: half, type: WidthType.DXA },
              shading: { fill: ACCENT_BG, type: ShadingType.CLEAR },
              margins: { top: 100, bottom: 100, left: 140, right: 140 },
              children: [
                p("WITH KAIROS OS", {
                  bold: true,
                  size: 15,
                  color: ORANGE,
                  spacingAfter: 80,
                }),
                bullet("A blank slate that demands intent", "kairos"),
                bullet("Digests you request on purpose", "kairos"),
                bullet("Trap apps gated by reason + time", "kairos"),
                bullet("Actions that feel chosen, not stolen", "kairos"),
              ],
            }),
          ],
        }),
      ],
    }),
    p("", { spacingAfter: 160 }),
    new Table({
      width: { size: CONTENT_W, type: WidthType.DXA },
      columnWidths: [CONTENT_W],
      rows: [
        new TableRow({
          children: [
            new TableCell({
              borders: noBorder,
              width: { size: CONTENT_W, type: WidthType.DXA },
              shading: { fill: INK, type: ShadingType.CLEAR },
              margins: { top: 160, bottom: 160, left: 180, right: 180 },
              children: [
                p("CLOSING", {
                  bold: true,
                  size: 15,
                  color: ORANGE,
                  spacingAfter: 100,
                }),
                p(
                  "To be human is to choose. KAIROS OS restores that capacity by removing the autopilot of modern UI - so what remains is you, your intention, and the right moment to act.",
                  {
                    italics: true,
                    size: 22,
                    color: WHITE,
                    font: "Georgia",
                    spacingAfter: 120,
                  }
                ),
                p("KAIROS OS  ·  Disconnected by Design  ·  Intent over impulse", {
                  size: 15,
                  color: "FF8A3D",
                  spacingAfter: 40,
                }),
              ],
            }),
          ],
        }),
      ],
    })
  );

  const doc = new Document({
    styles: {
      default: {
        document: {
          run: { font: "Arial", size: 20, color: INK },
        },
      },
      paragraphStyles: [
        {
          id: "Heading1",
          name: "Heading 1",
          basedOn: "Normal",
          next: "Normal",
          quickFormat: true,
          run: { size: 36, bold: true, font: "Georgia", color: INK },
          paragraph: { spacing: { before: 80, after: 160 }, outlineLevel: 0 },
        },
        {
          id: "Heading2",
          name: "Heading 2",
          basedOn: "Normal",
          next: "Normal",
          quickFormat: true,
          run: { size: 26, bold: true, font: "Georgia", color: INK },
          paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 1 },
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
        {
          reference: "chronos",
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
        {
          reference: "kairos",
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
              top: MARGIN,
              right: MARGIN,
              bottom: MARGIN,
              left: MARGIN,
            },
          },
        },
        headers: {
          default: new Header({
            children: [
              new Paragraph({
                border: {
                  bottom: {
                    style: BorderStyle.SINGLE,
                    size: 6,
                    color: ORANGE,
                    space: 8,
                  },
                },
                spacing: { after: 120 },
                children: [
                  new TextRun({
                    text: "KAIROS OS",
                    bold: true,
                    size: 16,
                    color: INK,
                    font: "Arial",
                  }),
                  new TextRun({
                    text: "  ·  Application Document",
                    size: 16,
                    color: MUTED,
                    font: "Arial",
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
                border: {
                  top: {
                    style: BorderStyle.SINGLE,
                    size: 4,
                    color: RULE,
                    space: 8,
                  },
                },
                spacing: { before: 80 },
                tabStops: [{ type: TabStopType.RIGHT, position: CONTENT_W }],
                children: [
                  new TextRun({
                    text: "Disconnected by Design",
                    size: 14,
                    color: MUTED,
                    font: "Arial",
                  }),
                  new TextRun({ text: "\t" }),
                  new TextRun({
                    text: "Page ",
                    size: 14,
                    color: MUTED,
                    font: "Arial",
                  }),
                  new TextRun({
                    children: [PageNumber.CURRENT],
                    size: 14,
                    color: MUTED,
                    font: "Arial",
                  }),
                  new TextRun({
                    text: " / ",
                    size: 14,
                    color: MUTED,
                    font: "Arial",
                  }),
                  new TextRun({
                    children: [PageNumber.TOTAL_PAGES],
                    size: 14,
                    color: MUTED,
                    font: "Arial",
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
