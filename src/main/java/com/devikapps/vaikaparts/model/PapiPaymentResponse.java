package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
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
public class PapiPaymentResponse extends PaymentResponse {
  private String papiPaymentReference;
  private PapiPaymentMethod paymentMethod;
  private String notificationToken;
}
