package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.event.model.VerificationStatus.FAILED;
import static com.devikapps.vaikaparts.event.model.VerificationStatus.PENDING;
import static com.devikapps.vaikaparts.event.model.VerificationStatus.SUCCESS;
import static com.devikapps.vaikaparts.model.classifier.Country.MADAGASCAR;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.MGA;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.PAPI;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.config.PapiConf;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.PapiCallBackRequest;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.RPapiPaymentRequest;
import com.devikapps.vaikaparts.exception.PaymentConflictException;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.exception.PapiWebhookSecurityException;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.mapper.PapiPaymentMapper;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.PapiPayment;
import com.devikapps.vaikaparts.model.PapiPaymentRequest;
import com.devikapps.vaikaparts.model.PapiPaymentResponse;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.repository.PapiPaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.model.JPapiPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import com.devikapps.vaikaparts.service.papi.PapiSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PapiPaymentService {
  private final PapiConf conf;
  private final PaymentGatewayFactory gatewayFactory;
  private final PapiPaymentRepository papiPaymentRepository;
  private final PaymentPartyRepository paymentPartyRepository;
  private final PaymentPartyMapper paymentPartyMapper;
  private final PapiPaymentMapper papiPaymentMapper;
  private final PapiPaymentStatusService papiPaymentStatusService;
  private final PapiSignatureVerifier signatureVerifier;
  private final ObjectMapper objectMapper;

  public PapiPayment initiatePayment(RPapiPaymentRequest request) {
    validateRequest(request);

    JPapiPayment payment;
    var existing = papiPaymentRepository.findByMerchantReference(request.getReference());
    if (existing.isPresent()) {
      boolean paymentAlreadyActive =
          existing.get().getStatus() == PENDING || existing.get().getStatus() == SUCCESS;
      payment = resolveExistingPayment(existing.get(), request);
      if (paymentAlreadyActive) return papiPaymentMapper.toModel(payment);
    } else {
      payment = createPayment(request);
    }

    PapiPaymentResponse response =
        (PapiPaymentResponse)
            gatewayFactory.getGateway(PAPI).initiatePayment(toGatewayRequest(request, payment));

    JPapiPayment updated =
        papiPaymentStatusService.applyProviderResponse(request.getReference(), response);
    return papiPaymentMapper.toModel(updated);
  }

  public PapiPayment getPayment(String reference) {
    JPapiPayment payment =
        papiPaymentRepository
            .findByMerchantReference(reference)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No Papi payment for reference=%s", forJava(reference))));

    if (payment.getStatus() == PENDING) refreshPendingPayment(reference);
    return papiPaymentMapper.toModel(
        papiPaymentRepository.findByMerchantReference(reference).orElse(payment));
  }

  @Transactional
  public void handleCallBack(byte[] rawBody, String signatureHeader) {
    if (!signatureVerifier.isValid(rawBody, signatureHeader, conf.getWebhookSecret(), java.time.Instant.now())) {
      throw new PapiWebhookSecurityException("Papi callback signature is invalid or expired.");
    }

    final PapiCallBackRequest callback;
    try {
      callback = objectMapper.readValue(rawBody, PapiCallBackRequest.class);
    } catch (JsonProcessingException e) {
      throw new PaymentValidationException("Papi callback body is not valid JSON.");
    }
    if (callback == null
        || callback.merchantPaymentReference() == null
        || callback.merchantPaymentReference().isBlank()
        || callback.notificationToken() == null
        || callback.notificationToken().isBlank()) {
      throw new PaymentValidationException("Papi callback is missing payment identifiers.");
    }

    JPapiPayment payment =
        papiPaymentRepository
            .findByMerchantReferenceForUpdate(callback.merchantPaymentReference())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format(
                            "No Papi payment for reference=%s",
                            forJava(callback.merchantPaymentReference()))));

    if (payment.getNotificationToken() == null) {
      verifyUnpersistedCallbackToken(payment, callback);
    } else if (!constantTimeEquals(payment.getNotificationToken(), callback.notificationToken())) {
      throw new PapiWebhookSecurityException("Papi callback token does not match the payment.");
    }
    if (payment.getPapiPaymentReference() != null
        && !constantTimeEquals(payment.getPapiPaymentReference(), callback.paymentReference())) {
      throw new PapiWebhookSecurityException("Papi callback payment reference does not match.");
    }

    if (payment.getStatus() != PENDING) return;

    PaymentStatus status = resolveCallbackStatus(callback.paymentStatus());
    payment.setStatus(toVerificationStatus(status));
    if (callback.paymentReference() != null) {
      payment.setPapiPaymentReference(callback.paymentReference());
    }
    payment.setUpdatedAt(now());
    papiPaymentRepository.save(payment);
  }

  private void verifyUnpersistedCallbackToken(
      JPapiPayment payment, PapiCallBackRequest callback) {
    try {
      PapiPaymentResponse providerPayment =
          (PapiPaymentResponse)
              gatewayFactory.getGateway(PAPI).getPaymentStatus(payment.getMerchantReference());
      if (!payment.getMerchantReference().equals(providerPayment.getTransactionId())
          || !constantTimeEquals(
              providerPayment.getNotificationToken(), callback.notificationToken())) {
        throw new PapiWebhookSecurityException(
            "Papi callback token cannot be matched to the payment.");
      }
      payment.setNotificationToken(providerPayment.getNotificationToken());
      if (providerPayment.getPapiPaymentReference() != null) {
        payment.setPapiPaymentReference(providerPayment.getPapiPaymentReference());
      }
    } catch (PaymentGatewayException e) {
      throw new PapiWebhookSecurityException(
          "Papi callback token could not be verified against the payment.");
    }
  }

  private JPapiPayment resolveExistingPayment(
      JPapiPayment existing, RPapiPaymentRequest request) {
    if (existing.getStatus() == PENDING || existing.getStatus() == SUCCESS) {
      boolean samePayment =
          existing.getAmount().compareTo(request.getAmount()) == 0
              && existing.getPaymentMethod().equals(request.getPaymentMethod().name())
              && existing
                  .getPayer()
                  .getPhoneNumber()
                  .equals(normalizeMadagascarPhoneNumber(request.getPayer().getPhoneNumber()));
      if (!samePayment) {
        throw new PaymentConflictException(
            "A Papi payment already exists for this reference with different payment details.");
      }
      return existing;
    }

    // Papi permits a new attempt after FAILED, including changed payment details.
    existing.setStatus(PENDING);
    existing.setNotificationToken(null);
    existing.setPapiPaymentReference(null);
    existing.setAmount(request.getAmount());
    existing.setPaymentMethod(request.getPaymentMethod().name());
    existing.setPayer(resolvePaymentParty(toStoredParty(request.getPayer())));
    existing.setDescription(request.getDescription());
    existing.setType(request.getType());
    existing.setUpdatedAt(now());
    return papiPaymentRepository.save(existing);
  }

  private JPapiPayment createPayment(RPapiPaymentRequest request) {
    LocalDateTime createdAt = now();
    PaymentParty payee =
        PaymentParty.builder()
            .phoneNumber(conf.getPartnerMsisdn())
            .name(conf.getPartnerName())
            .country(MADAGASCAR)
            .build();

    return papiPaymentRepository.save(
        JPapiPayment.builder()
            .id(randomUUID().toString())
            .transactionId(request.getReference())
            .merchantReference(request.getReference())
            .paymentMethod(request.getPaymentMethod().name())
            .payer(resolvePaymentParty(toStoredParty(request.getPayer())))
            .payee(resolvePaymentParty(toStoredParty(payee)))
            .amount(request.getAmount())
            .description(request.getDescription())
            .provider(PAPI)
            .currency(MGA)
            .status(PENDING)
            .type(request.getType())
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .build());
  }

  private PapiPaymentRequest toGatewayRequest(RPapiPaymentRequest request, JPapiPayment payment) {
    PaymentParty payer = toStoredParty(request.getPayer());
    if (request.getPaymentMethod()
        == com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod.AIRTEL_MONEY) {
      payer.setPhoneNumber(payer.getPhoneNumber().substring(1));
    }
    return PapiPaymentRequest.builder()
        .transactionId(request.getReference())
        .reference(request.getReference())
        .paymentMethod(request.getPaymentMethod())
        .temporaryCode(request.getTemporaryCode())
        .payerEmail(request.getPayerEmail())
        .amount(request.getAmount())
        .currency(MGA)
        .description(request.getDescription())
        .payer(payer)
        .payee(paymentPartyMapper.toModel(payment.getPayee()))
        .provider(PAPI)
        .type(request.getType())
        .build();
  }

  private void validateRequest(RPapiPaymentRequest request) {
    if (request.getAmount() == null
        || request.getAmount().compareTo(BigDecimal.valueOf(300)) < 0
        || request.getAmount().stripTrailingZeros().scale() > 0) {
      throw new PaymentValidationException("Papi amount must be an integer of at least 300 MGA.");
    }
    if (request.getPayer() == null
        || request.getPayer().getName() == null
        || request.getPayer().getName().isBlank()
        || request.getPayer().getPhoneNumber() == null
        || request.getPayer().getPhoneNumber().isBlank()) {
      throw new PaymentValidationException("Papi payer name and phoneNumber are required.");
    }
    if (request.getPayer().getName().length() > 255
        || request.getDescription() == null
        || request.getDescription().length() > 255) {
      throw new PaymentValidationException("Papi name and description must not exceed 255 characters.");
    }
    normalizeMadagascarPhoneNumber(request.getPayer().getPhoneNumber());
    if (request.getPaymentMethod() == null) {
      throw new PaymentValidationException("Papi paymentMethod is required.");
    }
    if (request.getPaymentMethod().name().equals("ORANGE_MONEY")
        && (request.getTemporaryCode() == null || request.getTemporaryCode().isBlank())) {
      throw new PaymentValidationException("Orange Money requires a temporaryCode.");
    }
    if (conf.getPartnerName() == null
        || conf.getPartnerName().isBlank()
        || conf.getPartnerMsisdn() == null
        || conf.getPartnerMsisdn().isBlank()) {
      throw new PaymentValidationException("Papi partner name and MSISDN must be configured.");
    }
  }

  private void refreshPendingPayment(String reference) {
    try {
      PapiPaymentResponse response =
          (PapiPaymentResponse)
              gatewayFactory.getGateway(PAPI).getPaymentStatus(reference);
      if (!reference.equals(response.getTransactionId())) return;
      JPapiPayment payment =
          papiPaymentRepository
              .findByMerchantReference(reference)
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(
                          format("No Papi payment for reference=%s", forJava(reference))));
      if (payment.getNotificationToken() != null
          && !constantTimeEquals(payment.getNotificationToken(), response.getNotificationToken())) {
        return;
      }
      papiPaymentStatusService.applyProviderResponse(reference, response);
    } catch (PaymentGatewayException e) {
      log.warn(
          "Papi status refresh failed for reference={}; returning the locally stored status",
          forJava(reference));
    }
  }

  private JPaymentParty resolvePaymentParty(PaymentParty party) {
    return paymentPartyRepository
        .findJPaymentPartyByPhoneNumber(party.getPhoneNumber())
        .orElseGet(
            () -> {
              JPaymentParty jParty = paymentPartyMapper.toPersistence(party);
              jParty.setId(UUID.randomUUID().toString());
              return paymentPartyRepository.save(jParty);
            });
  }

  private PaymentParty toStoredParty(PaymentParty party) {
    return PaymentParty.builder()
        .id(party.getId())
        .name(party.getName())
        .country(MADAGASCAR)
        .phoneNumber(normalizeMadagascarPhoneNumber(party.getPhoneNumber()))
        .build();
  }

  private String normalizeMadagascarPhoneNumber(String phoneNumber) {
    String digits = phoneNumber.replaceAll("[^0-9]", "");
    if (digits.startsWith("261") && digits.length() == 12) {
      return "0" + digits.substring(3);
    }
    if (digits.length() == 9) return "0" + digits;
    if (digits.length() == 10 && digits.startsWith("0")) return digits;
    throw new PaymentValidationException("Papi requires a valid Madagascar phone number.");
  }

  private PaymentStatus resolveCallbackStatus(String status) {
    if (status == null) throw new PaymentValidationException("Papi callback paymentStatus is missing.");
    return switch (status) {
      case "PENDING" -> PaymentStatus.PENDING;
      case "SUCCESS" -> PaymentStatus.COMPLETED;
      case "FAILED" -> PaymentStatus.FAILED;
      default -> throw new PaymentValidationException("Papi callback paymentStatus is invalid.");
    };
  }

  private com.devikapps.vaikaparts.event.model.VerificationStatus toVerificationStatus(
      PaymentStatus status) {
    if (status == null) return PENDING;
    return switch (status) {
      case COMPLETED -> SUCCESS;
      case FAILED -> FAILED;
      case PENDING -> PENDING;
    };
  }

  private boolean constantTimeEquals(String expected, String received) {
    if (expected == null || received == null) return false;
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8));
  }
}
