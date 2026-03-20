package com.devikapps.vaikaparts.gateway.mvola;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.PaymentRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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
