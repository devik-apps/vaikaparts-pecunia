package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.classifier.PaymentCurrency;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public abstract class PaymentRequest {
  private String transactionId;
  private BigDecimal amount;
  private PaymentCurrency currency;
  private String description;
  private PaymentParty payer;
  private PaymentParty payee;
  private PaymentProvider provider;
  private PaymentType type;
}
