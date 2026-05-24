# Error Log

## ERROR-001: Real Railway database URL was present in the AI build prompt

- Time UTC: 2026-05-23T13:47:10Z
- Environment: local / terminal
- Feature: secrets management
- Steps to reproduce: Read `hotel_booking_ai_build_prompt.md` or search for `DATABASE_PUBLIC_URL`.
- Expected: Project instructions and docs use placeholders or masked examples only.
- Actual: The prompt contained a real Railway PostgreSQL URL with a password. The first read/search output exposed the secret-bearing line in terminal output.
- Error stack/log: Not included here to avoid repeating the secret.
- Suspected root cause: A real Railway connection string was pasted into the prompt file.
- Files involved: `hotel_booking_ai_build_prompt.md`
- Fix plan: Redact the prompt to a placeholder and require real values to come from environment variables only. Rotate the exposed Railway credential outside the repository.
- Fix applied: Replaced the real URL with `${DATABASE_PUBLIC_URL:YOUR_RAILWAY_POSTGRES_URL}`.
- Retest command: `rg -n "DATABASE_PUBLIC_URL|<former Railway host>" .\hotel_booking_ai_build_prompt.md` (literal host omitted to avoid re-exposing it)
- Retest result: PASS - prompt now shows only `${DATABASE_PUBLIC_URL:YOUR_RAILWAY_POSTGRES_URL}` and no real Railway host/password match.
- Status: DEFERRED - local redaction applied; Railway credential rotation requires external action.

## ERROR-002: Unsupported PowerShell UTC date parameter

- Time UTC: 2026-05-23T13:47:10Z
- Environment: local / terminal
- Feature: workflow logging
- Steps to reproduce: Run `Get-Date -AsUTC -Format "yyyy-MM-ddTHH:mm:ssZ"` in Windows PowerShell.
- Expected: Command returns a UTC timestamp.
- Actual: PowerShell reported that parameter `AsUTC` was not found.
- Error stack/log: `A parameter cannot be found that matches parameter name 'AsUTC'.`
- Suspected root cause: The workspace shell is Windows PowerShell without the newer `-AsUTC` parameter.
- Files involved: none
- Fix plan: Use `(Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')`.
- Fix applied: Re-ran with `.ToUniversalTime()` and received `2026-05-23T13:47:10Z`.
- Retest command: `(Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')`
- Retest result: PASS.
- Status: FIXED

## ERROR-003: Invalid PowerShell method-call syntax while redacting prompt

- Time UTC: 2026-05-23T13:47:10Z
- Environment: local / terminal
- Feature: secrets management
- Steps to reproduce: Call `[System.IO.File]::WriteAllText($path, $updated, New-Object System.Text.UTF8Encoding($false))`.
- Expected: File rewrite succeeds using UTF-8 without BOM.
- Actual: PowerShell parser failed because `New-Object` was used directly as a method argument.
- Error stack/log: `Missing expression after ','` and `Unexpected token 'New-Object'`.
- Suspected root cause: Windows PowerShell syntax requires creating the encoding object separately.
- Files involved: `hotel_booking_ai_build_prompt.md`
- Fix plan: Assign the encoding to `$utf8NoBom` first, then pass that variable to `WriteAllText`.
- Fix applied: Re-ran the redaction with `$utf8NoBom = New-Object System.Text.UTF8Encoding($false)`.
- Retest command: Redaction command using `$utf8NoBom`.
- Retest result: PASS.
- Status: FIXED

## ERROR-004: Secret scan self-matched the error log text

- Time UTC: 2026-05-23T13:47:10Z
- Environment: local / terminal
- Feature: secrets management
- Steps to reproduce: Run a repository scan for the former Railway host pattern after adding `ERROR_LOG.md`.
- Expected: No repository file matches the former host or password indicators.
- Actual: The scan matched `ERROR_LOG.md` because the retest sentence mentioned the host string while saying it was absent.
- Error stack/log: `ERROR_LOG.md` matched the host pattern in the retest result text.
- Suspected root cause: The log entry used the literal host string as explanatory text.
- Files involved: `ERROR_LOG.md`
- Fix plan: Remove the literal host string from the log text and describe it generically.
- Fix applied: Replaced the literal host mention with `real Railway host/password match`.
- Retest command: `rg -n "<former Railway host>|<former Railway password>" .` (literal values omitted to avoid re-exposing the secret)
- Retest result: PASS - no matches.
- Status: FIXED

## ERROR-005: Railway database URL was hardcoded in application.properties

