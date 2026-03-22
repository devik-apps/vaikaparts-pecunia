package com.devikapps.vaikaparts.gateway.mvola;

import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_MSISDN;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.config.MvolaConf;
import com.devikapps.vaikaparts.model.MvolaPaymentRequest;
import com.devikapps.vaikaparts.model.MvolaPaymentResponse;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.service.MvolaTokenService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

class MvolaPaymentGatewayIT extends FacadeIT {

  private static final long SANDBOX_WAIT_MS = 3_000L;
  private static final String CUSTOMER_MSISDN = "0343500003";
  private static final String DESCRIPTION = "Integration test payment";
  private static final String TOKEN_PREFIX = "eyJ";

  @Autowired private MvolaConf mvolaConf;
  @Autowired private MvolaPaymentGateway subject;

  @Test
  void should_acquire_valid_jwt_bearer_token_from_sandbox() {
    final MvolaTokenService tokenService = new MvolaTokenService(mvolaConf, new RestTemplate());

    final String token = tokenService.getToken();

    assertTrue(token.startsWith(TOKEN_PREFIX));

    assertEquals(2, token.chars().filter(c -> c == '.').count());
  }

  @Test
  void should_return_same_cached_token_on_second_call() {
    final MvolaTokenService tokenService = new MvolaTokenService(mvolaConf, new RestTemplate());

    final String firstToken = tokenService.getToken();
    final String secondToken = tokenService.getToken();

    assertEquals(
        firstToken, secondToken, "Second call should return cached token, not fetch a new one");
  }

  @Test
  void should_initiate_payment_and_receive_pending_status_from_sandbox()
      throws InterruptedException {
    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) subject.initiatePayment(buildValidMvolaRequest());

    assertEquals(PaymentStatus.PENDING, response.getStatus());
    assertEquals(MVOLA, response.getProvider());
    assertTrue(
        response
            .getServerCorrelationId()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    assertTrue(
        response.getNotificationMethod().equals("polling")
            || response.getNotificationMethod().equals("callback"));

    Thread.sleep(SANDBOX_WAIT_MS);
  }

  @Test
  void should_preserve_client_transaction_id_on_initiate_payment_response()
      throws InterruptedException {
    final MvolaPaymentRequest request = buildValidMvolaRequest();

    final MvolaPaymentResponse response = (MvolaPaymentResponse) subject.initiatePayment(request);

    assertEquals(
        request.getTransactionId(),
        response.getTransactionId(),
        "Client-side transactionId must be preserved in the response");

    Thread.sleep(SANDBOX_WAIT_MS);
  }

  @Test
  void should_preserve_amount_and_currency_on_initiate_payment_response()
      throws InterruptedException {
    final MvolaPaymentRequest request = buildValidMvolaRequest();

    final MvolaPaymentResponse response = (MvolaPaymentResponse) subject.initiatePayment(request);

    assertEquals(new BigDecimal("100"), response.getAmount());
    assertEquals(AR, response.getCurrency());

    Thread.sleep(SANDBOX_WAIT_MS);
  }

  @Test
  void should_get_payment_status_returning_matching_server_correlation_id()
      throws InterruptedException {
    final MvolaPaymentResponse initiated =
        (MvolaPaymentResponse) subject.initiatePayment(buildValidMvolaRequest());

    Thread.sleep(SANDBOX_WAIT_MS);

    final MvolaPaymentResponse statusResponse =
        (MvolaPaymentResponse) subject.getPaymentStatus(initiated.getServerCorrelationId());

    assertEquals(
        initiated.getServerCorrelationId(),
        statusResponse.getServerCorrelationId(),
        "serverCorrelationId in status response must match the one from initiate");
    assertEquals(MVOLA, statusResponse.getProvider());
    assertTrue(
        statusResponse.getStatus() == PaymentStatus.PENDING
            || statusResponse.getStatus() == PaymentStatus.COMPLETED
            || statusResponse.getStatus() == PaymentStatus.FAILED);
  }

  @Test
  void should_get_payment_status_with_pending_status_before_manual_approval()
      throws InterruptedException {
    final MvolaPaymentResponse initiated =
        (MvolaPaymentResponse) subject.initiatePayment(buildValidMvolaRequest());

    Thread.sleep(1_000L);

    final MvolaPaymentResponse statusResponse =
        (MvolaPaymentResponse) subject.getPaymentStatus(initiated.getServerCorrelationId());

    assertEquals(
        PaymentStatus.PENDING,
        statusResponse.getStatus(),
        "Status must be PENDING immediately after initiation before manual approval");

    Thread.sleep(SANDBOX_WAIT_MS);
  }

  private MvolaPaymentRequest buildValidMvolaRequest() {
    return MvolaPaymentRequest.builder()
        .transactionId(randomUUID().toString())
        .amount(new BigDecimal("100"))
        .currency(AR)
        .description(DESCRIPTION)
        .provider(MVOLA)
        .type(PaymentType.PROFILE_UNLOCK)
        .payer(PaymentParty.builder().phoneNumber(CUSTOMER_MSISDN).build())
        .payee(PaymentParty.builder().phoneNumber(MVOLA_MSISDN).build())
        .correlationId(randomUUID().toString())
        .build();
  }
}
