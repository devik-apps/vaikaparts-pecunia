package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.RMvolaPaymentRequest;
import com.devikapps.vaikaparts.model.MvolaPaymentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = PaymentPartyMapper.class)
public interface PaymentRequestMapper {
  String MVOLA = "java(com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA)";
  String AMOUNT = "java(java.math.BigDecimal.valueOf(request.getAmount()))";

  @Mapping(target = "provider", expression = MVOLA)
  @Mapping(target = "amount", expression = AMOUNT)
  @Mapping(target = "transactionId", ignore = true)
  @Mapping(target = "correlationId", ignore = true)
  @Mapping(target = "callbackUrl", ignore = true)
  MvolaPaymentRequest toMvolaPaymentRequest(RMvolaPaymentRequest request);
}
