package com.devikapps.vaikaparts.conf;

import static java.util.UUID.randomUUID;

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

  public static final String AIRTEL_MONEY_BASE_URL = "https://openapiuat.airtel.mg";
  public static final String AIRTEL_MONEY_CLIENT_ID = randomUUID().toString();
  public static final String AIRTEL_MONEY_CLIENT_SECRET = randomUUID().toString();
  public static final String AIRTEL_MONEY_MSISDN = "330000005";
  public static final String AIRTEL_MONEY_PARTNER = "TestAirtelMoney";

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("app.security.api-key", () -> randomUUID().toString());

    registry.add("payment.mvola.token-url", () -> MVOLA_BASE_URL_TOKEN);
    registry.add("payment.mvola.base-url", () -> MVOLA_BASE_URL);
    registry.add("payment.mvola.partner-msisdn", () -> MVOLA_MSISDN);
    registry.add("payment.mvola.partner-name", () -> "TestMVola");
    registry.add("payment.mvola.callback-url", () -> "");
    registry.add("payment.mvola.consumer.key", () -> "af2oL4QUGM4beCdjlz2EMQRJaO4a");
    registry.add("payment.mvola.consumer.secret", () -> "9KbZoElwPUcFw2G6fLSC7ZiT9mUa");

    registry.add("payment.airtel-money.client-id", () -> AIRTEL_MONEY_CLIENT_ID);
    registry.add("payment.airtel-money.client-secret", () -> AIRTEL_MONEY_CLIENT_SECRET);
    registry.add("payment.airtel-money.base-url", () -> AIRTEL_MONEY_BASE_URL);
    registry.add("payment.airtel-money.country", () -> "MG");
    registry.add("payment.airtel-money.currency", () -> "MGA");
    registry.add("payment.airtel-money.partner-name", () -> AIRTEL_MONEY_PARTNER);
    registry.add("payment.airtel-money.partner-msisdn", () -> AIRTEL_MONEY_MSISDN);
  }
}
