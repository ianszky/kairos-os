const PptxGenJS = require("pptxgenjs");
const path = require("path");
const fs = require("fs");

const pres = new PptxGenJS();
pres.defineLayout({ name: "WIDE_16x9", width: 10, height: 5.625 });
pres.layout = "WIDE_16x9";
pres.author = "KaiOS";
pres.title = "KaiOS — FIT × OGIS Disconnected by Design";
pres.subject = "Competition presentation material";

const C = {
  bg: "050505",
  surface: "111111",
  fg: "F5F5F5",
  muted: "8A8A8A",
  border: "333333",
  orange: "FF6B00",
  orangeDim: "331A00",
  black: "000000",
};

const FONT_DISPLAY = "Doto";
const FONT_BODY = "Google Sans";

const ASSETS = path.join(__dirname, "assets");
const ICONS = path.join(ASSETS, "icons");
const SHOTS = path.join("C:/Dev/kairos-os/screenshots/integrations");

const img = (name) => path.join(ASSETS, name);
const icon = (name) => path.join(ICONS, `${name}.png`);
const shot = (name) => path.join(SHOTS, name);

function bg(slide, color = C.bg) {
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0,
    y: 0,
    w: 10,
    h: 5.625,
    fill: { color },
    line: { color },
  });
}

function softGlow(slide) {
  // Ambient orange glow as a soft ellipse behind hero content
  slide.addShape(pres.shapes.OVAL, {
    x: 6.2,
    y: 0.8,
    w: 4.2,
    h: 4.2,
    fill: { color: C.orange, transparency: 92 },
    line: { color: C.orange, transparency: 100 },
  });
}

function footerMark(slide, page, total = 16) {
  slide.addImage({
    path: img("logomark-for-dark.png"),
    x: 0.5,
    y: 5.22,
    w: 0.22,
    h: 0.22,
  });
  slide.addText("KaiOS  ·  FIT × OGIS", {
    x: 0.8,
    y: 5.2,
    w: 6,
    h: 0.28,
    fontFace: FONT_BODY,
    fontSize: 10,
    color: C.muted,
    margin: 0,
    valign: "middle",
  });
  slide.addText(String(page), {
    x: 9.0,
    y: 5.2,
    w: 0.5,
    h: 0.28,
    fontFace: FONT_BODY,
    fontSize: 10,
    color: C.muted,
    align: "right",
    margin: 0,
    valign: "middle",
  });
}

function sectionLabel(slide, text, x = 0.5, y = 0.35) {
  slide.addText(text.toUpperCase(), {
    x,
    y,
    w: 9,
    h: 0.28,
    fontFace: FONT_DISPLAY,
    fontSize: 11,
    color: C.orange,
    charSpacing: 3,
    margin: 0,
  });
}

function slideTitle(slide, text, opts = {}) {
  slide.addText(text, {
    x: opts.x ?? 0.5,
    y: opts.y ?? 0.65,
    w: opts.w ?? 9,
    h: opts.h ?? 0.7,
    fontFace: FONT_DISPLAY,
    fontSize: opts.fontSize ?? 32,
    color: C.fg,
    bold: true,
    margin: 0,
    valign: "top",
  });
}

function card(slide, x, y, w, h) {
  slide.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x,
    y,
    w,
    h,
    fill: { color: C.surface },
    line: { color: C.border, width: 1 },
    rectRadius: 0.1,
  });
}

function iconCircle(slide, iconName, x, y, size = 0.42) {
  slide.addShape(pres.shapes.OVAL, {
    x,
    y,
    w: size,
    h: size,
    fill: { color: C.orangeDim },
    line: { color: C.orange, width: 1 },
  });
  const pad = size * 0.22;
  slide.addImage({
    path: icon(iconName),
    x: x + pad,
    y: y + pad,
    w: size - pad * 2,
    h: size - pad * 2,
  });
}

