package com.devikapps.vaikaparts.conf;

import com.devikapps.vaikaparts.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

@InfraGenerated
@TestConfiguration
public class EnvConf {

  public static final String MVOLA_BASE_URL =
      "https://devapi.mvola.mg/mvola/mm/transactions/type/merchantpay/1.0.0";
  public static final String MVOLA_BASE_URL_TOKEN = "https://developer.mvola.mg/oauth2/token";
  public static final String MVOLA_MSISDN = "0343500004";

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("payment.mvola.token-url", () -> MVOLA_BASE_URL_TOKEN);
    registry.add("payment.mvola.base-url", () -> MVOLA_BASE_URL);
    registry.add("payment.mvola.partner-msisdn", () -> MVOLA_MSISDN);
    registry.add("payment.mvola.partner-name", () -> "TestMVola");
    registry.add("payment.mvola.callback-url", () -> "");
    registry.add("payment.mvola.consumer.key", () -> "af2oL4QUGM4beCdjlz2EMQRJaO4a");
    registry.add("payment.mvola.consumer.secret", () -> "9KbZoElwPUcFw2G6fLSC7ZiT9mUa");
  }
}
