package com.devikapps.vaikaparts.config;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirtelMoneyConf {

  @Value("${payment.airtel-money.client-id}")
  @NotBlank
  private String clientId;

  @Value("${payment.airtel-money.client-secret}")
  @NotBlank
  private String clientSecret;

  @Value("${payment.airtel-money.base-url}")
  @NotBlank
  private String baseUrl;

  @Value("${payment.airtel-money.country}")
  @NotBlank
  private String country;

  @Value("${payment.airtel-money.currency}")
  @NotBlank
  private String currency;

  @Value("${payment.airtel-money.partner-name}")
  private String partnerName;

  @Value("${payment.airtel-money.partner-msisdn}")
  private String partnerMsisdn;
}
