# Deployment Checklist

## Pre-Deploy

- Rotate any credential that was ever committed or displayed in logs.
- Set `SPRING_PROFILES_ACTIVE=prod`.
- Set PostgreSQL variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- Set `APP_PUBLIC_BASE_URL` to the public HTTPS staging/production origin.
- Set payment variables for the selected provider. For VNPay: `APP_PAYMENT_PROVIDER=vnpay`, `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, `VNPAY_RETURN_URL`, `VNPAY_IPN_URL`.
- Set SMTP variables: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
- Leave `app.seed-demo-data=false` and `app.payment.mock.enabled=false` in production.

## Deploy

```bash
./gradlew clean build
docker build -t hotelbooking:prod .
docker run --env-file .env -p 8080:8080 hotelbooking:prod
```

## Verify

```bash
curl -f http://localhost:8080/actuator/health
curl -I http://localhost:8080/h2-console/
curl -I http://localhost:8080/payments/mock/start/test
curl -I http://localhost:8080/payments/vnpay/return?vnp_TxnRef=missing
```

Expected:

- Health endpoint returns `UP`.
- H2 console is denied in prod.
- Mock payment endpoints are denied in prod.
- Payment return is reachable and read-only; payment confirmation still requires signed IPN/webhook.
- Flyway migrations finish before the app accepts traffic.

## Data Import

Use the Overpass importer only from an operator shell or protected job:

```bash
./gradlew bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --app.import-hotels=true --app.import-hotels.exit=true --source=overpass --city=HCMC --limit=100"
```

For production, run the packaged jar with the same `--app.import-hotels` arguments and production database variables.

## Backup And Restore

- Schedule PostgreSQL backups before and after large imports.
- For Railway PostgreSQL, create an on-demand backup before deploying schema changes.
- Test restore into a non-production database before relying on a backup plan.
- Keep import runs idempotent; rerun with `--dry-run=true` first for new cities or providers.
