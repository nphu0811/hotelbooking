# Decisions

## DECISION-001: Use user-mandated tracking files for this project

- Date UTC: 2026-05-23
- Decision: `ERROR_LOG.md`, `DECISIONS.md`, and `TASKS.md` are the canonical files for errors, technical decisions, and phase progress.
- Rationale: The latest user instruction explicitly requires these filenames.
- Consequence: When the build prompt also asks for files such as `logs/qa_errors.md` or `docs/architecture_decisions.md`, future implementation work should either mirror important entries or clearly cross-reference the canonical files.

## DECISION-002: Real secrets must be replaced with placeholders or masked values

- Date UTC: 2026-05-23
- Decision: Do not store, echo, or document real Railway URLs, API keys, JWT secrets, SMTP passwords, or payment credentials. Use environment variables, `.env.example`, and masked values in reports.
- Rationale: The project rules prohibit hardcoded secrets and fake verification output.
- Consequence: The exposed Railway database credential from the prompt must be rotated outside the repository before any real Railway verification is trusted.

## DECISION-003: Phase completion requires executed verification

- Date UTC: 2026-05-23
- Decision: No phase should be marked complete in `TASKS.md` and no final wording should claim completion until the relevant tests, browser checks, and Railway checks have actually been run.
- Rationale: The user explicitly prohibited claiming completion without tests or fabricating browser/Railway results.
- Consequence: Untested work must be reported as pending, blocked, or partially implemented with exact missing checks.

## DECISION-004: Treat prior Railway verification as historical until credential rotation

- Date UTC: 2026-05-23
- Status: SUPERSEDED FOR CURRENT DEVELOPMENT TASK BY DECISION-005
- Decision: Any Railway verification performed before rotating the previously exposed PostgreSQL credential is historical only and must not be used as final Definition-of-Done evidence.
- Rationale: A real Railway database URL/password was exposed earlier. Verification after exposure is not trustworthy until the credential is rotated and injected only through environment variables.
- Consequence: This remains the recommended production posture, but the current development task proceeds under the accepted-risk exception below.

## DECISION-005: User accepted existing Railway credential for current verification

- Date UTC: 2026-05-23
- Decision: For the current task, use the existing Railway database/credential from local `.env` without requiring rotation.
- Rationale: The user explicitly stated the Railway database already has data and requested continuing with it without fixing/rotating the credential.
- Consequence: Continue to avoid printing or committing secret values. Treat the prior rotation requirement as an accepted risk for this development task, while still recommending rotation before broader sharing/deployment.
