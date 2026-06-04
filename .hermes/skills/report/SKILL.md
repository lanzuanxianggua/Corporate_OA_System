---
name: report
description: "Aggregate dev-cycle artifacts (plan, diff, tests, review) into a markdown report under .hermes/reports/. Companion to /dev-cycle."
version: 1.0.0
author: project
license: MIT
platforms: [windows]
metadata:
  hermes:
    tags: [reporting, summary, dev-cycle, handoff]
    related_skills: [dev-cycle, github-pr-workflow]
---

# /report — Build a development report

Aggregate the artifacts left by a `/dev-cycle` run into a single
markdown report suitable for:
- Daily standup
- Code review handoff
- PR description draft
- Personal end-of-session summary

## When to use

- After a `/dev-cycle` finishes all 4 stages successfully
- At the end of a long work session
- Before pushing a PR

## Output

Write the report to `.hermes/reports/YYYY-MM-DD_HHMMSS-<slug>.md`.

## Required sections

1. **Summary** (1-3 sentences) — what changed, in plain English.
2. **Plan reference** — link to the plan from stage 1
   (`.hermes/plans/...`).
3. **Diff stats** — `git diff --stat main...HEAD` output.
4. **Test results** — which test classes ran, pass/fail counts, any
   skipped tests with reason.
5. **Review findings** — output of the `requesting-code-review` stage
   (security, quality gates, auto-fixes applied).
6. **Files changed** — list with line count and a one-line note per file.
7. **Follow-ups** — anything deferred, tech debt, or "next time" notes.
8. **Suggested PR title + body** — drafted but NOT auto-applied.

## Commands you can run

```bash
# Diff stats
git diff --stat main...HEAD

# Full diff (truncate if >5000 lines)
git diff main...HEAD

# Changed files with status
git diff --name-status main...HEAD

# Recent commits on this branch
git log --oneline main..HEAD

# Test summary (if mvn output is in target/surefire-reports/)
ls code/backend/<module>/target/surefire-reports/ 2>/dev/null
```

## What NOT to include

- API keys, passwords, JWT secrets (auto-redact)
- Internal `localhost` URLs (replace with `<local-dev>`)
- Personal commentary unrelated to the change

## Interactivity

- If the user invokes `/report` without context, scan the most recent
  dev-cycle artifacts in `.hermes/plans/` and `.hermes/reports/` and ask
  which one to summarize.
- If multiple branches have unmerged work, list them and ask.
- Default to "draft only, do not commit/push" — always confirm.
