package com.devikapps.vaikaparts.gateway;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.validator.PaymentRequestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPaymentGateway implements PaymentGateway {

  private final PaymentRequestValidator validator;

  @Override
  public final PaymentResponse initiatePayment(PaymentRequest request) {
    validator.validate(request);
    log.info(
        "Initiating payment via provider '{}' with transactionId '{}'",
        getProvider(),
        request.getTransactionId());
    return doInitiatePayment(request);
  }

  @Override
  public final PaymentResponse getPaymentStatus(String transactionId) {
    validator.validateNotBlank(transactionId, "transactionId");
    log.info(
        "Fetching payment status via provider '{}' for transactionId '{}'",
        getProvider(),
        forJava(transactionId));
    return doGetPaymentStatus(transactionId);
  }

  @Override
  public final PaymentResponse getPaymentDetails(String transactionId) {
    validator.validateNotBlank(transactionId, "transactionId");
    log.info(
        "Fetching payment details via provider '{}' for transactionId '{}'",
        getProvider(),
        forJava(transactionId));
    return doGetPaymentDetails(transactionId);
  }

  protected abstract PaymentResponse doInitiatePayment(PaymentRequest request);

  protected abstract PaymentResponse doGetPaymentStatus(String transactionId);

  protected abstract PaymentResponse doGetPaymentDetails(String transactionId);
}
