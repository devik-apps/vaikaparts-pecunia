package com.devikapps.vaikaparts.gateway.mvola;

import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.math.BigDecimal;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MvolaPaymentGatewayTest extends MvolaApiTestBase {

  private static final String SERVER_CORRELATION_ID = "421a22a2-ef1d-42bc-9452-f4939a3d5cdf";
  private static final String TRANSACTION_REF = "641235";

  private MvolaPaymentGateway gateway;
  private MvolaTokenService tokenService;

  @BeforeEach
  void set_up_gateway() {
    var mvolaProperties = new MvolaProperties();
    mvolaProperties.setConsumerKey("testKey");
    mvolaProperties.setConsumerSecret("testSecret");
    mvolaProperties.setBaseUrl(mockWebServer.url(MERCHANT_PAY_BASE_PATH).toString());
    mvolaProperties.setTokenUrl("https://developer.mvola.mg/oauth2/token");
    mvolaProperties.setPartnerMsisdn(PARTNER_MSISDN);
    mvolaProperties.setPartnerName(PARTNER_NAME);
    mvolaProperties.setCallbackUrl(null);

    tokenService = Mockito.mock(MvolaTokenService.class);
    MvolaResponseParser responseParser = new MvolaResponseParser();

    when(tokenService.getToken()).thenReturn(DUMMY_ACCESS_TOKEN);

    final PaymentRequestValidator validator = new PaymentRequestValidator();
    gateway = new MvolaPaymentGateway(validator, mvolaProperties, tokenService, responseParser);
  }

  @Test
  void should_return_mvola_as_provider() {
    assertEquals(MVOLA, gateway.getProvider());
  }

  @Test
  void should_return_pending_response_on_successful_initiate_payment() throws Exception {
    final String responseBody =
        format(
            """
            {
              "status": "pending",
              "serverCorrelationId": "%s",
              "notificationMethod": "polling"
            }
            """,
            SERVER_CORRELATION_ID);

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(202)
            .setBody(responseBody)
            .addHeader("Content-Type", "application/json"));

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.initiatePayment(buildValidMvolaRequest());

    assertEquals(PaymentStatus.PENDING, response.getStatus());
    assertEquals(SERVER_CORRELATION_ID, response.getServerCorrelationId());
    assertEquals("polling", response.getNotificationMethod());
    assertEquals(MVOLA, response.getProvider());
    assertNotNull(response.getRespondedAt());
  }

  @Test
  void should_send_bearer_token_on_initiate_payment() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(202)
            .setBody(
                format(
                    "{\"status\":\"pending\",\"serverCorrelationId\":\"%s\",\"notificationMethod\":\"polling\"}",
                    SERVER_CORRELATION_ID))
            .addHeader("Content-Type", "application/json"));

    gateway.initiatePayment(buildValidMvolaRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(format("Bearer %s", DUMMY_ACCESS_TOKEN), recorded.getHeader("Authorization"));
  }

  @Test
  void should_send_callback_url_when_provided_on_initiate_payment() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(202)
            .setBody(
                format(
                    "{\"status\":\"pending\",\"serverCorrelationId\":\"%s\",\"notificationMethod\":\"callback\"}",
                    SERVER_CORRELATION_ID))
            .addHeader("Content-Type", "application/json"));

    final MvolaPaymentRequest request = buildValidMvolaRequest();
    request.setCallbackUrl("https://myserver.com/callback");

    gateway.initiatePayment(request);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("https://myserver.com/callback", recorded.getHeader("X-Callback-URL"));
  }

  @Test
  void should_throw_payment_gateway_exception_on_401_during_initiate_payment() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(401)
            .setBody("{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}")
            .addHeader("Content-Type", "application/json"));

    assertThrows(
        PaymentGatewayException.class, () -> gateway.initiatePayment(buildValidMvolaRequest()));
  }

  @Test
  void should_throw_payment_gateway_exception_on_400_during_initiate_payment() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setBody("{\"ErrorDescription\":\"Missing required parameter\"}")
            .addHeader("Content-Type", "application/json"));

    assertThrows(
        PaymentGatewayException.class, () -> gateway.initiatePayment(buildValidMvolaRequest()));
  }

  @Test
  void should_throw_payment_validation_exception_when_request_is_null() {
    assertThrows(PaymentValidationException.class, () -> gateway.initiatePayment(null));
  }

  @Test
  void should_refresh_token_on_each_initiate_payment_call() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(202)
            .setBody(
                format(
                    "{\"status\":\"pending\",\"serverCorrelationId\":\"%s\",\"notificationMethod\":\"polling\"}",
                    SERVER_CORRELATION_ID))
            .addHeader("Content-Type", "application/json"));

    gateway.initiatePayment(buildValidMvolaRequest());

    verify(tokenService, times(1)).getToken();
  }

  @Test
  void should_return_pending_response_on_get_payment_status() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                format(
                    """
                    {
                      "status": "pending",
                      "serverCorrelationId": "%s",
                      "notificationMethod": "polling",
                      "objectReference": null
                    }
                    """,
                    SERVER_CORRELATION_ID))
            .addHeader("Content-Type", "application/json"));

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentStatus(SERVER_CORRELATION_ID);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
    assertEquals(SERVER_CORRELATION_ID, response.getServerCorrelationId());
    assertEquals(MVOLA, response.getProvider());
  }

  @Test
  void should_return_completed_response_with_object_reference_on_get_payment_status()
      throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                format(
                    """
                    {
                      "status": "completed",
                      "serverCorrelationId": "%s",
                      "notificationMethod": "polling",
                      "objectReference": "%s"
                    }
                    """,
                    SERVER_CORRELATION_ID, TRANSACTION_REF))
            .addHeader("Content-Type", "application/json"));

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentStatus(SERVER_CORRELATION_ID);

    assertEquals(PaymentStatus.COMPLETED, response.getStatus());
    assertEquals(TRANSACTION_REF, response.getObjectReference());
  }

  @Test
  void should_throw_payment_gateway_exception_on_401_during_get_payment_status() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(401)
            .setBody("{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}")
            .addHeader("Content-Type", "application/json"));

    assertThrows(
        PaymentGatewayException.class, () -> gateway.getPaymentStatus(SERVER_CORRELATION_ID));
  }

  @Test
  void should_throw_payment_validation_exception_when_server_correlation_id_is_blank() {
    assertThrows(PaymentValidationException.class, () -> gateway.getPaymentStatus(""));
  }

  @Test
  void should_return_completed_response_on_get_payment_details() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                format(
                    """
                    {
                      "amount": "5000",
                      "currency": "Ar",
                      "transactionReference": "%s",
                      "transactionStatus": "completed"
                    }
                    """,
                    TRANSACTION_REF))
            .addHeader("Content-Type", "application/json"));

    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) gateway.getPaymentDetails(TRANSACTION_REF);

    assertEquals(TRANSACTION_REF, response.getTransactionId());
    assertEquals(PaymentStatus.COMPLETED, response.getStatus());
    assertEquals(new BigDecimal("5000"), response.getAmount());
    assertEquals(AR, response.getCurrency());
    assertEquals(MVOLA, response.getProvider());
  }

  @Test
  void should_throw_payment_gateway_exception_on_404_during_get_payment_details() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(404)
            .setBody("{\"ErrorDescription\":\"Transaction not found\"}")
            .addHeader("Content-Type", "application/json"));

    assertThrows(PaymentGatewayException.class, () -> gateway.getPaymentDetails(TRANSACTION_REF));
  }

  @Test
  void should_throw_payment_validation_exception_when_transaction_reference_is_blank() {
    assertThrows(PaymentValidationException.class, () -> gateway.getPaymentDetails("   "));
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
