package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentPartyMapper {

  PaymentParty toModel(JPaymentParty jPaymentParty);

  JPaymentParty toPersistence(PaymentParty paymentParty);
}
