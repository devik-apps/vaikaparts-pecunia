package com.devikapps.vaikaparts.client.airtel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.razafindratelo.airtel_money_client.api.AuthenticationApi;
import dev.razafindratelo.airtel_money_client.model.TokenRequest;
import dev.razafindratelo.airtel_money_client.model.TokenResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

public class AirtelMoneyAuthenticationApiTest extends AbstractAirtelMoneyTestBase {

  private static final String DEFAULT_TOKEN_EXPIRATION = "3600";
  private static final String AUTH_REQ =
      """
      {
          "access_token": "%s",
          "expires_in": "%S",
          "token_type": "bearer"
        }
      """;
  private AuthenticationApi subject;

  @BeforeEach
  void set_up() {
    subject = new AuthenticationApi(apiClient);
  }

  @Test
  void should_return_access_token_on_successful_response() {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBodyWithDefaultTokenExpiration()));

    final TokenResponse response =
        subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest());

    assertEquals(DUMMY_ACCESS_TOKEN, response.getAccessToken());
  }

  @Test
  void should_return_expires_in_as_string_on_successful_response() {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBodyWithDefaultTokenExpiration()));

    final TokenResponse response =
        subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest());

    assertEquals("3600", response.getExpiresIn());
  }

  @Test
  void should_return_token_type_bearer_on_successful_response() {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBodyWithDefaultTokenExpiration()));

    final TokenResponse response =
        subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest());

    assertEquals(TokenResponse.TokenTypeEnum.BEARER, response.getTokenType());
  }

  @Test
  void should_return_short_lived_token_when_expires_in_is_180() {
    var autReqTimeout = "180";

    mockWebServer.enqueue(jsonResponse(200, AUTH_REQ.formatted(DUMMY_ACCESS_TOKEN, autReqTimeout)));

    final TokenResponse response =
        subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest());

    assertEquals(autReqTimeout, response.getExpiresIn());
  }

  @Test
  void should_send_post_request_to_correct_path() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBodyWithDefaultTokenExpiration()));

    subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("POST", recorded.getMethod());
    assertEquals("/auth/oauth2/token", recorded.getPath());
  }

  @Test
  void should_send_content_type_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBodyWithDefaultTokenExpiration()));

    subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(JSON_MIME_TYPE, recorded.getHeader("Content-Type"));
  }

  @Test
  void should_send_client_id_in_request_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBodyWithDefaultTokenExpiration()));

    subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(CLIENT_ID));
  }

  @Test
  void should_send_grant_type_client_credentials_in_request_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildSuccessBodyWithDefaultTokenExpiration()));

    subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains("client_credentials"));
  }

  @Test
  void should_throw_on_401_unauthorized() {
    mockWebServer.enqueue(jsonResponse(401, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest()));
  }

  @Test
  void should_throw_on_400_bad_request() {
    mockWebServer.enqueue(jsonResponse(400, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest()));
  }

  @Test
  void should_throw_on_500_internal_server_error() {
    mockWebServer.enqueue(jsonResponse(500, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () ->
            subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest()));
  }

  @Test
  void should_throw_when_content_type_parameter_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () -> subject.getAccessToken(null, ACCEPT_ALL_MIME_TYPE, buildValidTokenRequest()));
  }

  @Test
  void should_throw_when_token_request_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () -> subject.getAccessToken(JSON_MIME_TYPE, ACCEPT_ALL_MIME_TYPE, null));
  }

  private TokenRequest buildValidTokenRequest() {
    final TokenRequest request = new TokenRequest();
    request.setClientId(CLIENT_ID);
    request.setClientSecret(CLIENT_SECRET);
    request.setGrantType(TokenRequest.GrantTypeEnum.CLIENT_CREDENTIALS);
    return request;
  }

  private String buildSuccessBodyWithDefaultTokenExpiration() {
    return AUTH_REQ.formatted(DUMMY_ACCESS_TOKEN, DEFAULT_TOKEN_EXPIRATION);
  }
}