function phoneFrame(slide, shotPath, x, y, w, h) {
  // Outer device depth
  slide.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: x - 0.04,
    y: y - 0.04,
    w: w + 0.08,
    h: h + 0.08,
    fill: { color: "1A1A1A" },
    line: { color: C.border, width: 1 },
    rectRadius: 0.18,
    shadow: {
      type: "outer",
      color: "000000",
      blur: 18,
      opacity: 0.55,
      offset: 6,
      angle: 180,
    },
  });
  slide.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x,
    y,
    w,
    h,
    fill: { color: C.black },
    line: { color: "222222", width: 1 },
    rectRadius: 0.16,
  });
  slide.addImage({
    path: shotPath,
    x: x + 0.05,
    y: y + 0.05,
    w: w - 0.1,
    h: h - 0.1,
    rounding: { tl: 0.12, tr: 0.12, br: 0.12, bl: 0.12 },
  });
}

// ─────────────────────────────────────────────────────────────
// 1. TITLE
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  softGlow(s);

  s.addImage({
    path: img("logo-for-dark.png"),
    x: 0.55,
    y: 1.35,
    w: 3.6,
    h: 1.0,
  });

  s.addText("Disconnected by Design", {
    x: 0.55,
    y: 2.55,
    w: 5.5,
    h: 0.45,
    fontFace: FONT_DISPLAY,
    fontSize: 22,
    color: C.fg,
    margin: 0,
  });

  s.addText(
    "An agentic Android launcher that replaces the app grid\nwith a single line of intent.",
    {
      x: 0.55,
      y: 3.15,
      w: 5.2,
      h: 0.7,
      fontFace: FONT_BODY,
      fontSize: 14,
      color: C.muted,
      margin: 0,
    }
  );

  s.addText("FIT × OGIS Hackathon  ·  Competition Presentation", {
    x: 0.55,
    y: 4.35,
    w: 5.5,
    h: 0.3,
    fontFace: FONT_BODY,
    fontSize: 12,
    color: C.orange,
    margin: 0,
  });

  // Team placeholder
  s.addText("[ Team names ]", {
    x: 0.55,
    y: 4.75,
    w: 4,
    h: 0.28,
    fontFace: FONT_BODY,
    fontSize: 12,
    color: C.muted,
    italic: true,
    margin: 0,
  });

  phoneFrame(s, shot("screenshot_1.png"), 6.55, 0.55, 2.7, 4.55);
}

// ─────────────────────────────────────────────────────────────
// 2. PROBLEM
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "The Problem");
  slideTitle(s, "Phones were engineered\nfor engagement — not agency.");

  const problems = [
    {
      icon: "alert",
      title: "Visual bombardment",
      body: "Colorful grids, badge counts, and infinite scrolls trigger dopamine loops.",
    },
    {
      icon: "bell",
      title: "Notification overload",
      body: "Push alerts shatter focus by constantly pulling you out of the moment.",
    },
    {
      icon: "bolt",
      title: "Zero-friction access",
      body: "One tap opens any trap. Doomscrolling needs no conscious decision.",
    },
    {
      icon: "eye",
      title: "Broad & shallow",
      body: "Quantity of interactions over quality — loneliness dressed as connection.",
    },
  ];

  problems.forEach((p, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.5 + col * 4.7;
    const y = 2.35 + row * 1.35;
    card(s, x, y, 4.4, 1.2);
    iconCircle(s, p.icon, x + 0.22, y + 0.35);
    s.addText(p.title, {
      x: x + 0.85,
      y: y + 0.25,
      w: 3.3,
      h: 0.35,
      fontFace: FONT_DISPLAY,
      fontSize: 16,
      color: C.fg,
      margin: 0,
    });
    s.addText(p.body, {
      x: x + 0.85,
      y: y + 0.6,
      w: 3.3,
      h: 0.45,
      fontFace: FONT_BODY,
      fontSize: 12,
      color: C.muted,
      margin: 0,
    });
  });

  footerMark(s, 2);
}

