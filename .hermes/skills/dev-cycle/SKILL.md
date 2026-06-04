---
name: dev-cycle
description: "Four-stage dev loop for Corporate_OA_System: plan → implement → test → review. Composes bundled plan / subagent-driven-development / test-driven-development / requesting-code-review skills. Project-specific (Java + Spring Boot)."
version: 1.0.0
author: project
license: MIT
platforms: [windows]
metadata:
  hermes:
    tags: [workflow, dev-loop, planning, testing, review, java, spring-boot]
    related_skills: [plan, subagent-driven-development, test-driven-development, requesting-code-review, github-pr-workflow]
---

# /dev-cycle — Project-specific four-stage dev loop

This skill orchestrates a 4-stage loop for Corporate_OA_System:
**plan → implement → test → review**, with a 5th optional **report** step
handled by the companion `/report` skill.

This skill is a thin orchestrator: it composes the bundled Hermes skills
(`plan`, `subagent-driven-development`, `test-driven-development`,
`requesting-code-review`, `github-pr-workflow`) and binds them to the
project's Java/Maven conventions from `CLAUDE.md`.

## When to use

- New feature, bug fix, or refactor that touches 1+ files
- Multi-step work that benefits from explicit checkpoints
- Any change you intend to commit and (eventually) push

## When NOT to use

- Single-line fixes / typos
- Read-only exploration
- Pure config / docs changes (skip the test + review stages)

## Stages

### Stage 1 — PLAN

Invoke the bundled **`plan`** skill (write a markdown plan to
`.hermes/plans/YYYY-MM-DD_HHMMSS-<slug>.md`).

Project-specific requirements for the plan:
- **Goal**: one-sentence, user-visible outcome.
- **Files**: absolute paths under `code/backend/...` or `code/frontend/...`.
- **Module layering**: respect the `oa-platform-* → oa-common → oa-model →
  oa-mapper → oa-service → domain modules → oa-web` dependency direction.
  Domain modules (`oa-hr`, `oa-finance`, etc.) MUST NOT import each other.
- **Tests**: name the test class to add. For controllers, extend
  `BaseControllerTest`; for HR tests use `@OaWebMvcTest`.
- **Validation commands**: list the exact `mvn` / `pnpm` commands the
  implementation stage will run.
- **Risks**: note any new dependency, DB migration, or workflow callback
  impact.

**Stop here for non-trivial work** and ask the user to confirm the plan
before stage 2.

### Stage 2 — IMPLEMENT

Invoke the bundled **`subagent-driven-development`** skill with the plan
from stage 1. This dispatches fresh subagents per task with a two-stage
review (spec compliance + quality).

Project-specific guidance for the implementation stage:
- **Maven**: prefer `mvn -q -DskipTests package` for compile checks;
  full tests with `mvn -pl oa-<module> test -Dspring.profiles.active=ci`.
- **Frontend**: `pnpm typecheck` after any TS/Vue change; do not run
  `pnpm dev` (hermes will hang on the long-running dev server — use
  background mode if you must).
- **Auth/permission**: any new endpoint must use `@RequirePermission`
  with `module:action` format; never bypass `AuthInterceptor`.
- **Exception handling**: throw `BusinessException` (code -1),
  `AuthException` (code 401), or `SystemException` — never `RuntimeException`.
- **DB**: all new tables need `delFlag TINYINT DEFAULT 0` for logical delete.
- **MyBatis-Plus**: use `@TableField(fill = FieldFill.INSERT/UPDATE)` for
  `createTime` / `updateTime`.

### Stage 3 — TEST

Invoke the bundled **`test-driven-development`** skill (RED → GREEN →
REFACTOR) on the implemented code.

Project-specific test commands (in order of cost):
```bash
# 1. Fastest: focused single-class test (CI profile, ~5s)
mvn -pl oa-<module> test -Dtest=<ClassName>Test -Dspring.profiles.active=ci

# 2. Module-only full suite (~30s-2min)
mvn -pl oa-<module> test -Dspring.profiles.active=ci

# 3. Full backend suite (requires MySQL+Redis, ~5-10min)
mvn verify -Dspring.profiles.active=ci
```

CI profile uses `oa_system_test` DB and `test-jwt-secret-for-ci-only`
JWT secret. No env var setup required.

Frontend test:
```bash
cd code/frontend
pnpm typecheck
pnpm build
```

Playwright UI tests are skipped in dev loop; they belong in CI.

### Stage 4 — REVIEW

Invoke the bundled **`requesting-code-review`** skill (security scan,
quality gates, auto-fix, independent reviewer subagent).

Project-specific quality gates:
- `mvn checkstyle:check -pl oa-<module>` — must pass (config at
  `code/checkstyle.xml`).
- No new `System.out.println` / `printStackTrace` in production code
  (use the logger or `GlobalExceptionHandler`).
- New dependencies must be in `pom.xml` `<dependencyManagement>` of the
  parent POM, not module POMs.

### Stage 5 (optional) — REPORT

When the user types `/report`, the companion skill aggregates the diff,
test output, and review findings into a markdown report at
`.hermes/reports/YYYY-MM-DD_HHMMSS-<slug>.md`.

## Interruption handling

- If stage 1 (plan) is interrupted, no code is touched — safe.
- If stage 2 (implement) is interrupted, partial code may exist; run
  `/rollback` (TUI) or `git diff` to see state, then resume with
  `/dev-cycle continue`.
- Stages 3 and 4 are idempotent — safe to re-run.

## Output discipline

- **Never** commit or push without explicit user confirmation.
- **Never** run `git reset --hard` or `git checkout --` without showing
  the diff first.
- **Always** leave the working tree in a committable state when
  finishing a stage.
