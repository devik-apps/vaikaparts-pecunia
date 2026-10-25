package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.model.PapiPayment;
import com.devikapps.vaikaparts.repository.model.JPapiPayment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = PaymentPartyMapper.class)
public interface PapiPaymentMapper {
  PapiPayment toModel(JPapiPayment papiPayment);

  JPapiPayment toPersistence(PapiPayment papiPayment);
}
