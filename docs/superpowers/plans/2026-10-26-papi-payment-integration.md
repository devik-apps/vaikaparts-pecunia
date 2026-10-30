# Papi Payment Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Papi as a third, parallel payment provider, preserving MVola and Airtel Money behavior and routes.

**Architecture:** Add Papi-specific API models, HTTP client/gateway, joined persistence entity, service, controller routes and signed callback handling. Reuse the existing payment gateway factory, PostgreSQL/Flyway persistence, Spring HTTP stack and application status model. Additive migrations and API documentation keep existing integrations intact.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA, Flyway, PostgreSQL, JUnit 5, Mockito, WireMock.

**Spec:** `docs/superpowers/specs/2026-10-25-papi-payment-design.md`

## Global Constraints

- Keep the current MVola and Airtel Money routes, callbacks, clients and behavior intact.
- Papi API base URL defaults to `https://app.papi.mg/engine/api`; sandbox URL remains configurable.
- Send the API key only in the server-side `Token` header; never expose or log it.
- Configure the internal payment recipient with `PAPI_PARTNER_NAME` and `PAPI_PARTNER_MSISDN`, because `payments.payee_id` is mandatory in the existing schema.
- Papi accepts only `MVOLA`, `AIRTEL_MONEY`, and `ORANGE_MONEY`, integer `MGA` amounts of at least 300, and a required `reference`, `description`, `clientName`, and `phoneNumber`.
- `temporaryCode` is required only for `ORANGE_MONEY`; do not include card or Hosted Checkout flows.
- Papi statuses are `PENDING`, `SUCCESS`, and `FAILED`; HTTP 200 alone is not evidence of successful payment.
- Verify callbacks using raw request bytes, HMAC-SHA256, the full `pwhsec_` secret, constant-time comparison and a 300-second timestamp tolerance before parsing JSON.
- Add database changes through new Flyway migrations; do not edit migrations already applied to user databases.
- Use the project’s existing test conventions and add focused tests before implementation steps.

## Review Focus

- Fractional, sub-300, or non-MGA amounts must fail before reaching Papi. Pin this in the Papi request validator tests in Task 2.
- Papi can return HTTP 200 with `FAILED`, and MVola/Airtel initiation normally returns `PENDING`. Pin status mapping in Task 2 and service persistence in Task 4.
- Forged, stale, malformed, mismatched-token, unknown-reference, and duplicate callbacks must not mark a payment paid. Pin signature cases in Task 3 and callback behavior in Task 4.
- A retry must reuse the caller’s same reference and payload; a changed payload under a pending/successful reference must not initiate another charge. Pin idempotency and conflict handling in Task 4.
- Existing databases declare only `MVOLA` and `AR` in their original PostgreSQL enums even though the current application also uses Airtel Money and MGA. Pin additive enum migration behavior and existing-provider regression in Task 1.

---

### Task 1: Additive database enum compatibility

**Files:**
- Create: `src/main/resources/db/migration/V1_0_6__Add_papi_payment_provider_and_mga_currency.sql`
- Test: `src/test/java/com/devikapps/vaikaparts/repository/migration/PaymentEnumMigrationIT.java`

**Interfaces:**
- Consumes: Existing PostgreSQL types `payment_provider` and `payment_currency` from V1_0_0.
- Produces: `payment_provider` supports `MVOLA`, `AIRTEL_MONEY`, `PAPI`; `payment_currency` supports existing `AR` and `MGA`.

- [ ] **Step 1: Write migration compatibility tests**

Assert the migrated enum labels include the original `MVOLA`/`AR`, existing application values `AIRTEL_MONEY`/`MGA`, and new `PAPI`; verify a row can be inserted with each supported provider/currency pair.

- [ ] **Step 2: Run the focused migration test to verify it fails**

Run: `./gradlew.bat test --tests '*PaymentEnumMigrationIT'`
Expected: FAIL because the current schema does not define all application enum values.

- [ ] **Step 3: Add enum values in a new Flyway migration**

Use PostgreSQL `ALTER TYPE ... ADD VALUE IF NOT EXISTS` for `AIRTEL_MONEY`, `PAPI`, and `MGA`; retain all prior values and do not modify earlier migrations.

