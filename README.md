# LUMIERE Hotel Booking

![LUMIERE Banner](C:/Users/admin/.gemini/antigravity-ide/brain/aaf695af-57ca-402b-9720-79cef5b9ee02/readme_banner_1781080851979.png)

*Premium hotel booking web application built with Spring Boot, Thymeleaf, and modern UI/UX.*

## ✨ Highlights
- **Responsive UI** with light/dark theme (LUMIERE style)
- Secure authentication: password, OTP, Google/Facebook OAuth
- Admin dashboard for rooms, bookings, users, refunds
- Payment integrations: mock, VNPay, MoMo
- AI-powered room recommendations (OpenAI)

## 🛠️ Tech Stack
| Layer | Tools |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring MVC, Spring Security |
| Views | Thymeleaf, CSS, vanilla JavaScript |
| Data | Spring Data JPA, Flyway, PostgreSQL, H2 |
| Auth | Form login, OTP, OAuth2 |
| Payments | Mock, VNPay, MoMo adapters |
| Messaging | Console, Brevo email/SMS |
| Testing | JUnit, Spring Test, Testcontainers, Playwright |
| Runtime | Gradle, Docker Compose |

## 🚀 Quick Start
```bash
git clone https://github.com/nphu0811/hotelbooking.git
cd hotelbooking
cp .env.example .env
./gradlew bootRun --args="--spring.profiles.active=local"
```
Open <http://localhost:8080>.

## 📦 Useful Commands
```bash
# Backend tests
./gradlew test

# Build jar
./gradlew bootJar

# Full build
./gradlew clean build

# Playwright end‑to‑end tests
npm install
npm run test:e2e
```

## 🐳 Docker
```bash
docker compose up -d postgres   # start PostgreSQL
```
The `app` service is production‑ready; provide required env vars (see below).

## ⚙️ Production Configuration
Set `SPRING_PROFILES_ACTIVE=prod` and supply:

```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
APP_PUBLIC_BASE_URL
APP_PAYMENT_PROVIDER
APP_EMAIL_PROVIDER
APP_SMS_PROVIDER
MAIL_FROM
```

### Payment provider vars
*VNPay* → `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, `VNPAY_RETURN_URL`, `VNPAY_IPN_URL`  
*MoMo* → `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY`, `MOMO_CREATE_URL`, `MOMO_RETURN_URL`, `MOMO_IPN_URL`

### Brevo (email/SMS)
`BREVO_API_KEY`, `BREVO_SMS_SENDER`

### Optional
`OPENAI_API_KEY`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `FACEBOOK_CLIENT_ID`, `FACEBOOK_CLIENT_SECRET`, ...

## 📂 Project Layout
```
src/main/java/com/example/demo
  config/      # security, providers, validators
  controller/  # web routes
  entity/      # JPA models
  payment/     # adapters
  repository/  # Spring Data repos
  service/     # business logic

src/main/resources
  templates/   # Thymeleaf views
  static/      # CSS, JS, images
  db/migration/ # Flyway scripts
```

## 🧭 Notes
- Keep secrets out of version control; use environment variables.
- Use `local` profile for development (H2, mock providers, seeded data).
- AI recommendation endpoint works when `OPENAI_API_KEY` is set.

---

*Happy coding! 🎉*
