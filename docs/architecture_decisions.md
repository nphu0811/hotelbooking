# Architecture Decisions

Canonical project decisions are tracked in `DECISIONS.md`.

## Layered MVC

- Keep the current Spring Boot package `com.example.demo`.
- Use layered MVC packages: `controller`, `service`, `repository`, `entity`, `config`.
- Reason: current skeleton and project memory indicate layered architecture is the current decision.

## Local Dev Profile

- Use H2 with seeded synthetic hotel data for local/test.
- Use PostgreSQL/Flyway for production/Railway.
- Reason: real Railway credential was exposed and must be rotated before trusted verification.

## External Services

- Payment and email default to mock/sandbox-safe behavior.
- Real payment/email integrations must be activated through environment variables only.