- [ ] **Step 4: Run the focused migration test**

Run: `./gradlew.bat test --tests '*PaymentEnumMigrationIT'`
Expected: PASS on a fresh database and a database already migrated through V1_0_5.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V1_0_6__Add_papi_payment_provider_and_mga_currency.sql src/test/java/com/devikapps/vaikaparts/repository/migration/PaymentEnumMigrationIT.java
git commit -m "fix(db): add Papi payment enum values"
```

### Task 2: Papi client, configuration and gateway

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/config/PapiConf.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/config/RestTemplateConf.java`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/java/com/devikapps/vaikaparts/client/papi/PapiClient.java`
- Create: `src/main/java/com/devikapps/vaikaparts/client/papi/model/PapiInitiatePaymentRequest.java`
- Create: `src/main/java/com/devikapps/vaikaparts/client/papi/model/PapiPaymentResponse.java`
- Create: `src/main/java/com/devikapps/vaikaparts/client/papi/model/PapiErrorResponse.java`
- Create: `src/main/java/com/devikapps/vaikaparts/model/PapiPaymentRequest.java`
- Create: `src/main/java/com/devikapps/vaikaparts/model/PapiPaymentResponse.java`
- Create: `src/main/java/com/devikapps/vaikaparts/model/classifier/PapiPaymentMethod.java`
- Create: `src/main/java/com/devikapps/vaikaparts/exception/PaymentConflictException.java`
- Create: `src/main/java/com/devikapps/vaikaparts/gateway/papi/PapiPaymentGateway.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/model/classifier/PaymentProvider.java`
- Create: `src/test/java/com/devikapps/vaikaparts/gateway/papi/PapiPaymentGatewayTest.java`
- Create: `src/test/java/com/devikapps/vaikaparts/client/papi/PapiClientTest.java`

**Interfaces:**
- Consumes: `PaymentGateway`, `PaymentRequestValidator`, Spring `RestClient`/`RestTemplate` conventions, Papi server API.
- Produces: `PapiClient.initiate(PapiInitiatePaymentRequest) -> client.papi.model.PapiPaymentResponse`; `PapiClient.getPayment(String reference) -> client.papi.model.PapiPaymentResponse`; `PapiPaymentGateway implements PaymentGateway` and reports `PaymentProvider.PAPI`.

- [ ] **Step 1: Add tests for client request contract and response parsing**

Verify `POST /v1/payments`, `GET /v1/payments/{encodedReference}`, the `Token` and JSON headers, and parsing of `data`, `error`, Papi references, notification token, and statuses.

- [ ] **Step 2: Run the client tests to verify they fail**

Run: `./gradlew.bat test --tests '*PapiClientTest'`
Expected: FAIL because the Papi client and DTOs do not exist.

- [ ] **Step 3: Implement configuration, HTTP client and API DTOs**

Bind `PAPI_API_KEY`, `PAPI_BASE_URL`, `PAPI_NOTIFICATION_URL`, `PAPI_WEBHOOK_SECRET`, `PAPI_TEST_MODE`, `PAPI_PARTNER_NAME`, and `PAPI_PARTNER_MSISDN`; default the base URL to `https://app.papi.mg/engine/api`. Use the existing Spring HTTP client stack and never log credentials.

- [ ] **Step 4: Add gateway tests for payload and status mapping**

Verify MVola/Airtel requests omit `temporaryCode`; Orange requests include it; `PENDING` maps to `PaymentStatus.PENDING`, Papi `SUCCESS` to `PaymentStatus.COMPLETED`, and Papi `FAILED` to `PaymentStatus.FAILED`, including HTTP 200 failure bodies. Verify an integer `MGA` amount at least 300 and reject fractional, smaller, missing, or unsupported values before the HTTP request. Verify 400/401/409/5xx errors become the project’s gateway exceptions and a 409 is distinguishable as an idempotency conflict.

- [ ] **Step 5: Run the gateway tests to verify they fail**

Run: `./gradlew.bat test --tests '*PapiPaymentGatewayTest'`
Expected: FAIL because the Papi gateway and mappings do not exist.

