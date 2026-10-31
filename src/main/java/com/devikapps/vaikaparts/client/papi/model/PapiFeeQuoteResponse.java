package com.devikapps.vaikaparts.client.papi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PapiFeeQuoteResponse(Data data, PapiErrorResponse error) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(
      String paymentMethod,
      BigDecimal amount,
      BigDecimal payerFee,
      BigDecimal totalAmount,
      String currency) {}
}
