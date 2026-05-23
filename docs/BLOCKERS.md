# Blockers

## BLOCKER-001: Railway credential rotation and env-only runtime verification

- Status: OPEN
- Impact: Railway migration/browser DB verification cannot be trusted until the previously exposed credential is rotated and injected through environment variables.
- Required action: Rotate the Railway PostgreSQL password/URL in Railway, then provide it through environment variables in the terminal or deployment platform.
- Workaround: Continue local implementation with H2/mock services and placeholder configs.
