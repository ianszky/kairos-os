# KAIROS OS Agent Rules

## Verification & Pull Requests (Keep One Door Open)
When an evaluator agent has successfully verified a feature and is ready to mark it as PASS:
1. Ensure all code is cleanly committed to the current branch.
2. Push the branch to the remote repository (`git push -u origin <branch-name>`).
3. If the GitHub CLI (`gh`) is available, use it to automatically open a Pull Request against the `main` branch.
4. If `gh` is not available, provide the URL to the GitHub repository so the user can open the PR manually.
5. After pushing, run `git worktree remove . --force` (if safe) or instruct the user on the PR.