// ─────────────────────────────────────────────────────────────
// 3. CHRONOS VS KAIROS
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "The Narrative");
  slideTitle(s, "Two words for time.\nOnly one restores choice.");

  // Chronos card
  card(s, 0.5, 2.3, 4.35, 2.4);
  s.addText("CHRONOS", {
    x: 0.75,
    y: 2.5,
    w: 3.9,
    h: 0.35,
    fontFace: FONT_DISPLAY,
    fontSize: 18,
    color: C.muted,
    charSpacing: 4,
    margin: 0,
  });
  s.addText("khronos  ·  sequential time", {
    x: 0.75,
    y: 2.9,
    w: 3.9,
    h: 0.3,
    fontFace: FONT_BODY,
    fontSize: 13,
    color: C.muted,
    margin: 0,
  });
  s.addText(
    "The endless ticking clock.\nThe infinite scroll.\nHours lost without intention.\nModern OS default mode.",
    {
      x: 0.75,
      y: 3.35,
      w: 3.9,
      h: 1.15,
      fontFace: FONT_BODY,
      fontSize: 14,
      color: C.fg,
      margin: 0,
    }
  );

  // Kairos card
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: 5.15,
    y: 2.3,
    w: 4.35,
    h: 2.4,
    fill: { color: C.surface },
    line: { color: C.orange, width: 1.5 },
    rectRadius: 0.1,
  });
  s.addText("KAIROS", {
    x: 5.4,
    y: 2.5,
    w: 3.9,
    h: 0.35,
    fontFace: FONT_DISPLAY,
    fontSize: 18,
    color: C.orange,
    charSpacing: 4,
    margin: 0,
  });
  s.addText("kairos  ·  the right moment", {
    x: 5.4,
    y: 2.9,
    w: 3.9,
    h: 0.3,
    fontFace: FONT_BODY,
    fontSize: 13,
    color: C.muted,
    margin: 0,
  });
  s.addText(
    "Qualitative time.\nThe opportune instant\nfor deliberate action.\nEvery tap must earn itself.",
    {
      x: 5.4,
      y: 3.35,
      w: 3.9,
      h: 1.15,
      fontFace: FONT_BODY,
      fontSize: 14,
      color: C.fg,
      margin: 0,
    }
  );

  footerMark(s, 3);
}

// ─────────────────────────────────────────────────────────────
// 4. THE IDEA
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  softGlow(s);
  sectionLabel(s, "The Solution");
  slideTitle(s, "Intentional friction,\nby design.");

  s.addText(
    "KaiOS replaces the traditional home screen with a blank canvas and a blinking cursor. To do anything, you must consciously articulate what you want.",
    {
      x: 0.5,
      y: 2.2,
      w: 5.3,
      h: 1.0,
      fontFace: FONT_BODY,
      fontSize: 15,
      color: C.fg,
      margin: 0,
    }
  );

  const cmds = [
    '@alarm set an alarm for 6am tomorrow',
    '@gmail display my most important emails',
    '@instagram /open — reason + 5m limit',
  ];
  cmds.forEach((cmd, i) => {
    const y = 3.35 + i * 0.45;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x: 0.5,
      y,
      w: 5.3,
      h: 0.38,
      fill: { color: C.surface },
      line: { color: C.border, width: 1 },
      rectRadius: 0.06,
    });
    s.addText(`>  ${cmd}`, {
      x: 0.65,
      y,
      w: 5.0,
      h: 0.38,
      fontFace: FONT_DISPLAY,
      fontSize: 11,
      color: i === 2 ? C.orange : C.fg,
      margin: 0,
      valign: "middle",
    });
  });

  phoneFrame(s, shot("screenshot_1.png"), 6.55, 0.7, 2.7, 4.35);
  footerMark(s, 4);
}

// ─────────────────────────────────────────────────────────────
// 5. PRODUCT HERO / THESIS
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Thesis");
  slideTitle(s, "Choose intention,\nnot impulse.", { fontSize: 36 });

  s.addText(
    "True connection requires the space to think. KaiOS transforms the smartphone from a landscape of distraction into an instrument of narrow, deep, and uncompromised focus.",
    {
      x: 0.5,
      y: 2.35,
      w: 5.5,
      h: 1.2,
      fontFace: FONT_BODY,
      fontSize: 16,
      color: C.muted,
      margin: 0,
    }
  );

  const pillars = [
    { n: "01", t: "Blank slate" },
    { n: "02", t: "Stated intent" },
    { n: "03", t: "Deep focus" },
  ];
  pillars.forEach((p, i) => {
    const x = 0.5 + i * 1.85;
    s.addText(p.n, {
      x,
      y: 3.85,
      w: 1.6,
      h: 0.3,
      fontFace: FONT_DISPLAY,
      fontSize: 12,
      color: C.orange,
      margin: 0,
    });
    s.addText(p.t, {
      x,
      y: 4.2,
      w: 1.6,
      h: 0.35,
      fontFace: FONT_BODY,
      fontSize: 14,
      color: C.fg,
      margin: 0,
    });
  });

  phoneFrame(s, shot("extra-friction-111139.png"), 6.55, 0.7, 2.7, 4.35);
  footerMark(s, 5);
}

