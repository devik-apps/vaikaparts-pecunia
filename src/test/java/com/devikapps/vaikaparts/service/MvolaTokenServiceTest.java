package com.devikapps.vaikaparts.service;

import static java.util.Base64.getDecoder;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.config.MvolaConf;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class MvolaTokenServiceTest {

  public static final String TEST_ACCESS_TOKEN = randomUUID().toString();
  private static final int EXPIRE_DURATION_IN_MS = 3_600;

  @Mock private MvolaConf properties;

  @Mock private RestTemplate restTemplate;

  private MvolaTokenService subject;

  @BeforeEach
  void set_up() {
    when(properties.getConsumerKey()).thenReturn("testKey");
    when(properties.getConsumerSecret()).thenReturn("testSecret");
    when(properties.getTokenUrl()).thenReturn("https://developer.mvola.mg/oauth2/token");

    subject = new MvolaTokenService(properties, restTemplate);
  }

  @Test
  void should_fetch_and_return_access_token_on_first_call() {
    mockSuccessfulTokenResponse(TEST_ACCESS_TOKEN, EXPIRE_DURATION_IN_MS);

    final String token = subject.getToken();

    assertEquals(TEST_ACCESS_TOKEN, token);
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
  }

  @Test
  void should_send_basic_auth_header_with_base64_encoded_credentials() {
    mockSuccessfulTokenResponse(TEST_ACCESS_TOKEN, EXPIRE_DURATION_IN_MS);

    subject.getToken();

    var captor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(Map.class));

    final String authHeader = captor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    assertNotNull(authHeader);
    assertTrue(authHeader.startsWith("Basic "));

    final String decoded = new String(getDecoder().decode(authHeader.substring("Basic ".length())));
    assertEquals("testKey:testSecret", decoded);
  }

  @Test
  void should_send_correct_grant_type_and_scope_in_body() {
    mockSuccessfulTokenResponse(TEST_ACCESS_TOKEN, EXPIRE_DURATION_IN_MS);

    subject.getToken();

    var captor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(Map.class));

    MultiValueMap<String, String> body =
        (MultiValueMap<String, String>) captor.getValue().getBody();

    assertNotNull(body);
    assertEquals("client_credentials", body.getFirst("grant_type"));
    assertEquals("EXT_INT_MVOLA_SCOPE", body.getFirst("scope"));
  }

  @Test
  void should_return_cached_token_without_fetching_again_when_still_valid() {
    mockSuccessfulTokenResponse(TEST_ACCESS_TOKEN, EXPIRE_DURATION_IN_MS);

    subject.getToken();
    subject.getToken();
    subject.getToken();

    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
  }

  @Test
  void should_fetch_new_token_when_cached_token_is_expired() {
    mockSuccessfulTokenResponse("token-first", 0);
    subject.getToken();

    mockSuccessfulTokenResponse("token-second", EXPIRE_DURATION_IN_MS);
    final String token = subject.getToken();

    assertEquals("token-second", token);
    verify(restTemplate, times(2)).postForEntity(anyString(), any(), eq(Map.class));
  }

  @Test
  void should_throw_payment_gateway_exception_when_response_body_is_null() {
    final ResponseEntity<Map> response = ResponseEntity.ok(null);
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(response);

    assertThrows(PaymentGatewayException.class, subject::getToken);
  }

  @Test
  void should_throw_payment_gateway_exception_when_access_token_missing_from_response() {
    final Map<String, Object> body = Map.of("scope", "EXT_INT_MVOLA_SCOPE");
    ResponseEntity<Map> response = ResponseEntity.ok(body);
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(response);

    assertThrows(PaymentGatewayException.class, subject::getToken);
  }

  @Test
  void should_use_default_expiry_of_EXPIRE_DURATION_IN_MS_when_expires_in_missing() {
    final Map<String, Object> body = Map.of("access_token", TEST_ACCESS_TOKEN);
    ResponseEntity<Map> response = ResponseEntity.ok(body);
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(response);

    final String token = subject.getToken();

    assertEquals(TEST_ACCESS_TOKEN, token);
    // Second call should use the cache since default expiry is EXPIRE_DURATION_IN_MSs
    subject.getToken();
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
  }

  private void mockSuccessfulTokenResponse(final String accessToken, final int expiresIn) {
    final Map<String, Object> body = new HashMap<>();
    body.put("access_token", accessToken);
    body.put("expires_in", expiresIn);
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(body));
  }
}
