# KAIROS OS Agent Rules

## Branch & Worktree Management
- **Branch Creation**: If the branch currently working on is the `main` branch, you MUST switch to a new branch before implementing anything. Make sure the new branch name clearly defines the features we are building.
- **Worktree Isolation**: To prevent code leaking, dependency conflicts, and files polluting parallel agent runs, always create a separate Git worktree for the new branch under a `.worktrees/` directory inside the repository.
  - Create the worktree: `git worktree add .worktrees/<branch-name> <branch-name>`
  - Perform all code modifications, builds, and commands inside that worktree directory (e.g. setting working directory to `.worktrees/<branch-name>`) instead of the main root directory.
- **Freeing the Branch for User Checkout**: Git prevents checking out a branch in the main repository if it is currently checked out in an active worktree. To ensure the user can checkout the feature branch at any time:
  - Once changes are committed, you **MUST** free the branch name before ending your turn or task. Do this by either:
    1. **Removing the worktree**: Run `git worktree remove --force .worktrees/<branch-name>` (make sure all edits are committed first so they are not lost).
    2. **Detaching HEAD in the worktree**: Run `git checkout --detach` inside the worktree directory. This frees up the branch name while leaving the worktree folder intact.


## Documentation & Tech Stack
- Always refer to the `find-docs` skill to find the latest documentation when implementing code for various tech stacks.
- Use the `mobile-android-design` skill for when building Jetpack Compose components.
- Use the `context` folder: refer to the PRD document for the global overview, and the Tech implementation document for technical details.

## State Documentation
- Keep `context/state.md` updated with the current task status.
- Keep `context/progress.md` updated with completed/open task history for agent automation.