// ─────────────────────────────────────────────────────────────
// 6. COMMAND INTERFACE
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Core Experience");
  slideTitle(s, "One command line.\nNative widgets in return.", { w: 5.5 });

  const points = [
    {
      t: "@app routing",
      b: "Direct natural-language commands to Gmail, Calendar, Slack, Notion, and more.",
    },
    {
      t: "Server-driven UI",
      b: "Responses render as structured Compose widgets — not chatbot walls of text.",
    },
    {
      t: "Read → Execute",
      b: "Read via widgets. Act via deep links into the real apps you already use.",
    },
  ];
  points.forEach((p, i) => {
    const y = 2.25 + i * 0.95;
    iconCircle(s, ["terminal", "layers", "bolt"][i], 0.5, y + 0.1);
    s.addText(p.t, {
      x: 1.15,
      y,
      w: 4.5,
      h: 0.32,
      fontFace: FONT_DISPLAY,
      fontSize: 16,
      color: C.fg,
      margin: 0,
    });
    s.addText(p.b, {
      x: 1.15,
      y: y + 0.35,
      w: 4.5,
      h: 0.5,
      fontFace: FONT_BODY,
      fontSize: 13,
      color: C.muted,
      margin: 0,
    });
  });

  phoneFrame(s, shot("01-gmail.png"), 6.55, 0.55, 2.7, 4.55);
  footerMark(s, 6);
}

// ─────────────────────────────────────────────────────────────
// 7. INTENT GATE
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Anti-Doomscroll");
  slideTitle(s, "Intent Gate.\nTrap apps earn their time.", { w: 5.5 });

  s.addText(
    "Utility apps open instantly. Trap apps require a reason and a time limit — then KaiOS enforces the contract.",
    {
      x: 0.5,
      y: 2.2,
      w: 5.2,
      h: 0.7,
      fontFace: FONT_BODY,
      fontSize: 14,
      color: C.muted,
      margin: 0,
    }
  );

  const gates = [
    { t: "Utility", b: "Clock, Camera, Maps — immediate access" },
    { t: "Trap", b: "Instagram, TikTok, feeds — gated by reason + timer" },
    { t: "Cooling-off", b: "Guardrail changes wait 12 hours — no impulse undos" },
  ];
  gates.forEach((g, i) => {
    const y = 3.05 + i * 0.6;
    card(s, 0.5, y, 5.2, 0.52);
    s.addText(g.t, {
      x: 0.7,
      y,
      w: 1.5,
      h: 0.52,
      fontFace: FONT_DISPLAY,
      fontSize: 13,
      color: C.orange,
      margin: 0,
      valign: "middle",
    });
    s.addText(g.b, {
      x: 2.3,
      y,
      w: 3.2,
      h: 0.52,
      fontFace: FONT_BODY,
      fontSize: 12,
      color: C.fg,
      margin: 0,
      valign: "middle",
    });
  });

  phoneFrame(s, shot("extra-instagram-access-111307.png"), 6.55, 0.55, 2.7, 4.55);
  footerMark(s, 7);
}

