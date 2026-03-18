package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;

import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.gateway.mvola.MvolaProperties;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class MvolaTokenService {

  private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 60L;
  private static final String GRANT_TYPE = "client_credentials";
  private static final String SCOPE = "EXT_INT_MVOLA_SCOPE";

  private final MvolaProperties properties;
  private final RestTemplate restTemplate;

  private volatile String cachedToken;
  private volatile LocalDateTime tokenExpiresAt;

  public MvolaTokenService(MvolaProperties properties, final RestTemplate restTemplate) {
    this.properties = properties;
    this.restTemplate = restTemplate;
  }

  public synchronized String getToken() {
    if (isTokenValid()) return cachedToken;

    return fetchNewToken();
  }

  private boolean isTokenValid() {
    return cachedToken != null
        && tokenExpiresAt != null
        && now().isBefore(tokenExpiresAt.minusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));
  }

  @SuppressWarnings("unchecked")
  private String fetchNewToken() {
    log.info("Fetching new MVola access token.");

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.set(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader());
    headers.set("Cache-Control", "no-cache");

    final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", GRANT_TYPE);
    body.add("scope", SCOPE);

    var response =
        restTemplate.postForEntity(
            properties.getTokenUrl(), new HttpEntity<>(body, headers), Map.class);

    if (response.getBody() == null || !response.getBody().containsKey("access_token"))
      throw new PaymentGatewayException(
          format(
              "MVola token response did not contain an access_token. Status code: %s",
              response.getStatusCode().value()));

    cachedToken = (String) response.getBody().get("access_token");
    final int expiresIn = (Integer) response.getBody().getOrDefault("expires_in", 3600);
    tokenExpiresAt = now().plusSeconds(expiresIn);

    log.info("MVola access token acquired, expires in {}s.", expiresIn);
    return cachedToken;
  }

  private String buildBasicAuthHeader() {
    final String credentials =
        format("%s:%s", properties.getConsumerKey(), properties.getConsumerSecret());
    return format("Basic %s", Base64.getEncoder().encodeToString(credentials.getBytes()));
  }
}
