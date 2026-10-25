package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class RPapiPaymentRequest {
  @NotNull @Min(300) private BigDecimal amount;

  @NotNull private PaymentType type;

  @NotBlank @Size(max = 255) private String description;

  @NotBlank @Size(max = 255) private String reference;

  @NotNull private PapiPaymentMethod paymentMethod;

  @NotNull @Valid private PaymentParty payer;

  private String temporaryCode;

  @Email private String payerEmail;
}
