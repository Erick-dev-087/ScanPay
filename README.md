# ScanPay — Kotlin Android + Ktor + Astra Cassandra

The new project is self-contained in this folder. Open **Kotlin_Scan_Pay** in Android Studio.

The original Java/XML app supplied the navigation, QR scanner, payment confirmation and green Material visual direction. The local checkout of [qr-pay-system](https://github.com/Erick-dev-087/qr-pay-system) at commit `c69783f` supplied the authentication, till/paybill QR and STK/callback contracts. This implementation uses Kotlin on Android and the server, and native Cassandra CQL rather than PostgreSQL or Astra document collections.

## Implemented

- Customer and merchant registration/login, encrypted Android session storage, JWT role checks and server-side logout revocation.
- Static till/paybill QR codes (customer enters amount) and dynamic bill QR codes (fixed amount).
- Registered-payload validation, camera permission handling, QR image upload and manual QR-content entry.
- QR image preview, PNG sharing, actual file saving and reopening a merchant's saved QRs.
- M-Pesa STK initiation, persistent idempotency keys, payment status polling and provider receipt verification.
- Merchant revenue and customer spending dashboards, 7/30/90-day charts, categories from merchant category codes and paginated payment history.
- Monthly Cassandra history partitions, prepared CQL, logged index batches and conditional payment-state updates.
- No successful-payment inference from timeouts, no real-to-mock fallback, and no invented wallet balance.

## Build and run

Use JDK 17 or newer (Android Studio's bundled JBR works). Android SDK 35 and minimum Android API 28 are used.

PowerShell, from this folder:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
./gradlew.bat :backend:test :app:assembleDebug
./gradlew.bat :backend:run
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`.

The emulator uses `http://10.0.2.2:5000/api/`. For a physical phone or deployment:

```powershell
./gradlew.bat :app:assembleDebug -PSCANPAY_API_URL=https://your-backend.example/api/
```

Release builds block cleartext HTTP. Use HTTPS for the deployed API. The endpoint must end with `/api/`.

## Astra configuration

Your existing **JSON** `.env` is supported. Conventional dotenv files are also supported; see [.env.example](.env.example). Process environment values override either format. The APK never reads this file.

The server needs an Astra application token, database ID, existing keyspace and a JWT secret of at least 32 characters. It obtains the secure connect bundle using Astra DevOps and stores it in ignored `.local/`. Alternatively set `ASTRA_SECURE_CONNECT_BUNDLE` to an existing bundle. Configure a token with the keyspace privileges needed by [schema.cql](backend/src/main/resources/schema.cql).

On startup, the server connects through the Cassandra Java driver and creates the tables if `ASTRA_AUTO_MIGRATE=true`. No keyspace is created. Set that flag to false after provisioning if runtime credentials should have only data access. Database errors produce failed requests, never in-memory success.
All application tables use the `scanpay_` prefix to avoid colliding with the existing tables/collections in your Astra keyspace.

Health check: `GET /api/health` checks an actual CQL query.
Run `./gradlew.bat :backend:checkAstra` for a read-only connection check that does not create tables.

History covers the past 366 days. Analytics reads all transactions in the requested 1–365-day range, and includes only confirmed successful payments. Dates use Africa/Nairobi. Amounts are whole KES from 1 to 500,000 to match the STK request.

## M-Pesa setup

The supplied environment has placeholder Daraja consumer credentials, so **live STK is not configured yet**. Set these on the server:

- `DARAJA_CONSUMER_KEY`, `DARAJA_CONSUMER_SECRET`, `DARAJA_PASSKEY`.
- `DARAJA_APPROVED_SHORTCODES`: the shortcode(s) provisioned for those credentials. A public till number alone does not authorize an STK request. This implementation uses one provisioned credential/passkey set; independent merchant credential sets or an aggregator need a payment-provider extension.
- `PAYMENT_CALLBACK_SECRET`: a random secret with at least 32 characters.
- `DARAJA_CALLBACK_URL`: a public HTTPS URL routed to **this backend**, ending in `/api/payment/stk_callback?secret=<the callback secret>`. A proxy that supplies `X-Callback-Secret` can omit the query parameter. Avoid logging the secret at the proxy.
- `DARAJA_BASE_URL`: Safaricom sandbox by default; use approved production credentials and endpoint for real payments.

The flow is scan → merchant/amount confirmation → STK request → PIN entered in the M-Pesa prompt → callback → confirmed receipt. ScanPay never asks for an M-Pesa PIN.

For paybill merchants, register the paybill **account number** separately from the shortcode. A per-QR reference can override the account; both are limited to 12 characters to avoid silently truncating an STK account.

Callbacks match checkout ID, amount and payer phone. Cassandra conditional updates permit only one transition out of pending. Repeated callbacks cannot credit money twice. Merchant payments go to M-Pesa directly, so this app does not represent those payments as a ScanPay wallet.

## Explicit local test mode

Set `PAYMENT_MODE=mock` before starting the backend. No M-Pesa request or prompt is sent. Register a merchant, create a QR, register a customer, scan and initiate payment. Only in this mode, an authenticated customer can complete their own test payment with:

```http
POST /api/payment/simulate_callback
Authorization: Bearer <customer token>
Content-Type: application/json

{"transaction_id":"<payment ID>"}
```

The resulting receipt starts with `TEST-`. The simulation route is absent in Daraja mode. An STK acceptance timeout remains pending/unknown and requires status checking or provider reconciliation; it never becomes a paid receipt.

## API

All paths below start with `/api`. Except registration/login, config/health and the shared-secret callback, endpoints require a bearer JWT.

| Method | Path | Purpose |
|---|---|---|
| POST | /auth/register/user | Customer registration |
| POST | /auth/register/vendor | Merchant registration including shortcode type/account |
| POST | /auth/login | Login |
| POST | /auth/logout | Revoke current JWT |
| POST | /qr/generate | Merchant-only static/dynamic QR |
| POST | /qr/scan | Customer-only registered QR validation |
| GET | /merchant/qr | Merchant's stored QRs |
| POST | /payment/initiate | Customer payment; requires Idempotency-Key header |
| GET | /payment/{id}/status | Payer or recipient payment status |
| POST | /payment/stk_callback | Daraja callback protected by shared secret |
| GET | /user/profile, /merchant/profile | Current account summary |
| GET | /user/transactions, /merchant/transactions | page/per_page history |
| GET | /user/analytics, /merchant/analytics | days-based analytics |

## Verification and remaining integration work

`:backend:test` covers QR generation/checksums, real callback receipts, invalid amounts/phones, Daraja authentication/rejection/rail selection and Nairobi date-range analytics. No test sends money.
The live Astra integration test also passed: native persistence, payload lookup, history partitions, duplicate confirmation rejection, analytics and retrieval after a fresh connection were verified. Its uniquely identified fixture records were removed afterward.

To run that optional test again:

```powershell
$env:SCANPAY_INTEGRATION_TESTS = 'true'
./gradlew.bat :backend:test
Remove-Item Env:SCANPAY_INTEGRATION_TESTS
```

The debug APK build and Android lint passed. Lint has zero errors; resource, localization and other warnings remain.

Live end-to-end M-Pesa verification still requires working Daraja credentials, a public callback, merchant provisioning and an M-Pesa-capable phone. Password-reset email delivery and wallet top-ups are not implemented; their endpoints return 501, and those actions are absent from the current navigation. Production onboarding, provider reconciliation of uncertain requests, rate limits and audit/monitoring infrastructure remain deployment work.

The partial starter previously contained Astra and JWT credentials in source. Those hardcoded defaults have been removed. Rotate the exposed credentials before deployment.

