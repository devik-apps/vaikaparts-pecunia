package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.event.model.PaymentVerificationRequested;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.exception.PaymentVerificationRequestedException;
import com.devikapps.vaikaparts.gateway.PaymentGateway;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
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
  @Transactional(noRollbackFor = PaymentVerificationRequestedException.class)
  public void accept(PaymentVerificationRequested event) {
    log.info(
        "Processing PaymentVerificationRequested event={}, paymentId={}, attempt={}",
        event.getId(),
        forJava(event.getPaymentId()),
        event.getAttemptNb());

    JPayment payment = fetchPayment(event.getPaymentId());
    JPaymentVerificationRequested paymentVerificationRequested =
        fetchPaymentVerificationRequested(event.getId());

    try {
      final PaymentGateway gateway = gatewayFactory.getGateway(payment.getProvider());
      final PaymentResponse response = gateway.getPaymentStatus(payment.getTransactionId());
      final VerificationStatus providerStatus = resolveVerificationStatus(response.getStatus());

      switch (providerStatus) {
        case SUCCESS -> handleSuccess(payment, paymentVerificationRequested, event);
        case PENDING -> handlePending(payment, paymentVerificationRequested, event);
        case FAILED -> handleFailed(payment, paymentVerificationRequested, event);
      }

    } catch (Exception e) {
      handleProcessingError(paymentVerificationRequested, event, e);
    }
  }

  private void handleSuccess(
      JPayment payment,
      JPaymentVerificationRequested paymentVerificationRequested,
      final PaymentVerificationRequested event) {
    log.info(
        "Payment verified as SUCCESS for paymentId={}, transactionId={}, event={}",
        event.getPaymentId(),
        payment.getTransactionId(),
        event.getId());

    payment.setStatus(VerificationStatus.SUCCESS);
    payment.setUpdatedAt(now());
    paymentRepository.save(payment);

    paymentVerificationRequested.setStatus(VerificationStatus.SUCCESS);
    paymentVerificationRequested.setAttemptNb(event.getAttemptNb());
    paymentVerificationRequested.setErrorMessage(null);
    paymentVerificationRequested.setLastVerifiedAt(now());
    paymentVerificationRequested.setCompletedAt(now());
    paymentVerificationRequested.setLastVerifiedAt(now());
    paymentRequestedRepository.save(paymentVerificationRequested);
  }

  private void handlePending(
      JPayment payment,
      JPaymentVerificationRequested paymentVerificationRequested,
      PaymentVerificationRequested event) {

    final var currentAttempt = event.getAttemptNb();
    final var maxAttempts = event.getMaxVerificationAttemptNb();

    if (currentAttempt >= maxAttempts) {
      log.warn(
          "Max verification attempts ({}) reached for paymentId={}, event={}. Marking as FAILED.",
          maxAttempts,
          forJava(event.getPaymentId()),
          event.getId());
      markAsFailed(
          payment,
          paymentVerificationRequested,
          event,
          format("Max verification attempts (%d) reached with status still PENDING.", maxAttempts));
      return;
    }

    log.info(
        "Payment still PENDING for paymentId={}, attempt {}/{}. Will retry.",
        forJava(event.getPaymentId()),
        currentAttempt,
        maxAttempts);

    paymentVerificationRequested.setStatus(VerificationStatus.PENDING);
    paymentVerificationRequested.setAttemptNb(currentAttempt);
    paymentVerificationRequested.setLastVerifiedAt(now());
    paymentVerificationRequested.setLastVerifiedAt(now());
    paymentRequestedRepository.save(paymentVerificationRequested);
  }

  private void handleFailed(
      final JPayment payment,
      JPaymentVerificationRequested paymentVerificationRequested,
      final PaymentVerificationRequested event) {

    final var failedAttempts = paymentVerificationRequested.getFailedAttemptNb() + 1;
    paymentVerificationRequested.setFailedAttemptNb(failedAttempts);

    if (failedAttempts > MAX_FAILED_RETRIES) {
      log.warn(
          "Provider returned FAILED {} time(s) for paymentId={}, event={}. Marking as FAILED.",
          failedAttempts,
          forJava(event.getPaymentId()),
          event.getId());
      markAsFailed(
          payment,
          paymentVerificationRequested,
          event,
          format("Provider returned FAILED status after %d retries.", failedAttempts));
      return;
    }

    log.info(
        "Provider returned FAILED for paymentId={}, failed attempt {}/{}. Will retry.",
        forJava(event.getPaymentId()),
        failedAttempts,
        MAX_FAILED_RETRIES);

    paymentVerificationRequested.setLastVerifiedAt(now());
    paymentVerificationRequested.setLastVerifiedAt(now());
    paymentRequestedRepository.save(paymentVerificationRequested);
  }

  private void markAsFailed(
      JPayment payment,
      JPaymentVerificationRequested paymentVerificationRequested,
      final PaymentVerificationRequested event,
      final String reason) {
    payment.setStatus(VerificationStatus.FAILED);
    payment.setUpdatedAt(now());
    paymentRepository.save(payment);

    paymentVerificationRequested.setStatus(VerificationStatus.FAILED);
    paymentVerificationRequested.setAttemptNb(event.getAttemptNb());
    paymentVerificationRequested.setErrorMessage(reason);
    paymentVerificationRequested.setLastVerifiedAt(now());
    paymentVerificationRequested.setCompletedAt(now());
    paymentVerificationRequested.setLastVerifiedAt(now());
    paymentRequestedRepository.save(paymentVerificationRequested);
  }

  private JPayment fetchPayment(@NotNull final String paymentId) {
    return paymentRepository
        .findById(paymentId)
        .orElseThrow(
            () -> {
              log.error("Payment not found={}", forJava(paymentId));
              return new EntityNotFoundException(format("Payment not found: %s", paymentId));
            });
  }

  private JPaymentVerificationRequested fetchPaymentVerificationRequested(
      @NotNull final String eventId) {
    return paymentRequestedRepository
        .findById(eventId)
        .orElseThrow(
            () -> {
              log.error("PaymentVerificationRequested event log not found={}", forJava(eventId));
              return new EntityNotFoundException(
                  format("PaymentVerificationRequested event log not found: %s", forJava(eventId)));
            });
  }

  private VerificationStatus resolveVerificationStatus(final PaymentStatus providerStatus) {
    if (providerStatus == null) return VerificationStatus.PENDING;

    return switch (providerStatus) {
      case COMPLETED -> VerificationStatus.SUCCESS;
      case FAILED -> VerificationStatus.FAILED;
      default -> VerificationStatus.PENDING;
    };
  }

  private void handleProcessingError(
      final JPaymentVerificationRequested paymentVerificationRequested,
      final PaymentVerificationRequested event,
      final Exception e) {
    log.error(
        "Failed to process PaymentVerificationRequested event={}, attempt={}",
        event.getId(),
        event.getAttemptNb(),
        e);

    paymentVerificationRequested.setErrorMessage(e.getMessage());
    paymentVerificationRequested.setLastVerifiedAt(now());
    paymentRequestedRepository.save(paymentVerificationRequested);

    throw new PaymentVerificationRequestedException(
        format("PaymentVerificationRequested processing failed for event=%s", event.getId()), e);
  }
}