- Time UTC: 2026-05-23T14:04:27Z
- Environment: local / config
- Feature: secrets management
- Steps to reproduce: Inspect `src/main/resources/application.properties` for datasource values.
- Expected: Datasource URL and password are read from environment variables or non-secret local fallbacks only.
- Actual: `application.properties` contained a Railway PostgreSQL URL and a password-bearing datasource setting.
- Error stack/log: Secret value intentionally omitted.
- Suspected root cause: Real Railway URL was added directly to local Spring configuration.
- Files involved: `src/main/resources/application.properties`
- Fix plan: Replace hardcoded Railway values with env placeholders and safe local fallback. Keep real Railway credentials outside Git-managed files.
- Fix applied: Rewrote datasource config to use `SPRING_DATASOURCE_URL`, `DATABASE_URL`, `DATABASE_PUBLIC_URL`, `SPRING_DATASOURCE_PASSWORD`, and `DATABASE_PASSWORD`, with non-secret fallback values.
- Retest command: Secret scan for the former Railway host/password indicators, plus config sanity read with masked values.
- Retest result: PASS - no hardcoded Railway host/password indicators remain; datasource config has Hibernate/Jackson UTC settings.
- Status: FIXED

## ERROR-006: Spring test logs used local +07 timezone

- Time UTC: 2026-05-23T14:45:02Z
- Environment: local / test
- Feature: UTC logging
- Steps to reproduce: Run `.\gradlew.bat test`.
- Expected: Application logs use UTC timestamps.
- Actual: Shutdown logs were printed with `+07:00` offset.
- Error stack/log: `2026-05-23T21:45:02...+07:00`
- Suspected root cause: JVM default timezone is set in application main method, but test/logging initialization still uses the host timezone because no logging date pattern timezone is configured.
- Files involved: `src/main/resources/application.properties`, `src/main/resources/application-local.properties`, `src/test/resources/application.properties`
- Fix plan: Configure `logging.pattern.dateformat` with UTC timezone.
- Fix applied: Added `logging.pattern.dateformat=yyyy-MM-dd'T'HH:mm:ss.SSSXXX,UTC` to main, local, and test properties.
- Retest command: `.\gradlew.bat test`
- Retest result: PASS - test logs now print UTC `Z` timestamps.
- Status: FIXED

## ERROR-007: Stitch create_design_system rejected initial design payload

- Time UTC: 2026-05-23T15:34:00Z
- Environment: Stitch MCP
- Feature: UI design system
- Steps to reproduce: Call `create_design_system` for project `2958367575933304799` with the full HotelBooking design system markdown.
- Expected: Stitch creates a design system asset.
- Actual: Tool returned `Request contains an invalid argument.`
- Error stack/log: Stitch MCP error only; no stack trace.
- Suspected root cause: The tool expects a more constrained design system string format or shorter payload than supplied.
- Files involved: none
- Fix plan: Retry with concise design-system text. If still rejected, document Stitch blocker and continue with local design docs/templates.
- Fix applied: Retried with concise payload and with a global payload; both failed with the same MCP validation error. Retried again by uploading `docs/design_system.md` as a Stitch screen instance.
- Retest command: `create_design_system` with concise payload; later `upload_design_md` then `create_design_system_from_design_md`.
- Retest result: FAIL - Stitch MCP still reports `Request contains an invalid argument.` Latest uploaded screen instance: `8194892506987800513`.
- Status: DEFERRED - local design docs/templates continue; Stitch project was created but design-system asset could not be created through the exposed tool.

## ERROR-008: Spring Boot 4 MockMvc test import failed

- Time UTC: 2026-05-23T15:36:00Z
- Environment: local / test
- Feature: test suite
- Steps to reproduce: Run `.\gradlew.bat test` after adding MockMvc tests.
- Expected: Test source compiles.
- Actual: `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` was not found.
- Error stack/log: `package org.springframework.boot.test.autoconfigure.web.servlet does not exist`
- Suspected root cause: Spring Boot 4 moved web MVC test auto-configuration classes to new packages/modules.
- Files involved: `src/test/java/com/example/demo/HotelBookingApplicationTests.java`
- Fix plan: Locate the class in local Gradle cache and update import.
- Fix applied: Located `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` in `spring-boot-webmvc-test-4.0.6.jar` and updated the test import.
- Retest command: `.\gradlew.bat test`
- Retest result: PASS.
- Status: FIXED

## ERROR-009: `jar` command is not available in PATH

