package com.devikapps.vaikaparts.service;

import com.devikapps.vaikaparts.model.Payment;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;

public interface PaymentService {

  PaymentResponse initiatePayment(PaymentRequest request);

  Payment getPayment(String transactionId);
}
