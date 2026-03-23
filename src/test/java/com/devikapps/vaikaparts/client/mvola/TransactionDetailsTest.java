package com.devikapps.vaikaparts.client.mvola;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.wso2.client.api.ApiException;

class TransactionDetailsTest extends MvolaApiTestBase {

  private static final String SAMPLE_TRANSACTION_REF = "641235";

  @Test
  void should_send_http_get_request() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          UUID.randomUUID().toString(),
          format("msisdn;%s", PARTNER_MSISDN),
          CACHE_CONTROL,
          null,
          null,
          null,
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("GET", recorded.getMethod());
  }

  @Test
  void should_embed_transaction_reference_in_request_path() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          UUID.randomUUID().toString(),
          format("msisdn;%s", PARTNER_MSISDN),
          CACHE_CONTROL,
          null,
          null,
          null,
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertNotNull(recorded.getPath());
    assertTrue(
        recorded.getPath().contains(SAMPLE_TRANSACTION_REF),
        format(
            "Expected path to contain transactionReference '%s', but was: %s",
            SAMPLE_TRANSACTION_REF, recorded.getPath()));
  }

  @Test
  void should_not_have_request_body() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          UUID.randomUUID().toString(),
          format("msisdn;%s", PARTNER_MSISDN),
          CACHE_CONTROL,
          null,
          null,
          null,
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(0L, recorded.getBody().size());
  }

  @Test
  void should_send_version_header() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          UUID.randomUUID().toString(),
          format("msisdn;%s", PARTNER_MSISDN),
          CACHE_CONTROL,
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
  void should_send_x_correlation_id_header() throws Exception {
    final String correlationId = UUID.randomUUID().toString();
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          correlationId,
          format("msisdn;%s", PARTNER_MSISDN),
          CACHE_CONTROL,
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
  void should_send_user_account_identifier_header() throws Exception {
    final String identifier = format("msisdn;%s", PARTNER_MSISDN);
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          UUID.randomUUID().toString(),
          identifier,
          CACHE_CONTROL,
          null,
          null,
          null,
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(identifier, recorded.getHeader("UserAccountIdentifier"));
  }

  @Test
  void should_send_cache_control_header() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          UUID.randomUUID().toString(),
          format("msisdn;%s", PARTNER_MSISDN),
          CACHE_CONTROL,
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
  void should_send_cell_id_a_when_provided() throws Exception {
    final String cellId = "CELL-ABC";
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          UUID.randomUUID().toString(),
          format("msisdn;%s", PARTNER_MSISDN),
          CACHE_CONTROL,
          cellId,
          null,
          null,
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(cellId, recorded.getHeader("CellIdA"));
  }

  @Test
  void should_not_send_cell_id_a_when_not_provided() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          UUID.randomUUID().toString(),
          format("msisdn;%s", PARTNER_MSISDN),
          CACHE_CONTROL,
          null,
          null,
          null,
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertNull(recorded.getHeader("CellIdA"));
  }

  @Test
  void should_send_geo_location_a_when_provided() throws Exception {
    final String geoLocation = "47.5079,-18.9143";
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    try {
      defaultApi.transactionReferenceGet(
          SAMPLE_TRANSACTION_REF,
          API_VERSION,
          UUID.randomUUID().toString(),
          format("msisdn;%s", PARTNER_MSISDN),
          CACHE_CONTROL,
          null,
          geoLocation,
          null,
          null);
    } catch (ApiException ignored) {
    }

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(geoLocation, recorded.getHeader("GeoLocationA"));
  }
}