// ─────────────────────────────────────────────────────────────
// 8. NOTIFICATION DIGEST (placeholder for screenshot)
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Attention Hygiene");
  slideTitle(s, "Silence the noise.\nSurface what matters.", { w: 5.5 });

  const notif = [
    {
      icon: "bolt",
      t: "Critical",
      b: "Calls, calendar alerts, VIP contacts — instant passthrough.",
    },
    {
      icon: "bell",
      t: "Digest",
      b: "Everything else is held, summarized by AI, and shown on request.",
    },
    {
      icon: "brain",
      t: "Ask for it",
      b: "@digest daily digest — you choose when to engage.",
    },
  ];
  notif.forEach((n, i) => {
    const y = 2.25 + i * 0.9;
    iconCircle(s, n.icon, 0.5, y + 0.05);
    s.addText(n.t, {
      x: 1.15,
      y,
      w: 4.5,
      h: 0.3,
      fontFace: FONT_DISPLAY,
      fontSize: 16,
      color: C.fg,
      margin: 0,
    });
    s.addText(n.b, {
      x: 1.15,
      y: y + 0.32,
      w: 4.5,
      h: 0.45,
      fontFace: FONT_BODY,
      fontSize: 13,
      color: C.muted,
      margin: 0,
    });
  });

  // Placeholder phone for Ian to drop digest screenshot
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: 6.51,
    y: 0.51,
    w: 2.78,
    h: 4.63,
    fill: { color: "1A1A1A" },
    line: { color: C.border, width: 1 },
    rectRadius: 0.18,
  });
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: 6.55,
    y: 0.55,
    w: 2.7,
    h: 4.55,
    fill: { color: C.surface },
    line: { color: C.orange, width: 1 },
    rectRadius: 0.16,
  });
  s.addImage({
    path: img("logomark-for-dark.png"),
    x: 7.5,
    y: 2.0,
    w: 0.8,
    h: 0.8,
  });
  s.addText("DIGEST\nSCREENSHOT", {
    x: 6.7,
    y: 2.95,
    w: 2.4,
    h: 0.7,
    fontFace: FONT_DISPLAY,
    fontSize: 14,
    color: C.orange,
    align: "center",
    margin: 0,
  });
  s.addText("Drop your capture here", {
    x: 6.7,
    y: 3.7,
    w: 2.4,
    h: 0.35,
    fontFace: FONT_BODY,
    fontSize: 11,
    color: C.muted,
    align: "center",
    margin: 0,
  });

  footerMark(s, 8);
}

// ─────────────────────────────────────────────────────────────
// 9. INTEGRATIONS
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Usefulness");
  slideTitle(s, "Your tools. One intent line.", { h: 0.55 });

  const grid = [
    { file: "01-gmail.png", label: "Gmail" },
    { file: "02-googlecalendar.png", label: "Calendar" },
    { file: "03-slack.png", label: "Slack" },
    { file: "05-notion.png", label: "Notion" },
    { file: "06-github.png", label: "GitHub" },
    { file: "extra-friction-111139.png", label: "Intent Gate" },
  ];

  grid.forEach((g, i) => {
    const col = i % 6;
    const x = 0.4 + col * 1.55;
    const y = 1.55;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x,
      y,
      w: 1.45,
      h: 3.15,
      fill: { color: C.surface },
      line: { color: C.border, width: 1 },
      rectRadius: 0.1,
    });
    s.addImage({
      path: shot(g.file),
      x: x + 0.08,
      y: y + 0.08,
      w: 1.29,
      h: 2.7,
    });
    s.addText(g.label, {
      x,
      y: y + 2.82,
      w: 1.45,
      h: 0.28,
      fontFace: FONT_BODY,
      fontSize: 11,
      color: C.fg,
      align: "center",
      margin: 0,
    });
  });

  footerMark(s, 9);
}

