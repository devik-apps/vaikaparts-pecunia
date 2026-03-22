package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_MSISDN;
import static com.devikapps.vaikaparts.model.classifier.Country.MADAGASCAR;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.MvolaCallBackRequest;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.model.MvolaPayment;
import com.devikapps.vaikaparts.model.MvolaPaymentRequest;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JMvolaPayment;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MvolaPaymentServiceIT extends FacadeIT {

  private static final String UUID_REG_EXP =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  private static final long CONSUMER_WAIT_MS = 5_000L;
  private static final String CUSTOMER_MSISDN = "0343500003";

  @Autowired private MvolaPaymentService subject;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private PaymentRequestedRepository paymentRequestedRepository;
  @Autowired private PaymentPartyRepository paymentPartyRepository;

  @AfterEach
  void clean_up() {
    paymentRequestedRepository.deleteAll();
    paymentRepository.deleteAll();
    paymentPartyRepository.deleteAll();
  }

  @Test
  void should_persist_payment_with_pending_status_after_initiate() throws InterruptedException {
    final MvolaPaymentRequest request = buildValidRequest();

    final var response = (MvolaPayment) subject.initiatePayment(request);

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
    final var response = (MvolaPayment) subject.initiatePayment(buildValidRequest());

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
    final MvolaPayment response = (MvolaPayment) subject.initiatePayment(buildValidRequest());

    Thread.sleep(CONSUMER_WAIT_MS);

    assertEquals(VerificationStatus.PENDING, response.getStatus());
    assertEquals(MVOLA, response.getProvider());
    assertTrue(
        response.getNotificationMethod().equals("polling")
            || response.getNotificationMethod().equals("callback"));
  }

  @Test
  void should_return_server_correlation_id_as_transaction_id() throws InterruptedException {
    final MvolaPayment response = (MvolaPayment) subject.initiatePayment(buildValidRequest());

    Thread.sleep(CONSUMER_WAIT_MS);

    assertTrue(response.getServerCorrelationId().matches(UUID_REG_EXP));
  }

  @Test
  void should_reuse_existing_payment_party_when_phone_number_already_exists()
      throws InterruptedException {
    // First initiation creates the parties
    subject.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    final long partyCountAfterFirst = paymentPartyRepository.count();

    // Second initiation with same phone numbers must not create duplicate parties
    subject.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    assertEquals(
        partyCountAfterFirst,
        paymentPartyRepository.count(),
        "No new payment parties should be created on second initiation with same MSISDNs");
  }

  @Test
  void should_create_payment_verification_requested_event_log_in_database()
      throws InterruptedException {
    final MvolaPayment response = (MvolaPayment) subject.initiatePayment(buildValidRequest());

    Thread.sleep(CONSUMER_WAIT_MS);

    final JPayment payment =
        paymentRepository
            .findJPaymentByTransactionId(response.getTransactionId())
            .orElseThrow(() -> new AssertionError("Payment not found"));

    final List<JPaymentVerificationRequested> logs =
        paymentRequestedRepository.findAllByPaymentId(payment.getId());

    assertFalse(logs.isEmpty());
    assertEquals(payment.getId(), logs.getFirst().getPayment().getId());
    assertEquals(5, logs.getFirst().getMaxVerificationAttemptNb());
  }

  @Test
  void should_have_pending_status_on_event_log_after_first_consumer_execution()
      throws InterruptedException {
    final MvolaPayment response = (MvolaPayment) subject.initiatePayment(buildValidRequest());

    Thread.sleep(CONSUMER_WAIT_MS);

    final JPayment payment =
        paymentRepository
            .findJPaymentByTransactionId(response.getTransactionId())
            .orElseThrow(() -> new AssertionError("Payment not found"));

    final JPaymentVerificationRequested log =
        paymentRequestedRepository.findAllByPaymentId(payment.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new AssertionError("Event log not found"));

    assertEquals(VerificationStatus.PENDING, log.getStatus());
  }

  @Test
  void should_retrieve_persisted_payment_by_transaction_id() throws InterruptedException {
    final MvolaPayment response = (MvolaPayment) subject.initiatePayment(buildValidRequest());

    Thread.sleep(CONSUMER_WAIT_MS);

    final var payment = subject.getPayment(response.getTransactionId());

    assertEquals(response.getTransactionId(), payment.getTransactionId());
    assertEquals(MVOLA, payment.getProvider());
  }

  @Test
  void should_throw_entity_not_found_when_payment_does_not_exist() {
    assertThrows(EntityNotFoundException.class, () -> subject.getPayment("non-existent-tx-id"));
  }

  @Test
  void should_update_payment_status_to_success_on_completed_callback() throws InterruptedException {
    final MvolaPayment response = (MvolaPayment) subject.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    subject.handleCallBack(
        buildCallbackRequest(response.getServerCorrelationId(), "completed", "TX-REF-001"));

    final JPayment updated =
        paymentRepository
            .findJPaymentByTransactionId(response.getTransactionId())
            .orElseThrow(() -> new AssertionError("Payment not found"));

    assertEquals(VerificationStatus.SUCCESS, updated.getStatus());
  }

  @Test
  void should_update_payment_status_to_failed_on_failed_callback() throws InterruptedException {
    final MvolaPayment response = (MvolaPayment) subject.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    subject.handleCallBack(
        buildCallbackRequest(response.getServerCorrelationId(), "failed", "TX-REF-002"));

    final JPayment updated =
        paymentRepository
            .findJPaymentByTransactionId(response.getTransactionId())
            .orElseThrow(() -> new AssertionError("Payment not found"));

    assertEquals(VerificationStatus.FAILED, updated.getStatus());
  }

  @Test
  void should_store_mvola_transaction_id_from_callback() throws InterruptedException {
    final MvolaPayment response = (MvolaPayment) subject.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    subject.handleCallBack(
        buildCallbackRequest(response.getServerCorrelationId(), "completed", "TX-REF-REAL-999"));

    final JMvolaPayment updated =
        (JMvolaPayment)
            paymentRepository
                .findJPaymentByTransactionId(response.getTransactionId())
                .orElseThrow(() -> new AssertionError("Payment not found"));

    assertEquals("TX-REF-REAL-999", updated.getMvolaTransactionId());
  }

  @Test
  void should_throw_entity_not_found_on_callback_when_payment_does_not_exist() {
    assertThrows(
        EntityNotFoundException.class,
        () ->
            subject.handleCallBack(
                buildCallbackRequest("non-existent-correlation-id", "completed", "TX123")));
  }

  @Test
  void should_return_payments_for_given_customer_msisdn() throws InterruptedException {
    subject.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    final Page<MvolaPayment> result =
        subject.findPaymentsByPaymentPartyMsisdn(CUSTOMER_MSISDN, 0, 10);

    assertFalse(result.isEmpty());
    result.getContent().forEach(p -> assertEquals(MVOLA, p.getProvider()));
  }

  @Test
  void should_return_empty_page_for_unknown_msisdn() {
    final Page<MvolaPayment> result = subject.findPaymentsByPaymentPartyMsisdn("0343599999", 0, 10);

    assertTrue(result.isEmpty());
  }

  @Test
  void should_not_return_payments_from_other_customers() throws InterruptedException {
    subject.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    final Page<MvolaPayment> result = subject.findPaymentsByPaymentPartyMsisdn("0343599999", 0, 10);

    assertTrue(result.isEmpty());
  }

  @Test
  void should_return_all_payments_for_customer_across_multiple_initiations()
      throws InterruptedException {
    subject.initiatePayment(buildValidRequest());
    Thread.sleep(1_000L);
    subject.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    final Page<MvolaPayment> result =
        subject.findPaymentsByPaymentPartyMsisdn(CUSTOMER_MSISDN, 0, 10);

    assertEquals(2, result.getTotalElements());
  }

  @Test
  void should_return_payments_sorted_by_created_at_descending() throws InterruptedException {
    subject.initiatePayment(buildValidRequest());
    Thread.sleep(1_000L);
    subject.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    final Page<MvolaPayment> result =
        subject.findPaymentsByPaymentPartyMsisdn(CUSTOMER_MSISDN, 0, 10);

    final List<MvolaPayment> content = result.getContent();
    assertTrue(
        content.get(0).getCreatedAt().isAfter(content.get(1).getCreatedAt())
            || content.get(0).getCreatedAt().isEqual(content.get(1).getCreatedAt()),
        "Payments must be sorted by createdAt descending");
  }

  private MvolaPaymentRequest buildValidRequest() {
    return MvolaPaymentRequest.builder()
        .amount(new BigDecimal("100"))
        .currency(AR)
        .description("Integration test payment")
        .provider(MVOLA)
        .type(PaymentType.PROFILE_UNLOCK)
        .payer(
            PaymentParty.builder()
                .id(randomUUID().toString())
                .name("Customer")
                .phoneNumber(CUSTOMER_MSISDN)
                .country(MADAGASCAR)
                .build())
        .payee(
            PaymentParty.builder()
                .id(randomUUID().toString())
                .name("TestMVola")
                .phoneNumber(MVOLA_MSISDN)
                .country(MADAGASCAR)
                .build())
        .build();
  }

  private MvolaCallBackRequest buildCallbackRequest(
      final String serverCorrelationId, final String status, final String transactionReference) {
    return MvolaCallBackRequest.builder()
        .serverCorrelationId(serverCorrelationId)
        .transactionStatus(status)
        .transactionReference(transactionReference)
        .build();
  }
}
