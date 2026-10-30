package com.devikapps.vaikaparts.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class PapiConf {
  @Value("${payment.papi.api-key:}")
  private String apiKey;

  @Value("${payment.papi.base-url:https://app.papi.mg/engine/api}")
  private String baseUrl;

  @Value("${payment.papi.notification-url:}")
  private String notificationUrl;

  @Value("${payment.papi.webhook-secret:}")
  private String webhookSecret;

  @Value("${payment.papi.test-mode:false}")
  private boolean testMode;

  @Value("${payment.papi.partner-name:}")
  private String partnerName;

  @Value("${payment.papi.partner-msisdn:}")
  private String partnerMsisdn;
}