// ─────────────────────────────────────────────────────────────
// 10. ARCHITECTURE OVERVIEW
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Architecture");
  slideTitle(s, "Thin client. Fat backend.\nHybrid intelligence.", { h: 0.7 });

  // Three columns
  const cols = [
    {
      icon: "phone",
      title: "Android Client",
      items: [
        "Kotlin + Jetpack Compose",
        "Command input & widgets",
        "Notification listener",
        "Floating intent bubble",
        "On-device Gemma (AICore)",
      ],
    },
    {
      icon: "server",
      title: "Next.js Backend",
      items: [
        "Intent router & orchestrator",
        "Composio tool execution",
        "Server-driven UI JSON",
        "Session + context store",
        "OAuth token vault",
      ],
    },
    {
      icon: "cloud",
      title: "AI + Integrations",
      items: [
        "Gemini Flash / Pro tiers",
        "Gmail · Calendar · Drive",
        "Slack · Notion · GitHub",
        "Spotify & utilities",
        "MCP / Composio toolkits",
      ],
    },
  ];

  cols.forEach((col, i) => {
    const x = 0.5 + i * 3.15;
    card(s, x, 2.15, 3.0, 2.7);
    iconCircle(s, col.icon, x + 0.25, 2.35);
    s.addText(col.title, {
      x: x + 0.85,
      y: 2.4,
      w: 2.0,
      h: 0.35,
      fontFace: FONT_DISPLAY,
      fontSize: 14,
      color: C.fg,
      margin: 0,
      valign: "middle",
    });
    col.items.forEach((item, j) => {
      s.addText(item, {
        x: x + 0.25,
        y: 3.0 + j * 0.32,
        w: 2.5,
        h: 0.3,
        fontFace: FONT_BODY,
        fontSize: 12,
        color: C.muted,
        margin: 0,
      });
    });
  });

  footerMark(s, 10);
}

// ─────────────────────────────────────────────────────────────
// 11. ARCHITECTURE — TIERED ROUTING DEEP DIVE
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Architecture Deep Dive");
  slideTitle(s, "Tiered routing.\nPrivacy meets performance.", { h: 0.7 });

  const tiers = [
    {
      tier: "T0",
      where: "On device",
      model: "No model",
      use: "/open & heuristics",
      lat: "<200ms",
    },
    {
      tier: "T1",
      where: "On device",
      model: "Gemma via AICore",
      use: "Simple intents, offline",
      lat: "<500ms",
    },
    {
      tier: "T2",
      where: "Cloud",
      model: "Gemini Flash",
      use: "Tool calling & widgets",
      lat: "2–5s",
    },
    {
      tier: "T3",
      where: "Cloud",
      model: "Gemini Pro",
      use: "Deep reasoning",
      lat: "5–10s",
    },
  ];

  // Header row
  const headers = ["Tier", "Where", "Model", "Use case", "Latency"];
  const widths = [0.9, 1.5, 2.2, 2.8, 1.3];
  let hx = 0.5;
  headers.forEach((h, i) => {
    s.addText(h.toUpperCase(), {
      x: hx,
      y: 2.2,
      w: widths[i],
      h: 0.3,
      fontFace: FONT_DISPLAY,
      fontSize: 11,
      color: C.orange,
      charSpacing: 1,
      margin: 0,
    });
    hx += widths[i];
  });

  tiers.forEach((t, i) => {
    const y = 2.6 + i * 0.55;
    card(s, 0.5, y, 9.0, 0.48);
    const vals = [t.tier, t.where, t.model, t.use, t.lat];
    let x = 0.65;
    vals.forEach((v, j) => {
      s.addText(v, {
        x,
        y,
        w: widths[j] - 0.1,
        h: 0.48,
        fontFace: j === 0 ? FONT_DISPLAY : FONT_BODY,
        fontSize: j === 0 ? 14 : 13,
        color: j === 0 ? C.orange : C.fg,
        margin: 0,
        valign: "middle",
      });
      x += widths[j];
    });
  });

  footerMark(s, 11);
}

