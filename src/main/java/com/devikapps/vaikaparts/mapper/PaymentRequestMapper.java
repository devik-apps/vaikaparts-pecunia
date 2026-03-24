package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.RPaymentRequest;
import com.devikapps.vaikaparts.model.AirtelMoneyPaymentRequest;
import com.devikapps.vaikaparts.model.MvolaPaymentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = PaymentPartyMapper.class)
public interface PaymentRequestMapper {
  String MVOLA = "java(com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA)";
  String AIRTEL_MONEY =
      "java(com.devikapps.vaikaparts.model.classifier.PaymentProvider.AIRTEL_MONEY)";
  String AMOUNT = "java(java.math.BigDecimal.valueOf(request.getAmount()))";

  @Mapping(target = "provider", expression = MVOLA)
  @Mapping(target = "amount", expression = AMOUNT)
  @Mapping(target = "transactionId", ignore = true)
  @Mapping(target = "correlationId", ignore = true)
  @Mapping(target = "callbackUrl", ignore = true)
  @Mapping(target = "payee", ignore = true)
  MvolaPaymentRequest toMvolaPaymentRequest(RPaymentRequest request);

  @Mapping(target = "provider", expression = AIRTEL_MONEY)
  @Mapping(target = "amount", expression = AMOUNT)
  @Mapping(target = "transactionId", ignore = true)
  @Mapping(target = "reference", ignore = true)
  @Mapping(target = "payee", ignore = true)
  AirtelMoneyPaymentRequest toAirtelMoneyPaymentRequest(RPaymentRequest request);
}
