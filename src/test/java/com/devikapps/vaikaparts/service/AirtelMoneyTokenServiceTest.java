package com.devikapps.vaikaparts.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.config.AirtelMoneyConf;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import dev.razafindratelo.airtel_money_client.api.AuthenticationApi;
import dev.razafindratelo.airtel_money_client.model.TokenRequest;
import dev.razafindratelo.airtel_money_client.model.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

@ExtendWith(MockitoExtension.class)
class AirtelMoneyTokenServiceTest {

  private static final String TEST_CLIENT_ID = randomUUID().toString();
  private static final String TEST_CLIENT_SECRET = randomUUID().toString();
  private static final String TEST_ACCESS_TOKEN = randomUUID().toString();
  private static final String DEFAULT_TOKEN_EXPIRATION = "3600";
  @Mock private AirtelMoneyConf conf;
  @Mock private AuthenticationApi authenticationApi;
  private AirtelMoneyTokenService subject;

  @BeforeEach
  void set_up() {
    when(conf.getClientId()).thenReturn(TEST_CLIENT_ID);
    when(conf.getClientSecret()).thenReturn(TEST_CLIENT_SECRET);
    subject = new AirtelMoneyTokenService(conf, authenticationApi);
  }

  @Test
  void should_return_access_token_from_api_response() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse(TEST_ACCESS_TOKEN, DEFAULT_TOKEN_EXPIRATION));

    assertEquals(TEST_ACCESS_TOKEN, subject.getToken());
  }

  @Test
  void should_send_client_credentials_grant_type_to_api() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse(TEST_ACCESS_TOKEN, DEFAULT_TOKEN_EXPIRATION));

    subject.getToken();

    ArgumentCaptor<TokenRequest> captor = ArgumentCaptor.forClass(TokenRequest.class);
    verify(authenticationApi).getAccessToken(any(), any(), captor.capture());
    assertEquals(TokenRequest.GrantTypeEnum.CLIENT_CREDENTIALS, captor.getValue().getGrantType());
  }

  @Test
  void should_send_client_id_from_properties_to_api() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse(TEST_ACCESS_TOKEN, DEFAULT_TOKEN_EXPIRATION));

    subject.getToken();

    ArgumentCaptor<TokenRequest> captor = ArgumentCaptor.forClass(TokenRequest.class);
    verify(authenticationApi).getAccessToken(any(), any(), captor.capture());
    assertEquals(TEST_CLIENT_ID, captor.getValue().getClientId());
  }

  @Test
  void should_send_client_secret_from_properties_to_api() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse(TEST_ACCESS_TOKEN, DEFAULT_TOKEN_EXPIRATION));

    subject.getToken();

    ArgumentCaptor<TokenRequest> captor = ArgumentCaptor.forClass(TokenRequest.class);
    verify(authenticationApi).getAccessToken(any(), any(), captor.capture());
    assertEquals(TEST_CLIENT_SECRET, captor.getValue().getClientSecret());
  }

  @Test
  void should_return_cached_token_on_second_call_within_expiry() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse("cached-token", DEFAULT_TOKEN_EXPIRATION));

    subject.getToken();
    String secondCall = subject.getToken();

    verify(authenticationApi, times(1)).getAccessToken(any(), any(), any());
    assertEquals("cached-token", secondCall);
  }

  @Test
  void should_fetch_new_token_when_cache_is_expired() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse("first-token", "0"))
        .thenReturn(buildTokenResponse("second-token", DEFAULT_TOKEN_EXPIRATION));

    subject.getToken();
    String secondCall = subject.getToken();

    verify(authenticationApi, times(2)).getAccessToken(any(), any(), any());
    assertEquals("second-token", secondCall);
  }

  @Test
  void should_call_api_only_once_when_token_is_still_valid() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse("valid-token", DEFAULT_TOKEN_EXPIRATION));

    subject.getToken();
    subject.getToken();
    subject.getToken();

    verify(authenticationApi, times(1)).getAccessToken(any(), any(), any());
  }

  @Test
  void should_default_to_3600s_when_expires_in_is_null() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse(TEST_ACCESS_TOKEN, null));

    assertEquals(TEST_ACCESS_TOKEN, subject.getToken());
  }

  @Test
  void should_default_to_3600s_when_expires_in_is_blank() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse(TEST_ACCESS_TOKEN, "  "));

    assertEquals(TEST_ACCESS_TOKEN, subject.getToken());
  }

  @Test
  void should_default_to_3600s_when_expires_in_is_not_a_number() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse(TEST_ACCESS_TOKEN, "not-a-number"));

    assertEquals(TEST_ACCESS_TOKEN, subject.getToken());
  }

  @Test
  void should_accept_expires_in_of_180_seconds() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse("short-lived-token", "180"))
        .thenReturn(buildTokenResponse("second-token", DEFAULT_TOKEN_EXPIRATION));

    subject.getToken();
    // Token expires in 180s with 60s buffer — expires effectively in 120s.
    // Second call within the same second should still use cache.
    assertEquals("short-lived-token", subject.getToken());
  }

  @Test
  void should_throw_payment_gateway_exception_when_access_token_is_blank() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenReturn(buildTokenResponse("  ", DEFAULT_TOKEN_EXPIRATION));

    assertThrows(PaymentGatewayException.class, () -> subject.getToken());
  }

  @Test
  void should_propagate_rest_client_exception_from_api() {
    when(authenticationApi.getAccessToken(any(), any(), any()))
        .thenThrow(
            new RestClientResponseException(
                "Unauthorized", HttpStatus.UNAUTHORIZED.value(), "Unauthorized", null, null, null));

    assertThrows(RestClientResponseException.class, () -> subject.getToken());
  }

  private TokenResponse buildTokenResponse(String accessToken, String expiresIn) {
    TokenResponse response = new TokenResponse();
    response.setAccessToken(accessToken);
    response.setExpiresIn(expiresIn);
    response.setTokenType(TokenResponse.TokenTypeEnum.BEARER);
    return response;
  }
}