// ─────────────────────────────────────────────────────────────
// 12. PRIVACY
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Privacy");
  slideTitle(s, "Your data. Your device.\nYour time.", { h: 0.7 });

  const privacy = [
    {
      icon: "chip",
      t: "Local-first intelligence",
      b: "Simple tasks never leave the phone. On-device Gemma classifies and executes offline.",
    },
    {
      icon: "key",
      t: "Tokens stay on the server",
      b: "OAuth tokens and API keys never touch the Android client. Backend vault only.",
    },
    {
      icon: "eye",
      t: "Notifications are ephemeral",
      b: "Payloads are classified, then discarded. Only summaries persist — never the raw stream.",
    },
    {
      icon: "lock",
      t: "Encrypted transport",
      b: "All client ↔ backend traffic over HTTPS. Sandboxed tool processes; no cross-app data bleed.",
    },
  ];

  privacy.forEach((p, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.5 + col * 4.7;
    const y = 2.2 + row * 1.4;
    card(s, x, y, 4.45, 1.25);
    iconCircle(s, p.icon, x + 0.25, y + 0.38);
    s.addText(p.t, {
      x: x + 0.9,
      y: y + 0.25,
      w: 3.3,
      h: 0.35,
      fontFace: FONT_DISPLAY,
      fontSize: 15,
      color: C.fg,
      margin: 0,
    });
    s.addText(p.b, {
      x: x + 0.9,
      y: y + 0.62,
      w: 3.3,
      h: 0.5,
      fontFace: FONT_BODY,
      fontSize: 12,
      color: C.muted,
      margin: 0,
    });
  });

  footerMark(s, 12);
}

// ─────────────────────────────────────────────────────────────
// 13. ENTERPRISE / BUSINESS
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Business Feasibility");
  slideTitle(s, "Deep-Work mode.\nEnterprise-ready focus.", { h: 0.7 });

  s.addText(
    "KaiOS is not only a personal detox - it is a command line for knowledge work.",
    {
      x: 0.5,
      y: 2.15,
      w: 9,
      h: 0.4,
      fontFace: FONT_BODY,
      fontSize: 15,
      color: C.muted,
      margin: 0,
    }
  );

  const biz = [
    {
      icon: "business",
      t: "Workspace native",
      b: "Gmail, Calendar, Drive, Slack, Notion, GitHub — action-oriented briefs instead of tab chaos.",
    },
    {
      icon: "clock",
      t: "Scheduled Deep-Work",
      b: "Activate manually or auto-trigger during working hours. Trap apps stay gated while work tools stay sharp.",
    },
    {
      icon: "building",
      t: "Corporate path",
      b: "Managed profiles, policy-aligned distraction control, and B2B licensing as the distribution model.",
    },
    {
      icon: "shield",
      t: "Feasible stack",
      b: "Composio + Gemini + thin Kotlin client — production integrations without rebuilding OAuth from scratch.",
    },
  ];

  biz.forEach((b, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.5 + col * 4.7;
    const y = 2.7 + row * 1.15;
    card(s, x, y, 4.45, 1.05);
    iconCircle(s, b.icon, x + 0.22, y + 0.3, 0.4);
    s.addText(b.t, {
      x: x + 0.8,
      y: y + 0.18,
      w: 3.4,
      h: 0.3,
      fontFace: FONT_DISPLAY,
      fontSize: 14,
      color: C.fg,
      margin: 0,
    });
    s.addText(b.b, {
      x: x + 0.8,
      y: y + 0.5,
      w: 3.4,
      h: 0.45,
      fontFace: FONT_BODY,
      fontSize: 12,
      color: C.muted,
      margin: 0,
    });
  });

  footerMark(s, 13);
}

// ─────────────────────────────────────────────────────────────
// 14. JUDGING ALIGNMENT
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Why KaiOS Wins");
  slideTitle(s, "Mapped to the brief.", { h: 0.5 });

  const criteria = [
    {
      w: "30%",
      name: "Creativity",
      how: "Chronos vs Kairos narrative · blank-slate UI · intentional friction as product",
    },
    {
      w: "25%",
      name: "Innovativeness",
      how: "Hybrid on-device + cloud AI · MCP/Composio routing · server-driven widgets",
    },
    {
      w: "20%",
      name: "Feasibility",
      how: "Shipped Android launcher · thin client · proven APIs · tiered models",
    },
    {
      w: "15%",
      name: "Usefulness",
      how: "Real Workspace + Slack/Notion/GitHub workflows · Deep-Work enterprise path",
    },
    {
      w: "10%",
      name: "Interest",
      how: "Visceral empty home screen · Intent Gate · floating bubble · daily digest",
    },
  ];

  criteria.forEach((c, i) => {
    const y = 1.45 + i * 0.7;
    card(s, 0.5, y, 9.0, 0.6);
    s.addText(c.w, {
      x: 0.7,
      y,
      w: 0.9,
      h: 0.6,
      fontFace: FONT_DISPLAY,
      fontSize: 18,
      color: C.orange,
      margin: 0,
      valign: "middle",
    });
    s.addText(c.name, {
      x: 1.7,
      y,
      w: 2.2,
      h: 0.6,
      fontFace: FONT_DISPLAY,
      fontSize: 15,
      color: C.fg,
      margin: 0,
      valign: "middle",
    });
    s.addText(c.how, {
      x: 4.0,
      y,
      w: 5.2,
      h: 0.6,
      fontFace: FONT_BODY,
      fontSize: 13,
      color: C.muted,
      margin: 0,
      valign: "middle",
    });
  });

  footerMark(s, 14);
}

