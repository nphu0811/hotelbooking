# Blockers

## BLOCKER-001: Railway credential rotation and env-only runtime verification

- Status: ACCEPTED RISK / CLOSED FOR CURRENT TASK
- Impact: The credential had previously been exposed, but the user explicitly instructed to continue using the existing Railway database without rotation.
- Current action: Verification was rerun from local `.env` variables without printing secrets, and app read/write behavior was confirmed against Railway PostgreSQL.
- Future recommendation: Rotate the Railway PostgreSQL password/URL before sharing the project or deploying beyond this controlled development context.

## BLOCKER-002: Browser automation backend unavailable

- Status: DEFERRED / EXTERNAL TOOL BLOCKER
- Impact: Screenshot/DOM browser automation cannot be completed in this Codex session.
- Current action: Browser plugin workflow was retried and still returned `Browser is not available: iab`. HTTP QA fallback passed against the latest local app.
- Future recommendation: Rerun Browser QA when an in-app browser backend is attached.

## BLOCKER-003: Stitch design-system asset creation rejected

- Status: DEFERRED / EXTERNAL TOOL BLOCKER
- Impact: The local design system is implemented, but a Stitch design-system asset could not be created.
- Current action: Retried `create_design_system`, then uploaded `docs/design_system.md` and retried `create_design_system_from_design_md`; Stitch still returned `Request contains an invalid argument`.
- Future recommendation: Retry with a fixed Stitch MCP contract or create the asset manually in Stitch.
