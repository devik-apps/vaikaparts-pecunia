package com.devikapps.vaikaparts.gateway.mvola;

import com.devikapps.vaikaparts.model.PaymentRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString(callSuper = true)
public class MvolaPaymentRequest extends PaymentRequest {
  private String correlationId;
  private String callbackUrl;
}
