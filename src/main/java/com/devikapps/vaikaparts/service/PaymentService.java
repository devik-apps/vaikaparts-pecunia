package com.devikapps.vaikaparts.service;

import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;

public interface PaymentService {

  PaymentResponse initiatePayment(PaymentRequest request);

  PaymentResponse getPaymentStatus(String transactionId, PaymentProvider provider);

  PaymentResponse getPaymentDetails(String transactionId, PaymentProvider provider);
}