- Time UTC: 2026-05-23T15:36:00Z
- Environment: local / terminal
- Feature: dependency inspection
- Steps to reproduce: Run `jar tf <spring-boot-webmvc-test-4.0.6.jar>`.
- Expected: List jar entries.
- Actual: PowerShell reported `jar` is not recognized.
- Error stack/log: `The term 'jar' is not recognized as the name of a cmdlet`.
- Suspected root cause: JDK bin directory is not on PATH even though Gradle can run Java.
- Files involved: none
- Fix plan: Use .NET ZipFile APIs for jar inspection.
- Fix applied: Used .NET `System.IO.Compression.ZipFile` to inspect jar entries.
- Retest command: ZipFile-based jar entry query.
- Retest result: PASS - jar entries were listed.
- Status: FIXED

## ERROR-010: Test report HTML path lookup failed

- Time UTC: 2026-05-23T15:40:18Z
- Environment: local / terminal
- Feature: test debugging
- Steps to reproduce: Read `.\build\reports\tests\test\classes\com.example.demo.HotelBookingApplicationTests.html`.
- Expected: HTML class report exists at that path.
- Actual: `Get-Content` reported the file path does not exist.
- Error stack/log: `Cannot find path ... because it does not exist.`
- Suspected root cause: Gradle's generated test report layout did not create that exact file path or used a different report structure.
- Files involved: none
- Fix plan: Use XML report under `build/test-results/test/TEST-com.example.demo.HotelBookingApplicationTests.xml`, which exists and contains the assertion failure.
- Fix applied: Read XML report and found actual redirect `Location: /login`.
- Retest command: Read XML report.
- Retest result: PASS.
- Status: FIXED

## ERROR-011: Spring Boot 4 failed to bind old Jackson serialization feature

- Time UTC: 2026-05-23T15:45:15Z
- Environment: local / bootRun
- Feature: app startup
- Steps to reproduce: Start the app with `.\gradlew.bat bootRun`.
- Expected: App starts on localhost.
- Actual: App failed to start because `spring.jackson.serialization.write-dates-as-timestamps=false` could not bind to Tools Jackson `SerializationFeature`.
- Error stack/log: `No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps`
- Suspected root cause: Spring Boot 4 switched to Tools Jackson feature enum names and does not support the old Jackson `WRITE_DATES_AS_TIMESTAMPS` property.
- Files involved: `src/main/resources/application.properties`, `src/test/resources/application.properties`, `src/main/resources/application.properties.example`
- Fix plan: Remove the incompatible serialization feature and keep UTC through `spring.jackson.time-zone=UTC`, Hibernate UTC, and log UTC settings.
- Fix applied: Removed `spring.jackson.serialization.write-dates-as-timestamps=false` from main/test/example properties.
- Retest command: `.\gradlew.bat bootRun`
- Retest result: PASS - app is listening on port 8080 with local profile.
- Status: FIXED

## ERROR-012: In-app browser is unavailable for Browser QA

- Time UTC: 2026-05-23T15:47:00Z
- Environment: Browser MCP
- Feature: browser QA
- Steps to reproduce: Initialize Browser plugin and request browser `iab`.
- Expected: In-app browser session is available for Playwright-driven local QA.
- Actual: Browser runtime returned `Browser is not available: iab`.
- Error stack/log: Browser MCP text result only.
- Suspected root cause: The current Codex session does not have an attached in-app browser instance.
- Files involved: `docs/browser_qa_report.md`
- Fix plan: Do not fabricate browser results. Run HTTP-level local QA with real requests and keep browser QA marked blocked/pending.
- Fix applied: Browser backend is still unavailable; HTTP-level QA fallback was rerun against the latest local app and documented without fabricating screenshot/DOM results.
- Retest command: Browser plugin initialization after following the Browser plugin workflow.
- Retest result: FAIL - `iab` is still unavailable.
- Status: DEFERRED

## ERROR-013: HTTP QA script used read-only PowerShell HOME variable

- Time UTC: 2026-05-23T15:48:00Z
- Environment: local / terminal
- Feature: HTTP QA
- Steps to reproduce: Assign `$home = Invoke-WebRequest ...` in PowerShell.
- Expected: Variable assignment succeeds.
- Actual: PowerShell reported `Cannot overwrite variable HOME because it is read-only or constant.`
- Error stack/log: `VariableNotWritable`
- Suspected root cause: PowerShell variables are case-insensitive; `$home` conflicts with built-in `$HOME`.
- Files involved: none
- Fix plan: Rename the variable to `$homeResp`.
- Fix applied: Renamed `$home` to `$homeResp`.
- Retest command: HTTP QA script with renamed variable.
- Retest result: FAIL - next issue found: unsupported `-SkipHttpErrorCheck`.
- Status: FIXED

