# Frontend Build Report

## Commands Run
- `.\gradlew.bat clean build --no-daemon`

## Build Result
- First run failed during `clean` because a previous local Java process held `build/codex-bootrun.out` and `build/codex-bootrun.err`.
- Stopped the stale local Java process that owned the bootRun logs.
- Second run failed one auth test because password-login failures now redirect to `/login/password`.
- Updated the test expectations and added coverage for `/login/otp`.
- Final run passed.

## Errors Found
- Locked build output files from stale local process.
- Test expectation still pointed to old `/login?error` password failure route.

## Fixes Applied
- Stopped stale Java process `20460`.
- Updated `HotelBookingApplicationTests.repeatedLoginFailuresShowCaptchaAndLockAccount`.
- Added `otpLoginRouteRendersSeparateOtpEntryPage`.

## Final Build Status
PASS
