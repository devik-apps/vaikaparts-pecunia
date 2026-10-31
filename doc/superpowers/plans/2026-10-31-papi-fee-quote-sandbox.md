# PAPI Fee Quote and Sandbox Support — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Vaika safely quote and retain the exact PAPI payer fee and total for MVola, Airtel Money, and Orange Money, and verify the flow against a PAPI sandbox shop.

**Architecture:** Pecunia remains the only service holding PAPI's `Token`. Add a typed proxy for PAPI's `GET /v1/payments/fees`, expose it as an authenticated Pecunia endpoint, and preserve `payerFee`/`totalAmount` returned by both quote and payment APIs.

**Tech Stack:** Java 21, Spring Boot, Gradle, RestTemplate, JPA/Flyway, JUnit/Mockito, OpenAPI.

**Contract references:** [PAPI fee quote API](https://docs.papi.mg/fr/docs/api/direct-mobile-money/get-payment-fees/), [PAPI direct payment API and sandbox behavior](https://docs.papi.mg/fr/docs/api/direct-mobile-money/initiate-payment/), [PAPI own-checkout guide](https://docs.papi.mg/fr/docs/developper-guide/payment-api/).

## Global Constraints

- Domain supplies a base payment amount of exactly `3000 MGA`; Pecunia must not add fees to the requested `amount`.
- The displayed payer total is PAPI's `totalAmount` (`amount + payerFee`), not a locally calculated commission.
- `payerFee` may be zero and varies with the payment method and PAPI merchant settings.
- Keep the PAPI API key in Pecunia only; never return it, the provider `Token`, or callback secrets.
- Sandbox validation uses a PAPI sandbox-shop key and sandbox test numbers; do not test with a production key.

## Review Focus

- A zero payer fee is a valid quote and must produce `totalAmount == amount`.
- Unsupported or disabled methods and amounts below 300 MGA must fail without fabricating a quote.
- Provider fee data missing, negative, wrong-currency, or inconsistent with `amount + payerFee` must not be reported as valid.
- A missing/invalid PAPI API key or a provider timeout must return a safe service error without leaking credentials.
- Existing PAPI payment/status responses must remain compatible when fee fields are absent on historical rows.

---

### Task 1: Add the typed PAPI fee-quote client

**Files:**
- Create: `src/main/java/com/devikapps/vaikaparts/client/papi/model/PapiFeeQuoteResponse.java`
- Modify: `src/main/java/com/devikapps/vaikaparts/client/papi/PapiClient.java`
- Test: `src/test/java/com/devikapps/vaikaparts/client/papi/PapiClientTest.java`

**Interfaces:**
- Consumes PAPI `GET /v1/payments/fees?paymentMethod=<method>&amount=<integer>`.
- Produces a typed quote with `paymentMethod`, `amount`, `payerFee`, `totalAmount`, and `currency`.

- [ ] Add client tests for all three supported methods, URL encoding/query parameters, `Token` header, zero/nonzero fee, and upstream 400/401/5xx/network errors.
- [ ] Run `./gradlew test --tests '*PapiClientTest' --no-daemon` and confirm the new tests fail before implementation.
- [ ] Implement `PapiClient.getFees(paymentMethod, amount)` using the existing configured base URL, API key header, and error mapping.
- [ ] Validate the returned method, integer MGA base amount, nonnegative fee, MGA currency, and exact `totalAmount == amount + payerFee` before returning it.
- [ ] Run `./gradlew test --tests '*PapiClientTest' --no-daemon` and confirm all quote-client tests pass.
- [ ] Commit as `feat: add PAPI fee quote client`.

### Task 2: Expose the quote through Pecunia and retain actual payment fees

**Files:**
- Modify: `src/main/java/com/devikapps/vaikaparts/endpoint/rest/controller/PapiController.java`
- Create: `src/main/java/com/devikapps/vaikaparts/model/PapiFeeQuote.java` and REST response model as required by current endpoint conventions.
- Modify: `src/main/java/com/devikapps/vaikaparts/model/PapiPayment.java`, `model/PapiPaymentResponse.java`, `gateway/papi/PapiPaymentGateway.java`, `service/PapiPaymentStatusService.java`, `mapper/PapiPaymentMapper.java`, and `repository/model/JPapiPayment.java`.
- Add migration: `src/main/resources/db/migration/V1_0_8__Persist_papi_fee_totals.sql`.
- Modify: `doc/api.yaml`.
- Test: add `PapiControllerTest` and service/status persistence tests under matching `src/test/java` packages.

**Interfaces:**
- Produces authenticated `GET /v1/payments/papi/fees?paymentMethod=MVOLA&amount=3000` with a quote response containing method, amount, payer fee, total, and currency.
- Extends `PapiPayment` responses additively with nullable `payerFee` and `totalAmount` so older stored payments remain readable.

- [ ] Add controller tests for each method, missing/invalid parameters, auth protection, and safe propagation of disabled-method/upstream errors.
- [ ] Add service tests proving initiation and status refresh save the actual PAPI `payerFee` and `totalAmount`, including a zero fee.
- [ ] Run focused tests and confirm they fail because the endpoint and fields do not exist.
- [ ] Add the authenticated `/fees` mapping under `/v1/payments/papi`; validate integer `amount >= 300` and the three allowed methods before calling the PAPI client.
- [ ] Add nullable fee columns to `papi_payments`; update the persistence mapper/status service and PAPI response mapping without changing the base `amount`.
- [ ] Add the fee endpoint and optional payment response fields to `doc/api.yaml`; document that totals are merchant-configured and quote values are the display source.
- [ ] Run focused Pecunia tests and `make format-check`; confirm migration and OpenAPI validation pass.
- [ ] Commit as `feat: expose PAPI payer fee quotes`.

### Task 3: Verify Pecunia against the PAPI sandbox

**Files:**
- Test/configuration only; do not commit any PAPI key or real payer credential.

- [ ] Obtain a sandbox-shop API key and confirm all three methods are enabled on that sandbox shop.
- [ ] Configure `PAPI_API_KEY` from the sandbox shop and `PAPI_BASE_URL=https://app.papi.mg/engine/api`; use sandbox test accounts only.
- [ ] Call the Pecunia quote endpoint for each method with `amount=3000`; confirm the response preserves PAPI's exact `payerFee` and `totalAmount` (including a valid `payerFee=0` if returned).
- [ ] Initiate a sandbox payment for MVola `0341230001`, Airtel `331230001`, and Orange `0321230001`; obtain the Orange temporary code from the sandbox instructions rather than inventing one.
- [ ] Confirm pending/success/failure status reads retain the same method, base amount, payer fee, and total, and confirm PAPI sandbox dashboard marks the transactions as tests.
- [ ] Run `make test-unit`; run `make test-integration` when Docker-backed dependencies are available.
- [ ] Record the sandbox verification evidence and commit no secrets or real customer data.

## Handoff

Domain depends on Task 2's authenticated quote and payment response fields. Frontend depends on the domain API contract, not on PAPI credentials. PAPI's documentation states that sandbox shops always create test payments and gives success test numbers for the three operators; do not use a production shop/key for this smoke test.
