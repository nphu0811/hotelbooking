# Payment Provider Setup And Verification

VNPay is the first production payment provider for this project. MoMo is not exposed as a production configuration option until a real adapter and sandbox run are completed.

## VNPay Required Environment Variables

```properties
APP_PAYMENT_PROVIDER=vnpay
APP_PAYMENT_MOCK_ENABLED=false
VNPAY_TMN_CODE=<sandbox-or-production-tmn-code>
VNPAY_HASH_SECRET=<sandbox-or-production-hmac-secret>
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=https://<public-staging-host>/payments/vnpay/return
VNPAY_IPN_URL=https://<public-staging-host>/payments/vnpay/webhook
```

Production profile rejects mock payment, missing provider values, and placeholder-like values.
Do not commit provider credentials.

## Public Webhook Requirement

VNPay IPN needs a publicly reachable HTTPS URL. For local provider verification, expose the app through an approved tunnel or deploy to staging first, then set `VNPAY_IPN_URL` to that public endpoint.

## Sandbox Verification Procedure

1. Start the app with `SPRING_PROFILES_ACTIVE=prod` and the VNPay sandbox variables.
2. Register and verify a test user.
3. Create a booking and start payment.
4. Confirm the redirect URL goes to the VNPay sandbox host and contains a signed `vnp_SecureHash`.
5. Complete the sandbox payment in VNPay.
6. Confirm VNPay POSTs/IPNs to `/payments/vnpay/webhook`.
7. Verify:
   - valid signature confirms payment and booking,
   - duplicate webhook does not create a second confirmation,
   - invalid signature is rejected,
   - wrong amount is rejected,
   - wrong currency is rejected,
   - wrong booking/order id is rejected,
   - refund request moves to provider settlement state.

## Local Signed Fixture

When sandbox credentials or a public webhook URL are unavailable, run the local signed fixture tests:

```powershell
.\gradlew.bat test --tests "*VnPayPaymentFlowTests"
```

These tests exercise the project-side VNPay HMAC, webhook validation, idempotency, amount/currency checks, and refund state transitions with signed local payloads. They do not prove that VNPay accepted a real payment or delivered a real IPN.

## Current Verification Status

As of this repository verification pass, VNPay sandbox was not executed end-to-end against the provider because no confirmed public staging webhook URL was available. Project-side signed fixture verification passes.
