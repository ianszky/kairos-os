import os
from reportlab.lib.pagesizes import letter
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, ListFlowable, ListItem
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.colors import HexColor

def generate_pdf():
    # Ensure output directory exists
    output_dir = r"C:\Dev\kairos-os\output\pdf"
    os.makedirs(output_dir, exist_ok=True)
    
    pdf_path = os.path.join(output_dir, "KAIROS_OS_Loop_Engineering_Plan.pdf")
    doc = SimpleDocTemplate(pdf_path, pagesize=letter,
                            rightMargin=50, leftMargin=50,
                            topMargin=50, bottomMargin=50)

    styles = getSampleStyleSheet()
    
    # Custom styles
    title_style = ParagraphStyle(
        'CustomTitle',
        parent=styles['Heading1'],
        fontSize=22,
        spaceAfter=20,
        alignment=TA_CENTER,
        textColor=HexColor("#2C3E50")
    )
    
    heading2_style = ParagraphStyle(
        'CustomHeading2',
        parent=styles['Heading2'],
        fontSize=16,
        spaceBefore=15,
        spaceAfter=10,
        textColor=HexColor("#2980B9"),
        borderPadding=5
    )
    
    heading3_style = ParagraphStyle(
        'CustomHeading3',
        parent=styles['Heading3'],
        fontSize=13,
        spaceBefore=10,
        spaceAfter=5,
        textColor=HexColor("#34495E")
    )
    
    body_style = ParagraphStyle(
        'CustomBody',
        parent=styles['Normal'],
        fontSize=11,
        spaceAfter=8,
        leading=16,
        alignment=TA_JUSTIFY
    )
    
    bullet_style = ParagraphStyle(
        'CustomBullet',
        parent=styles['Normal'],
        fontSize=11,
        spaceAfter=5,
        leading=16,
        leftIndent=20
    )

    story = []

    # Title
    story.append(Paragraph("KAIROS OS: Prototyping with Loop Engineering", title_style))
    story.append(Spacer(1, 10))

    # Executive Summary
    story.append(Paragraph("1. Executive Summary", heading2_style))
    story.append(Paragraph("This document outlines a strategy to go from concept to an actual prototype of KAIROS OS within the 1-month 'Disconnected by Design' hackathon timeline. To achieve this ambitious goal, we will utilize <b>Loop Engineering</b>—a methodology that replaces manual step-by-step AI prompting with automated, self-contained systems (loops) that discover work, execute it, verify it, and persist state. By shifting from 'doing the work' to 'designing the loops', we can safely accelerate development.", body_style))

    # Architectural Mapping to Loops
    story.append(Paragraph("2. Applying the Five Moves of Loop Engineering", heading2_style))
    story.append(Paragraph("Every loop we design will implement the five essential moves to ensure reliability and avoid common pitfalls like 'Nodding Loops' (missing verification) and 'Amnesiac Loops' (missing persistence):", body_style))
    
    moves = [
        "<b>Discovery:</b> Loop runs on a timer to read the KAIROS PRD and Tech Spec, surfacing new actionable tickets (e.g., 'Implement Intent Gate UI', 'Setup Composio Gmail Integration').",
        "<b>Handoff:</b> Using <font face='Courier'>--worktree</font>, each generated ticket gets an isolated git directory to avoid parallel agents colliding.",
        "<b>Verification:</b> We enforce the <i>Maker-Checker</i> principle. The Generator agent writes the code, while a separate Evaluator agent tests it (e.g., runs Next.js API tests, compiles Jetpack Compose previews). The Evaluator assumes the code is BROKEN until proven otherwise.",
        "<b>Persistence:</b> Progress is recorded in <font face='Courier'>./state/hackathon_progress.md</font> and automated pull requests are raised to GitHub.",
        "<b>Scheduling:</b> Loops will be scheduled via local timers during the day for rapid iteration, and via GitHub Actions for overnight verification runs."
    ]
    for move in moves:
        story.append(Paragraph(f"• {move}", bullet_style))

    # The Loop Pipelines
    story.append(Paragraph("3. The KAIROS Development Loops", heading2_style))
    
    story.append(Paragraph("Loop A: The Android Client Loop", heading3_style))
    story.append(Paragraph("Builds the thin Kotlin + Jetpack Compose frontend. Triggered by frontend UI tickets. The Evaluator agent uses the Android Lint tool and unit tests to verify the UI. To prevent 'Comprehension Rot', humans will review PRs for the visual aesthetic of the blinking cursor interface and the Floating Bubble overlay.", body_style))

    story.append(Paragraph("Loop B: The Next.js Fat Backend Loop", heading3_style))
    story.append(Paragraph("Builds the intent router and MCP integrations. The Generator agent implements the Gemini 3.5 Flash tiered routing and hooks up Composio SDKs. The Evaluator agent writes and runs Jest API tests against the endpoints (e.g., simulating a <font face='Courier'>@gmail</font> command request) to ensure JSON widget payloads are correctly structured.", body_style))

    # Guardrails against costs
    story.append(Paragraph("4. Guardrails & Operational Discipline", heading2_style))
    story.append(Paragraph("To ensure our agentic loops don't spin out of control, we will enforce strict guardrails:", body_style))
    
    guardrails = [
        "<b>Token Caps:</b> To prevent 'Token Blowout' overnight, we will set strict per-run token limits and a maximum number of retry attempts for the Evaluator agent.",
        "<b>Human-in-the-Loop (Keep One Door Open):</b> We will never auto-merge. To prevent 'Cognitive Surrender', team members will conduct morning triages to review the PRs generated by the loops.",
        "<b>Skills as Intent Debt Payoff:</b> KAIROS OS architecture rules (e.g., 'Always return Widget JSON, never Markdown') will be encoded into <font face='Courier'>SKILL.md</font> files, ensuring the agents always have the correct context without bloating the prompt."
    ]
    for guard in guardrails:
        story.append(Paragraph(f"• {guard}", bullet_style))

    # Conclusion
    story.append(Paragraph("5. Conclusion", heading2_style))
    story.append(Paragraph("By applying Loop Engineering, we can offload the mechanical labor of boilerplate Android setup and Next.js wiring to autonomous systems. This frees our human judgment to focus on what matters most for the hackathon: perfecting the minimalist 'Kairos' philosophy, refining the Intent Gates, and ensuring the final prototype perfectly answers the question: <i>'What does it mean to be human?'</i>", body_style))

    doc.build(story)
    print(f"Successfully generated PDF at {pdf_path}")

if __name__ == "__main__":
    generate_pdf()