- [ ] **Step 6: Implement the Papi gateway**

Build the Papi body from the Papi-specific request, send initiation and lookup through `PapiClient`, map its rail to application provider `PAPI`, preserve the merchant reference as the transaction identifier, and map Papi statuses without treating HTTP 200 as automatic success. Implement `getPaymentStatus` by looking up the merchant reference; delegate `getPaymentDetails` to that lookup.

- [ ] **Step 7: Run both focused test classes**

Run: `./gradlew.bat test --tests '*PapiClientTest' --tests '*PapiPaymentGatewayTest'`
Expected: PASS, including amount validation and the three provider outcomes.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/devikapps/vaikaparts/config/PapiConf.java src/main/java/com/devikapps/vaikaparts/config/RestTemplateConf.java src/main/resources/application.yml src/main/java/com/devikapps/vaikaparts/client/papi src/main/java/com/devikapps/vaikaparts/model/PapiPaymentRequest.java src/main/java/com/devikapps/vaikaparts/model/PapiPaymentResponse.java src/main/java/com/devikapps/vaikaparts/model/classifier/PapiPaymentMethod.java src/main/java/com/devikapps/vaikaparts/exception/PaymentConflictException.java src/main/java/com/devikapps/vaikaparts/gateway/papi/PapiPaymentGateway.java src/main/java/com/devikapps/vaikaparts/model/classifier/PaymentProvider.java src/test/java/com/devikapps/vaikaparts/client/papi/PapiClientTest.java src/test/java/com/devikapps/vaikaparts/gateway/papi/PapiPaymentGatewayTest.java
git commit -m "feat(payment): add Papi payment gateway"
```

### Task 3: Papi persistence and callback signature verification

**Files:**
- Create: `src/main/resources/db/migration/V1_0_7__Create_papi_payments_table.sql`
- Create: `src/main/java/com/devikapps/vaikaparts/repository/model/JPapiPayment.java`
- Create: `src/main/java/com/devikapps/vaikaparts/model/PapiPayment.java`
- Create: `src/main/java/com/devikapps/vaikaparts/mapper/PapiPaymentMapper.java`
- Create: `src/main/java/com/devikapps/vaikaparts/repository/PapiPaymentRepository.java`
- Create: `src/main/java/com/devikapps/vaikaparts/service/papi/PapiSignatureVerifier.java`
- Create: `src/test/java/com/devikapps/vaikaparts/service/papi/PapiSignatureVerifierTest.java`
- Create: `src/test/java/com/devikapps/vaikaparts/service/PapiPaymentPersistenceIT.java`

**Interfaces:**
- Consumes: Task 1 enum values and Task 2 Papi request/response types.
- Produces: `JPapiPayment extends JPayment` storing `papiPaymentReference`, `merchantReference`, `paymentMethod`, and `notificationToken`; `PapiPaymentRepository.findByMerchantReference(String reference) -> Optional<JPapiPayment>`; `PapiSignatureVerifier.isValid(byte[] rawBody, String signatureHeader, String secret, Instant now) -> boolean`.

- [ ] **Step 1: Write signature verifier tests**

Use the documented Papi test vector with tolerance disabled via supplied `now`; assert valid HMAC succeeds and altered body, malformed/missing header, wrong secret, invalid hex, and timestamp beyond 300 seconds fail.

- [ ] **Step 2: Run signature tests to verify they fail**

Run: `./gradlew.bat test --tests '*PapiSignatureVerifierTest'`
Expected: FAIL because the verifier does not exist.

- [ ] **Step 3: Implement raw-byte HMAC verification**

Parse `t` and `v1`, compute HMAC-SHA256 over UTF-8 `t + "."` followed by the exact raw bytes, compare decoded bytes with `MessageDigest.isEqual`, and enforce 300 seconds using the injected/current time.

- [ ] **Step 4: Add persistence mapping tests**

Verify the Papi fields persist through the joined `payments`/`papi_payments` mapping, including null `notificationToken` before initiation response and values after response.

- [ ] **Step 5: Run persistence tests to verify they fail**

Run: `./gradlew.bat test --tests '*PapiPaymentPersistenceIT'`
Expected: FAIL because the entity, mapper, and table do not exist.

- [ ] **Step 6: Implement additive Papi persistence**

Widen `payments.transaction_id` to 255 characters (the Papi merchant reference limit), then create a joined table keyed to `payments.id`, store merchant reference with a unique constraint, and add the Papi entity/domain model/MapStruct mapper/repository following the MVola and Airtel patterns. Keep the Papi `paymentMethod` separate from application `PaymentProvider.PAPI`.

- [ ] **Step 7: Run verifier and persistence tests**

Run: `./gradlew.bat test --tests '*PapiSignatureVerifierTest' --tests '*PapiPaymentPersistenceIT'`
Expected: PASS, with all signature vectors and Flyway/JPA mapping assertions green.

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/db/migration/V1_0_7__Create_papi_payments_table.sql src/main/java/com/devikapps/vaikaparts/repository/model/JPapiPayment.java src/main/java/com/devikapps/vaikaparts/model/PapiPayment.java src/main/java/com/devikapps/vaikaparts/mapper/PapiPaymentMapper.java src/main/java/com/devikapps/vaikaparts/service/papi/PapiSignatureVerifier.java src/test/java/com/devikapps/vaikaparts/service/papi/PapiSignatureVerifierTest.java src/test/java/com/devikapps/vaikaparts/service/PapiPaymentPersistenceIT.java
git commit -m "feat(payment): persist Papi payment metadata"
```

