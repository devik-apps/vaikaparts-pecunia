package com.devikapps.vaikaparts.service;

import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;

public interface PaymentService {

  PaymentResponse initiatePayment(PaymentRequest request);

  PaymentResponse getPaymentStatus(String transactionId);

  PaymentResponse getPaymentDetails(String transactionId);
}
