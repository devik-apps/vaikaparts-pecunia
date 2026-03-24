package com.devikapps.vaikaparts.service;

import static java.util.UUID.randomUUID;

import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.Payment;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;

public interface PaymentService {

  Payment initiatePayment(PaymentRequest request);

  Payment getPayment(String transactionId);

  VerificationStatus resolveVerificationStatus(String statusCode);

  default JPaymentParty resolvePaymentParty(
      final PaymentParty party,
      PaymentPartyRepository paymentPartyRepository,
      PaymentPartyMapper paymentPartyMapper) {
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
