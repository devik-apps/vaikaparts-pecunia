package com.devikapps.vaikaparts.client.airtel;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.razafindratelo.airtel_money_client.api.TransactionApi;
import dev.razafindratelo.airtel_money_client.model.TransactionEnquiryResponse;
import dev.razafindratelo.airtel_money_client.model.TransactionStatus;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

public class AirtelMoneyTransactionApiTest extends AbstractAirtelMoneyTestBase {

  public static final String SUCCESS = "success";
  private TransactionApi subject;

  @BeforeEach
  void set_up() {
    subject = new TransactionApi(apiClient);
  }

  @Test
  void should_return_ts_status_on_successful_transaction() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertNotNull(response.getData().getTransaction());
    assertEquals(TransactionStatus.TS, response.getData().getTransaction().getStatus());
  }

  @Test
  void should_return_airtel_money_id_on_successful_transaction() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertNotNull(response.getData().getTransaction());
    assertEquals(AIRTEL_MONEY_ID, response.getData().getTransaction().getAirtelMoneyId());
  }

  @Test
  void should_return_partner_transaction_id_on_successful_transaction() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertNotNull(response.getData().getTransaction());
    assertEquals(PARTNER_TRANSACTION_ID, response.getData().getTransaction().getId());
  }

  @Test
  void should_return_message_on_successful_transaction() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertNotNull(response.getData().getTransaction());
    assertEquals(SUCCESS, response.getData().getTransaction().getMessage());
  }

  @Test
  void should_deserialize_tf_status_as_transaction_failed() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                null,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TF.toString(),
                "Transaction Failed")));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertNotNull(response.getData().getTransaction());
    assertEquals(TransactionStatus.TF, response.getData().getTransaction().getStatus());
  }

  @Test
  void should_deserialize_tip_status_as_transaction_in_progress() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                null,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TIP.toString(),
                "Transaction is in progress.")));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertNotNull(response.getData().getTransaction());
    assertEquals(TransactionStatus.TIP, response.getData().getTransaction().getStatus());
  }

  @Test
  void should_deserialize_ta_status_as_transaction_ambiguous() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                null,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TA.toString(),
                "Transaction Ambiguous")));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertNotNull(response.getData().getTransaction());
    assertEquals(TransactionStatus.TA, response.getData().getTransaction().getStatus());
  }

  @Test
  void should_deserialize_te_status_as_transaction_expired() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                null,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TE.toString(),
                "Transaction Expired")));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertNotNull(response.getData().getTransaction());
    assertEquals(TransactionStatus.TE, response.getData().getTransaction().getStatus());
  }

  @Test
  void should_return_status_envelope_with_success_true() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            format("%s%s", BEARER_PREFIX, DUMMY_ACCESS_TOKEN));

    assertTrue(response.getStatus().getSuccess());
    assertEquals("200", response.getStatus().getCode());
    assertEquals("SUCCESS", response.getStatus().getMessage());
  }

  @Test
  void should_return_status_envelope_with_success_false_on_non_final_status() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            """
            {
                "data": {
                    "transaction": {
                        "airtel_money_id": null,
                        "id": "%s",
                        "message": "Ambiguous",
                        "status": "TA"
                    }
                },
                "status": {
                    "code": "200",
                    "message": "SUCCESS",
                    "result_code": "ESB000010",
                    "response_code": "DP00800001000",
                    "success": false
                }
            }
            """
                .formatted(PARTNER_TRANSACTION_ID)));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertFalse(response.getStatus().getSuccess());
    assertNotNull(response.getData().getTransaction());
    assertEquals(TransactionStatus.TA, response.getData().getTransaction().getStatus());
  }

  @Test
  void should_return_response_code_in_status_envelope() {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    final TransactionEnquiryResponse response =
        subject.getTransactionStatus(
            PARTNER_TRANSACTION_ID,
            ACCEPT_ALL_MIME_TYPE,
            X_COUNTRY,
            X_CURRENCY,
            BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    assertEquals("DP00800001006", response.getStatus().getResponseCode());
  }

  @Test
  void should_send_get_request_to_correct_path() throws Exception {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    subject.getTransactionStatus(
        PARTNER_TRANSACTION_ID,
        ACCEPT_ALL_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("GET", recorded.getMethod());
    assertEquals("/standard/v1/payments/" + PARTNER_TRANSACTION_ID, recorded.getPath());
  }

  @Test
  void should_send_bearer_authorization_header() throws Exception {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    subject.getTransactionStatus(
        PARTNER_TRANSACTION_ID,
        ACCEPT_ALL_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(BEARER_PREFIX + DUMMY_ACCESS_TOKEN, recorded.getHeader("Authorization"));
  }

  @Test
  void should_send_x_country_header() throws Exception {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    subject.getTransactionStatus(
        PARTNER_TRANSACTION_ID,
        ACCEPT_ALL_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(X_COUNTRY, recorded.getHeader("X-Country"));
  }

  @Test
  void should_send_x_currency_header() throws Exception {
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(
                AIRTEL_MONEY_ID,
                PARTNER_TRANSACTION_ID,
                TransactionStatus.TS.toString(),
                SUCCESS)));

    subject.getTransactionStatus(
        PARTNER_TRANSACTION_ID,
        ACCEPT_ALL_MIME_TYPE,
        X_COUNTRY,
        X_CURRENCY,
        BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(X_CURRENCY, recorded.getHeader("X-Currency"));
  }

  @Test
  void should_include_transaction_id_in_path() throws Exception {
    final String customId = "my-custom-txn-xyz";
    mockWebServer.enqueue(
        jsonResponse(
            200,
            buildEnquiryBody(AIRTEL_MONEY_ID, customId, TransactionStatus.TS.toString(), SUCCESS)));

    subject.getTransactionStatus(
        customId, ACCEPT_ALL_MIME_TYPE, X_COUNTRY, X_CURRENCY, BEARER_PREFIX + DUMMY_ACCESS_TOKEN);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertNotNull(recorded.getPath());
    assertTrue(
        recorded.getPath().endsWith("/" + customId),
        "Path must end with the transaction id, got: " + recorded.getPath());
  }

  @Test
  void should_throw_on_401_unauthorized() {
    mockWebServer.enqueue(jsonResponse(401, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_on_400_bad_request() {
    mockWebServer.enqueue(jsonResponse(400, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_on_403_forbidden() {
    mockWebServer.enqueue(jsonResponse(403, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_on_404_not_found() {
    mockWebServer.enqueue(jsonResponse(404, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_on_408_timeout() {
    mockWebServer.enqueue(jsonResponse(408, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_on_500_internal_server_error() {
    mockWebServer.enqueue(jsonResponse(500, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_on_502_bad_gateway() {
    mockWebServer.enqueue(jsonResponse(502, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_on_504_gateway_timeout() {
    mockWebServer.enqueue(jsonResponse(504, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_when_id_parameter_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                null,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_when_authorization_parameter_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID, ACCEPT_ALL_MIME_TYPE, X_COUNTRY, X_CURRENCY, null));
  }

  @Test
  void should_throw_when_x_country_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                null,
                X_CURRENCY,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  @Test
  void should_throw_when_x_currency_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getTransactionStatus(
                PARTNER_TRANSACTION_ID,
                ACCEPT_ALL_MIME_TYPE,
                X_COUNTRY,
                null,
                BEARER_PREFIX + DUMMY_ACCESS_TOKEN));
  }

  private String buildEnquiryBody(
      final String airtelMoneyId, final String id, final String status, final String message) {
    final String airtelIdValue = airtelMoneyId != null ? "\"" + airtelMoneyId + "\"" : "null";
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
        .formatted(airtelIdValue, id, message, status);
  }
}
