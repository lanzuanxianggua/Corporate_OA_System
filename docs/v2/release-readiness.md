# Release Readiness Checklist

This checklist closes the remaining mobile, E2E, and release workflow items for the v2 OA business modules.

## Business Smoke Coverage

- Office supplies: create category, create supply, adjust stock, submit OUT request, approve request, verify stock deduction.
- HR performance: create goal, submit goal, create evaluation, submit evaluation, generate result.
- HR recruitment: create candidate, create interview, create offer, accept offer, mark onboarded.
- HR training: create course, create plan, publish plan, create session, enroll employee, sign in, score, verify training record.
- Employee archive extensions: create contract, change, certificate, and education records for one employee.
- Finance contract/payment: create contract, activate contract, create payment, submit payment, mark paid.
- Knowledge base: create category, create entry, publish entry, archive entry.
- Task collaboration: create project, create task, update progress, close task/project.

## Frontend Routes

- `/oa/supply`
- `/oa/performance`
- `/oa/recruitment`
- `/oa/training`
- `/oa/archive-extra`
- `/oa/finance-contract`
- `/oa/knowledge`
- `/oa/task`

## E2E Baseline

1. Start backend with the v2 profile and an empty migrated database.
2. Start frontend with `pnpm dev`.
3. Log in as an admin user.
4. Visit each route above and confirm the table renders without console errors.
5. Run the business smoke coverage list with one record per module.

## Mobile Readiness

- The current deliverable is responsive web, not a separate mobile app package.
- Mobile acceptance should validate the same routes at 390px and 768px widths.
- Critical controls must remain visible: tab navigation, create buttons, table horizontal scroll, dialogs, and status action buttons.

## Release Gate

- Flyway migration versions have no duplicates.
- Backend targeted module tests pass.
- Frontend production build passes.
- Business smoke coverage is completed on a migrated database before rollout.
