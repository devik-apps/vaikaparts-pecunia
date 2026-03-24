package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.PaymentCurrency;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RPaymentRequest {
  private @NotNull double amount;
  private @NotNull PaymentCurrency currency;
  private @NotNull @NotBlank String description;
  private @NotNull PaymentType type;
  private @NotNull @Valid PaymentParty payer;
  private @NotNull @Valid PaymentParty payee;
}
