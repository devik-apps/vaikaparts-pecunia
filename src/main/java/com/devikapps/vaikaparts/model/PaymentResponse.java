package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.classifier.PaymentCurrency;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@SuperBuilder
public abstract class PaymentResponse {
  private String transactionId;
  private PaymentStatus status;
  private BigDecimal amount;
  private PaymentCurrency currency;
  private PaymentProvider provider;
  private String errorDescription;
  private LocalDateTime respondedAt;
}
