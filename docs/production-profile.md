# Production Profile

Run production with:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
.\gradlew.bat bootRun
```

The production profile must be configured exclusively through environment variables. It must not use H2, seed demo data, or enable mock payment/email providers.

## Required Variables

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `APP_PAYMENT_PROVIDER`
- `APP_PUBLIC_BASE_URL`
- `VNPAY_TMN_CODE`
- `VNPAY_HASH_SECRET`
- `VNPAY_PAY_URL`
- `VNPAY_RETURN_URL`
- `VNPAY_IPN_URL`

## Forbidden Production Values

Production startup fails if a required value is empty or contains placeholder-like values such as:

- `mock`
- `changeme`
- `change-me`
- `your-key`
- `your_`
- `default`
- `placeholder`
- `example`

Production startup also fails if the datasource URL starts with `jdbc:h2:`.

## Production Defaults

- `app.seed-demo-data=false`
- `spring.h2.console.enabled=false`
- `app.payment.mock.enabled=false`
- `app.email.provider=smtp`
- `server.servlet.session.cookie.http-only=true`
- `server.servlet.session.cookie.secure=true`
- `server.servlet.session.cookie.same-site=lax`

`APP_PUBLIC_BASE_URL`, `VNPAY_RETURN_URL`, and `VNPAY_IPN_URL` must be public HTTPS URLs. `SameSite=Lax` is required so a browser returning from VNPay can keep the user session on a top-level redirect while still protecting cross-site POSTs.

## First Admin Account

Production must not auto-create an admin account. Provision the first admin through a separate one-time script or database operation with explicit approval, audit logging, and credential rotation after use.
