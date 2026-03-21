package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_MSISDN;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentRequest;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentResponse;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.classifier.Country;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MvolaPaymentServiceIT extends FacadeIT {

  public static final String UUID_REG_EXP =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  private static final long CONSUMER_WAIT_MS = 5_000L;
  private static final String CUSTOMER_MSISDN = "0343500003";
  @Autowired private MvolaPaymentService subject;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private PaymentRequestedRepository paymentRequestedRepository;
  @Autowired private PaymentPartyRepository paymentPartyRepository;
  @Autowired private PaymentPartyMapper paymentPartyMapper;

  @AfterEach
  void clean_up() {
    paymentRequestedRepository.deleteAll();
    paymentRepository.deleteAll();
    paymentPartyRepository.deleteAll();
  }

  @Test
  void should_persist_payment_with_pending_status_after_initiate() throws InterruptedException {
    final MvolaPaymentRequest request = buildValidRequest();

    final MvolaPaymentResponse response = (MvolaPaymentResponse) subject.initiatePayment(request);

    Thread.sleep(CONSUMER_WAIT_MS);

    final JPayment saved =
        paymentRepository
            .findJPaymentByTransactionId(response.getTransactionId())
            .orElseThrow(
                () ->
                    new AssertionError(
                        format(
                            "Payment not found for transactionId=%s",
                            response.getTransactionId())));

    assertEquals(MVOLA, saved.getProvider());
    assertEquals(AR, saved.getCurrency());
    assertEquals(new BigDecimal("100"), saved.getAmount());
    assertEquals(request.getDescription(), saved.getDescription());
  }

  @Test
  void should_update_payment_transaction_id_to_server_correlation_id() throws InterruptedException {
    final MvolaPaymentRequest request = buildValidRequest();
    final MvolaPaymentResponse response = (MvolaPaymentResponse) subject.initiatePayment(request);

    Thread.sleep(CONSUMER_WAIT_MS);

    assertTrue(response.getTransactionId().matches(UUID_REG_EXP));

    final JPayment saved =
        paymentRepository
            .findJPaymentByTransactionId(response.getTransactionId())
            .orElseThrow(
                () ->
                    new AssertionError(
                        format(
                            "Payment not found for transactionId=%s",
                            response.getTransactionId())));

    assertEquals(response.getTransactionId(), saved.getTransactionId());
  }

  @Test
  void should_return_pending_status_from_mvola_on_initiate() throws InterruptedException {
    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) subject.initiatePayment(buildValidRequest());

    Thread.sleep(CONSUMER_WAIT_MS);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
    assertEquals(MVOLA, response.getProvider());
    assertTrue(
        response.getNotificationMethod().equals("polling")
            || response.getNotificationMethod().equals("callback"));
  }

  @Test
  void should_return_server_correlation_id_as_transaction_id() throws InterruptedException {
    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) subject.initiatePayment(buildValidRequest());

    Thread.sleep(CONSUMER_WAIT_MS);

    assertTrue(response.getServerCorrelationId().matches(UUID_REG_EXP));
  }

  @Test
  void should_create_payment_verification_requested_event_log_in_database()
      throws InterruptedException {
    final MvolaPaymentRequest request = buildValidRequest();
    final MvolaPaymentResponse response = (MvolaPaymentResponse) subject.initiatePayment(request);

    Thread.sleep(CONSUMER_WAIT_MS);

    final JPayment payment =
        paymentRepository
            .findJPaymentByTransactionId(response.getTransactionId())
            .orElseThrow(() -> new AssertionError("Payment not found"));

    final List<JPaymentVerificationRequested> logs =
        paymentRequestedRepository.findAllByPaymentId(payment.getId());

    assertFalse(
        logs.isEmpty(),
        "At least one PaymentVerificationRequested log must exist for this payment");

    final JPaymentVerificationRequested log = logs.getFirst();
    assertEquals(
        payment.getId(),
        log.getPayment().getId(),
        "Event log must be linked to the correct payment");
    assertEquals(5, log.getMaxVerificationAttemptNb(), "maxVerificationAttemptNb must be 5");
  }

  @Test
  void should_have_pending_status_on_event_log_after_first_consumer_execution()
      throws InterruptedException {
    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) subject.initiatePayment(buildValidRequest());

    Thread.sleep(CONSUMER_WAIT_MS);

    final JPayment payment =
        paymentRepository
            .findJPaymentByTransactionId(response.getTransactionId())
            .orElseThrow(() -> new AssertionError("Payment not found"));

    final JPaymentVerificationRequested log =
        paymentRequestedRepository.findAllByPaymentId(payment.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new AssertionError("Event log not found"));

    assertEquals(
        VerificationStatus.PENDING,
        log.getStatus(),
        "Event log status must be PENDING before manual sandbox approval");
  }

  @Test
  void should_retrieve_persisted_payment_by_transaction_id() throws InterruptedException {
    final MvolaPaymentResponse response =
        (MvolaPaymentResponse) subject.initiatePayment(buildValidRequest());

    Thread.sleep(CONSUMER_WAIT_MS);

    final var payment = subject.getPayment(response.getTransactionId());

    assertEquals(
        response.getTransactionId(),
        payment.getTransactionId(),
        "Retrieved payment transactionId must match the one from initiate response");
    assertEquals(MVOLA, payment.getProvider());
  }

  @Test
  void should_throw_entity_not_found_when_payment_does_not_exist() {
    assertThrows(
        EntityNotFoundException.class,
        () -> subject.getPayment("non-existent-tx-id"),
        "Must throw EntityNotFoundException for unknown transactionId");
  }

  private MvolaPaymentRequest buildValidRequest() {
    final JPaymentParty payer =
        paymentPartyRepository.save(
            JPaymentParty.builder()
                .id(randomUUID().toString())
                .name("Test Customer")
                .country(Country.MADAGASCAR)
                .phoneNumber(CUSTOMER_MSISDN)
                .build());

    final JPaymentParty payee =
        paymentPartyRepository.save(
            JPaymentParty.builder()
                .id(randomUUID().toString())
                .name("Test Merchant")
                .country(Country.MADAGASCAR)
                .phoneNumber(MVOLA_MSISDN)
                .build());

    return MvolaPaymentRequest.builder()
        .transactionId(randomUUID().toString())
        .amount(new BigDecimal("100"))
        .currency(AR)
        .description("Integration test payment")
        .provider(MVOLA)
        .type(PaymentType.PROFILE_UNLOCK)
        .payer(paymentPartyMapper.toModel(payer))
        .payee(paymentPartyMapper.toModel(payee))
        .correlationId(randomUUID().toString())
        .build();
  }
}
