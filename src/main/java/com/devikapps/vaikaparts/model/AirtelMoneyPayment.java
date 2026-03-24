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
public class AirtelMoneyPayment extends Payment {

  // Only populated once the transaction reaches TS (Transaction Success).
  private String airtelMoneyId;
}
