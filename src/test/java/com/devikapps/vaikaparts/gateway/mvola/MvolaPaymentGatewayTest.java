package com.devikapps.vaikaparts.gateway.mvola;

import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_BASE_URL_TOKEN;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.client.MvolaApiTestBase;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.service.MvolaTokenService;
import com.devikapps.vaikaparts.service.util.MvolaResponseParser;
import com.devikapps.vaikaparts.validator.PaymentRequestValidator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MvolaPaymentGatewayTest extends MvolaApiTestBase {

  public static final String NOTIFICATION_METHOD = "polling";
  public static final String CALL_BACK_NOTIF_METHOD = "callback";
  public static final String TEST_DEBIT_MSISDN = "+261343500003";
  public static final String DEBITER_MSISDN_LOCAL_FORMAT = "0343500003";
  private static final String SERVER_CORRELATION_ID = randomUUID().toString();
  private static final String TRANSACTION_REF = randomUUID().toString();
  private MvolaPaymentGateway gateway;
  private MvolaTokenService tokenService;

  @BeforeEach
  void set_up_gateway() {
    final var mvolaProperties = new MvolaProperties();
    mvolaProperties.setConsumerKey(randomUUID().toString());
    mvolaProperties.setConsumerSecret(randomUUID().toString());
    mvolaProperties.setBaseUrl(mockWebServer.url(MERCHANT_PAY_BASE_PATH).toString());
    mvolaProperties.setTokenUrl(MVOLA_BASE_URL_TOKEN);
    mvolaProperties.setPartnerMsisdn(PARTNER_MSISDN);
    mvolaProperties.setPartnerName(PARTNER_NAME);
    mvolaProperties.setCallbackUrl("https://default.callback.com/mvola");

    tokenService = Mockito.mock(MvolaTokenService.class);
    when(tokenService.getToken()).thenReturn(DUMMY_ACCESS_TOKEN);

    final PaymentRequestValidator validator = new PaymentRequestValidator();
    gateway =
        new MvolaPaymentGateway(
            validator, mvolaProperties, tokenService, new MvolaResponseParser());

    gateway.init();
  }

  @Test
  void should_return_mvola_as_provider() {
    assertEquals(MVOLA, gateway.getProvider());
  }

  @Test
  void should_map_server_correlation_id_from_initiate_payment_response() {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(SERVER_CORRELATION_ID, response.getServerCorrelationId());
  }

  @Test
  void should_map_status_pending_from_initiate_payment_response() {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(PaymentStatus.PENDING, response.getStatus());
  }

  @Test
  void should_map_notification_method_polling_from_initiate_payment_response() {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(NOTIFICATION_METHOD, response.getNotificationMethod());
  }

  @Test
  void should_map_notification_method_callback_from_initiate_payment_response() {
    enqueueInitiateResponse(CALL_BACK_NOTIF_METHOD);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(CALL_BACK_NOTIF_METHOD, response.getNotificationMethod());
  }

  @Test
  void should_preserve_client_transaction_id_on_initiate_payment_response() {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentRequest request = buildValidMvolaRequest();
    final MvolaPaymentResponse response = (MvolaPaymentResponse) gateway.initiatePayment(request);

    assertEquals(request.getTransactionId(), response.getTransactionId());
  }

  @Test
  void should_preserve_amount_on_initiate_payment_response() {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentRequest request = buildValidMvolaRequest();
    final MvolaPaymentResponse response = (MvolaPaymentResponse) gateway.initiatePayment(request);

    assertEquals(new BigDecimal("5000"), response.getAmount());
  }

  @Test
  void should_preserve_currency_on_initiate_payment_response() {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(AR, response.getCurrency());
  }

  @Test
  void should_set_provider_to_mvola_on_initiate_payment_response() {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(MVOLA, response.getProvider());
  }

  @Test
  void should_set_responded_at_within_acceptable_window_on_initiate_payment_response() {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final LocalDateTime before = now().minusSeconds(1);
    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.initiatePayment(buildValidMvolaRequest());
    final LocalDateTime after = now().plusSeconds(1);

    assertTrue(response.getRespondedAt().isAfter(before));
    assertTrue(response.getRespondedAt().isBefore(after));
  }

  @Test
  void should_send_bearer_token_on_initiate_payment() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(
        format("Bearer %s", DUMMY_ACCESS_TOKEN),
        mockWebServer.takeRequest().getHeader("Authorization"));
  }

  @Test
  void should_send_version_1_0_header_on_initiate_payment() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals("1.0", mockWebServer.takeRequest().getHeader("Version"));
  }

  @Test
  void should_send_no_cache_header_on_initiate_payment() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals("no-cache", mockWebServer.takeRequest().getHeader("Cache-Control"));
  }

  @Test
  void should_send_explicit_callback_url_when_provided_on_request() throws Exception {
    enqueueInitiateResponse(CALL_BACK_NOTIF_METHOD);

    final MvolaPaymentRequest request = buildValidMvolaRequest();
    request.setCallbackUrl("https://myserver.com/callback");
    gateway.initiatePayment(request);

    assertEquals(
        "https://myserver.com/callback", mockWebServer.takeRequest().getHeader("X-Callback-URL"));
  }

  @Test
  void should_fall_back_to_default_callback_url_when_not_set_on_request() throws Exception {
    enqueueInitiateResponse(CALL_BACK_NOTIF_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(
        "https://default.callback.com/mvola",
        mockWebServer.takeRequest().getHeader("X-Callback-URL"));
  }

  @Test
  void should_send_amount_in_request_body() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    final JsonObject body = parseRequestBody();
    assertEquals("5000", body.get("amount").getAsString());
  }

  @Test
  void should_send_currency_ar_in_request_body() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals("Ar", parseRequestBody().get("currency").getAsString());
  }

  @Test
  void should_send_request_date_in_iso_format_with_z_suffix() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    final String requestDate = parseRequestBody().get("requestDate").getAsString();
    assertTrue(requestDate.endsWith("Z"));
    assertTrue(requestDate.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
  }

  @Test
  void should_send_same_value_for_requesting_and_original_transaction_reference() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    final JsonObject body = parseRequestBody();
    assertEquals(
        body.get("requestingOrganisationTransactionReference").getAsString(),
        body.get("originalTransactionReference").getAsString());
  }

  @Test
  void should_include_exactly_three_metadata_entries_in_request_body() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(3, parseRequestBody().getAsJsonArray("metadata").size());
  }

  @Test
  void should_call_token_service_exactly_once_per_initiate_payment() {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    gateway.initiatePayment(buildValidMvolaRequest());

    verify(tokenService, times(1)).getToken();
  }

  @Test
  void should_strip_plus_261_prefix_from_payer_msisdn() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentRequest request = buildValidMvolaRequest();
    request.getPayer().setPhoneNumber(TEST_DEBIT_MSISDN);
    gateway.initiatePayment(request);

    assertEquals(DEBITER_MSISDN_LOCAL_FORMAT, debitMsisdn(parseRequestBody()));
  }

  @Test
  void should_strip_261_prefix_from_payer_msisdn() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentRequest request = buildValidMvolaRequest();
    request.getPayer().setPhoneNumber(TEST_DEBIT_MSISDN);
    gateway.initiatePayment(request);

    assertEquals(DEBITER_MSISDN_LOCAL_FORMAT, debitMsisdn(parseRequestBody()));
  }

  @Test
  void should_leave_already_normalized_msisdn_unchanged() throws Exception {
    enqueueInitiateResponse(NOTIFICATION_METHOD);

    final MvolaPaymentRequest request = buildValidMvolaRequest();
    request.getPayer().setPhoneNumber(DEBITER_MSISDN_LOCAL_FORMAT);
    gateway.initiatePayment(request);

    assertEquals(DEBITER_MSISDN_LOCAL_FORMAT, debitMsisdn(parseRequestBody()));
  }

  @Test
  void should_throw_payment_gateway_exception_on_401_during_initiate_payment() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(401)
            .setBody("{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}")
            .addHeader("Content-Type", "application/json"));

    final PaymentGatewayException ex =
        assertThrows(
            PaymentGatewayException.class, () -> gateway.initiatePayment(buildValidMvolaRequest()));

    assertTrue(ex.getMessage().contains("401") || ex.getMessage().contains("initiatePayment"));
  }

  @Test
  void should_throw_payment_gateway_exception_on_400_during_initiate_payment() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setBody("{\"ErrorDescription\":\"Missing required parameter\"}")
            .addHeader("Content-Type", "application/json"));

    final PaymentGatewayException ex =
        assertThrows(
            PaymentGatewayException.class, () -> gateway.initiatePayment(buildValidMvolaRequest()));

    assertTrue(ex.getMessage().contains("400") || ex.getMessage().contains("initiatePayment"));
  }

  @Test
  void should_throw_payment_gateway_exception_on_500_during_initiate_payment() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setBody("{\"ErrorDescription\":\"Internal Server Error\"}")
            .addHeader("Content-Type", "application/json"));

    assertThrows(
        PaymentGatewayException.class, () -> gateway.initiatePayment(buildValidMvolaRequest()));
  }

  @Test
  void should_throw_payment_validation_exception_when_request_is_null() {
    assertThrows(PaymentValidationException.class, () -> gateway.initiatePayment(null));
  }

  @Test
  void should_throw_payment_validation_exception_when_amount_is_null() {
    final MvolaPaymentRequest request = buildValidMvolaRequest();
    request.setAmount(null);

    assertThrows(PaymentValidationException.class, () -> gateway.initiatePayment(request));
  }

  @Test
  void should_throw_payment_validation_exception_when_payer_is_null() {
    final MvolaPaymentRequest request = buildValidMvolaRequest();
    request.setPayer(null);

    assertThrows(PaymentValidationException.class, () -> gateway.initiatePayment(request));
  }

  @Test
  void should_map_server_correlation_id_from_payment_status_response() {
    enqueueStatusResponse(PaymentStatus.PENDING.toString().toLowerCase(), null);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentStatus(SERVER_CORRELATION_ID);

    assertEquals(SERVER_CORRELATION_ID, response.getServerCorrelationId());
  }

  @Test
  void should_map_pending_status_from_payment_status_response() {
    enqueueStatusResponse(PaymentStatus.PENDING.toString().toLowerCase(), null);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentStatus(SERVER_CORRELATION_ID);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
  }

  @Test
  void should_map_completed_status_from_payment_status_response() {
    enqueueStatusResponse(PaymentStatus.COMPLETED.toString().toLowerCase(), TRANSACTION_REF);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentStatus(SERVER_CORRELATION_ID);

    assertEquals(PaymentStatus.COMPLETED, response.getStatus());
  }

  @Test
  void should_map_object_reference_when_status_is_completed() {
    enqueueStatusResponse(PaymentStatus.COMPLETED.toString().toLowerCase(), TRANSACTION_REF);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentStatus(SERVER_CORRELATION_ID);

    assertEquals(TRANSACTION_REF, response.getObjectReference());
  }

  @Test
  void should_map_failed_status_from_payment_status_response() {
    enqueueStatusResponse(PaymentStatus.FAILED.toString().toLowerCase(), null);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentStatus(SERVER_CORRELATION_ID);

    assertEquals(PaymentStatus.FAILED, response.getStatus());
  }

  @Test
  void should_have_null_object_reference_when_status_is_pending() {
    enqueueStatusResponse(PaymentStatus.PENDING.toString().toLowerCase(), null);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentStatus(SERVER_CORRELATION_ID);

    assertNull(response.getObjectReference());
  }

  @Test
  void should_set_provider_to_mvola_on_payment_status_response() {
    enqueueStatusResponse(PaymentStatus.PENDING.toString().toLowerCase(), null);

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentStatus(SERVER_CORRELATION_ID);

    assertEquals(MVOLA, response.getProvider());
  }

  @Test
  void should_throw_payment_gateway_exception_on_401_during_get_payment_status() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(401)
            .setBody("{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}")
            .addHeader("Content-Type", "application/json"));

    final PaymentGatewayException ex =
        assertThrows(
            PaymentGatewayException.class, () -> gateway.getPaymentStatus(SERVER_CORRELATION_ID));

    assertTrue(ex.getMessage().contains("getPaymentStatus"));
  }

  @Test
  void should_throw_payment_gateway_exception_on_404_during_get_payment_status() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(404)
            .setBody("{\"ErrorDescription\":\"serverCorrelationId not found\"}")
            .addHeader("Content-Type", "application/json"));

    assertThrows(
        PaymentGatewayException.class, () -> gateway.getPaymentStatus(SERVER_CORRELATION_ID));
  }

  @Test
  void should_throw_payment_validation_exception_when_server_correlation_id_is_blank() {
    assertThrows(PaymentValidationException.class, () -> gateway.getPaymentStatus(""));
  }

  @Test
  void should_throw_payment_validation_exception_when_server_correlation_id_is_null() {
    assertThrows(PaymentValidationException.class, () -> gateway.getPaymentStatus(null));
  }

  @Test
  void should_map_transaction_reference_from_payment_details_response() {
    enqueueDetailsResponse(PaymentStatus.COMPLETED.toString().toLowerCase());

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentDetails(TRANSACTION_REF);

    assertEquals(TRANSACTION_REF, response.getTransactionId());
  }

  @Test
  void should_map_completed_status_from_payment_details_response() {
    enqueueDetailsResponse(PaymentStatus.COMPLETED.toString().toLowerCase());

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentDetails(TRANSACTION_REF);

    assertEquals(PaymentStatus.COMPLETED, response.getStatus());
  }

  @Test
  void should_map_failed_status_from_payment_details_response() {
    enqueueDetailsResponse(PaymentStatus.FAILED.toString().toLowerCase());

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentDetails(TRANSACTION_REF);

    assertEquals(PaymentStatus.FAILED, response.getStatus());
  }

  @Test
  void should_map_amount_from_payment_details_response() {
    enqueueDetailsResponse(PaymentStatus.COMPLETED.toString().toLowerCase());

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentDetails(TRANSACTION_REF);

    assertEquals(new BigDecimal("5000"), response.getAmount());
  }

  @Test
  void should_map_currency_ar_from_payment_details_response() {
    enqueueDetailsResponse(PaymentStatus.COMPLETED.toString().toLowerCase());

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentDetails(TRANSACTION_REF);

    assertEquals(AR, response.getCurrency());
  }

  @Test
  void should_set_provider_to_mvola_on_payment_details_response() {
    enqueueDetailsResponse(PaymentStatus.COMPLETED.toString().toLowerCase());

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentDetails(TRANSACTION_REF);

    assertEquals(MVOLA, response.getProvider());
  }

  @Test
  void should_throw_payment_gateway_exception_on_404_during_get_payment_details() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(404)
            .setBody("{\"ErrorDescription\":\"Transaction not found\"}")
            .addHeader("Content-Type", "application/json"));

    final PaymentGatewayException ex =
        assertThrows(
            PaymentGatewayException.class, () -> gateway.getPaymentDetails(TRANSACTION_REF));

    assertTrue(ex.getMessage().contains("getPaymentDetails"));
  }

  @Test
  void should_throw_payment_validation_exception_when_transaction_reference_is_blank() {
    assertThrows(PaymentValidationException.class, () -> gateway.getPaymentDetails("   "));
  }

  @Test
  void should_throw_payment_validation_exception_when_transaction_reference_is_null() {
    assertThrows(PaymentValidationException.class, () -> gateway.getPaymentDetails(null));
  }

  private void enqueueInitiateResponse(final String notificationMethod) {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(202)
            .setBody(
                format(
                    "{\"status\":\"%s\",\"serverCorrelationId\":\"%s\",\"notificationMethod\":\"%s\"}",
                    PaymentStatus.PENDING.toString().toLowerCase(),
                    SERVER_CORRELATION_ID,
                    notificationMethod))
            .addHeader("Content-Type", "application/json"));
  }

  private void enqueueStatusResponse(final String status, final String objectReference) {
    final String ref =
        objectReference != null
            ? format("\"objectReference\":\"%s\"", objectReference)
            : "\"objectReference\":null";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                format(
                    "{\"status\":\"%s\",\"serverCorrelationId\":\"%s\",\"notificationMethod\":\"%s\",%s}",
                    status, SERVER_CORRELATION_ID, "polling", ref))
            .addHeader("Content-Type", "application/json"));
  }

  private void enqueueDetailsResponse(final String transactionStatus) {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                format(
                    "{\"amount\":\"%s\",\"currency\":\"Ar\",\"transactionReference\":\"%s\",\"transactionStatus\":\"%s\"}",
                    "5000", MvolaPaymentGatewayTest.TRANSACTION_REF, transactionStatus))
            .addHeader("Content-Type", "application/json"));
  }

  private JsonObject parseRequestBody() throws Exception {
    return JsonParser.parseString(mockWebServer.takeRequest().getBody().readUtf8())
        .getAsJsonObject();
  }

  private String debitMsisdn(final JsonObject body) {
    return body.getAsJsonArray("debitParty").get(0).getAsJsonObject().get("value").getAsString();
  }

  private MvolaPaymentRequest buildValidMvolaRequest() {
    return MvolaPaymentRequest.builder()
        .transactionId(randomUUID().toString())
        .amount(new BigDecimal("5000"))
        .currency(AR)
        .description("Test payment")
        .provider(MVOLA)
        .type(PaymentType.PROFILE_UNLOCK)
        .payer(PaymentParty.builder().phoneNumber(CUSTOMER_MSISDN).build())
        .payee(PaymentParty.builder().phoneNumber(PARTNER_MSISDN).build())
        .correlationId(randomUUID().toString())
        .build();
  }
}
