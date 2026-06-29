---
name: "kairos-architecture"
description: "Core architectural guidelines for KAIROS OS. Pays off 'Intent Debt' so agents know exactly how to build the OS."
---

# KAIROS OS Architecture Rules

When building or modifying KAIROS OS, all agents must adhere to these rules:

## 1. Thin-Client / Fat-Backend Paradigm
- The Android app is ONLY a presentation layer. It captures input and renders widgets. 
- ALL AI inference, API integrations, and heavy logic live in the Next.js backend.

## 2. Server-Driven UI
- The Next.js backend must NEVER return raw markdown. It must return structured JSON layout definitions (`KairosResponse`).
- The Android client parses these JSON widgets to render Compose UI elements.
- Read via Widgets, Execute via Deep Links.

## 3. Intent Gates
- Utility apps (Clock, Camera) are launched instantly via `/open`.
- Trap apps (Instagram, TikTok) require the user to specify intent and duration before launching.

## 4. Evaluator Verification
- Any backend endpoint or frontend widget written must include a way to test it.
- Do not consider a task finished unless the `kairos_evaluator` agent has successfully verified it.
