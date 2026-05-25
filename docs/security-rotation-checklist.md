# Security Rotation Checklist

Use this checklist when a database URL, password, API key, webhook secret, mail password, or service token may have been exposed.

## Railway/PostgreSQL Credential Rotation

1. Open the Railway project and create a new PostgreSQL password/connection string.
2. Revoke or rotate the old credential so it can no longer connect.
3. Update Railway service variables:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - Any `DATABASE_URL`/`DATABASE_PUBLIC_URL` variables used by scripts.
4. Update local shell or private `.env` values. Do not commit the new values.
5. Redeploy with `SPRING_PROFILES_ACTIVE=prod`.
6. Run Flyway migrations against the new credential.
7. Run smoke tests:

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
```

8. Verify app health and one read-only search flow.
9. Verify admin login only with an intentionally provisioned admin account.
10. Review logs for failed connections using the old credential.

## If A Secret Was Committed

- Remove the secret from the working tree.
- Rotate/revoke the exposed secret immediately.
- Treat the old value as compromised even if the repo is private.
- Purge it from Git history before sharing or making the repository public.
- Recreate any derived credentials or tokens that may have been generated from it.

## Current Repository Notes

- `.env` is ignored and must remain local-only.
- No new secret should be written to this repository.
- Existing docs mention a previously exposed Railway credential. The actual value was not printed during this pass, but the credential should still be rotated before any portfolio, MVP, or production deployment.