### Task 4: Papi service, routes and signed callbacks

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/PapiController.java`
- Create: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/RPapiPaymentRequest.java`
- Create: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/PapiCallBackRequest.java`
- Create: `src/main/java/com/devikapps/vaikaparts/exception/PapiWebhookSecurityException.java`
- Create: `src/main/java/com/devikapps/vaikaparts/service/PapiPaymentService.java`
- Create: `src/main/java/com/devikapps/vaikaparts/service/PapiPaymentStatusService.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/config/SecurityConf.java`
- Create: `src/test/java/com/devikapps/vaikaparts/service/PapiPaymentServiceTest.java`
- Create: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/PapiControllerIT.java`

**Interfaces:**
- Consumes: Tasks 2 and 3; existing `PaymentRepository`, `PaymentPartyRepository`, `PaymentGatewayFactory`, `PaymentPartyMapper`, `PapiPaymentMapper`.
- Produces: `PapiPaymentService.initiatePayment(RPapiPaymentRequest) -> PapiPayment`; `getPayment(String reference) -> PapiPayment`; `handleCallBack(byte[] rawBody, String signatureHeader) -> void`.

- [ ] **Step 1: Write service and controller tests for the request contract**

Test `POST /v1/payments/papi` with `amount`, `description`, `type`, `payer`, `paymentMethod`, and stable `reference`; require `payer.name`; enforce `reference` length ≤255; require nonblank `temporaryCode` only for Orange Money; set internal `currency=MGA` and `provider=PAPI`.

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `./gradlew.bat test --tests '*PapiPaymentServiceTest' --tests '*PapiControllerIT'`
Expected: FAIL because the route and service do not exist.

- [ ] **Step 3: Implement initiation and local lookup**

Commit the pending payment and stable caller reference before calling the gateway so an immediate callback can find it. Retain the same reference and payload for network retries. Save the returned Papi payment reference and `notificationToken` under a row lock; preserve a terminal callback status if the callback wins the race with the initiation response. For an existing reference, return the existing PENDING/SUCCESS payment without a second charge when its amount, method and phone match; reject mismatches with a conflict; after FAILED, clear the prior attempt identifiers and allow a new POST to Papi with the same merchant reference (Papi creates a new attempt and returns a new Papi payment reference). `GET /v1/payments/papi/{reference}` returns the locally stored payment and refreshes PENDING status using Papi's read endpoint; a provider read error leaves the local PENDING state unchanged.

- [ ] **Step 4: Write callback tests for authentication, matching and idempotence**

Assert invalid/missing/stale signatures, malformed JSON, unknown merchant reference, and mismatched `notificationToken` return non-2xx and do not mutate payment state; valid PENDING/SUCCESS/FAILED notifications update the mapped internal `VerificationStatus`; duplicate valid final notifications return 2xx without repeating side effects.

