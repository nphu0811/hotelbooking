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
- Retest command: `rg -n "DATABASE_PUBLIC_URL|kodama\.proxy\.rlwy\.net" .\hotel_booking_ai_build_prompt.md`
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
- Fix applied: Retried with concise payload and with a global payload; both failed with the same MCP validation error.
- Retest command: `create_design_system` with concise payload.
- Retest result: FAIL - Stitch MCP still reports `Request contains an invalid argument.`
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
- Fix applied: Pending.
- Retest command: Browser plugin initialization.
- Retest result: FAIL.
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
