package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.Country.MADAGASCAR;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.PaymentVerificationRequested;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.gateway.PaymentGateway;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentCurrency;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PaymentVerificationRequestedServiceIT extends FacadeIT {

  private static final long CONSUMER_PROCESSING_WAIT_MS = 5000L;
  private static final int MAX_ATTEMPTS = 5;
  private static final int MAX_FAILED_RETRIES = 2;
  private static final String TRANSACTION_ID = "TXN-001";

  @Autowired private EventProducer<PaymentVerificationRequested> eventProducer;

  @Autowired private PaymentRepository paymentRepository;

  @Autowired private PaymentRequestedRepository paymentRequestedRepository;

  @Autowired private PaymentPartyRepository paymentPartyRepository;

  @MockitoBean private PaymentGatewayFactory gatewayFactory;

  @MockitoBean private PaymentGateway mockGateway;

  private JPayment persistedPayment;
  private JPaymentVerificationRequested persistedEventLog;

  @BeforeEach
  void set_up() {
    paymentRequestedRepository.deleteAll();
    paymentRepository.deleteAll();
    paymentPartyRepository.deleteAll();

    final JPaymentParty payer =
        paymentPartyRepository.save(
            JPaymentParty.builder()
                .id(randomUUID().toString())
                .phoneNumber("034-123-4567")
                .country(MADAGASCAR)
                .name(randomUUID().toString())
                .build());

    final JPaymentParty payee =
        paymentPartyRepository.save(
            JPaymentParty.builder()
                .id(randomUUID().toString())
                .phoneNumber("034-001-7983")
                .country(MADAGASCAR)
                .name(randomUUID().toString())
                .build());

    persistedPayment =
        paymentRepository.save(
            JPayment.builder()
                .id(randomUUID().toString())
                .transactionId(TRANSACTION_ID)
                .description("Test payment")
                .payer(payer)
                .payee(payee)
                .provider(PaymentProvider.MVOLA)
                .type(PaymentType.PROFILE_UNLOCK)
                .amount(new BigDecimal("5000"))
                .currency(PaymentCurrency.AR)
                .status(VerificationStatus.PENDING)
                .createdAt(now())
                .updatedAt(now())
                .build());

    persistedEventLog =
        paymentRequestedRepository.save(
            JPaymentVerificationRequested.builder()
                .id(randomUUID().toString())
                .payment(persistedPayment)
                .status(VerificationStatus.PENDING)
                .attemptNb(0)
                .failedAttemptNb(0)
                .maxVerificationAttemptNb(MAX_ATTEMPTS)
                .createdAt(now())
                .build());

    when(gatewayFactory.getGateway(PaymentProvider.MVOLA)).thenReturn(mockGateway);
  }

  @Test
  void should_mark_payment_and_event_log_as_success_when_provider_returns_completed()
      throws InterruptedException {
    when(mockGateway.getPaymentStatus(TRANSACTION_ID))
        .thenReturn(buildResponse(PaymentStatus.COMPLETED));

    eventProducer.accept(List.of(buildEvent(0)));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    final JPayment updatedPayment =
        paymentRepository.findById(persistedPayment.getId()).orElseThrow();
    assertEquals(VerificationStatus.SUCCESS, updatedPayment.getStatus());
    assertNotNull(updatedPayment.getUpdatedAt());

    final JPaymentVerificationRequested updatedLog =
        paymentRequestedRepository.findById(persistedEventLog.getId()).orElseThrow();
    assertEquals(VerificationStatus.SUCCESS, updatedLog.getStatus());
    assertNull(updatedLog.getErrorMessage());
    assertNotNull(updatedLog.getLastVerifiedAt());
    assertNotNull(updatedLog.getCompletedAt());

    verify(mockGateway, times(1)).getPaymentStatus(TRANSACTION_ID);
  }

  @Test
  void should_keep_pending_status_when_provider_returns_pending_and_attempt_below_max()
      throws InterruptedException {
    when(mockGateway.getPaymentStatus(TRANSACTION_ID))
        .thenReturn(buildResponse(PaymentStatus.PENDING));

    eventProducer.accept(List.of(buildEvent(2)));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    final JPayment unchangedPayment =
        paymentRepository.findById(persistedPayment.getId()).orElseThrow();
    assertEquals(VerificationStatus.PENDING, unchangedPayment.getStatus());

    final JPaymentVerificationRequested updatedLog =
        paymentRequestedRepository.findById(persistedEventLog.getId()).orElseThrow();
    assertEquals(VerificationStatus.PENDING, updatedLog.getStatus());
    assertEquals(2, updatedLog.getAttemptNb());
    assertNotNull(updatedLog.getLastVerifiedAt());
    assertNull(updatedLog.getCompletedAt());

    verify(mockGateway, times(1)).getPaymentStatus(TRANSACTION_ID);
  }

  @Test
  void should_mark_as_failed_when_provider_returns_pending_and_attempt_equals_max()
      throws InterruptedException {
    when(mockGateway.getPaymentStatus(TRANSACTION_ID))
        .thenReturn(buildResponse(PaymentStatus.PENDING));

    eventProducer.accept(List.of(buildEvent(MAX_ATTEMPTS)));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    final JPayment updatedPayment =
        paymentRepository.findById(persistedPayment.getId()).orElseThrow();
    assertEquals(VerificationStatus.FAILED, updatedPayment.getStatus());
    assertNotNull(updatedPayment.getUpdatedAt());

    final JPaymentVerificationRequested updatedLog =
        paymentRequestedRepository.findById(persistedEventLog.getId()).orElseThrow();
    assertEquals(VerificationStatus.FAILED, updatedLog.getStatus());
    assertNotNull(updatedLog.getErrorMessage());
    assertTrue(updatedLog.getErrorMessage().contains("PENDING"));
    assertNotNull(updatedLog.getCompletedAt());

    verify(mockGateway, times(1)).getPaymentStatus(TRANSACTION_ID);
  }

  @Test
  void should_mark_as_failed_when_provider_returns_pending_and_attempt_exceeds_max()
      throws InterruptedException {
    when(mockGateway.getPaymentStatus(TRANSACTION_ID))
        .thenReturn(buildResponse(PaymentStatus.PENDING));

    eventProducer.accept(List.of(buildEvent(MAX_ATTEMPTS + 1)));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    final JPayment updatedPayment =
        paymentRepository.findById(persistedPayment.getId()).orElseThrow();
    assertEquals(VerificationStatus.FAILED, updatedPayment.getStatus());

    final JPaymentVerificationRequested updatedLog =
        paymentRequestedRepository.findById(persistedEventLog.getId()).orElseThrow();
    assertEquals(VerificationStatus.FAILED, updatedLog.getStatus());
    assertNotNull(updatedLog.getCompletedAt());

    verify(mockGateway, times(1)).getPaymentStatus(TRANSACTION_ID);
  }

  @Test
  void should_increment_failed_attempt_and_keep_pending_on_first_provider_failure()
      throws InterruptedException {
    setEventLogFailedAttemptNb(0);
    when(mockGateway.getPaymentStatus(TRANSACTION_ID))
        .thenReturn(buildResponse(PaymentStatus.FAILED));

    eventProducer.accept(List.of(buildEvent(0)));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    final JPayment unchangedPayment =
        paymentRepository.findById(persistedPayment.getId()).orElseThrow();
    assertEquals(VerificationStatus.PENDING, unchangedPayment.getStatus());

    final JPaymentVerificationRequested updatedLog =
        paymentRequestedRepository.findById(persistedEventLog.getId()).orElseThrow();
    assertEquals(1, updatedLog.getFailedAttemptNb());
    assertNotNull(updatedLog.getLastVerifiedAt());
    assertNull(updatedLog.getCompletedAt());

    verify(mockGateway, times(1)).getPaymentStatus(TRANSACTION_ID);
  }

  @Test
  void should_increment_failed_attempt_and_keep_pending_on_second_provider_failure()
      throws InterruptedException {
    setEventLogFailedAttemptNb(1);
    when(mockGateway.getPaymentStatus(TRANSACTION_ID))
        .thenReturn(buildResponse(PaymentStatus.FAILED));

    eventProducer.accept(List.of(buildEvent(0)));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    final JPayment unchangedPayment =
        paymentRepository.findById(persistedPayment.getId()).orElseThrow();
    assertEquals(VerificationStatus.PENDING, unchangedPayment.getStatus());

    final JPaymentVerificationRequested updatedLog =
        paymentRequestedRepository.findById(persistedEventLog.getId()).orElseThrow();
    assertEquals(2, updatedLog.getFailedAttemptNb());
    assertNull(updatedLog.getCompletedAt());

    verify(mockGateway, times(1)).getPaymentStatus(TRANSACTION_ID);
  }

  @Test
  void should_mark_both_as_failed_when_provider_failure_exceeds_max_failed_retries()
      throws InterruptedException {
    setEventLogFailedAttemptNb(MAX_FAILED_RETRIES);
    when(mockGateway.getPaymentStatus(TRANSACTION_ID))
        .thenReturn(buildResponse(PaymentStatus.FAILED));

    eventProducer.accept(List.of(buildEvent(0)));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    final JPayment updatedPayment =
        paymentRepository.findById(persistedPayment.getId()).orElseThrow();
    assertEquals(VerificationStatus.FAILED, updatedPayment.getStatus());
    assertNotNull(updatedPayment.getUpdatedAt());

    final JPaymentVerificationRequested updatedLog =
        paymentRequestedRepository.findById(persistedEventLog.getId()).orElseThrow();
    assertEquals(VerificationStatus.FAILED, updatedLog.getStatus());
    assertEquals(MAX_FAILED_RETRIES + 1, updatedLog.getFailedAttemptNb());
    assertNotNull(updatedLog.getErrorMessage());
    assertNotNull(updatedLog.getCompletedAt());

    verify(mockGateway, times(1)).getPaymentStatus(TRANSACTION_ID);
  }

  @Test
  void should_not_call_gateway_when_payment_not_found() throws InterruptedException {
    final PaymentVerificationRequested event =
        PaymentVerificationRequested.builder()
            .id(randomUUID().toString())
            .paymentId("non-existent-payment-id")
            .maxVerificationAttemptNb(MAX_ATTEMPTS)
            .build();

    eventProducer.accept(List.of(event));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    verify(mockGateway, never()).getPaymentStatus(anyString());
  }

  @Test
  void should_not_call_gateway_when_event_log_not_found() throws InterruptedException {
    final PaymentVerificationRequested event =
        PaymentVerificationRequested.builder()
            .id(randomUUID().toString())
            .paymentId(persistedPayment.getId())
            .maxVerificationAttemptNb(MAX_ATTEMPTS)
            .build();

    eventProducer.accept(List.of(event));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    verify(mockGateway, never()).getPaymentStatus(anyString());
  }

  @Test
  void should_save_error_message_on_event_log_when_gateway_throws_unexpected_exception()
      throws InterruptedException {
    when(mockGateway.getPaymentStatus(TRANSACTION_ID))
        .thenThrow(new RuntimeException("Gateway connection timeout"));

    eventProducer.accept(List.of(buildEvent(0)));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    final JPaymentVerificationRequested updatedLog =
        paymentRequestedRepository.findById(persistedEventLog.getId()).orElseThrow();
    assertNotNull(updatedLog.getErrorMessage());
    assertEquals("Gateway connection timeout", updatedLog.getErrorMessage());

    final JPayment unchangedPayment =
        paymentRepository.findById(persistedPayment.getId()).orElseThrow();
    assertEquals(VerificationStatus.PENDING, unchangedPayment.getStatus());

    verify(mockGateway, times(1)).getPaymentStatus(TRANSACTION_ID);
  }

  @Test
  void should_save_error_message_on_event_log_when_provider_is_unsupported()
      throws InterruptedException {
    when(gatewayFactory.getGateway(PaymentProvider.MVOLA))
        .thenThrow(new RuntimeException("Payment Provider not supported : MVOLA"));

    eventProducer.accept(List.of(buildEvent(0)));
    Thread.sleep(CONSUMER_PROCESSING_WAIT_MS);

    final JPaymentVerificationRequested updatedLog =
        paymentRequestedRepository.findById(persistedEventLog.getId()).orElseThrow();
    assertNotNull(updatedLog.getErrorMessage());

    verify(mockGateway, never()).getPaymentStatus(anyString());
  }

  private PaymentVerificationRequested buildEvent(final int attemptNb) {
    final PaymentVerificationRequested event =
        PaymentVerificationRequested.builder()
            .id(persistedEventLog.getId())
            .paymentId(persistedPayment.getId())
            .maxVerificationAttemptNb(MAX_ATTEMPTS)
            .build();
    event.setAttemptNb(attemptNb);
    return event;
  }

  private PaymentResponse buildResponse(final PaymentStatus status) {
    final PaymentResponse response = new PaymentResponse() {};
    response.setStatus(status);
    return response;
  }

  private void setEventLogFailedAttemptNb(final int failedAttemptNb) {
    persistedEventLog.setFailedAttemptNb(failedAttemptNb);
    persistedEventLog = paymentRequestedRepository.save(persistedEventLog);
  }
}
