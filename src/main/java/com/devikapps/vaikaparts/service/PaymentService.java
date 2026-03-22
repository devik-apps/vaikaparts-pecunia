package com.devikapps.vaikaparts.service;

import com.devikapps.vaikaparts.model.Payment;
import com.devikapps.vaikaparts.model.PaymentRequest;

public interface PaymentService {

  Payment initiatePayment(PaymentRequest request);

  Payment getPayment(String transactionId);
}
