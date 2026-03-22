package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.event.model.VerificationStatus.FAILED;
import static com.devikapps.vaikaparts.event.model.VerificationStatus.PENDING;
import static com.devikapps.vaikaparts.event.model.VerificationStatus.SUCCESS;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static com.devikapps.vaikaparts.service.util.Paginator.PAGE_FIELD;
import static com.devikapps.vaikaparts.service.util.Paginator.SIZE_FIELD;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.MvolaCallBackRequest;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.PaymentVerificationRequested;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.mapper.MvolaPaymentMapper;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.MvolaPayment;
import com.devikapps.vaikaparts.model.MvolaPaymentResponse;
import com.devikapps.vaikaparts.model.Payment;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JMvolaPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import com.devikapps.vaikaparts.service.util.Paginator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class MvolaPaymentService implements PaymentService {

  private final PaymentGatewayFactory gatewayFactory;
  private final PaymentRepository paymentRepository;
  private final PaymentRequestedRepository paymentRequestedRepository;
  private final EventProducer<PaymentVerificationRequested> eventProducer;
  private final PaymentPartyMapper paymentPartyMapper;
  private final MvolaPaymentMapper mvolaPaymentMapper;
  private final PaymentPartyRepository paymentPartyRepository;
  private final Paginator paginator;

  @Transactional
  public Page<MvolaPayment> findPaymentsByPaymentPartyMsisdn(
      @NotNull String customerMsisdn, Integer page, Integer size) {
    log.info(
        "Request all MVOLA payment of customer msisdn : {}, with page={} and size={}",
        forJava(customerMsisdn),
        page,
        size);

    var pagination = paginator.apply(page, size);

    var pageable =
        PageRequest.of(
            pagination.get(PAGE_FIELD),
            pagination.get(SIZE_FIELD),
            Sort.by("createdAt").descending());

    var payments =
        paymentRepository.findAllByPayer_PhoneNumberAndProvider(customerMsisdn, MVOLA, pageable);

    return payments.map(p -> mvolaPaymentMapper.toModel((JMvolaPayment) p));
  }

  @Override
  @Transactional
  public Payment initiatePayment(PaymentRequest request) {
    log.info(
        "MVola initiatePayment for phoneNumber={}", forJava(request.getPayer().getPhoneNumber()));

    var payment = buildPaymentFromPaymentRequest(request);
    log.info("MVola initiatePayment. Payment created with id={}", payment.getId());

    var paymentVerificationRequestedInstance =
        JPaymentVerificationRequested.builder()
            .id(randomUUID().toString())
            .payment(payment)
            .status(PENDING)
            .createdAt(payment.getCreatedAt())
            .maxVerificationAttemptNb(5)
            .build();

    var paymentVerificationRequested =
        PaymentVerificationRequested.builder()
            .id(paymentVerificationRequestedInstance.getId())
            .paymentId(payment.getId())
            .maxVerificationAttemptNb(5)
            .build();

    paymentRequestedRepository.save(paymentVerificationRequestedInstance);
    log.info(
        "MVola initiatePayment. PaymentVerificationRequested event created with id={}",
        paymentVerificationRequested.getId());

    request.setTransactionId(randomUUID().toString());
    var response = (MvolaPaymentResponse) gatewayFactory.getGateway(MVOLA).initiatePayment(request);

    log.info(
        "MVola initiatePayment. MVola Gateway have response with transactionId={}",
        response.getServerCorrelationId());
    payment.setTransactionId(response.getServerCorrelationId());
    payment.setServerCorrelationId(response.getServerCorrelationId());
    payment.setNotificationMethod(response.getNotificationMethod());

    response.setTransactionId(response.getServerCorrelationId());

    paymentRepository.save(payment);

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            eventProducer.accept(List.of(paymentVerificationRequested));
          }
        });

    return mvolaPaymentMapper.toModel(payment);
  }

  @Override
  public Payment getPayment(String transactionId) {
    log.info("MVola getPayment for transactionId={}", forJava(transactionId));
    var payment =
        (JMvolaPayment)
            paymentRepository
                .findJPaymentByTransactionId(transactionId)
                .orElseThrow(
                    () ->
                        new EntityNotFoundException(
                            format(
                                "No payment with transactionId=%s found", forJava(transactionId))));

    return mvolaPaymentMapper.toModel(payment);
  }

  @Transactional
  public void handleCallBack(final MvolaCallBackRequest request) {
    log.info(
        "MVola handleCallBack for serverCorrelationId={}, status={}",
        forJava(request.getServerCorrelationId()),
        forJava(request.getTransactionStatus()));

    final JMvolaPayment payment =
        (JMvolaPayment)
            paymentRepository
                .findJPaymentByTransactionId(request.getServerCorrelationId())
                .orElseThrow(
                    () ->
                        new EntityNotFoundException(
                            format(
                                "No payment found for serverCorrelationId=%s",
                                forJava(request.getServerCorrelationId()))));

    final VerificationStatus newStatus =
        switch (request.getTransactionStatus().toLowerCase()) {
          case "completed" -> SUCCESS;
          case "failed" -> FAILED;
          default -> PENDING;
        };

    payment.setStatus(newStatus);
    payment.setMvolaTransactionId(request.getTransactionReference());
    payment.setUpdatedAt(now());

    paymentRepository.save(payment);

    log.info("MVola callback processed — paymentId={}, newStatus={}", payment.getId(), newStatus);
  }

  private JMvolaPayment buildPaymentFromPaymentRequest(PaymentRequest request) {
    final var createdAt = now();

    var payment =
        JMvolaPayment.builder()
            .id(randomUUID().toString())
            .payer(resolvePaymentParty(request.getPayer()))
            .payee(resolvePaymentParty(request.getPayee()))
            .amount(request.getAmount())
            .description(request.getDescription())
            .provider(MVOLA)
            .currency(AR)
            .status(PENDING)
            .type(request.getType())
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .build();

    return paymentRepository.save(payment);
  }

  private JPaymentParty resolvePaymentParty(final PaymentParty party) {
    return paymentPartyRepository
        .findJPaymentPartyByPhoneNumber(party.getPhoneNumber())
        .orElseGet(
            () -> {
              var jParty = paymentPartyMapper.toPersistence(party);
              jParty.setId(randomUUID().toString());
              return paymentPartyRepository.save(jParty);
            });
  }
}
