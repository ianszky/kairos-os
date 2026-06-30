---
name: "morning-triage"
description: "The Discovery move of our Loop Engineering setup. Reads project state and issues to find actionable tickets for the agents."
---

# Morning Triage (Discovery Loop)

## Read (The Discovery Inputs)
- Review the `state/progress.md` file.
- Check for any new open issues or pending architecture tasks.
- Review recent commits to understand current context.

## Judge (Setting the Ceiling)
For each candidate finding, decide:
- Is it actionable right now, or is it noise?
- Does it block the current hackathon milestone? (High Priority)
- Is it already being worked on by another agent? (Skip)
- Only keep actionable findings that are ready for a worktree.

## Write (Persistence)
Append your findings to `state/progress.md`. You MUST use the Agentic Ticketing format. Do not use one-liners or tables.
For each new ticket, write:
### [Ticket ID]. [Ticket Title]
**Status:** OPEN
**Priority:** [High/Medium/Low]
**Context:** [Why this needs to be built based on the PRD]
**Technical Requirements:**
- [Exact technical constraints]
**Acceptance Criteria (For the Evaluator):**
- [ ] [Testable condition 1]
- [ ] [Testable condition 2]

Commit the file so it survives clearing context.

## Hand Off
For each kept finding, prepare it for the `kairos_developer` agent by assigning it an isolated `--worktree` branch.
