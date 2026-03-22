package com.devikapps.vaikaparts.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MvolaPayment extends Payment {
  private String serverCorrelationId;
  private String mvolaTransactionId;
  private String notificationMethod;
}
