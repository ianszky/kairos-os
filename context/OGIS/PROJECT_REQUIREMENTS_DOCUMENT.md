# KAIROS OS — Project Requirements Document

> *"True connection requires the space to think. KAIROS OS embraces the 'Disconnected by Design' philosophy by replacing the overwhelming grid of apps with a single line of intent, transforming the smartphone from a landscape of distraction into an instrument of narrow, deep, and uncompromised focus."*
> 
> **"KAIROS OS doesn't just reduce your screen time — it respects your device's intelligence. Simple tasks never leave your phone. Your data, your device, your time."**

---

## 1. Executive Summary

**KAIROS OS** is an agentic Android launcher that replaces the traditional app-grid home screen with a minimalist, text-first command interface powered by AI. Named after the ancient Greek concept of *Kairos* — the qualitative, opportune moment for action (as opposed to *Chronos*, the endless ticking of quantitative time) — the launcher forces users to articulate intent before their phone can offer them anything.

Built for the **FIT × OGIS "Disconnected by Design"** hackathon, KAIROS OS directly addresses the competition's central thesis: that convenience does not always equal benefit, and that intentionally choosing discomfort over the bombardment of information can restore meaningful human agency.

**Timeline:** 1 month  
**Platform:** Android (launcher application)  
**Architecture:** Thin Android client ↔ Next.js backend ↔ AI models + MCP integrations

---

## 2. Problem Statement

Modern smartphones are engineered to maximize engagement through:

- **Visual bombardment** — Colorful app grids, badge counts, and infinite scrolls trigger dopamine loops that encourage passive consumption.
- **Notification overload** — Push notifications shatter focus by constantly pulling users out of their current task.
- **Broad and shallow connections** — Social media platforms prioritize quantity of interactions over quality, leading to feelings of loneliness and weakened boundaries.
- **Zero-friction access** — One tap opens any app instantly, enabling mindless doomscrolling without conscious decision-making.

The result: users spend hours on their phones without intention, contributing to digital fatigue, anxiety, and a fundamental erosion of agency over their own attention.

---

## 3. Proposed Solution

KAIROS OS introduces **intentional friction** by design. The user's home screen is a blank canvas with a blinking cursor — nothing more. To do anything, the user must consciously articulate what they want:

```
@alarm set an alarm for 6am tomorrow morning
@google-calendar set the meeting for Ms. Tenorio tomorrow at 3pm
@gmail display my most important emails
@notes Shopping list for tomorrow: eggs, chicken, rice
@spotify give me something random to play
```

The launcher acts as a **thin client** that routes natural-language commands to a powerful backend AI system. The backend classifies intent, calls the appropriate tools/APIs, and returns structured responses — either as rendered UI widgets or deep-link commands to native apps.

---

## 4. Alignment with Hackathon Theme

### 4.1 "Disconnected by Design" — Aim & Background

| Theme Requirement | How KAIROS OS Addresses It |
|---|---|
| *Challenges the assumption that convenience always equals benefit* | Replaces one-tap app access with a text-based interface that requires conscious intent |
| *Explores the importance of intentionally choosing disconnection* | The blank-slate home screen creates a "digital pause" — users must think before acting |
| *Shifts from "broad and shallow" to "narrow and deep" connections* | Commands are targeted and specific; no infinite feeds or suggested content |
| *Addresses overwhelming information exposure* | Notifications are intercepted and batched into digests; no ambient noise |
| *Uses everyday discomfort as a starting point* | The friction of the blank cursor is the designed discomfort that breaks habitual scrolling |

### 4.2 Software Concept Requirements

| Requirement | Implementation |
|---|---|
| **Unique** | No existing Android launcher strips the OS to a text-command interface with AI routing |
| **Innovative** | Combines MCP protocol, tiered LLM routing, and server-driven UI in a mobile context |
| **Futuristic** | Envisions the post-app paradigm where intent replaces icons |
| **AI-driven** | Core experience is powered by multi-tier AI models for intent classification, tool calling, and notification analysis |
| **On-theme** | Every design decision enforces intentional friction and deliberate digital engagement |

### 4.3 Judging Criteria Alignment

