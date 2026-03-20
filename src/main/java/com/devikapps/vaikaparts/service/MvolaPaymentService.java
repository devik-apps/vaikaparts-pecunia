package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.event.model.VerificationStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.PaymentVerificationRequested;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentResponse;
import com.devikapps.vaikaparts.mapper.MvolaPaymentMapper;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.Payment;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.model.JMvolaPayment;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class MvolaPaymentService implements PaymentService {

  private final PaymentGatewayFactory gatewayFactory;
  private final PaymentRepository paymentRepository;
  private final EventProducer<PaymentVerificationRequested> eventProducer;
  private final PaymentPartyMapper paymentPartyMapper;
  private final MvolaPaymentMapper mvolaPaymentMapper;

  @Override
  @Transactional
  public PaymentResponse initiatePayment(PaymentRequest request) {
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

    log.info(
        "MVola initiatePayment. PaymentVerificationRequested event created with id={}",
        paymentVerificationRequested.getId());

    eventProducer.accept(List.of(paymentVerificationRequested));

    var response = (MvolaPaymentResponse) gatewayFactory.getGateway(MVOLA).initiatePayment(request);
    payment.setTransactionId(response.getTransactionId());
    response.setTransactionId(response.getServerCorrelationId());
    paymentRepository.save(payment);
    return response;
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

  private JPayment buildPaymentFromPaymentRequest(PaymentRequest request) {
    final var createdAt = now();

    var payment =
        JMvolaPayment.builder()
            .id(randomUUID().toString())
            .payee(paymentPartyMapper.toPersistence(request.getPayee()))
            .payer(paymentPartyMapper.toPersistence(request.getPayer()))
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
}