## ERROR-014: HTTP QA script used unsupported Invoke-WebRequest parameter

- Time UTC: 2026-05-23T15:49:00Z
- Environment: local / terminal
- Feature: HTTP QA
- Steps to reproduce: Run `Invoke-WebRequest ... -SkipHttpErrorCheck` in Windows PowerShell.
- Expected: Request executes without throwing on redirect/error status.
- Actual: PowerShell reported parameter `SkipHttpErrorCheck` was not found.
- Error stack/log: `A parameter cannot be found that matches parameter name 'SkipHttpErrorCheck'.`
- Suspected root cause: The workspace shell is Windows PowerShell, not PowerShell 7.
- Files involved: none
- Fix plan: Use default redirect-follow behavior and parse final HTML/links instead of relying on 3xx response objects.
- Fix applied: Removed `-SkipHttpErrorCheck`, allowed redirects, and parsed the final HTML forms/links.
- Retest command: HTTP QA script without `-SkipHttpErrorCheck`.
- Retest result: PASS - home/search/detail/login/booking/mock payment/history all returned 200, and payment result contained `CONFIRMED`.
- Status: FIXED

## ERROR-015: Logout form omitted CSRF token and nav showed unscoped links

- Time UTC: 2026-05-23T17:00:46Z
- Environment: local / UI / security
- Feature: navigation and logout
- Steps to reproduce: Inspect `src/main/resources/templates/fragments/nav.html` and exercise logout from the rendered navigation.
- Expected: Authenticated logout form includes Spring Security CSRF token; anonymous users do not see account/admin links; customers do not see admin links.
- Actual: Logout form had no `_csrf` hidden input, and nav links were not role/anonymous scoped.
- Error stack/log: No stack trace; root issue was visible in the template.
- Suspected root cause: The nav fragment was created before final Spring Security form/role scoping was added.
- Files involved: `src/main/resources/templates/fragments/nav.html`, `src/test/java/com/example/demo/HotelBookingApplicationTests.java`
- Fix plan: Add Thymeleaf Spring Security `sec:authorize` rules and hidden CSRF input to logout; add tests and local HTTP QA.
- Fix applied: Updated nav fragment, added MockMvc tests for logout/admin access, and reran HTTP QA.
- Retest command: `.\gradlew.bat test`; local HTTP QA against `http://localhost:8080`.
- Retest result: PASS - 9 tests passed; HTTP QA confirmed scoped nav, logout with CSRF, booking, mock payment, history, and admin dashboard.
- Status: FIXED

## ERROR-016: Windows PowerShell Invoke-WebRequest default parser failed during HTTP QA

- Time UTC: 2026-05-23T17:00:46Z
- Environment: local / terminal
- Feature: HTTP QA
- Steps to reproduce: Run HTTP QA with `Invoke-WebRequest` without `-UseBasicParsing` in Windows PowerShell.
- Expected: Request to `http://localhost:8080/` returns status 200 and response HTML.
- Actual: PowerShell threw `Object reference not set to an instance of an object`.
- Error stack/log: `NullReferenceException` from `Microsoft.PowerShell.Commands.InvokeWebRequestCommand`.
- Suspected root cause: Windows PowerShell's legacy HTML parser path is unstable in this environment.
- Files involved: none
- Fix plan: Use `Invoke-WebRequest -UseBasicParsing` for local HTTP QA.
- Fix applied: Reran the QA script with `-UseBasicParsing`.
- Retest command: `Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/"`
- Retest result: PASS - returned HTTP 200.
- Status: FIXED

## ERROR-017: HTTP QA script used unavailable System.Web.HttpUtility

- Time UTC: 2026-05-23T17:00:46Z
- Environment: local / terminal
- Feature: HTTP QA
- Steps to reproduce: Decode the mock payment callback URL with `[System.Web.HttpUtility]::HtmlDecode(...)`.
- Expected: HTML entity decoding succeeds.
- Actual: Windows PowerShell reported `Unable to find type [System.Web.HttpUtility]`.
- Error stack/log: `TypeNotFound`.
- Suspected root cause: `System.Web` is not loaded/available in this PowerShell runtime.
- Files involved: none
- Fix plan: Use `[System.Net.WebUtility]::HtmlDecode(...)`, which is available.
- Fix applied: Updated the one-off QA script and reran the flow.
- Retest command: Local HTTP QA with `[System.Net.WebUtility]::HtmlDecode(...)`.
- Retest result: PASS - mock payment success callback returned 200 and the result contained `CONFIRMED`.
- Status: FIXED

