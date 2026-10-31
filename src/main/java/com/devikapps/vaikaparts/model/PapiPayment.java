package com.devikapps.vaikaparts.model;

import java.math.BigDecimal;
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
public class PapiPayment extends Payment {
  private String papiPaymentReference;
  private String merchantReference;
  private String paymentMethod;
  private String notificationToken;
  private BigDecimal payerFee;
  private BigDecimal totalAmount;
}
