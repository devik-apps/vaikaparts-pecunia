package com.devikapps.vaikaparts.gateway;

import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;

public interface PaymentGateway {

  PaymentResponse initiatePayment(PaymentRequest request);

  PaymentResponse getPaymentStatus(String transactionId);

  PaymentResponse getPaymentDetails(String transactionId);

  PaymentProvider getProvider();
}
