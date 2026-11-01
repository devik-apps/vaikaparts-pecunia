package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.event.model.VerificationStatus.PENDING;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.model.PapiPaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentCurrency;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.repository.PapiPaymentRepository;
import com.devikapps.vaikaparts.repository.model.JPapiPayment;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PapiPaymentStatusService {
  private final PapiPaymentRepository papiPaymentRepository;

  @Transactional
  public JPapiPayment applyProviderResponse(
      String merchantReference, PapiPaymentResponse response) {
    JPapiPayment payment =
        papiPaymentRepository
            .findByMerchantReferenceForUpdate(merchantReference)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No Papi payment for reference=%s", forJava(merchantReference))));

    if (!merchantReference.equals(response.getTransactionId())
        || response.getAmount() == null
        || payment.getAmount().compareTo(response.getAmount()) != 0
        || response.getPaymentMethod() == null
        || !payment.getPaymentMethod().equals(response.getPaymentMethod().name())
        || response.getCurrency() != PaymentCurrency.MGA) {
      throw new PaymentGatewayException(
          "Papi response does not match the locally stored payment details.");
    }

    if (response.getPayerFee() != null || response.getTotalAmount() != null) {
      if (response.getPayerFee() == null
          || response.getTotalAmount() == null
          || response.getPayerFee().signum() < 0
          || response.getPayerFee().stripTrailingZeros().scale() > 0
          || response.getTotalAmount().stripTrailingZeros().scale() > 0
          || response.getTotalAmount().compareTo(payment.getAmount().add(response.getPayerFee()))
              != 0) {
        throw new PaymentGatewayException("Papi response contains invalid fee totals.");
      }
      payment.setPayerFee(response.getPayerFee());
      payment.setTotalAmount(response.getTotalAmount());
    }

    if (response.getPapiPaymentReference() != null) {
      payment.setPapiPaymentReference(response.getPapiPaymentReference());
    }
    if (response.getNotificationToken() != null) {
      payment.setNotificationToken(response.getNotificationToken());
    }

    if (payment.getStatus() == PENDING && response.getStatus() != null) {
      payment.setStatus(toVerificationStatus(response.getStatus()));
      payment.setUpdatedAt(now());
    }

    return papiPaymentRepository.save(payment);
  }

  private com.devikapps.vaikaparts.event.model.VerificationStatus toVerificationStatus(
      PaymentStatus status) {
    return switch (status) {
      case COMPLETED -> com.devikapps.vaikaparts.event.model.VerificationStatus.SUCCESS;
      case FAILED -> com.devikapps.vaikaparts.event.model.VerificationStatus.FAILED;
      case PENDING -> PENDING;
    };
  }
}
