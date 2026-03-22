package com.devikapps.vaikaparts.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString(callSuper = true)
public class MvolaPaymentResponse extends PaymentResponse {
  private String serverCorrelationId;
  private String notificationMethod;
  private String objectReference;
}
