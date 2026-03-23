package com.devikapps.vaikaparts.client.airtel;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.razafindratelo.airtel_money_client.api.CollectionApi;
import dev.razafindratelo.airtel_money_client.model.PaymentInitiationResponse;
import dev.razafindratelo.airtel_money_client.model.PaymentRequest;
import dev.razafindratelo.airtel_money_client.model.PaymentSubscriber;
import dev.razafindratelo.airtel_money_client.model.PaymentTransaction;
import java.math.BigDecimal;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

public class AirtelMoneyCollectionApiTest extends AbstractAirtelMoneyTestBase {

  private CollectionApi subject;

  @BeforeEach
  void set_up() {
    subject = new CollectionApi(apiClient);
  }

  @Test
  void should_return_transaction_id_on_successful_initiation() {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    final PaymentInitiationResponse response =
        subject.initiatePayment(
            ACCEPT_ALL_MIME_TYPE,
            JSON_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
            buildValidPaymentRequest());

    Assertions.assertNotNull(response.getData().getTransaction());
    assertEquals(PARTNER_TRANSACTION_ID, response.getData().getTransaction().getId());
  }

  @Test
  void should_return_success_status_in_data_on_successful_initiation() {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    final PaymentInitiationResponse response =
        subject.initiatePayment(
            ACCEPT_ALL_MIME_TYPE,
            JSON_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
            buildValidPaymentRequest());

    Assertions.assertNotNull(response.getData().getTransaction());
    assertEquals("SUCCESS", response.getData().getTransaction().getStatus());
  }

  @Test
  void should_return_status_envelope_with_success_true() {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    final PaymentInitiationResponse response =
        subject.initiatePayment(
            ACCEPT_ALL_MIME_TYPE,
            JSON_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
            buildValidPaymentRequest());

    assertTrue(response.getStatus().getSuccess());
    assertEquals("200", response.getStatus().getCode());
    assertEquals("SUCCESS", response.getStatus().getMessage());
  }

  @Test
  void should_return_response_code_in_status_envelope() {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    final PaymentInitiationResponse response =
        subject.initiatePayment(
            ACCEPT_ALL_MIME_TYPE,
            JSON_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
            buildValidPaymentRequest());

    assertEquals("DP00800001006", response.getStatus().getResponseCode());
  }

  @Test
  void should_return_result_code_in_status_envelope() {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    final PaymentInitiationResponse response =
        subject.initiatePayment(
            ACCEPT_ALL_MIME_TYPE,
            JSON_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
            buildValidPaymentRequest());

    assertEquals("ESB000010", response.getStatus().getResultCode());
  }

  @Test
  void should_send_post_request_to_correct_path() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    subject.initiatePayment(
        ACCEPT_ALL_MIME_TYPE,
        JSON_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
        buildValidPaymentRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("POST", recorded.getMethod());
    assertEquals("/merchant/v1/payments/", recorded.getPath());
  }

  @Test
  void should_send_bearer_authorization_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    subject.initiatePayment(
        ACCEPT_ALL_MIME_TYPE,
        JSON_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
        buildValidPaymentRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(
        format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN), recorded.getHeader("Authorization"));
  }

  @Test
  void should_send_x_country_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    subject.initiatePayment(
        ACCEPT_ALL_MIME_TYPE,
        JSON_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
        buildValidPaymentRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(X_COUNTRY, recorded.getHeader("X-Country"));
  }

  @Test
  void should_send_x_currency_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    subject.initiatePayment(
        ACCEPT_ALL_MIME_TYPE,
        JSON_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
        buildValidPaymentRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(X_CURRENCY, recorded.getHeader("X-Currency"));
  }

  @Test
  void should_send_msisdn_without_country_code_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    subject.initiatePayment(
        ACCEPT_ALL_MIME_TYPE,
        JSON_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
        buildValidPaymentRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(CUSTOMER_MSISDN));
    assertFalse(body.contains(format("261%s", CUSTOMER_MSISDN)));
  }

  @Test
  void should_send_transaction_id_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    subject.initiatePayment(
        ACCEPT_ALL_MIME_TYPE,
        JSON_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
        buildValidPaymentRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(PARTNER_TRANSACTION_ID));
  }

  @Test
  void should_send_reference_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    subject.initiatePayment(
        ACCEPT_ALL_MIME_TYPE,
        JSON_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
        buildValidPaymentRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains("Testing transaction"));
  }

  @Test
  void should_send_amount_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBody()));

    subject.initiatePayment(
        ACCEPT_ALL_MIME_TYPE,
        JSON_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
        buildValidPaymentRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains("1000"));
  }

  @Test
  void should_throw_on_401_unauthorized() {
    mockWebServer.enqueue(jsonResponse(401, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_on_400_bad_request() {
    mockWebServer.enqueue(jsonResponse(400, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_on_403_forbidden() {
    mockWebServer.enqueue(jsonResponse(403, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_on_408_timeout() {
    mockWebServer.enqueue(jsonResponse(408, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_on_429_too_many_requests() {
    mockWebServer.enqueue(jsonResponse(429, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_on_500_internal_server_error() {
    mockWebServer.enqueue(jsonResponse(500, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_on_502_bad_gateway() {
    mockWebServer.enqueue(jsonResponse(502, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_on_503_service_unavailable() {
    mockWebServer.enqueue(jsonResponse(503, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_on_504_gateway_timeout() {
    mockWebServer.enqueue(jsonResponse(504, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_when_authorization_parameter_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                null,
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_when_payment_request_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                null));
  }

  @Test
  void should_throw_when_x_country_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                null,
                X_CURRENCY,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  @Test
  void should_throw_when_x_currency_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.initiatePayment(
                ACCEPT_ALL_MIME_TYPE,
                JSON_MIME_TYPE,
                X_COUNTRY,
                null,
                format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN),
                buildValidPaymentRequest()));
  }

  private PaymentRequest buildValidPaymentRequest() {
    final PaymentSubscriber subscriber = new PaymentSubscriber();
    subscriber.setCountry(X_COUNTRY);
    subscriber.setCurrency(X_CURRENCY);
    subscriber.setMsisdn(CUSTOMER_MSISDN);

    final PaymentTransaction transaction = new PaymentTransaction();
    transaction.setId(PARTNER_TRANSACTION_ID);
    transaction.setAmount(new BigDecimal("1000"));
    transaction.setCountry(X_COUNTRY);
    transaction.setCurrency(X_CURRENCY);

    final PaymentRequest request = new PaymentRequest();
    request.setReference("Testing transaction");
    request.setSubscriber(subscriber);
    request.setTransaction(transaction);
    return request;
  }

  private String buildSuccessBody() {
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
}
