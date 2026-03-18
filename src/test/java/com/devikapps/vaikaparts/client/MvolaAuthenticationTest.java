package com.devikapps.vaikaparts.client;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.wso2.client.api.ApiClient;
import org.wso2.client.api.ApiException;
import org.wso2.client.api.MVOLA_Merchant_Pay_API.DefaultApi;

class MvolaAuthenticationTest extends MvolaApiTestBase {

  @Test
  void should_produce_valid_basic_auth_header_from_consumer_key_and_secret() {
    final var consumerKey = "myConsumerKey";
    final var consumerSecret = "myConsumerSecret";
    final var rawCredentials = format("%s:%s", consumerKey, consumerSecret);

    final var expected =
        format("Basic %s", Base64.getEncoder().encodeToString(rawCredentials.getBytes()));

    final String actual = okhttp3.Credentials.basic(consumerKey, consumerSecret);

    assertEquals(expected, actual);
  }

  @Test
  void should_send_bearer_token_in_authorization_header() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
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
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(format("Bearer %s", DUMMY_ACCESS_TOKEN), recorded.getHeader("Authorization"));
  }

  @Test
  void should_reflect_updated_access_token_on_next_request() throws Exception {
    final var updatedToken = "updatedToken.v2";

    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
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
          null);
    } catch (ApiException ignored) {
    }

    apiClient.setAccessToken(updatedToken);

    try {
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
          null);
    } catch (ApiException ignored) {
    }

    mockWebServer.takeRequest(); // discard first request
    final RecordedRequest secondRequest = mockWebServer.takeRequest();

    assertEquals(format("Bearer %s", updatedToken), secondRequest.getHeader("Authorization"));
  }

  @Test
  void should_not_send_bearer_token_when_access_token_is_null() throws Exception {
    final String baseUrl = mockWebServer.url(MERCHANT_PAY_BASE_PATH).toString();
    final ApiClient clientWithoutToken = new ApiClient();
    clientWithoutToken.setBasePath(baseUrl);
    clientWithoutToken.setVerifyingSsl(false);

    final DefaultApi apiWithoutToken = new DefaultApi(clientWithoutToken);
    apiWithoutToken.setCustomBaseUrl(baseUrl);

    mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(""));

    try {
      apiWithoutToken.rootPost(
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
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String authHeader = recorded.getHeader("Authorization");

    // OAuth.applyToParams only sets the header when accessToken != null
    assertTrue(
        authHeader == null || !authHeader.startsWith("Bearer "),
        "Authorization header must not contain a Bearer token when none is set");
  }
}
