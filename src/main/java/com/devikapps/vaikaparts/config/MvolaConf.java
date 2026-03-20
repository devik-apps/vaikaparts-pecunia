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
public class MvolaConf {

  @Value("${payment.mvola.consumer.key}")
  @NotBlank
  private String consumerKey;

  @Value("${payment.mvola.consumer.secret}")
  @NotBlank
  private String consumerSecret;

  @Value("${payment.mvola.base-url}")
  @NotBlank
  private String baseUrl;

  @Value("${payment.mvola.token-url}")
  @NotBlank
  private String tokenUrl;

  @Value("${payment.mvola.partner-msisdn}")
  @NotBlank
  private String partnerMsisdn;

  @Value("${payment.mvola.partner-name}")
  @NotBlank
  private String partnerName;

  @Value("${payment.mvola.callback-url}")
  private String callbackUrl;
}
