package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.model.AirtelMoneyPayment;
import com.devikapps.vaikaparts.repository.model.JAirtelMoneyPayment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = PaymentPartyMapper.class)
public interface AirtelMoneyPaymentMapper {

  AirtelMoneyPayment toModel(JAirtelMoneyPayment jAirtelMoneyPayment);

  JAirtelMoneyPayment toPersistence(AirtelMoneyPayment airtelMoneyPayment);
}
