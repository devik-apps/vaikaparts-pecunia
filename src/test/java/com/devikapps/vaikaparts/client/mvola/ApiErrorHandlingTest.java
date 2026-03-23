package com.devikapps.vaikaparts.client.mvola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.wso2.client.api.ApiException;

class ApiErrorHandlingTest extends MvolaApiTestBase {

  private static final String SAMPLE_CORRELATION_ID = "421a22a2-ef1d-42bc-9452-f4939a3d5cdf";
  private static final String SAMPLE_TRANSACTION_REF = "641235";
  private static final String IDENTIFIER = "msisdn;0340017983";

  @Test
  void should_throw_api_exception_with_code_400_on_bad_request() {
    final String body =
        "{\"ErrorCategory\":\"Validation\","
            + "\"ErrorCode\":\"ERR400\","
            + "\"ErrorDescription\":\"Missing required parameter\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(400, ex.getCode());
  }

  @Test
  void should_throw_api_exception_with_code_401_on_unauthorized() {
    final String body =
        "{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\",\"description\":\"Invalid"
            + " Credentials. Make sure you have given the correct access token\"}}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(401, ex.getCode());
  }

  @Test
  void should_throw_api_exception_with_code_402_on_request_failed() {
    final String body =
        "{\"ErrorCategory\":\"Transaction\","
            + "\"ErrorCode\":\"ERR402\","
            + "\"ErrorDescription\":\"Request Failed\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(402).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(402, ex.getCode());
  }

  @Test
  void should_throw_api_exception_with_code_403_on_forbidden() {
    final String body =
        "{\"ErrorCategory\":\"Authorization\","
            + "\"ErrorCode\":\"ERR403\","
            + "\"ErrorDescription\":\"Forbidden\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(403).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(403, ex.getCode());
  }

  @Test
  void should_throw_api_exception_with_code_404_on_not_found() {
    final String body =
        "{\"ErrorCategory\":\"NotFound\","
            + "\"ErrorCode\":\"ERR404\","
            + "\"ErrorDescription\":\"Resource not found\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(404, ex.getCode());
  }

  @Test
  void should_throw_api_exception_with_code_409_on_conflict() {
    final String body =
        "{\"ErrorCategory\":\"Conflict\","
            + "\"ErrorCode\":\"ERR409\","
            + "\"ErrorDescription\":\"Duplicate idempotent key\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(409).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(409, ex.getCode());
  }

  @Test
  void should_throw_api_exception_with_code_429_on_too_many_requests() {
    final String body =
        "{\"ErrorCategory\":\"Throttle\","
            + "\"ErrorCode\":\"ERR429\","
            + "\"ErrorDescription\":\"Too Many Requests\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(429).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(429, ex.getCode());
  }

  @Test
  void should_throw_api_exception_with_code_500_on_internal_server_error() {
    final String body =
        "{\"ErrorCategory\":\"Server\","
            + "\"ErrorCode\":\"ERR500\","
            + "\"ErrorDescription\":\"Internal Server Error\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(500, ex.getCode());
  }

  @Test
  void should_throw_api_exception_with_code_502_on_bad_gateway() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(502).setBody("Bad Gateway"));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(502, ex.getCode());
  }

  @Test
  void should_throw_api_exception_with_code_503_on_service_unavailable() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(503, ex.getCode());
  }

  @Test
  void should_preserve_response_body_in_api_exception() {
    final String errorBody = "{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(errorBody));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(errorBody, ex.getResponseBody());
  }

  @Test
  void should_preserve_response_headers_in_api_exception() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(401)
            .addHeader("X-Request-ID", "req-abc-123")
            .setBody("Unauthorized"));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertNotNull(ex.getResponseHeaders());
  }

  @Test
  void should_include_code_and_body_in_exception_message() {
    final String errorBody = "{\"ErrorDescription\":\"Missing required parameter\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody(errorBody));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    final String message = ex.getMessage();
    assertNotNull(message);
    assertEquals(400, ex.getCode());
    assertEquals(errorBody, ex.getResponseBody());
  }

  @Test
  void status_get_should_throw_api_exception_with_code_401_on_unauthorized() {
    final String body = "{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.statusServerCorrelationIdGet(
                    SAMPLE_CORRELATION_ID,
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    PARTNER_NAME,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(401, ex.getCode());
  }

  @Test
  void status_get_should_throw_api_exception_with_code_404_on_not_found() {
    final String body =
        "{\"ErrorCategory\":\"NotFound\","
            + "\"ErrorCode\":\"ERR404\","
            + "\"ErrorDescription\":\"serverCorrelationId not found\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.statusServerCorrelationIdGet(
                    SAMPLE_CORRELATION_ID,
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    PARTNER_NAME,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(404, ex.getCode());
  }

  @Test
  void transaction_get_should_throw_api_exception_with_code_401_on_unauthorized() {
    final String body = "{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.transactionReferenceGet(
                    SAMPLE_TRANSACTION_REF,
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(401, ex.getCode());
  }

  @Test
  void transaction_get_should_throw_api_exception_with_code_404_on_not_found() {
    final String body =
        "{\"ErrorCategory\":\"NotFound\","
            + "\"ErrorCode\":\"ERR404\","
            + "\"ErrorDescription\":\"Transaction not found\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody(body));

    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.transactionReferenceGet(
                    SAMPLE_TRANSACTION_REF,
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(404, ex.getCode());
  }
}
