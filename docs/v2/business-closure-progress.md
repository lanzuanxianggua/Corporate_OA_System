# Business Closure Progress

Date: 2026-06-07

## Completed

- Fixed duplicate Flyway versions:
  - `V988__msg_notification_types.sql` -> `V958__msg_notification_types.sql`
  - `V972__mt_core_tables.sql` -> `V965__mt_core_tables.sql`
  - `V973__mt_permissions.sql` -> `V966__mt_permissions.sql`
- Added `V992__missing_business_modules.sql` for office supplies, employee archive extensions, finance contracts, finance payments, and related permissions.
- Completed backend business coverage for:
  - Office supplies: categories, supplies, stock, request approval.
  - HR performance: goals, evaluations, generated results.
  - HR recruitment: candidates, interviews, offers, onboarding status.
  - HR training: plans, sessions, enrollments, sign-in, score, credit records.
  - Employee archive extensions: contracts, changes, certificates, education.
  - Finance contracts/payments: contract lifecycle and payment lifecycle.
- Completed frontend entry pages for:
  - `/oa/supply`
  - `/oa/performance`
  - `/oa/recruitment`
  - `/oa/training`
  - `/oa/archive-extra`
  - `/oa/finance-contract`
  - `/oa/knowledge`
  - `/oa/task`
- Added release and E2E readiness checklist: `docs/v2/release-readiness.md`.

## Validation

- Flyway duplicate version check: passed, no duplicate versions.
- Backend: `mvn -q test -DskipITs` passed.
- Frontend: `pnpm build` passed.