- [ ] **Step 5: Run callback tests to verify they fail**

Run: `./gradlew.bat test --tests '*PapiControllerIT'`
Expected: FAIL because the callback route and verifier integration do not exist.

- [ ] **Step 6: Implement the signed notification route**

Expose `POST /v1/payments/papi/callback` as unauthenticated provider ingress through `SecurityConf` (both CSRF exemption and permit-all matcher); read `byte[]` before JSON parsing; validate `X-Papi-Signature`; then match `merchantPaymentReference` and `notificationToken`, map status and persist once. If an immediate callback arrives before initiation has saved the token, retrieve the authoritative Papi payment by reference and compare its token before processing. Return 2xx for verified duplicates and non-2xx for untrusted/unknown notifications.

- [ ] **Step 7: Run service and controller tests**

Run: `./gradlew.bat test --tests '*PapiPaymentServiceTest' --tests '*PapiControllerIT'`
Expected: PASS for initiation, lookup, Orange validation, conflict handling, signed callbacks and duplicate delivery.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/PapiController.java src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/RPapiPaymentRequest.java src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/model/PapiCallBackRequest.java src/main/java/com/devikapps/vaikaparts/exception/PapiWebhookSecurityException.java src/main/java/com/devikapps/vaikaparts/service/PapiPaymentService.java src/main/java/com/devikapps/vaikaparts/service/PapiPaymentStatusService.java src/main/java/com/devikapps/vaikaparts/config/SecurityConf.java src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/ApiExceptionHandler.java src/test/java/com/devikapps/vaikaparts/service/PapiPaymentServiceTest.java src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/PapiControllerIT.java
git commit -m "feat(payment): expose Papi payment endpoints"
```

### Task 5: OpenAPI, environment template and regression verification

**Files:**
- Modify: `doc/api.yaml`
- Modify: `.env.template`
- Modify: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/health/SwaggerDocIT.java`
- Test: `src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/MvolaControllerIT.java`
- Test: Existing Airtel controller/service/gateway tests.

**Interfaces:**
- Consumes: Completed route and DTOs from Task 4; Papi public API rules from the design.
- Produces: OpenAPI operation documentation for create/read/callback; documented server environment variables with no real values.

- [ ] **Step 1: Add an OpenAPI assertion to `SwaggerDocIT`**

Assert the documented route names, required request fields, accepted Papi methods, Orange code condition, callback signature header, and Papi payment response schema.

- [ ] **Step 2: Run OpenAPI checks to verify they fail**

Run: `./gradlew.bat test --tests '*SwaggerDocIT'`
Expected: FAIL because the Papi operations are not yet in `doc/api.yaml`.

- [ ] **Step 3: Document the additive API and configuration**

Add `POST /v1/payments/papi`, `GET /v1/payments/papi/{reference}`, and `POST /v1/payments/papi/callback` to `doc/api.yaml`. Add only placeholder variable names for `PAPI_API_KEY`, `PAPI_BASE_URL`, `PAPI_NOTIFICATION_URL`, `PAPI_WEBHOOK_SECRET`, `PAPI_TEST_MODE`, `PAPI_PARTNER_NAME`, and `PAPI_PARTNER_MSISDN` to `.env.template`.

- [ ] **Step 4: Run OpenAPI and payment regression tests**

Run: `./gradlew.bat test --tests '*SwaggerDocIT' --tests '*MvolaControllerIT' --tests '*MvolaPaymentServiceTest' --tests '*AirtelMoneyPaymentServiceTest' --tests '*AirtelMoneyPaymentGatewayTest'`
Expected: PASS; existing MVola and Airtel endpoint contracts remain unchanged.

- [ ] **Step 5: Run the full test suite**

Run: `./gradlew.bat test`
Expected: PASS with no Flyway, JPA, OpenAPI, or existing-provider regressions.

- [ ] **Step 6: Commit**

```bash
git add doc/api.yaml .env.template src/test/java/com/devikapps/vaikaparts/endpoint/rest/controller/health/SwaggerDocIT.java
git commit -m "docs(payment): document Papi payment API"
```
