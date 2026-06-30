# KAIROS OS Agent Rules

## Verification & Pull Requests (Keep One Door Open)
When an evaluator agent has successfully verified a feature and is ready to mark it as PASS:
1. Ensure all code is cleanly committed to the current branch.
2. Push the branch to the remote repository (`git push -u origin <branch-name>`).
3. If the GitHub CLI (`gh`) is available, use it to automatically open a Pull Request against the `main` branch.
4. If `gh` is not available, provide the URL to the GitHub repository so the user can open the PR manually.
5. After pushing, you MUST step out of your worktree folder (e.g., `cd C:\Dev\kairos-os`) and then run `git worktree remove <your-worktree-path> --force` to free the branch for the user.

## State File Persistence (No Amnesiac Loops)
Whenever ANY agent modifies `state/progress.md` (e.g., to mark a task as DONE, or add a new task), you MUST perfectly preserve the "Agentic Ticketing Format". 
Do NOT revert the file to a simple table or one-liners. Every ticket must retain its:
- **Status**, **Priority**, **Context**, **Technical Requirements**, and **Acceptance Criteria**.
If marking a task as DONE, change `Status: OPEN` to `Status: DONE` and do not delete its acceptance criteria.

## The Maker-Checker Loop (Generator & Evaluator)
Whenever a development task is initiated (e.g., via the `/goal` command or triggered by `morning-triage`), you MUST automatically follow the Maker-Checker workflow without the user explicitly asking:
1. **The Generator:** Assign the work to the `kairos_developer` subagent in an isolated `Workspace: branch` environment. 
2. **The Handoff:** The `kairos_developer` must NOT declare the task finished when it completes its draft. It must automatically spawn the `kairos_evaluator` subagent and hand off the work.
3. **The Evaluator:** The `kairos_evaluator` acts as an adversarial QA. It runs tests and linters. If it finds issues, it kicks it back to the developer. 
4. **Completion:** The loop only ends, and the state file is only updated, when the evaluator explicitly grants a `PASS`.
