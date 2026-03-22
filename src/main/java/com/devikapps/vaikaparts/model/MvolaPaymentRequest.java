package com.devikapps.vaikaparts.model;

import static java.lang.String.format;

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
public class MvolaPaymentRequest extends PaymentRequest {
  private String correlationId;
  private String callbackUrl;

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
        \ttype=%s,\s
        \tcorrelation_id=%s,\s
        \tcallback_url=%s
        }\
        """,
        getTransactionId(),
        getAmount(),
        getCurrency(),
        getDescription(),
        getPayer(),
        getPayee(),
        getProvider(),
        getType(),
        correlationId,
        callbackUrl);
  }
}
