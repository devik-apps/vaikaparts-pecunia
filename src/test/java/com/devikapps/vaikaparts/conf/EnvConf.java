package com.devikapps.vaikaparts.conf;

import com.devikapps.vaikaparts.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

@InfraGenerated
@TestConfiguration
public class EnvConf {

  private final String MVOLA_BASE_URL =
      "https://devapi.mvola.mg/mvola/mm/transactions/type/merchantpay/1.0.0";
  private final String MVOLA_BASE_URL_TOKEN = "https://developer.mvola.mg/oauth2/token";

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("payment.mvola.token-url", () -> MVOLA_BASE_URL_TOKEN);
    registry.add("payment.mvola.base-url", () -> MVOLA_BASE_URL);
    registry.add("payment.mvola.partner-msisdn", () -> "0343500003");
    registry.add("payment.mvola.partner-name", () -> "Test VaikaParts customer");
    registry.add("payment.mvola.callback-url", () -> "");
    registry.add("payment.mvola.consumer.key", () -> "af2oL4QUGM4beCdjlz2EMQRJaO4a");
    registry.add("payment.mvola.consumer.secret", () -> "9KbZoElwPUcFw2G6fLSC7ZiT9mUa");
  }
}
