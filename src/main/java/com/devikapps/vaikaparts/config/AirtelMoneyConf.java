package com.devikapps.vaikaparts.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Getter
@Setter
public class AirtelMoneyConf {

  @Value("${payment.airtel.client-id}")
  @NotBlank
  private String clientId;

  @Value("${payment.airtel.client-secret}")
  @NotBlank
  private String clientSecret;

  @Value("${payment.airtel.base-url}")
  @NotBlank
  private String baseUrl;

  @Value("${payment.airtel.country}")
  @NotBlank
  private String country;

  @Value("${payment.airtel.currency}")
  @NotBlank
  private String currency;
}
