package com.devikapps.vaikaparts.model;

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
public class AirtelMoneyPaymentResponse extends PaymentResponse {

  // Only present when the transaction has reached TS (Transaction Success).
  private String airtelMoneyId;

  private String message;
}
