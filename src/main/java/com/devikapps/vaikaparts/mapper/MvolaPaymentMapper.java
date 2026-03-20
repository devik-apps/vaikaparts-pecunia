package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.model.MvolaPayment;
import com.devikapps.vaikaparts.repository.model.JMvolaPayment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = PaymentPartyMapper.class)
public interface MvolaPaymentMapper {

  MvolaPayment toModel(JMvolaPayment jMvolaPayment);

  JMvolaPayment toPersistence(MvolaPayment mvolaPayment);
}
