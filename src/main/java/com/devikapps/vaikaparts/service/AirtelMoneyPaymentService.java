package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.event.model.VerificationStatus.FAILED;
import static com.devikapps.vaikaparts.event.model.VerificationStatus.PENDING;
import static com.devikapps.vaikaparts.event.model.VerificationStatus.SUCCESS;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.MGA;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.AIRTEL_MONEY;
import static com.devikapps.vaikaparts.service.util.Paginator.PAGE_FIELD;
import static com.devikapps.vaikaparts.service.util.Paginator.SIZE_FIELD;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.AirtelMoneyCallBackRequest;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.PaymentVerificationRequested;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.mapper.AirtelMoneyPaymentMapper;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.AirtelMoneyPayment;
import com.devikapps.vaikaparts.model.AirtelMoneyPaymentResponse;
import com.devikapps.vaikaparts.model.Payment;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JAirtelMoneyPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import com.devikapps.vaikaparts.service.util.Paginator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class AirtelMoneyPaymentService implements PaymentService {

  private final PaymentGatewayFactory gatewayFactory;
  private final PaymentRepository paymentRepository;
  private final PaymentRequestedRepository paymentRequestedRepository;
  private final EventProducer<PaymentVerificationRequested> eventProducer;
  private final PaymentPartyMapper paymentPartyMapper;
  private final AirtelMoneyPaymentMapper airtelMoneyPaymentMapper;
  private final PaymentPartyRepository paymentPartyRepository;
  private final Paginator paginator;

  @Override
  @Transactional
  public Payment initiatePayment(PaymentRequest request) {
    log.info(
        "Airtel Money initiatePayment for phoneNumber={}",
        forJava(request.getPayer().getPhoneNumber()));

    var payment = buildPaymentFromRequest(request);
    log.info("Airtel Money payment created with id={}", payment.getId());

    JPaymentVerificationRequested verificationLog =
        JPaymentVerificationRequested.builder()
            .id(randomUUID().toString())
            .payment(payment)
            .status(PENDING)
            .createdAt(payment.getCreatedAt())
            .maxVerificationAttemptNb(5)
            .build();

    PaymentVerificationRequested verificationEvent =
        PaymentVerificationRequested.builder()
            .id(verificationLog.getId())
            .paymentId(payment.getId())
            .maxVerificationAttemptNb(5)
            .build();

    paymentRequestedRepository.save(verificationLog);
    log.info(
        "Airtel Money PaymentVerificationRequested log saved with id={}", verificationLog.getId());

    request.setTransactionId(randomUUID().toString());
    var response =
        (AirtelMoneyPaymentResponse)
            gatewayFactory.getGateway(AIRTEL_MONEY).initiatePayment(request);

    log.info(
        "Airtel Money gateway response received. TransactionId={}",
        forJava(response.getTransactionId()));

    payment.setTransactionId(response.getTransactionId());
    paymentRepository.save(payment);

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            eventProducer.accept(List.of(verificationEvent));
            log.info(
                "Airtel Money PaymentVerificationRequested event published after commit."
                    + " EventId={}",
                forJava(verificationEvent.getId()));
          }
        });

    return airtelMoneyPaymentMapper.toModel(payment);
  }

  @Override
  public Payment getPayment(@NotNull @NotBlank String transactionId) {
    log.info("Airtel Money getPayment for transactionId={}", forJava(transactionId));

    var payment =
        (JAirtelMoneyPayment)
            paymentRepository
                .findJPaymentByTransactionId(transactionId)
                .orElseThrow(
                    () ->
                        new EntityNotFoundException(
                            format(
                                "No Airtel Money payment with transactionId=%s found",
                                forJava(transactionId))));

    return airtelMoneyPaymentMapper.toModel(payment);
  }

  @Transactional
  public Page<AirtelMoneyPayment> findPaymentsByPaymentPartyMsisdn(
      @NotNull String customerMsisdn, Integer page, Integer size) {

    log.info(
        "Requesting all Airtel Money payments for msisdn={}, page={}, size={}",
        forJava(customerMsisdn),
        page,
        size);

    Map<String, Integer> pagination = paginator.apply(page, size);
    Pageable pageable =
        PageRequest.of(
            pagination.get(PAGE_FIELD),
            pagination.get(SIZE_FIELD),
            Sort.by("createdAt").descending());

    return paymentRepository
        .findAllByPayer_PhoneNumberAndProvider(customerMsisdn, AIRTEL_MONEY, pageable)
        .map(p -> airtelMoneyPaymentMapper.toModel((JAirtelMoneyPayment) p));
  }

  @Transactional
  public void handleCallBack(AirtelMoneyCallBackRequest request) {
    log.info(
        "Airtel Money handleCallBack for transactionId={}, statusCode={}",
        forJava(request.getTransaction().getId()),
        forJava(request.getTransaction().getStatusCode()));

    var payment =
        (JAirtelMoneyPayment)
            paymentRepository
                .findJPaymentByTransactionId(request.getTransaction().getId())
                .orElseThrow(
                    () ->
                        new EntityNotFoundException(
                            format(
                                "No Airtel Money payment found for transactionId=%s",
                                forJava(request.getTransaction().getId()))));

    VerificationStatus newStatus =
        resolveVerificationStatus(request.getTransaction().getStatusCode());

    payment.setStatus(newStatus);
    payment.setAirtelMoneyId(request.getTransaction().getAirtelMoneyId());
    payment.setUpdatedAt(now());
    paymentRepository.save(payment);

    log.info(
        "Airtel Money callback processed. PaymentId={}, NewStatus={}", payment.getId(), newStatus);
  }

  @Override
  public VerificationStatus resolveVerificationStatus(String statusCode) {
    if (statusCode == null) return PENDING;
    return switch (statusCode.toUpperCase()) {
      case "TS" -> SUCCESS;
      case "TF" -> FAILED;
      default -> PENDING;
    };
  }

  private JAirtelMoneyPayment buildPaymentFromRequest(PaymentRequest request) {
    var createdAt = now();

    var payment =
        JAirtelMoneyPayment.builder()
            .id(randomUUID().toString())
            .payer(resolvePaymentParty(request.getPayer()))
            .payee(resolvePaymentParty(request.getPayee()))
            .amount(request.getAmount())
            .description(request.getDescription())
            .provider(AIRTEL_MONEY)
            .currency(MGA)
            .status(PENDING)
            .type(request.getType())
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .build();

    return paymentRepository.save(payment);
  }

  private JPaymentParty resolvePaymentParty(final PaymentParty party) {
    return resolvePaymentParty(party, paymentPartyRepository, paymentPartyMapper);
  }
}
