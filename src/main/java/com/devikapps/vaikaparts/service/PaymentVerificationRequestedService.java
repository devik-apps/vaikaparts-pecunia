package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.event.model.PaymentVerificationRequested;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.exception.PaymentVerificationRequestedException;
import com.devikapps.vaikaparts.gateway.PaymentGateway;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentVerificationRequestedService implements Consumer<PaymentVerificationRequested> {

  private static final int MAX_FAILED_RETRIES = 2;

  private final PaymentRequestedRepository paymentRequestedRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentGatewayFactory gatewayFactory;

  @Override
  @Transactional
  public void accept(final PaymentVerificationRequested event) {
    log.info(
        "Processing PaymentVerificationRequested event: {}, paymentId: {}, attempt: {}",
        forJava(event.getId()),
        forJava(event.getPaymentId()),
        event.getAttemptNb());

    final JPayment payment = fetchPayment(event.getPaymentId());
    final JPaymentVerificationRequested eventLog = fetchEventLog(event.getId());

    try {
      final PaymentGateway gateway = gatewayFactory.getGateway(payment.getProvider());
      final PaymentResponse response = gateway.getPaymentStatus(payment.getTransactionId());
      final VerificationStatus providerStatus = resolveVerificationStatus(response.getStatus());

      switch (providerStatus) {
        case SUCCESS -> handleSuccess(payment, eventLog, event);
        case PENDING -> handlePending(payment, eventLog, event);
        case FAILED -> handleFailed(payment, eventLog, event);
      }

    } catch (Exception e) {
      handleProcessingError(eventLog, event, e);
    }
  }

  private void handleSuccess(
      final JPayment payment,
      final JPaymentVerificationRequested eventLog,
      final PaymentVerificationRequested event) {
    log.info(
        "Payment verified as SUCCESS for paymentId: {}, event: {}",
        forJava(event.getPaymentId()),
        forJava(event.getId()));

    payment.setStatus(VerificationStatus.SUCCESS);
    payment.setUpdatedAt(LocalDateTime.now());
    paymentRepository.save(payment);

    eventLog.setStatus(VerificationStatus.SUCCESS);
    eventLog.setAttemptNb(event.getAttemptNb());
    eventLog.setErrorMessage(null);
    eventLog.setLastVerifiedAt(LocalDateTime.now());
    eventLog.setCompletedAt(LocalDateTime.now());
    eventLog.setLastVerifiedAt(LocalDateTime.now());
    paymentRequestedRepository.save(eventLog);
  }

  private void handlePending(
      final JPayment payment,
      final JPaymentVerificationRequested eventLog,
      final PaymentVerificationRequested event) {

    final int currentAttempt = event.getAttemptNb();
    final int maxAttempts = eventLog.getMaxVerificationAttemptNb();

    if (currentAttempt >= maxAttempts) {
      log.warn(
          "Max verification attempts ({}) reached for paymentId: {}, event: {}. Marking as FAILED.",
          maxAttempts,
          forJava(event.getPaymentId()),
          forJava(event.getId()));
      markAsFailed(
          payment,
          eventLog,
          event,
          format("Max verification attempts (%d) reached with status still PENDING.", maxAttempts));
      return;
    }

    log.info(
        "Payment still PENDING for paymentId: {}, attempt {}/{}. Will retry.",
        forJava(event.getPaymentId()),
        currentAttempt,
        maxAttempts);

    eventLog.setStatus(VerificationStatus.PENDING);
    eventLog.setAttemptNb(currentAttempt);
    eventLog.setLastVerifiedAt(LocalDateTime.now());
    eventLog.setLastVerifiedAt(LocalDateTime.now());
    paymentRequestedRepository.save(eventLog);
  }

  private void handleFailed(
      final JPayment payment,
      final JPaymentVerificationRequested eventLog,
      final PaymentVerificationRequested event) {

    final int failedAttempts = eventLog.getFailedAttemptNb() + 1;
    eventLog.setFailedAttemptNb(failedAttempts);

    if (failedAttempts > MAX_FAILED_RETRIES) {
      log.warn(
          "Provider returned FAILED {} time(s) for paymentId: {}, event: {}. Marking as FAILED.",
          failedAttempts,
          forJava(event.getPaymentId()),
          forJava(event.getId()));
      markAsFailed(
          payment,
          eventLog,
          event,
          format("Provider returned FAILED status after %d retries.", failedAttempts));
      return;
    }

    log.info(
        "Provider returned FAILED for paymentId: {}, failed attempt {}/{}. Will retry.",
        forJava(event.getPaymentId()),
        failedAttempts,
        MAX_FAILED_RETRIES);

    eventLog.setLastVerifiedAt(LocalDateTime.now());
    eventLog.setLastVerifiedAt(LocalDateTime.now());
    paymentRequestedRepository.save(eventLog);
  }

  private void markAsFailed(
      final JPayment payment,
      final JPaymentVerificationRequested eventLog,
      final PaymentVerificationRequested event,
      final String reason) {
    payment.setStatus(VerificationStatus.FAILED);
    payment.setUpdatedAt(LocalDateTime.now());
    paymentRepository.save(payment);

    eventLog.setStatus(VerificationStatus.FAILED);
    eventLog.setAttemptNb(event.getAttemptNb());
    eventLog.setErrorMessage(reason);
    eventLog.setLastVerifiedAt(LocalDateTime.now());
    eventLog.setCompletedAt(LocalDateTime.now());
    eventLog.setLastVerifiedAt(LocalDateTime.now());
    paymentRequestedRepository.save(eventLog);
  }

  private JPayment fetchPayment(final String paymentId) {
    return paymentRepository
        .findById(paymentId)
        .orElseThrow(
            () -> {
              log.error("Payment not found: {}", forJava(paymentId));
              return new IllegalStateException(format("Payment not found: %s", paymentId));
            });
  }

  private JPaymentVerificationRequested fetchEventLog(final String eventId) {
    return paymentRequestedRepository
        .findById(eventId)
        .orElseThrow(
            () -> {
              log.error("PaymentVerificationRequested event log not found: {}", forJava(eventId));
              return new IllegalStateException(
                  format("PaymentVerificationRequested event log not found: %s", eventId));
            });
  }

  private VerificationStatus resolveVerificationStatus(final Object providerStatus) {
    if (providerStatus == null) {
      return VerificationStatus.PENDING;
    }
    return switch (providerStatus.toString().toUpperCase()) {
      case "COMPLETED", "SUCCESS" -> VerificationStatus.SUCCESS;
      case "FAILED" -> VerificationStatus.FAILED;
      default -> VerificationStatus.PENDING;
    };
  }

  private void handleProcessingError(
      final JPaymentVerificationRequested eventLog,
      final PaymentVerificationRequested event,
      final Exception e) {
    log.error(
        "Failed to process PaymentVerificationRequested event: {}, attempt: {}",
        forJava(event.getId()),
        event.getAttemptNb(),
        e);

    eventLog.setErrorMessage(e.getMessage());
    eventLog.setLastVerifiedAt(LocalDateTime.now());
    paymentRequestedRepository.save(eventLog);

    throw new PaymentVerificationRequestedException(
        format("PaymentVerificationRequested processing failed for event: %s", event.getId()), e);
  }
}
