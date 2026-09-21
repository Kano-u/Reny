# AGENTS.md

## User Context

- The user has limited programming knowledge. Ask questions when requirements, tradeoffs, or product behavior are unclear.
- Prefer plain-language explanations and confirm important decisions before implementation.

## Build Environment

- There is no local Android build environment.
- Build and verify Android changes on GitHub Actions.
- Use the `gh` CLI for GitHub operations. `GH_TOKEN` contains the required credentials.

## Engineering Principles

- Follow Go's "less is more" philosophy.
- There is exactly one correct way to implement each requirement. Do not add fallback paths when the preferred command or approach fails because of permissions; fix the permissions instead.
- When several implementation options exist, ask the user which one to keep and explain each option's tradeoffs. Keep multiple approaches only when the user explicitly requests them.
- Follow the Arch Linux approach: do not preserve compatibility. Always prefer the latest stable solution.
- Keep only the essential implementation. Do not accumulate compatibility shims, legacy branches, historical notes, or code that preserves superseded behavior.

## Verification

- After code changes, run the relevant checks in GitHub Actions.
- Do not claim a change is verified without a successful cloud build or test result.
