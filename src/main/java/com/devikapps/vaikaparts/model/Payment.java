package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.model.classifier.PaymentCurrency;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
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
@SuperBuilder
@ToString
public abstract class Payment {
  private UUID id;
  private String transactionId;
  private String description;
  private PaymentParty payer;
  private PaymentParty payee;
  private PaymentProvider provider;
  private PaymentType type;
  private BigDecimal amount;
  private PaymentCurrency currency;
  private VerificationStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
