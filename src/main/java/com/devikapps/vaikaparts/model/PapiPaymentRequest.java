package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class PapiPaymentRequest extends PaymentRequest {
  @NotBlank
  @Size(max = 255)
  private String reference;

  private PapiPaymentMethod paymentMethod;

  private String temporaryCode;

  private String payerEmail;
}