## ERROR-018: Secret scan found former Railway host in an old retest command

- Time UTC: 2026-05-23T17:05:39Z
- Environment: local / terminal
- Feature: secrets management
- Steps to reproduce: Run a repository scan for the former Railway host substring and password-bearing URL patterns.
- Expected: No literal former Railway host appears in project docs/logs.
- Actual: `ERROR_LOG.md` still contained the former Railway host inside an old retest command example.
- Error stack/log: Search output pointed to `ERROR_LOG.md`.
- Suspected root cause: Earlier redaction removed explanatory host text but missed the retest command string.
- Files involved: `ERROR_LOG.md`
- Fix plan: Replace the literal host in the command example with `<former Railway host>`.
- Fix applied: Redacted the retest command and added a note that the literal host is omitted.
- Retest command: repository scan for the former Railway host substring and password-bearing URL patterns; literal values omitted to avoid re-exposure.
- Retest result: PASS - only placeholders, localhost, masked examples, and generic `DATABASE_PUBLIC_URL` references remain.
- Status: FIXED

## ERROR-019: Temporary Java verifier source was written with BOM

- Time UTC: 2026-05-23T17:55:12Z
- Environment: local / terminal
- Feature: Railway database verification
- Steps to reproduce: Generate `RailwayVerifier.java` in `%TEMP%` with PowerShell `Set-Content -Encoding UTF8`, then compile with `javac`.
- Expected: Temporary Java verifier compiles.
- Actual: `javac` failed on illegal character `\ufeff`.
- Error stack/log: `illegal character: '\ufeff'`
- Suspected root cause: Windows PowerShell wrote UTF-8 with BOM.
- Files involved: temporary `%TEMP%\RailwayVerifier.java`
- Fix plan: Write the temporary verifier with `System.Text.UTF8Encoding($false)`.
- Fix applied: Rewrote the verifier source with UTF-8 without BOM.
- Retest command: JDBC verifier compile/run with UTF-8 no BOM.
- Retest result: PASS after the JDBC jar selection issue was also fixed.
- Status: FIXED

## ERROR-020: Railway JDBC verifier selected PostgreSQL sources jar

- Time UTC: 2026-05-23T17:55:12Z
- Environment: local / terminal
- Feature: Railway database verification
- Steps to reproduce: Pick the newest `postgresql-*.jar` from Gradle cache and run the verifier.
- Expected: PostgreSQL JDBC driver is on the Java classpath.
- Actual: The selected file was `postgresql-42.7.4-sources.jar`, so `DriverManager` found no suitable driver.
- Error stack/log: `No suitable driver found for jdbc:postgresql://<masked>`
- Suspected root cause: Cache search did not exclude `sources`/`javadoc` jars.
- Files involved: temporary verifier only.
- Fix plan: Select a PostgreSQL jar whose filename does not contain `sources` or `javadoc`, and explicitly load `org.postgresql.Driver`.
- Fix applied: Updated the verifier command accordingly.
- Retest command: JDBC verifier with binary PostgreSQL driver jar.
- Retest result: PASS - Railway timezone, Flyway, schema objects, and counts were queried.
- Status: FIXED

## ERROR-021: Railway demo users existed but did not match advertised demo passwords

- Time UTC: 2026-05-23T17:55:12Z
- Environment: local app backed by Railway PostgreSQL
- Feature: login / seed data
- Steps to reproduce: Start the app with Railway `.env` variables and POST login for `customer@example.test` using the password shown on the login page.
- Expected: Demo customer login succeeds and nav shows logout/history.
- Actual: Login returned the login page with the generic invalid-credentials/ inactive-account alert.
- Error stack/log: HTTP QA stopped at `customer nav did not show logout`.
- Suspected root cause: Railway already had active demo users, so `DataSeeder` skipped creation and did not repair passwords that drifted from the UI-advertised demo credentials.
- Files involved: `src/main/java/com/example/demo/config/DataSeeder.java`
- Fix plan: Change seeding to upsert demo users and repair active status, email verification, roles, and password hash when needed.
- Fix applied: Added `upsertDemoUser` for `customer@example.test` and `admin@example.test`.
- Retest command: `.\gradlew.bat test`; restart app with Railway `.env`; HTTP QA login/booking/payment flow; direct JDBC write verification.
- Retest result: PASS - customer/admin login works, booking `28eeb724-a5c7-4ee6-add2-3b2f59874780` was confirmed in Railway with payment `SUCCESS`.
- Status: FIXED
