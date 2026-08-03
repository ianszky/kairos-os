#!/usr/bin/env python3
"""Generate the 6-page KAIROS OS application document PDF."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw
from reportlab.lib.colors import Color, HexColor, white
from reportlab.lib.pagesizes import letter
from reportlab.lib.units import inch
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

ROOT = Path(r"C:\Dev\kairos-os")
SHOTS = ROOT / "screenshots" / "integrations"
TMP = ROOT / "tmp" / "pdfs"
OUT = ROOT / "output" / "pdf" / "KAIROS_OS_Application_Document.pdf"

# Brand
ORANGE = HexColor("#FF6B00")
ORANGE_SOFT = HexColor("#FF8A3D")
INK = HexColor("#0A0A0A")
INK_SOFT = HexColor("#3A3A3A")
MUTED = HexColor("#6E6E6E")
RULE = HexColor("#DDDDDD")
PAPER = HexColor("#FFFFFF")
SURFACE = HexColor("#F5F5F5")
SURFACE_DARK = HexColor("#111111")
ACCENT_BG = HexColor("#FFF4EC")

PAGE_W, PAGE_H = letter  # 612 x 792
MARGIN_X = 0.65 * inch
MARGIN_TOP = 0.55 * inch
MARGIN_BOTTOM = 0.55 * inch
CONTENT_W = PAGE_W - 2 * MARGIN_X


def register_fonts() -> None:
    fonts = Path(r"C:\Windows\Fonts")
    pdfmetrics.registerFont(TTFont("Georgia", str(fonts / "georgia.ttf")))
    pdfmetrics.registerFont(TTFont("Georgia-Bold", str(fonts / "georgiab.ttf")))
    pdfmetrics.registerFont(TTFont("Georgia-Italic", str(fonts / "georgiai.ttf")))
    pdfmetrics.registerFont(TTFont("Segoe", str(fonts / "segoeui.ttf")))
    pdfmetrics.registerFont(TTFont("Segoe-Bold", str(fonts / "segoeuib.ttf")))
    pdfmetrics.registerFont(TTFont("Segoe-Italic", str(fonts / "segoeuii.ttf")))
    pdfmetrics.registerFont(TTFont("Segoe-Light", str(fonts / "segoeuil.ttf")))
    pdfmetrics.registerFont(TTFont("Consolas", str(fonts / "consola.ttf")))


def round_phone(src: Path, dest: Path, max_h: int = 1800, radius: int = 72) -> Path:
    """Crop status-bar-friendly phone shot and apply rounded mask."""
    im = Image.open(src).convert("RGBA")
    # Keep full frame; just scale
    if im.height > max_h:
        ratio = max_h / im.height
        im = im.resize((int(im.width * ratio), max_h), Image.Resampling.LANCZOS)
    w, h = im.size
    mask = Image.new("L", (w, h), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, w - 1, h - 1), radius=radius, fill=255)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask=mask)
    # Thin dark bezel
    bezel = Image.new("RGBA", (w + 16, h + 16), (0, 0, 0, 0))
    bdraw = ImageDraw.Draw(bezel)
    bdraw.rounded_rectangle((0, 0, w + 15, h + 15), radius=radius + 6, fill=(26, 24, 22, 255))
    bezel.paste(out, (8, 8), out)
    bezel.save(dest)
    return dest


def prepare_assets() -> dict[str, Path]:
    TMP.mkdir(parents=True, exist_ok=True)
    mapping = {
        "home": SHOTS / "extra-instagram-access-111307.png",
        "friction": SHOTS / "extra-friction-111139.png",
        "gmail": SHOTS / "01-gmail.png",
        "calendar": SHOTS / "02-googlecalendar.png",
        "slack": SHOTS / "03-slack.png",
        "promo_home": SHOTS / "screenshot_1.png",
        "promo_answers": SHOTS / "screenshot_2.png",
        "promo_agents": SHOTS / "screenshot_3.png",
        "promo_friction": SHOTS / "screenshot_8.png",
        "promo_notes": SHOTS / "screenshot_6.png",
        "promo_search": SHOTS / "screenshot_7.png",
        "logo": TMP / "logo-light.png",
        "logomark": TMP / "logomark-light.png",
        "wordmark": TMP / "wordmark-light.png",
    }
    phones = {}
    for key in ("home", "friction", "gmail", "calendar", "slack"):
        phones[key] = round_phone(mapping[key], TMP / f"phone_{key}.png", max_h=1600, radius=70)
    # Promo shots already framed; lightly shrink copies
    for key in (
        "promo_home",
        "promo_answers",
        "promo_agents",
        "promo_friction",
        "promo_notes",
        "promo_search",
    ):
        im = Image.open(mapping[key]).convert("RGBA")
        if im.height > 2000:
            ratio = 2000 / im.height
            im = im.resize((int(im.width * ratio), 2000), Image.Resampling.LANCZOS)
        dest = TMP / f"{key}.png"
        im.save(dest)
        phones[key] = dest
    phones["logo"] = mapping["logo"]
    phones["logomark"] = mapping["logomark"]
    phones["wordmark"] = mapping["wordmark"]
    # Prefer blank home asset if it includes the command bar; else use live home
    blank = TMP / "phone_blank_home.png"
    phones["blank"] = phones["home"]
    return phones


def wrap_text(c: canvas.Canvas, text: str, font: str, size: float, max_width: float) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        trial = word if not current else f"{current} {word}"
        if c.stringWidth(trial, font, size) <= max_width:
            current = trial
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_wrapped(
    c: canvas.Canvas,
    text: str,
    x: float,
    y: float,
    font: str,
    size: float,
    max_width: float,
    color: Color,
    leading: float | None = None,
    align: str = "left",
) -> float:
    leading = leading or size * 1.35
    lines = wrap_text(c, text, font, size, max_width)
    for line in lines:
        c.setFillColor(color)
        c.setFont(font, size)
        if align == "center":
            c.drawCentredString(x, y, line)
        elif align == "right":
            c.drawRightString(x, y, line)
        else:
            c.drawString(x, y, line)
        y -= leading
    return y


def draw_footer(c: canvas.Canvas, page: int, total: int = 6) -> None:
    y = 0.32 * inch
    c.setStrokeColor(RULE)
    c.setLineWidth(0.6)
    c.line(MARGIN_X, y + 12, PAGE_W - MARGIN_X, y + 12)
    c.setFillColor(MUTED)
    c.setFont("Segoe", 8)
    c.drawString(MARGIN_X, y, "KAIROS OS  ·  Application Document")
    c.drawRightString(PAGE_W - MARGIN_X, y, f"{page} / {total}")


def draw_section_label(c: canvas.Canvas, label: str, x: float, y: float) -> float:
    c.setFillColor(ORANGE)
    c.setFont("Segoe-Bold", 8.5)
    c.drawString(x, y, label.upper())
    # Underline accent
    w = c.stringWidth(label.upper(), "Segoe-Bold", 8.5)
    c.setStrokeColor(ORANGE)
    c.setLineWidth(1.2)
    c.line(x, y - 3, x + min(w, 48), y - 3)
    return y - 18


def draw_rounded_rect(
    c: canvas.Canvas,
    x: float,
    y: float,
    w: float,
    h: float,
    radius: float,
    fill: Color | None = None,
    stroke: Color | None = None,
    stroke_width: float = 0.8,
) -> None:
    c.saveState()
    if fill:
        c.setFillColor(fill)
    if stroke:
        c.setStrokeColor(stroke)
        c.setLineWidth(stroke_width)
    p = c.beginPath()
    p.moveTo(x + radius, y)
    p.lineTo(x + w - radius, y)
    p.arcTo(x + w - 2 * radius, y, x + w, y + 2 * radius, -90, 90)
    p.lineTo(x + w, y + h - radius)
    p.arcTo(x + w - 2 * radius, y + h - 2 * radius, x + w, y + h, 0, 90)
    p.lineTo(x + radius, y + h)
    p.arcTo(x, y + h - 2 * radius, x + 2 * radius, y + h, 90, 90)
    p.lineTo(x, y + radius)
    p.arcTo(x, y, x + 2 * radius, y + 2 * radius, 180, 90)
    p.close()
    if fill and stroke:
        c.drawPath(p, fill=1, stroke=1)
    elif fill:
        c.drawPath(p, fill=1, stroke=0)
    else:
        c.drawPath(p, fill=0, stroke=1)
    c.restoreState()


def draw_image_contain(
    c: canvas.Canvas, path: Path, x: float, y: float, max_w: float, max_h: float
) -> tuple[float, float]:
    im = Image.open(path)
    iw, ih = im.size
    scale = min(max_w / iw, max_h / ih)
    w, h = iw * scale, ih * scale
    c.drawImage(
        str(path),
        x,
        y,
        width=w,
        height=h,
        mask="auto",
        preserveAspectRatio=True,
        anchor="c",
    )
    # reportlab drawImage with anchor c places center at x,y - we want bottom-left
    # Fix: use bottom-left placement explicitly
    return w, h


def draw_image_bl(
    c: canvas.Canvas, path: Path, x: float, y: float, max_w: float, max_h: float
) -> tuple[float, float]:
    im = Image.open(path)
    iw, ih = im.size
    scale = min(max_w / iw, max_h / ih)
    w, h = iw * scale, ih * scale
    c.drawImage(str(path), x, y, width=w, height=h, mask="auto", preserveAspectRatio=True)
    return w, h


# ---------------------------------------------------------------------------
# Pages
# ---------------------------------------------------------------------------


def page_cover(c: canvas.Canvas, assets: dict[str, Path]) -> None:
    # Soft paper wash
    c.setFillColor(PAPER)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    # Top accent band
    c.setFillColor(INK)
    c.rect(0, PAGE_H - 0.18 * inch, PAGE_W, 0.18 * inch, fill=1, stroke=0)
    c.setFillColor(ORANGE)
    c.rect(0, PAGE_H - 0.22 * inch, PAGE_W, 0.04 * inch, fill=1, stroke=0)

    # Logo + wordmark
    logo_y = PAGE_H - MARGIN_TOP - 0.55 * inch
    draw_image_bl(c, assets["logomark"], MARGIN_X, logo_y + 0.02 * inch, 0.4 * inch, 0.4 * inch)
    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 22)
    c.drawString(MARGIN_X + 0.52 * inch, logo_y + 0.14 * inch, "KAIROS OS")
    c.setFillColor(MUTED)
    c.setFont("Segoe", 9)
    c.drawRightString(PAGE_W - MARGIN_X, logo_y + 0.18 * inch, "Application Document")

    # Eyebrow
    y = logo_y - 0.35 * inch
    y = draw_section_label(c, "Vision", MARGIN_X, y)

    # Hero headline
    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 28)
    lines = wrap_text(
        c,
        "An operating system for intentional action.",
        "Georgia-Bold",
        28,
        CONTENT_W * 0.62,
    )
    for line in lines:
        c.drawString(MARGIN_X, y, line)
        y -= 34

    y -= 4
    y = draw_wrapped(
        c,
        "True connection requires the space to think. KAIROS OS replaces the overwhelming "
        "grid of apps with a single line of intent - transforming the smartphone from a "
        "landscape of distraction into an instrument of narrow, deep, and uncompromised focus.",
        MARGIN_X,
        y,
        "Georgia-Italic",
        11,
        CONTENT_W * 0.58,
        INK_SOFT,
        leading=15.5,
    )

    # Right hero phone - blank slate home
    phone_max_h = 5.45 * inch
    phone_max_w = 2.4 * inch
    phone_x = PAGE_W - MARGIN_X - phone_max_w
    phone_y = 1.0 * inch
    draw_image_bl(c, assets["blank"], phone_x, phone_y, phone_max_w, phone_max_h)

    # Left column pillars
    y -= 18
    pillars = [
        ("App", "A text-first Android launcher. One cursor. No icon grid. No ambient noise."),
        ("Solution", "Intentional friction that makes the phone impossible to use unintentionally."),
        ("Vision", "Rescue attention from Chronos - endless scrolling - and restore Kairos: the right moment to act."),
    ]
    card_w = CONTENT_W * 0.55
    for title, body in pillars:
        draw_rounded_rect(c, MARGIN_X, y - 58, card_w, 62, 8, fill=SURFACE, stroke=RULE)
        c.setFillColor(ORANGE)
        c.setFont("Segoe-Bold", 9)
        c.drawString(MARGIN_X + 12, y - 16, title.upper())
        draw_wrapped(
            c,
            body,
            MARGIN_X + 12,
            y - 32,
            "Segoe",
            8.5,
            card_w - 24,
            INK_SOFT,
            leading=11.5,
        )
        y -= 74

    # Bottom strip
    draw_rounded_rect(
        c, MARGIN_X, 0.55 * inch, card_w, 0.38 * inch, 6, fill=ACCENT_BG, stroke=None
    )
    c.setFillColor(ORANGE)
    c.setFont("Segoe-Bold", 8)
    c.drawString(MARGIN_X + 12, 0.68 * inch, "DISCONNECTED BY DESIGN")
    c.setFillColor(INK_SOFT)
    c.setFont("Segoe", 8)
    c.drawString(MARGIN_X + 145, 0.68 * inch, "Intent over impulse  ·  Depth over noise")

    draw_footer(c, 1)


def page_core_functionality_1(c: canvas.Canvas, assets: dict[str, Path]) -> None:
    c.setFillColor(PAPER)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    y = PAGE_H - MARGIN_TOP
    y = draw_section_label(c, "01  ·  Core Functionality", MARGIN_X, y)

    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 22)
    c.drawString(MARGIN_X, y, "Command the phone. Don't browse it.")
    y -= 22
    y = draw_wrapped(
        c,
        "KAIROS OS centers every interaction on a blinking cursor. You name the app, "
        "state the intent, and receive a focused result - as a widget, a deep link, "
        "or a quiet confirmation.",
        MARGIN_X,
        y,
        "Segoe",
        10,
        CONTENT_W,
        INK_SOFT,
        leading=13.5,
    )
    y -= 14

    # Feature grid - 2x2
    features = [
        (
            "Text-first command interface",
            "A blank home screen with one input. Use @app mentions to route work to Gmail, "
            "Calendar, Slack, Notes, Spotify, and more - the same mental model as attaching "
            "connectors in a desktop AI app.",
        ),
        (
            "Tiered AI intent routing",
            "Simple /open utilities dispatch in milliseconds. Lightweight on-device models "
            "handle routine intent. Cloud models take complex tool-calling - so the OS feels "
            "fast when it should, and smart when it must.",
        ),
        (
            "Server-driven widgets",
            "Responses render as interactive cards inside the stream: prioritized emails, "
            "today's schedule, digests. Read via widgets. Execute via deep links only when "
            "you choose to enter the native app.",
        ),
        (
            "Background agents",
            "Delegate longer work - news digests, research, recurring jobs - then stay present. "
            "Running agents surface status without pulling you into another feed.",
        ),
    ]

    col_w = (CONTENT_W - 14) / 2
    row_h = 1.35 * inch
    start_y = y
    for i, (title, body) in enumerate(features):
        col = i % 2
        row = i // 2
        x = MARGIN_X + col * (col_w + 14)
        cy = start_y - row * (row_h + 10) - row_h
        draw_rounded_rect(c, x, cy, col_w, row_h, 9, fill=SURFACE, stroke=RULE)
        c.setFillColor(ORANGE)
        c.setFont("Segoe-Bold", 8)
        c.drawString(x + 12, cy + row_h - 18, f"0{i + 1}")
        c.setFillColor(INK)
        c.setFont("Georgia-Bold", 11)
        c.drawString(x + 12, cy + row_h - 36, title)
        draw_wrapped(
            c,
            body,
            x + 12,
            cy + row_h - 54,
            "Segoe",
            8.2,
            col_w - 24,
            INK_SOFT,
            leading=11.2,
        )

    y = start_y - 2 * (row_h + 10) - 8

    # Example commands strip
    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 11)
    c.drawString(MARGIN_X, y, "How it sounds in practice")
    y -= 14

    examples = [
        '@gmail show my important emails',
        '@googlecalendar show today\'s schedule',
        '@slack summarize unread highlights',
        '@alarm set an alarm for 6am tomorrow',
    ]
    chip_gap = 8
    chip_h = 22
    x = MARGIN_X
    for ex in examples:
        tw = c.stringWidth(ex, "Consolas", 7.5) + 16
        if x + tw > PAGE_W - MARGIN_X:
            x = MARGIN_X
            y -= chip_h + chip_gap
        draw_rounded_rect(c, x, y - 6, tw, chip_h, 5, fill=INK, stroke=None)
        c.setFillColor(ORANGE_SOFT)
        c.setFont("Consolas", 7.5)
        c.drawString(x + 8, y + 1, ex)
        x += tw + chip_gap

    y -= 36

    # Two phone screenshots
    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 11)
    c.drawString(MARGIN_X, y, "Widgets in the command stream")
    y -= 10

    phone_h = min(y - 0.72 * inch, 3.0 * inch)
    phone_w = (CONTENT_W - 20) / 2
    base_y = 0.62 * inch
    draw_image_bl(c, assets["gmail"], MARGIN_X, base_y, phone_w, phone_h)
    draw_image_bl(
        c, assets["calendar"], MARGIN_X + phone_w + 20, base_y, phone_w, phone_h
    )

    draw_footer(c, 2)


def page_core_functionality_2(c: canvas.Canvas, assets: dict[str, Path]) -> None:
    c.setFillColor(PAPER)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    y = PAGE_H - MARGIN_TOP
    y = draw_section_label(c, "01  ·  Core Functionality", MARGIN_X, y)

    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 22)
    c.drawString(MARGIN_X, y, "Guardrails that protect attention.")
    y -= 22
    y = draw_wrapped(
        c,
        "Convenience without boundaries becomes capture. KAIROS OS adds deliberate friction "
        "where distraction thrives - and stays quiet where urgency is real.",
        MARGIN_X,
        y,
        "Segoe",
        10,
        CONTENT_W * 0.92,
        INK_SOFT,
        leading=13.5,
    )
    y -= 16

    # Left text columns, right phones
    left_w = CONTENT_W * 0.48
    blocks = [
        (
            "Intent Gate",
            "Trap apps (Instagram, TikTok, and anything you mark) refuse a casual open. "
            "State a reason, pick a timebox, then go in with a mission. Leisure budgets "
            "make the cost of distraction visible before you spend it.",
        ),
        (
            "Notification interceptor",
            "Push noise is captured silently. Critical signals - calls, calendar, VIP "
            "contacts - pass through. Everything else waits for an intentional pull: "
            "your daily digest, on your terms.",
        ),
        (
            "Persistent context anchor",
            "When a native app opens, a floating bubble keeps the mission visible - "
            "countdown included - and one tap returns you to the command line without "
            "losing the thread.",
        ),
        (
            "Configurable, not impulsive",
            "You set Utility vs Trap apps and strictness during onboarding. Softening "
            "guardrails later takes friction (cooling-off), so a weak moment can't erase "
            "a clear-minded decision.",
        ),
    ]

    block_top = y
    for title, body in blocks:
        c.setFillColor(ORANGE)
        c.setFont("Segoe-Bold", 8.5)
        c.drawString(MARGIN_X, y, title.upper())
        y -= 14
        y = draw_wrapped(
            c, body, MARGIN_X, y, "Segoe", 8.5, left_w - 8, INK_SOFT, leading=11.5
        )
        y -= 12

    # Phones on right
    phone_w = CONTENT_W * 0.46
    phone_x = MARGIN_X + left_w + 14
    avail_h = block_top - 0.55 * inch
    each_h = (avail_h - 12) / 2
    draw_image_bl(c, assets["friction"], phone_x, 0.55 * inch + each_h + 12, phone_w, each_h)
    draw_image_bl(c, assets["home"], phone_x, 0.55 * inch, phone_w, each_h)

    draw_footer(c, 3)


def page_target_users(c: canvas.Canvas, assets: dict[str, Path]) -> None:
    c.setFillColor(PAPER)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    y = PAGE_H - MARGIN_TOP
    y = draw_section_label(c, "02  ·  Target Users", MARGIN_X, y)

    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 22)
    c.drawString(MARGIN_X, y, "Built for people who want their phone back.")
    y -= 22
    y = draw_wrapped(
        c,
        "KAIROS OS is not for everyone who wants a prettier launcher. It is for people "
        "ready to trade autopilot for agency - professionals, teams, and minimalists "
        "who feel the cost of shallow attention.",
        MARGIN_X,
        y,
        "Segoe",
        10,
        CONTENT_W,
        INK_SOFT,
        leading=13.5,
    )
    y -= 18

    personas = [
        {
            "tag": "Primary",
            "title": "The Overwhelmed Professional",
            "age": "Ages 22-40",
            "who": "Knowledge workers, students, and creatives who know their phone usage is excessive but lack tools that change the habit - not just track it.",
            "pain": "Doomscrolling, notification anxiety, broken deep-work blocks, guilt after unproductive sessions.",
            "goal": "A phone that serves intention instead of exploiting impulse.",
        },
        {
            "tag": "Secondary",
            "title": "The Enterprise User",
            "age": "Ages 25-45",
            "who": "Employees on company-issued devices who need Workspace, Slack, and calendars without social media as the default landscape.",
            "pain": "Burnout from context-switching, meeting overload, inbox black holes mid-task.",
            "goal": "A command line for business tools - and a Deep-Work profile for working hours.",
        },
        {
            "tag": "Tertiary",
            "title": "The Digital Minimalist",
            "age": "Ages 18-35",
            "who": "Privacy-conscious or philosophy-driven users chasing dumbphone calm with smartphone capability.",
            "pain": "Discomfort with the attention economy; apps designed to harvest, not help.",
            "goal": "A device that is hard to use unintentionally - by design.",
        },
    ]

    card_h = 2.05 * inch
    gap = 12
    for p in personas:
        draw_rounded_rect(c, MARGIN_X, y - card_h, CONTENT_W, card_h, 10, fill=SURFACE, stroke=RULE)
        # Accent bar
        c.setFillColor(ORANGE)
        c.rect(MARGIN_X, y - card_h, 4, card_h, fill=1, stroke=0)

        c.setFillColor(ORANGE)
        c.setFont("Segoe-Bold", 8)
        c.drawString(MARGIN_X + 16, y - 18, p["tag"].upper())
        c.setFillColor(MUTED)
        c.setFont("Segoe", 8)
        c.drawRightString(PAGE_W - MARGIN_X - 14, y - 18, p["age"])

        c.setFillColor(INK)
        c.setFont("Georgia-Bold", 14)
        c.drawString(MARGIN_X + 16, y - 38, p["title"])

        col1_x = MARGIN_X + 16
        col2_x = MARGIN_X + CONTENT_W * 0.52
        col_w = CONTENT_W * 0.42

        c.setFillColor(MUTED)
        c.setFont("Segoe-Bold", 7.5)
        c.drawString(col1_x, y - 58, "WHO")
        draw_wrapped(c, p["who"], col1_x, y - 72, "Segoe", 8.2, col_w, INK_SOFT, leading=11)

        c.setFillColor(MUTED)
        c.setFont("Segoe-Bold", 7.5)
        c.drawString(col2_x, y - 58, "PAIN")
        draw_wrapped(c, p["pain"], col2_x, y - 72, "Segoe", 8.2, col_w, INK_SOFT, leading=11)

        c.setFillColor(MUTED)
        c.setFont("Segoe-Bold", 7.5)
        c.drawString(col1_x, y - card_h + 28, "GOAL")
        c.setFillColor(INK)
        c.setFont("Segoe-Italic", 8.5)
        c.drawString(col1_x + 36, y - card_h + 28, p["goal"])

        y -= card_h + gap

    # Bottom note
    draw_rounded_rect(c, MARGIN_X, 0.52 * inch, CONTENT_W, 0.55 * inch, 8, fill=ACCENT_BG)
    c.setFillColor(INK)
    c.setFont("Segoe", 8.5)
    draw_wrapped(
        c,
        "Shared thread across all three: they do not need more apps. They need a layer "
        "that restores the capacity to choose - before the feed chooses for them.",
        MARGIN_X + 14,
        0.78 * inch,
        "Segoe",
        8.5,
        CONTENT_W - 28,
        INK_SOFT,
        leading=11.5,
    )

    draw_footer(c, 4)


def page_practical_applications(c: canvas.Canvas, assets: dict[str, Path]) -> None:
    c.setFillColor(PAPER)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    y = PAGE_H - MARGIN_TOP
    y = draw_section_label(c, "03  ·  Practical Applications", MARGIN_X, y)

    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 22)
    c.drawString(MARGIN_X, y, "A day run by intent, not habit.")
    y -= 20
    y = draw_wrapped(
        c,
        "From morning triage to evening wind-down, KAIROS OS turns scattered app rituals "
        "into deliberate, bounded actions.",
        MARGIN_X,
        y,
        "Segoe",
        10,
        CONTENT_W,
        INK_SOFT,
        leading=13.5,
    )
    y -= 14

    scenarios = [
        (
            "Morning triage",
            '@gmail show my important emails',
            "Surface only what needs a decision. Open Gmail when you must reply - not when "
            "a badge demands your presence.",
            "gmail",
        ),
        (
            "Protect the calendar",
            '@googlecalendar show today\'s schedule',
            "See the day as a sequence of commitments and focus blocks. Add meetings by "
            "saying them - without wandering the month grid.",
            "calendar",
        ),
        (
            "Catch up without drowning",
            '@slack summarize unread highlights',
            "Get channel and DM highlights as an action brief. Re-enter Slack with a "
            "reason, not because the red circle said so.",
            "slack",
        ),
    ]

    # Three scenario cards with small phone thumbs on the right of each
    for title, cmd, body, shot_key in scenarios:
        card_h = 1.55 * inch
        draw_rounded_rect(c, MARGIN_X, y - card_h, CONTENT_W, card_h, 10, fill=SURFACE, stroke=RULE)

        text_w = CONTENT_W * 0.58
        c.setFillColor(INK)
        c.setFont("Georgia-Bold", 12)
        c.drawString(MARGIN_X + 14, y - 22, title)

        # Command chip
        tw = c.stringWidth(cmd, "Consolas", 7.2) + 14
        draw_rounded_rect(c, MARGIN_X + 14, y - 48, tw, 18, 4, fill=INK)
        c.setFillColor(ORANGE_SOFT)
        c.setFont("Consolas", 7.2)
        c.drawString(MARGIN_X + 21, y - 43, cmd)

        draw_wrapped(
            c,
            body,
            MARGIN_X + 14,
            y - 68,
            "Segoe",
            8.5,
            text_w - 10,
            INK_SOFT,
            leading=11.5,
        )

        phone_w = CONTENT_W * 0.34
        phone_x = MARGIN_X + CONTENT_W - phone_w - 10
        draw_image_bl(c, assets[shot_key], phone_x, y - card_h + 8, phone_w, card_h - 16)

        y -= card_h + 10

    # Additional applications row
    draw_rounded_rect(c, MARGIN_X, 0.52 * inch, CONTENT_W, 0.95 * inch, 9, fill=INK)
    c.setFillColor(ORANGE)
    c.setFont("Segoe-Bold", 8)
    c.drawString(MARGIN_X + 14, 1.22 * inch, "ALSO IN THE FIELD")

    extras = [
        ("Bounded leisure", "Open Instagram for 5 minutes to reply to a DM - then the gate closes."),
        ("Capture without hop", "Drop a shopping list or launch plan into Kai Notes from the same stream."),
        ("Deep-work hours", "Enterprise profile prioritizes Workspace and silences social noise on company devices."),
        ("On-demand digest", "Pull a daily summary when you are ready - never when an algorithm is."),
    ]
    col_w = (CONTENT_W - 40) / 4
    for i, (t, b) in enumerate(extras):
        x = MARGIN_X + 14 + i * (col_w + 8)
        c.setFillColor(white)
        c.setFont("Georgia-Bold", 8.5)
        c.drawString(x, 1.02 * inch, t)
        draw_wrapped(c, b, x, 0.88 * inch, "Segoe", 7.2, col_w - 4, HexColor("#C9C4BC"), leading=9.5)

    draw_footer(c, 5)


def page_real_world_value(c: canvas.Canvas, assets: dict[str, Path]) -> None:
    c.setFillColor(PAPER)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    y = PAGE_H - MARGIN_TOP
    y = draw_section_label(c, "04  ·  Real-world Value", MARGIN_X, y)

    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 22)
    c.drawString(MARGIN_X, y, "Less noise. More agency. Measurable calm.")
    y -= 22
    y = draw_wrapped(
        c,
        "KAIROS OS creates value where attention leaks today: fewer context switches, "
        "fewer accidental doomscrolls, and faster paths to the one next action that matters.",
        MARGIN_X,
        y,
        "Segoe",
        10,
        CONTENT_W,
        INK_SOFT,
        leading=13.5,
    )
    y -= 16

    values = [
        (
            "Reclaim time from Chronos",
            "Replace open-ended scrolling with timeboxed, reason-backed sessions. Leisure "
            "budgets make distraction a conscious spend - not an ambient default.",
        ),
        (
            "Narrow and deep work",
            "One command stream keeps email, calendar, and chat in a single cognitive lane. "
            "Widgets answer; deep links open only when execution requires the full app.",
        ),
        (
            "Business-ready focus",
            "On corporate devices, Deep-Work mode turns the launcher into an executive "
            "command line for Workspace and team tools - cutting burnout from feed-shaped "
            "interfaces on company time.",
        ),
        (
            "Privacy-minded architecture",
            "The phone stays a thin client for presentation and local utilities. Secrets "
            "and OAuth live on the backend. Simple tasks can stay on-device when possible.",
        ),
    ]

    col_w = (CONTENT_W - 12) / 2
    box_h = 1.25 * inch
    top = y
    for i, (title, body) in enumerate(values):
        col = i % 2
        row = i // 2
        x = MARGIN_X + col * (col_w + 12)
        cy = top - row * (box_h + 10) - box_h
        draw_rounded_rect(c, x, cy, col_w, box_h, 9, fill=SURFACE, stroke=RULE)
        c.setFillColor(ORANGE)
        c.circle(x + 16, cy + box_h - 18, 5, fill=1, stroke=0)
        c.setFillColor(INK)
        c.setFont("Georgia-Bold", 11)
        c.drawString(x + 28, cy + box_h - 22, title)
        draw_wrapped(
            c, body, x + 14, cy + box_h - 42, "Segoe", 8.2, col_w - 28, INK_SOFT, leading=11.2
        )

    y = top - 2 * (box_h + 10) - 8

    # Chronos vs Kairos comparison
    c.setFillColor(INK)
    c.setFont("Georgia-Bold", 12)
    c.drawString(MARGIN_X, y, "The shift in practice")
    y -= 12

    half = (CONTENT_W - 12) / 2
    compare_h = 1.45 * inch
    # Chronos
    draw_rounded_rect(c, MARGIN_X, y - compare_h, half, compare_h, 9, fill=SURFACE, stroke=RULE)
    c.setFillColor(MUTED)
    c.setFont("Segoe-Bold", 8)
    c.drawString(MARGIN_X + 12, y - 18, "BEFORE  ·  CHRONOS")
    points_l = [
        "Icon grids that invite browsing",
        "Badges and push as constant pull",
        "Social apps open in one tap",
        "Hours lost without a decision",
    ]
    py = y - 36
    for pt in points_l:
        c.setFillColor(INK_SOFT)
        c.setFont("Segoe", 8.2)
        c.drawString(MARGIN_X + 12, py, "·  " + pt)
        py -= 14

    # Kairos
    draw_rounded_rect(
        c, MARGIN_X + half + 12, y - compare_h, half, compare_h, 9, fill=ACCENT_BG, stroke=ORANGE, stroke_width=1.1
    )
    c.setFillColor(ORANGE)
    c.setFont("Segoe-Bold", 8)
    c.drawString(MARGIN_X + half + 24, y - 18, "WITH KAIROS OS")
    points_r = [
        "A blank slate that demands intent",
        "Digests you request on purpose",
        "Trap apps gated by reason + time",
        "Actions that feel chosen, not stolen",
    ]
    py = y - 36
    for pt in points_r:
        c.setFillColor(INK)
        c.setFont("Segoe", 8.2)
        c.drawString(MARGIN_X + half + 24, py, "·  " + pt)
        py -= 14

    y -= compare_h + 16

    # Closing thesis
    draw_rounded_rect(c, MARGIN_X, 0.52 * inch, CONTENT_W, y - 0.52 * inch - 4, 10, fill=INK)
    c.setFillColor(ORANGE)
    c.setFont("Segoe-Bold", 8)
    c.drawString(MARGIN_X + 16, y - 22, "CLOSING")

    c.setFillColor(white)
    c.setFont("Georgia-Italic", 11)
    close = (
        "To be human is to choose. KAIROS OS restores that capacity by removing the "
        "autopilot of modern UI - so what remains is you, your intention, and the right moment to act."
    )
    draw_wrapped(
        c,
        close,
        MARGIN_X + 16,
        y - 42,
        "Georgia-Italic",
        11,
        CONTENT_W - 32,
        white,
        leading=15,
    )

    c.setFillColor(ORANGE_SOFT)
    c.setFont("Segoe", 8)
    c.drawString(MARGIN_X + 16, 0.68 * inch, "KAIROS OS  ·  Disconnected by Design  ·  Intent over impulse")

    draw_footer(c, 6)


def main() -> None:
    register_fonts()
    assets = prepare_assets()
    OUT.parent.mkdir(parents=True, exist_ok=True)

    c = canvas.Canvas(str(OUT), pagesize=letter)
    c.setTitle("KAIROS OS - Application Document")
    c.setAuthor("Team KAIROS")
    c.setSubject("Application document covering functionality, users, applications, and value")

    page_cover(c, assets)
    c.showPage()
    page_core_functionality_1(c, assets)
    c.showPage()
    page_core_functionality_2(c, assets)
    c.showPage()
    page_target_users(c, assets)
    c.showPage()
    page_practical_applications(c, assets)
    c.showPage()
    page_real_world_value(c, assets)
    c.save()
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    main()
