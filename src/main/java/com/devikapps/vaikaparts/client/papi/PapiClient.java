package com.devikapps.vaikaparts.client.papi;

import static java.lang.String.format;

import com.devikapps.vaikaparts.client.papi.model.PapiInitiatePaymentRequest;
import com.devikapps.vaikaparts.client.papi.model.PapiPaymentResponse;
import com.devikapps.vaikaparts.config.PapiConf;
import com.devikapps.vaikaparts.exception.PaymentConflictException;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

@Component
public class PapiClient {
  private static final String PAYMENTS_PATH = "/v1/payments";

  private final PapiConf conf;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public PapiClient(
      PapiConf conf,
      @Qualifier("papiRestTemplate") RestTemplate restTemplate,
      ObjectMapper objectMapper) {
    this.conf = conf;
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
  }

  public PapiPaymentResponse initiate(PapiInitiatePaymentRequest request) {
    return exchange(HttpMethod.POST, PAYMENTS_PATH, request);
  }

  public PapiPaymentResponse getPayment(String reference) {
    String encodedReference = UriUtils.encodePathSegment(reference, StandardCharsets.UTF_8);
    return exchange(HttpMethod.GET, PAYMENTS_PATH + "/" + encodedReference, null);
  }

  private PapiPaymentResponse exchange(
      HttpMethod method, String path, PapiInitiatePaymentRequest body) {
    if (conf.getApiKey() == null || conf.getApiKey().isBlank()) {
      throw new PaymentGatewayException("Papi API key is not configured.");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.set("Token", conf.getApiKey());
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

    try {
      var response =
          restTemplate.exchange(
              URI.create(trimTrailingSlash(conf.getBaseUrl()) + path),
              method,
              new HttpEntity<>(body, headers),
              PapiPaymentResponse.class);
      if (response.getBody() == null) {
        throw new PaymentGatewayException("Papi returned an empty response.");
      }
      return response.getBody();
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() == 409 && isPaymentConflict(e.getResponseBodyAsByteArray())) {
        throw new PaymentConflictException(
            format("Papi rejected the payment reference with HTTP %d.", e.getStatusCode().value()));
      }
      throw new PaymentGatewayException(
          format("Papi request failed with HTTP %d.", e.getStatusCode().value()));
    } catch (RestClientException e) {
      throw new PaymentGatewayException("Papi request failed due to a network or response error.");
    }
  }

  private boolean isPaymentConflict(byte[] responseBody) {
    try {
      PapiPaymentResponse response = objectMapper.readValue(responseBody, PapiPaymentResponse.class);
      return response.error() != null && "PAYMENT_CONFLICT".equals(response.error().code());
    } catch (IOException e) {
      return false;
    }
  }

  private String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      throw new PaymentGatewayException("Papi base URL is not configured.");
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