| Criterion | Weight | Strategy |
|---|---|---|
| **Creativity** | 30% | Philosophical narrative (Chronos vs. Kairos), blank-slate UI paradigm, agentic notification interceptor |
| **Innovativeness** | 25% | MCP-based app integration, tiered LLM router, server-driven dynamic widgets, intent gates |
| **Feasibility** | 20% | Thin client architecture, leveraging existing APIs and MCPs, tiered complexity to deliver MVP within timeline |
| **Usefulness** | 15% (+bonus) | Real productivity gains, Google Workspace integration, Enterprise Deep-Work mode for business use |
| **Interest Factor** | 10% | The visceral experience of a blank phone screen; floating bubble UX; daily digest concept |

> **Note:** Additional points for business use will be captured through the Enterprise Deep-Work mode feature.

---

## 5. Target Users

### 5.1 Primary Persona — "The Overwhelmed Professional"
- **Age:** 22–40
- **Profile:** Knowledge worker, student, or creative who recognizes their phone usage is excessive but lacks the tools to change behavior
- **Pain Points:** Doomscrolling, notification anxiety, inability to focus during deep work, guilt after unproductive phone sessions
- **Goal:** A phone experience that serves them rather than exploits them

### 5.2 Secondary Persona — "The Enterprise User"
- **Age:** 25–45
- **Profile:** Employee using a company-issued phone who needs to stay productive without social media distractions
- **Pain Points:** Corporate burnout, context-switching overhead, meeting overload
- **Goal:** A streamlined command-line for business tools (email, calendar, sheets) without the visual noise

### 5.3 Tertiary Persona — "The Digital Minimalist"
- **Age:** 18–35
- **Profile:** Privacy-conscious or philosophy-driven user who wants to fundamentally change their relationship with technology
- **Pain Points:** Philosophical discomfort with attention economy, desire for "dumbphone" utility with "smartphone" capability
- **Goal:** A phone that is impossible to use unintentionally

---

## 6. Core Functional Requirements

