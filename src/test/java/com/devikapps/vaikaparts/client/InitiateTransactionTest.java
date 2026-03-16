package com.devikapps.vaikaparts.client;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.wso2.client.api.ApiException;

class InitiateTransactionTest extends MvolaApiTestBase {

  @Test
  void should_send_http_post_request() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    assertEquals("POST", recorded.getMethod());
  }

  @Test
  void should_send_version_header() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    assertEquals(API_VERSION, recorded.getHeader("Version"));
  }

  @Test
  void should_send_x_correlation_id_header_with_supplied_value() throws Exception {
    final String correlationId = randomUUID().toString();
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          correlationId,
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
    assertEquals(correlationId, recorded.getHeader("X-CorrelationID"));
  }

  @Test
  void should_send_cache_control_header() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    assertEquals(CACHE_CONTROL, recorded.getHeader("Cache-Control"));
  }

  @Test
  void should_send_content_type_application_json() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    assertNotNull(recorded.getHeader("Content-Type"));
    assertTrue(requireNonNull(recorded.getHeader("Content-Type")).contains("application/json"));
  }

  @Test
  void should_forward_user_language_default_header() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    assertEquals(USER_LANGUAGE, recorded.getHeader("UserLanguage"));
  }

  @Test
  void should_forward_user_account_identifier_default_header() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    assertEquals(
        String.format("msisdn;%s", PARTNER_MSISDN), recorded.getHeader("UserAccountIdentifier"));
  }

  @Test
  void should_forward_partner_name_default_header() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    assertEquals(PARTNER_NAME, recorded.getHeader("partnerName"));
  }

  @Test
  void should_send_x_callback_url_when_provided() throws Exception {
    final String callbackUrl = "https://myserver.example.com/callback";
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
          CACHE_CONTROL,
          buildValidPostRequest(),
          null,
          null,
          null,
          null,
          null,
          null,
          callbackUrl);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(callbackUrl, recorded.getHeader("X-Callback-URL"));
  }

  @Test
  void should_not_send_x_callback_url_when_not_provided() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    assertNull(recorded.getHeader("X-Callback-URL"));
  }

  @Test
  void should_send_cell_id_a_when_provided() throws Exception {
    final String cellId = "CELL-001";
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
          CACHE_CONTROL,
          buildValidPostRequest(),
          cellId,
          null,
          null,
          null,
          null,
          null,
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(cellId, recorded.getHeader("CellIdA"));
  }

  @Test
  void should_include_amount_in_request_body() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    final JsonObject body = JsonParser.parseString(recorded.getBody().readUtf8()).getAsJsonObject();
    assertEquals("5000", body.get("amount").getAsString());
  }

  @Test
  void should_include_currency_ar_in_request_body() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    final JsonObject body = JsonParser.parseString(recorded.getBody().readUtf8()).getAsJsonObject();
    assertEquals("Ar", body.get("currency").getAsString());
  }

  @Test
  void should_include_debit_party_array_in_request_body() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    final JsonObject body = JsonParser.parseString(recorded.getBody().readUtf8()).getAsJsonObject();
    assertTrue(body.has("debitParty"));
    assertTrue(body.get("debitParty").isJsonArray());
    assertEquals(1, body.getAsJsonArray("debitParty").size());
  }

  @Test
  void should_include_credit_party_array_in_request_body() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    final JsonObject body = JsonParser.parseString(recorded.getBody().readUtf8()).getAsJsonObject();
    assertTrue(body.has("creditParty"));
    assertTrue(body.get("creditParty").isJsonArray());
  }

  @Test
  void should_include_metadata_array_in_request_body() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(202).setBody(""));

    try {
      defaultApi.rootPost(
          API_VERSION,
          randomUUID().toString(),
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
    final JsonObject body = JsonParser.parseString(recorded.getBody().readUtf8()).getAsJsonObject();
    assertTrue(body.has("metadata"));
    assertTrue(body.get("metadata").isJsonArray());
    assertEquals(3, body.getAsJsonArray("metadata").size());
  }
}
