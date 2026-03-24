package com.devikapps.vaikaparts.gateway.airtelmoney;

import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.MGA;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.AIRTEL_MONEY;
import static dev.razafindratelo.airtel_money_client.model.TransactionStatus.TS;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.client.airtel.AbstractAirtelMoneyTestBase;
import com.devikapps.vaikaparts.config.AirtelMoneyConf;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.mapper.AirtelMoneyTransactionStatusAdapter;
import com.devikapps.vaikaparts.model.AirtelMoneyPaymentRequest;
import com.devikapps.vaikaparts.model.AirtelMoneyPaymentResponse;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.service.AirtelMoneyTokenService;
import com.devikapps.vaikaparts.validator.PaymentRequestValidator;
import dev.razafindratelo.airtel_money_client.api.CollectionApi;
import dev.razafindratelo.airtel_money_client.api.TransactionApi;
import java.math.BigDecimal;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AirtelMoneyPaymentGatewayTest extends AbstractAirtelMoneyTestBase {

  private static final String PARTNER_TRANSACTION_ID = randomUUID().toString();

  private AirtelMoneyPaymentGateway subject;
  private AirtelMoneyTokenService tokenService;

  @BeforeEach
  void set_up_gateway() {
    AirtelMoneyConf props = new AirtelMoneyConf();
    props.setClientId(randomUUID().toString());
    props.setClientSecret(randomUUID().toString());
    props.setBaseUrl(mockWebServer.url("/").toString().replaceAll("/$", ""));
    props.setCountry(X_COUNTRY);
    props.setCurrency(X_CURRENCY);

    tokenService = mock(AirtelMoneyTokenService.class);
    AirtelMoneyTransactionStatusAdapter statusAdapter = new AirtelMoneyTransactionStatusAdapter();

    when(tokenService.getToken()).thenReturn(DUMMY_ACCESS_TOKEN);

    CollectionApi collectionApi = new CollectionApi(apiClient);
    TransactionApi transactionApi = new TransactionApi(apiClient);
    PaymentRequestValidator validator = new PaymentRequestValidator();

    subject =
        new AirtelMoneyPaymentGateway(
            validator, props, tokenService, statusAdapter, collectionApi, transactionApi);
  }

  @Test
  void should_return_airtel_money_as_provider() {
    assertEquals(AIRTEL_MONEY, subject.getProvider());
  }

  @Test
  void should_return_pending_status_on_successful_initiation() {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.initiatePayment(buildValidRequest());

    assertEquals(PaymentStatus.PENDING, response.getStatus());
  }

  @Test
  void should_return_transaction_id_from_request_on_initiation() {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.initiatePayment(buildValidRequest());

    assertEquals(PARTNER_TRANSACTION_ID, response.getTransactionId());
  }

  @Test
  void should_return_airtel_money_as_provider_in_response() {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.initiatePayment(buildValidRequest());

    assertEquals(AIRTEL_MONEY, response.getProvider());
  }

  @Test
  void should_set_responded_at_on_initiation_response() {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    var before = now().minusSeconds(1);
    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.initiatePayment(buildValidRequest());
    var after = now().plusSeconds(1);

    assertNotNull(response.getRespondedAt());
    assertTrue(
        response.getRespondedAt().isAfter(before) && response.getRespondedAt().isBefore(after));
  }

  @Test
  void should_send_bearer_token_on_initiation() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    subject.initiatePayment(buildValidRequest());

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("Bearer " + DUMMY_ACCESS_TOKEN, recorded.getHeader("Authorization"));
  }

  @Test
  void should_call_token_service_once_per_initiation() {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    subject.initiatePayment(buildValidRequest());

    verify(tokenService, times(1)).getToken();
  }

  @Test
  void should_send_x_country_header_on_initiation() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    subject.initiatePayment(buildValidRequest());

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(X_COUNTRY, recorded.getHeader("X-Country"));
  }

  @Test
  void should_send_x_currency_header_on_initiation() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    subject.initiatePayment(buildValidRequest());

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(X_CURRENCY, recorded.getHeader("X-Currency"));
  }

  @Test
  void should_normalize_msisdn_by_stripping_plus_261_prefix() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    AirtelMoneyPaymentRequest request = buildValidRequest();
    request.getPayer().setPhoneNumber(format("+261%s", TEST_MSISDN));

    subject.initiatePayment(request);

    RecordedRequest recorded = mockWebServer.takeRequest();
    String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(TEST_MSISDN), "Body must contain normalized msisdn");
    assertFalse(body.contains("+261"), "Body must not contain +261 prefix");
  }

  @Test
  void should_normalize_msisdn_by_stripping_261_prefix() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    var testMsisdn = format("261%s", TEST_MSISDN);

    AirtelMoneyPaymentRequest request = buildValidRequest();
    request.getPayer().setPhoneNumber(testMsisdn);

    subject.initiatePayment(request);

    RecordedRequest recorded = mockWebServer.takeRequest();
    String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(TEST_MSISDN));
    assertFalse(body.contains(testMsisdn));
  }

  @Test
  void should_normalize_msisdn_by_stripping_leading_zero() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    AirtelMoneyPaymentRequest request = buildValidRequest();
    var testMsisdn = format("0%s", TEST_MSISDN);

    request.getPayer().setPhoneNumber(testMsisdn);

    subject.initiatePayment(request);

    RecordedRequest recorded = mockWebServer.takeRequest();
    String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(TEST_MSISDN));
    assertFalse(body.contains(testMsisdn));
  }

  @Test
  void should_not_modify_msisdn_that_is_already_normalized() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    subject.initiatePayment(buildValidRequest()); // CUSTOMER_MSISDN = TEST_MSISDN

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertTrue(recorded.getBody().readUtf8().contains(CUSTOMER_MSISDN));
  }

  @Test
  void should_send_reference_in_initiation_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    subject.initiatePayment(buildValidRequest());

    RecordedRequest recorded = mockWebServer.takeRequest();
    String body = recorded.getBody().readUtf8();
    assertTrue(body.contains("Test payment reference"), "Body must contain reference");
  }

  @Test
  void should_send_amount_in_initiation_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    subject.initiatePayment(buildValidRequest());

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertTrue(recorded.getBody().readUtf8().contains("1000"));
  }

  @Test
  void should_send_transaction_id_in_initiation_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildInitiationSuccessBody()));

    subject.initiatePayment(buildValidRequest());

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertTrue(recorded.getBody().readUtf8().contains(PARTNER_TRANSACTION_ID));
  }

  @Test
  void should_throw_payment_gateway_exception_on_503_during_initiation() {
    mockWebServer.enqueue(jsonResponse(503, "{}"));

    assertThrows(PaymentGatewayException.class, () -> subject.initiatePayment(buildValidRequest()));
  }

  @Test
  void should_throw_payment_gateway_exception_on_504_during_initiation() {
    mockWebServer.enqueue(jsonResponse(504, "{}"));

    assertThrows(PaymentGatewayException.class, () -> subject.initiatePayment(buildValidRequest()));
  }

  @Test
  void should_throw_payment_validation_exception_when_request_is_null() {
    assertThrows(PaymentValidationException.class, () -> subject.initiatePayment(null));
  }

  @Test
  void should_return_completed_status_when_airtel_returns_ts() {
    mockWebServer.enqueue(
        jsonResponse(200, buildEnquiryBody(AIRTEL_MONEY_ID, TS.toString(), SUCCESS_MESSAGE)));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    assertEquals(PaymentStatus.COMPLETED, response.getStatus());
  }

  @Test
  void should_return_failed_status_when_airtel_returns_tf() {
    mockWebServer.enqueue(jsonResponse(200, buildEnquiryBody(null, "TF", "Transaction Failed")));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    assertEquals(PaymentStatus.FAILED, response.getStatus());
  }

  @Test
  void should_return_failed_status_when_airtel_returns_te() {
    mockWebServer.enqueue(jsonResponse(200, buildEnquiryBody(null, "TE", "Transaction Expired")));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    assertEquals(PaymentStatus.FAILED, response.getStatus());
  }

  @Test
  void should_return_pending_status_when_airtel_returns_tip() {
    mockWebServer.enqueue(
        jsonResponse(200, buildEnquiryBody(null, "TIP", "Transaction is in progress.")));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
  }

  @Test
  void should_return_pending_status_when_airtel_returns_ta() {
    mockWebServer.enqueue(jsonResponse(200, buildEnquiryBody(null, "TA", "Transaction Ambiguous")));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
  }

  @Test
  void should_return_airtel_money_id_when_status_is_ts() {
    mockWebServer.enqueue(
        jsonResponse(200, buildEnquiryBody(AIRTEL_MONEY_ID, TS.toString(), SUCCESS_MESSAGE)));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    assertEquals(AIRTEL_MONEY_ID, response.getAirtelMoneyId());
  }

  @Test
  void should_return_message_from_airtel_in_response() {
    mockWebServer.enqueue(
        jsonResponse(200, buildEnquiryBody(null, "TIP", "Transaction is in progress.")));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    assertEquals("Transaction is in progress.", response.getMessage());
  }

  @Test
  void should_return_transaction_id_in_status_response() {
    mockWebServer.enqueue(
        jsonResponse(200, buildEnquiryBody(AIRTEL_MONEY_ID, TS.toString(), SUCCESS_MESSAGE)));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    assertEquals(PARTNER_TRANSACTION_ID, response.getTransactionId());
  }

  @Test
  void should_send_bearer_token_on_status_query() throws Exception {
    mockWebServer.enqueue(
        jsonResponse(200, buildEnquiryBody(AIRTEL_MONEY_ID, TS.toString(), SUCCESS_MESSAGE)));

    subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("Bearer " + DUMMY_ACCESS_TOKEN, recorded.getHeader("Authorization"));
  }

  @Test
  void should_include_transaction_id_in_status_query_path() throws Exception {
    mockWebServer.enqueue(
        jsonResponse(200, buildEnquiryBody(AIRTEL_MONEY_ID, TS.toString(), SUCCESS_MESSAGE)));

    subject.getPaymentStatus(PARTNER_TRANSACTION_ID);

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertNotNull(recorded.getPath());
    assertTrue(recorded.getPath().endsWith("/" + PARTNER_TRANSACTION_ID));
  }

  @Test
  void should_throw_payment_gateway_exception_on_500_during_status_query() {
    mockWebServer.enqueue(jsonResponse(500, "{}"));

    assertThrows(
        PaymentGatewayException.class, () -> subject.getPaymentStatus(PARTNER_TRANSACTION_ID));
  }

  @Test
  void should_throw_payment_gateway_exception_on_502_during_status_query() {
    mockWebServer.enqueue(jsonResponse(502, "{}"));

    assertThrows(
        PaymentGatewayException.class, () -> subject.getPaymentStatus(PARTNER_TRANSACTION_ID));
  }

  @Test
  void should_throw_payment_gateway_exception_on_408_during_status_query() {
    mockWebServer.enqueue(jsonResponse(408, "{}"));

    assertThrows(
        PaymentGatewayException.class, () -> subject.getPaymentStatus(PARTNER_TRANSACTION_ID));
  }

  @Test
  void should_throw_payment_validation_exception_when_transaction_id_is_blank() {
    assertThrows(PaymentValidationException.class, () -> subject.getPaymentStatus("  "));
  }

  @Test
  void should_throw_payment_validation_exception_when_transaction_id_is_null() {
    assertThrows(PaymentValidationException.class, () -> subject.getPaymentStatus(null));
  }

  @Test
  void should_delegate_get_payment_details_to_get_payment_status() {
    mockWebServer.enqueue(
        jsonResponse(200, buildEnquiryBody(AIRTEL_MONEY_ID, TS.toString(), SUCCESS_MESSAGE)));

    PaymentResponse response = subject.getPaymentDetails(PARTNER_TRANSACTION_ID);

    assertEquals(PaymentStatus.COMPLETED, response.getStatus());
    assertEquals(PARTNER_TRANSACTION_ID, response.getTransactionId());
  }

  @Test
  void should_include_airtel_money_id_in_get_payment_details_response() {
    mockWebServer.enqueue(
        jsonResponse(200, buildEnquiryBody(AIRTEL_MONEY_ID, TS.toString(), SUCCESS_MESSAGE)));

    AirtelMoneyPaymentResponse response =
        (AirtelMoneyPaymentResponse) subject.getPaymentDetails(PARTNER_TRANSACTION_ID);

    assertEquals(AIRTEL_MONEY_ID, response.getAirtelMoneyId());
  }

  private AirtelMoneyPaymentRequest buildValidRequest() {
    return AirtelMoneyPaymentRequest.builder()
        .transactionId(PARTNER_TRANSACTION_ID)
        .amount(new BigDecimal("1000"))
        .currency(MGA)
        .description("Test payment description")
        .provider(AIRTEL_MONEY)
        .type(PaymentType.PROFILE_UNLOCK)
        .payer(PaymentParty.builder().phoneNumber(CUSTOMER_MSISDN).build())
        .payee(PaymentParty.builder().phoneNumber(SAMPLE_MSISDN).build())
        .reference("Test payment reference")
        .build();
  }

  private String buildInitiationSuccessBody() {
    return """
    {
        "data": {
            "transaction": {
                "id": "%s",
                "status": "SUCCESS"
            }
        },
        "status": {
            "code": "200",
            "message": "SUCCESS",
            "result_code": "ESB000010",
            "response_code": "DP00800001006",
            "success": true
        }
    }
    """
        .formatted(PARTNER_TRANSACTION_ID);
  }

  private String buildEnquiryBody(String airtelMoneyId, String status, String message) {
    String airtelIdValue = airtelMoneyId != null ? "\"" + airtelMoneyId + "\"" : "null";
    return """
    {
        "data": {
            "transaction": {
                "airtel_money_id": %s,
                "id": "%s",
                "message": "%s",
                "status": "%s"
            }
        },
        "status": {
            "code": "200",
            "message": "SUCCESS",
            "result_code": "ESB000010",
            "response_code": "DP00800001006",
            "success": true
        }
    }
    """
        .formatted(airtelIdValue, PARTNER_TRANSACTION_ID, message, status);
  }
}
