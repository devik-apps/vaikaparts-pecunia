package com.devikapps.vaikaparts.model;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.PaymentCurrency;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
public abstract class PaymentRequest {
  private String transactionId;
  private BigDecimal amount;
  private PaymentCurrency currency;
  private String description;
  private PaymentParty payer;
  private PaymentParty payee;
  private PaymentProvider provider;
  private PaymentType type;

  @Override
  public String toString() {
    return format(
        """
        {
        \ttransaction_id=%s,\s
        \tamount=%s,\s
        \tcurrency=%s,\s
        \tdescription=%s,\s
        \tpayer=%s,\s
        \tpayee=%s,\s
        \tprovider=%s,\s
        \ttype=%s\s
        }
        """,
        transactionId, amount, currency, description, payer, payee, provider, type);
  }
}