// ─────────────────────────────────────────────────────────────
// 15. ON-THEME CHECK
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  sectionLabel(s, "Theme Fit");
  slideTitle(s, "Disconnected by Design —\nevery requirement, met.", { h: 0.7 });

  const fits = [
    {
      t: "Convenience is not benefit",
      b: "One-tap grids are replaced by stated intent.",
    },
    {
      t: "Choose disconnection",
      b: "Blank home screen creates a digital pause.",
    },
    {
      t: "Narrow & deep",
      b: "Targeted commands — no feeds, no suggestions.",
    },
    {
      t: "Less information exposure",
      b: "Notifications batched into digests you request.",
    },
    {
      t: "Everyday discomfort",
      b: "The blinking cursor is the designed friction.",
    },
    {
      t: "Unique · AI-driven · Futuristic",
      b: "Post-app paradigm: intent replaces icons.",
    },
  ];

  fits.forEach((f, i) => {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const x = 0.5 + col * 3.15;
    const y = 2.2 + row * 1.35;
    card(s, x, y, 3.0, 1.2);
    s.addText(f.t, {
      x: x + 0.2,
      y: y + 0.22,
      w: 2.6,
      h: 0.4,
      fontFace: FONT_DISPLAY,
      fontSize: 13,
      color: C.orange,
      margin: 0,
    });
    s.addText(f.b, {
      x: x + 0.2,
      y: y + 0.65,
      w: 2.6,
      h: 0.4,
      fontFace: FONT_BODY,
      fontSize: 12,
      color: C.fg,
      margin: 0,
    });
  });

  footerMark(s, 15);
}

// ─────────────────────────────────────────────────────────────
// 16. CLOSING
// ─────────────────────────────────────────────────────────────
{
  const s = pres.addSlide();
  bg(s);
  softGlow(s);

  s.addImage({
    path: img("logomark-for-dark.png"),
    x: 4.55,
    y: 0.85,
    w: 0.9,
    h: 0.9,
  });

  s.addText("Choose intention,\nnot impulse.", {
    x: 0.5,
    y: 2.0,
    w: 9,
    h: 1.2,
    fontFace: FONT_DISPLAY,
    fontSize: 40,
    color: C.fg,
    align: "center",
    bold: true,
    margin: 0,
  });

  s.addText(
    "KaiOS rescues attention from Chronos —\nso every interaction can happen in Kairos.",
    {
      x: 1.5,
      y: 3.4,
      w: 7,
      h: 0.7,
      fontFace: FONT_BODY,
      fontSize: 16,
      color: C.muted,
      align: "center",
      margin: 0,
    }
  );

  s.addText("FIT × OGIS  ·  Disconnected by Design", {
    x: 0.5,
    y: 4.5,
    w: 9,
    h: 0.3,
    fontFace: FONT_BODY,
    fontSize: 13,
    color: C.orange,
    align: "center",
    margin: 0,
  });

  s.addText("[ Team names ]", {
    x: 0.5,
    y: 4.95,
    w: 9,
    h: 0.28,
    fontFace: FONT_BODY,
    fontSize: 12,
    color: C.muted,
    align: "center",
    italic: true,
    margin: 0,
  });
}

const outPath = path.join(__dirname, "KaiOS_OGIS_Presentation.pptx");
pres
  .writeFile({ fileName: outPath })
  .then(() => {
    console.log("Wrote", outPath);
  })
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