### 6.1 The Command Interface

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | The home screen SHALL display a minimal text input field with a blinking cursor as the primary and only interaction surface | P0 |
| FR-02 | The text input SHALL support an `@app` mention syntax to direct commands to specific services | P0 |
| FR-03 | The system SHALL display a dropdown/selector for available app connections (similar to Claude's connector UI) | P0 |
| FR-04 | The command stream SHALL render responses chronologically as a scrollable feed of interactions | P0 |
| FR-05 | The system SHALL support a `/open` command to directly launch native utility apps | P0 |

### 6.2 AI-Powered Intent Routing

| ID | Requirement | Priority |
|---|---|---|
| FR-06 | The backend SHALL classify incoming prompts into tiers: instant/heuristic commands vs. agentic/heavy tasks | P0 |
| FR-07 | Structured commands (e.g., `/open`) SHALL be processed by a fast microservice layer, bypassing the LLM entirely | P0 |
| FR-08 | Simple intent tasks (e.g., `@alarm set for 6am`) SHALL be processed by a lightweight AI model (e.g., Gemini Flash-Lite) | P0 |
| FR-09 | Complex tool-calling tasks (e.g., `@gmail display important emails`) SHALL be processed by a capable AI model (e.g., Gemini Flash) | P0 |
| FR-10 | The system SHALL maintain conversation context within active sessions to support follow-up commands | P1 |

### 6.3 App Integrations (MCP & Custom)

| ID | Requirement | Priority |
|---|---|---|
| FR-11 | The system SHALL support MCP-based integrations for Google Workspace apps (Gmail, Calendar, Sheets, Drive) | P0 |
| FR-12 | The system SHALL support native Android intent dispatch for offline utility apps (Clock, Calculator, Camera, Flashlight) | P0 |
| FR-13 | The system SHALL support integration with popular third-party services (Spotify, browser search) via their APIs or existing MCP servers | P1 |
| FR-14 | The system SHALL support custom in-house lightweight app implementations for utilities without existing API/MCP support | P2 |

### 6.4 Server-Driven UI Widgets

| ID | Requirement | Priority |
|---|---|---|
| FR-15 | The backend SHALL return structured JSON layout definitions for tool responses (not raw markdown text) | P0 |
| FR-16 | The Android client SHALL render dynamic UI widgets inline in the command stream (e.g., email cards, alarm confirmations) | P0 |
| FR-17 | Interactive widgets SHALL support action callbacks (e.g., "dismiss", "snooze", "open in app") | P1 |
| FR-18 | The paradigm SHALL be: **Read via Widgets, Execute via Deep Links** | P0 |

### 6.5 Intent Gate System (Anti-Doomscroll)

| ID | Requirement | Priority |
|---|---|---|
| FR-19 | The system SHALL categorize apps into two tiers: **Utility** (immediate access) and **Trap** (gated access) | P0 |
| FR-20 | Trap apps SHALL require the user to specify a reason and time limit before opening (e.g., `@instagram /open --reason "reply to DM" --time 5m`) | P0 |
| FR-21 | The system SHALL enforce the specified time limit via a foreground overlay/kill-switch | P1 |
| FR-22 | Users SHALL be able to configure which apps are Utility vs. Trap during onboarding and in settings | P0 |
| FR-23 | Changes to guardrail settings SHALL be subject to friction (e.g., 12-hour cooling-off period for mode changes) | P1 |

### 6.6 Agentic Notification Interceptor

| ID | Requirement | Priority |
|---|---|---|
| FR-24 | The launcher SHALL use Android's Notification Listener Service to capture all incoming notifications silently | P0 |
| FR-25 | Notifications SHALL be classified into **Critical** (instant passthrough) and **Digest** (batched and held) tiers | P0 |
| FR-26 | Critical tier SHALL include: phone calls, calendar alerts, and messages from user-defined VIP contacts | P0 |
| FR-27 | Digest tier notifications SHALL be summarized by the AI and presented only when the user requests them (e.g., `@launcher daily digest`) | P1 |
| FR-28 | Users SHALL configure notification tier rules during onboarding | P0 |

### 6.7 Persistent Context Anchor (Floating Bubble)

| ID | Requirement | Priority |
|---|---|---|
| FR-29 | When the launcher hands off to a native app, a floating overlay bubble SHALL remain visible | P1 |
| FR-30 | The floating bubble SHALL display a mission reminder / countdown timer when a Trap app is opened with a time limit | P1 |
| FR-31 | Tapping the floating bubble SHALL open the KAIROS command text box as an overlay on top of the current app | P1 |

### 6.8 Onboarding Flow

| ID | Requirement | Priority |
|---|---|---|
| FR-32 | First-time users SHALL go through a guided onboarding that explains the intentional friction philosophy | P0 |
| FR-33 | During onboarding, users SHALL configure: Trap vs. Utility app classifications, notification tier rules, strict vs. free mode preference | P0 |
| FR-34 | Onboarding SHALL set the psychological contract — making it clear that guardrail changes will require friction to modify | P0 |

### 6.9 Enterprise Deep-Work Mode

| ID | Requirement | Priority |
|---|---|---|
| FR-35 | The system SHALL support an Enterprise / Deep-Work mode profile that prioritizes business tools | P2 |
| FR-36 | In Deep-Work mode, the notification interceptor SHALL aggregate business communications (Slack, Teams, Email) into action-oriented briefs | P2 |
| FR-37 | Deep-Work mode SHALL be activatable manually or automatically during user-defined working hours | P2 |

---

## 7. Non-Functional Requirements

| ID | Requirement | Category |
|---|---|---|
| NFR-01 | Instant commands (`/open`) SHALL execute in <200ms end-to-end | Performance |
| NFR-02 | Lightweight model tasks SHALL respond within 1–2 seconds | Performance |
| NFR-03 | Complex agentic tasks SHALL respond within 3–6 seconds | Performance |
| NFR-04 | The Android app footprint SHALL remain under 50MB | Performance |
| NFR-05 | All API keys and OAuth tokens SHALL be stored on the backend, never on the device | Security |
| NFR-06 | The system SHALL handle network failures gracefully with offline fallbacks for utility commands | Reliability |
| NFR-07 | The launcher UI SHALL support Android 10+ (API 29+) | Compatibility |
| NFR-08 | The backend SHALL auto-scale to handle concurrent user sessions | Scalability |

---

## 8. Scope & Prioritization (MVP vs. Post-MVP)

### 8.1 MVP (Hackathon Deliverable — 1 Month)

| Feature | Status |
|---|---|
| Text-based command interface with `@app` routing | ✅ In Scope |
| Tiered AI intent routing (Flash-Lite + Flash) | ✅ In Scope |
| Google Workspace integrations (Gmail, Calendar) | ✅ In Scope |
| Native utility app dispatch (`/open`) | ✅ In Scope |
| Server-driven UI widgets for responses | ✅ In Scope |
| Intent Gate system for Trap apps | ✅ In Scope |
| Notification Interceptor (Critical + Digest) | ✅ In Scope |
| Guided onboarding flow | ✅ In Scope |
| Floating bubble overlay | ✅ In Scope |

### 8.2 Post-MVP (Future Roadmap)

| Feature | Status |
|---|---|
| Enterprise Deep-Work Mode | 🔮 Post-MVP |
| Slack / Microsoft Teams integration | 🔮 Post-MVP |
| Custom CRM integrations | 🔮 Post-MVP |
| Voice-to-command input | 🔮 Post-MVP |
| On-device model fallback for offline AI | 🔮 Post-MVP |
| App Store deployment & B2B corporate licensing | 🔮 Post-MVP |

---

## 9. Risks & Mitigations

| Risk | Severity | Mitigation |
|---|---|---|
| Google Workspace OAuth complexity consumes disproportionate dev time | High | Use existing Google MCP servers; limit MVP to Gmail + Calendar only |
| LLM latency makes the experience feel sluggish | High | Tiered routing architecture; fast microservice layer for simple commands |
| Android overlay/foreground service permissions vary across OEMs | Medium | Target stock Android behavior; document known OEM-specific limitations |
| Token costs escalate with heavy model usage | Medium | Flash-Lite handles majority of traffic; Flash reserved for complex tasks only |
| Users find the blank interface too intimidating | Medium | Strong onboarding flow; contextual hints; example command suggestions |
| Deep-linking fails for apps that don't support it | Low | Graceful fallback to standard app launch intent |

---

## 10. Success Metrics (Hackathon Demo)

| Metric | Target |
|---|---|
| End-to-end demo of 5+ distinct `@app` commands | ✅ |
| Sub-2-second response for simple commands | ✅ |
| Intent Gate + timer enforcement demo | ✅ |
| Notification interception + daily digest demo | ✅ |
| Smooth onboarding flow walkthrough | ✅ |
| Working Google Workspace integration (Calendar + Gmail) | ✅ |
| Floating bubble overlay with command re-entry | ✅ |

---

## 11. Philosophical Foundation

### 11.1 Thesis Statement (Zen Minimalist Pitch)

> *"True connection requires the space to think. KAIROS OS embraces the 'Disconnected by Design' philosophy by replacing the overwhelming grid of apps with a single line of intent, transforming the smartphone from a landscape of distraction into an instrument of narrow, deep, and uncompromised focus."*

### 11.2 The Kairos Narrative

In ancient Greek, there are two words for time:

- **Chronos** (χρόνος) — Quantitative, sequential time. The endless ticking clock. The infinite scroll. The hours lost to doomscrolling.
- **Kairos** (καιρός) — Qualitative time. The *right* moment. The *opportune* moment for deliberate action.

Modern operating systems trap us in Chronos — an unbroken stream of notifications, feeds, and stimuli that erodes our capacity for intentional engagement. **KAIROS OS** is built to rescue human attention from Chronos, forcing every interaction with our digital world to occur in a moment of Kairos — with absolute intention and purpose.

### 11.3 Addressing the Deeper Inquiry

> *"What does it mean to be human?"*

To be human is to choose. KAIROS OS restores the fundamental human capacity for choice by removing the autopilot of modern UI design. When there are no icons to tap, no feeds to scroll, and no notifications to react to, what remains is the user — alone with their intention, forced to decide what they truly want from their device.

> *"What is society?"*

Society is the sum of our attention. When that attention is harvested by algorithms optimized for engagement, society becomes shallow. KAIROS OS proposes a different social contract with technology: one where the device serves human intent rather than exploits human impulse.

---

*Document Version: 1.0*  
*Last Updated: June 25, 2026*  
*Authors: Ian Szky & Team KAIROS*
