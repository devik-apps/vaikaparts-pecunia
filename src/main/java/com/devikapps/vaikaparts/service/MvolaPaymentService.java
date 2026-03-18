package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MvolaPaymentService implements PaymentService {

  private final PaymentGatewayFactory gatewayFactory;

  @Override
  public PaymentResponse initiatePayment(PaymentRequest request) {
    log.info("MVola initiatePayment for transactionId={}", forJava(request.getTransactionId()));
    return gatewayFactory.getGateway(MVOLA).initiatePayment(request);
  }

  @Override
  public PaymentResponse getPaymentStatus(String transactionId) {
    log.info("MVola getPaymentStatus for transactionId={}", forJava(transactionId));
    return gatewayFactory.getGateway(MVOLA).getPaymentStatus(transactionId);
  }

  @Override
  public PaymentResponse getPaymentDetails(String transactionId) {
    log.info("MVola getPaymentDetails for transactionId={}", forJava(transactionId));
    return gatewayFactory.getGateway(MVOLA).getPaymentDetails(transactionId);
  }
}
