package com.devikapps.vaikaparts.service;

import static java.time.Instant.now;

import com.devikapps.vaikaparts.config.AirtelMoneyConf;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import dev.razafindratelo.airtel_money_client.api.AuthenticationApi;
import dev.razafindratelo.airtel_money_client.model.TokenRequest;
import dev.razafindratelo.airtel_money_client.model.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AirtelMoneyTokenService {

  private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 60;
  private static final int DEFAULT_EXPIRES_IN_SECONDS = 3600;

  private final AirtelMoneyConf conf;
  private final AuthenticationApi authenticationApi;

  private String cachedToken;
  private long tokenExpiryEpochSeconds;

  public synchronized String getToken() {
    if (isTokenValid()) {
      log.debug("Airtel Money token cache hit. Reusing cached token.");
      return cachedToken;
    }
    return fetchNewToken();
  }

  private boolean isTokenValid() {
    return cachedToken != null
        && now().getEpochSecond() < tokenExpiryEpochSeconds - TOKEN_EXPIRY_BUFFER_SECONDS;
  }

  private String fetchNewToken() {
    log.info("Fetching new Airtel Money access token.");

    TokenRequest request = new TokenRequest();
    request.setClientId(conf.getClientId());
    request.setClientSecret(conf.getClientSecret());
    request.setGrantType(TokenRequest.GrantTypeEnum.CLIENT_CREDENTIALS);

    TokenResponse response = authenticationApi.getAccessToken("application/json", "*/*", request);

    if (response == null || response.getAccessToken().isBlank()) {
      throw new PaymentGatewayException(
          "Airtel Money token response is empty or missing access_token.");
    }

    cachedToken = response.getAccessToken();
    tokenExpiryEpochSeconds = now().getEpochSecond() + resolveExpiresIn(response.getExpiresIn());

    log.info(
        "Airtel Money access token acquired, expires in {}s.",
        resolveExpiresIn(response.getExpiresIn()));
    return cachedToken;
  }

  private long resolveExpiresIn(String expiresIn) {
    if (expiresIn == null || expiresIn.isBlank()) {
      log.warn(
          "Airtel Money token response missing expires_in — defaulting to {}s.",
          DEFAULT_EXPIRES_IN_SECONDS);
      return DEFAULT_EXPIRES_IN_SECONDS;
    }
    try {
      return Long.parseLong(expiresIn.trim());
    } catch (NumberFormatException e) {
      log.warn(
          "Airtel Money token expires_in value '{}' is not a valid number — defaulting to {}s.",
          expiresIn,
          DEFAULT_EXPIRES_IN_SECONDS);
      return DEFAULT_EXPIRES_IN_SECONDS;
    }
  }
}
