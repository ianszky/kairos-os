---
name: KaiOS
description: A minimalist, agentic text-first Android launcher
colors:
  primary: "#ff6b00"
  neutral-bg: "#050505"
  neutral-surface: "#111111"
  neutral-fg: "#f5f5f5"
  neutral-muted: "#8a8a8a"
  neutral-border: "#333333"
typography:
  display:
    fontFamily: "'Doto', monospace"
    fontSize: "72px"
    fontWeight: 700
    letterSpacing: "-2px"
  body:
    fontFamily: "'Doto', monospace"
    fontSize: "14px"
    fontWeight: 700
  label:
    fontFamily: "'Doto', monospace"
    fontSize: "11px"
    fontWeight: 700
    letterSpacing: "0.08em"
rounded:
  sm: "4px"
  md: "8px"
  lg: "12px"
  xl: "16px"
spacing:
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#000000"
    rounded: "{rounded.md}"
    padding: "12px"
  chip:
    backgroundColor: "{colors.neutral-bg}"
    textColor: "{colors.neutral-fg}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
  bubble-user:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-muted}"
    rounded: "{rounded.lg}"
    padding: "14px 18px"
  bubble-ai:
    backgroundColor: "transparent"
    textColor: "{colors.neutral-fg}"
    rounded: "0px"
    padding: "14px 18px"
---

# Design System: KaiOS

## 1. Overview

**Creative North Star: "The Analog Terminal"**

KaiOS embraces the "Disconnected by Design" philosophy by replacing the overwhelming grid of apps with a single line of intent. Drawing heavy inspiration from hardware displays (specifically Nothing's dot-matrix aesthetic) and deep glowing ambient interfaces, the system uses a **Restrained** color strategy. A stark, high-contrast monochrome terminal acts as the silent void, pierced only by a signature glowing orange that commands focus. It explicitly rejects visual noise, dopamine loops, and the warm-neutral "SaaS cream" trend in favor of raw, typographic utilitarianism.

**Key Characteristics:**
- **Terminal-Native:** Text-first interaction, using the `Doto` monospace font.
- **High Contrast Monochrome:** Pure black `#050505` backgrounds with stark white text, avoiding gray washes.
- **Ambient Focus:** A deep orange accent (`#ff6b00`) with ambient glow (`rgba(255, 107, 0, 0.9)`) that denotes state, active AI listening, or critical action.
- **Flat Geometry:** Components are flat with crisp 1px borders, relying on color and typography rather than shadows for hierarchy.

## 2. Colors

The palette is starkly monochromatic with a single, highly committed accent color used exclusively for state and focus.

### Primary
- **Focus Orange** (`#ff6b00`): The sole brand color. Used for the active cursor, critical alerts, primary buttons, and ambient background glows during AI processing.

### Neutral
- **Void Black** (`#050505`): The infinite canvas of the home screen and global background.
- **Surface Black** (`#111111`): Secondary background for elevated containers like the terminal input and app drawer.
- **Terminal White** (`#f5f5f5`): Primary text, icons, and active foreground elements.
- **Matrix Muted** (`#8a8a8a`): Secondary text, inactive states, user chat bubbles, and timestamp metadata.
- **Grid Border** (`#333333`): Crisp 1px strokes defining containers, inputs, and dividers.

**The Drenched Highlight Rule.** The core interface is entirely monochromatic. Color is never used for decoration; the signature orange is reserved strictly for active states, critical intent, or ambient glow.

## 3. Typography

**Display Font:** 'Doto', monospace
**Body Font:** 'Doto', monospace
**Label/Mono Font:** 'Doto', monospace

**Character:** Raw, engineered, and unapologetic. The universal use of the Doto monospace font channels the technical precision of early computing dot-matrix displays.

### Hierarchy
- **Display** (700, 72px, tracking -2px): Reserved exclusively for the primary clock. Includes a soft orange text-shadow.
- **Headline** (700, 20px, tracking 0): Used for the terminal prompt symbol (`>`).
- **Body** (700, 14px): The primary reading size for the command stream, chat bubbles, and app chips.
- **Label** (700, 11px, tracking 0.08em, uppercase): Drawer headers, timestamps, and metadata.

**The Typographic Intent Rule.** Size and weight denote the importance of the command or response, never decoration. The single typeface unites the interface as a terminal.

## 4. Elevation

The interface is flat by default, relying on stark contrast, 1px borders, and surface color shifts (`#050505` to `#111111`) to separate content. 

### Shadow Vocabulary
- **Ambient Glow** (`box-shadow: 0 0 20px rgba(255, 107, 0, 0.15)`): A diffuse orange drop shadow behind the active terminal input or active AI state.
- **Device Depth** (`box-shadow: 0 0 50px rgba(0,0,0,0.8)`): Used strictly on the outer device frame in presentation mode.

**The Flat-By-Default Rule.** Surfaces are flat at rest. Shadows appear only as a response to state (focus within the terminal, active listening).

## 5. Components

### Buttons
- **Shape:** Gently curved edges (8px radius).
- **Primary:** Solid Focus Orange background with pure black uppercase text. Used for the 'Launch Intent' action.
- **Hover / Focus:** Transitions background to a slightly brighter orange (`#ff8533`) with a subtle `-1px` Y-axis shift.
- **Time Pill / Secondary:** Void Black background with Grid Border. Selected state changes border and text to Focus Orange with a 10% opacity orange background.

### Chat Bubbles
- **User Bubble:** Surface Black background, Grid Border, Matrix Muted text, 12px radius (4px bottom-right). 
- **AI Bubble:** Transparent background, no border except a 2px solid Focus Orange left-border, Terminal White text, 0px radius.

### Cards / Containers
- **Terminal Input:** Surface Black background, Grid Border, 16px radius, backdrop-filter blur. Focus state adds Focus Orange border and Ambient Glow.
- **App Drawer:** Surface Black background, Grid Border, 16px radius.

### Inputs / Fields
- **Terminal / Friction Input:** Transparent or Void Black background, Terminal White text. Focus removes default outline and uses container-level glow or direct border highlight.

## 6. Do's and Don'ts

### Do:
- **Do** use high-contrast monochrome as the default state for all UI elements.
- **Do** use the signature Focus Orange exclusively for active intent, ambient system status, and critical alerts.
- **Do** rely on typography and 1px borders to create hierarchy.
- **Do** use `backdrop-filter: blur(15px)` on floating containers like the app drawer and terminal.

### Don't:
- **Don't** use colorful app grids, badge counts, and infinite scrolls.
- **Don't** use "SaaS cream" backgrounds or low-contrast muted gray text.
- **Don't** use identical card grids or the hero-metric template.
- **Don't** use side-stripe borders greater than 1px as a colored accent (except for the AI chat bubble which deliberately uses a 2px left border).
