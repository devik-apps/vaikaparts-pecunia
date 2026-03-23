package com.devikapps.vaikaparts.client.mvola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.wso2.client.api.ApiException;

class RequiredParameterValidationTest extends MvolaApiTestBase {

  private static final String IDENTIFIER = String.format("msisdn;%s", PARTNER_MSISDN);

  @Test
  void root_post_should_throw_when_version_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    null,
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

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains("Missing the required parameter 'version' when calling rootPost(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void root_post_should_throw_when_x_correlation_id_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    null,
                    CACHE_CONTROL,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'xCorrelationID' when calling rootPost(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void root_post_should_throw_when_cache_control_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    null,
                    buildValidPostRequest(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains("Missing the required parameter 'cacheControl' when calling rootPost(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void root_post_should_throw_when_post_request_body_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.rootPost(
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains("Missing the required parameter 'postRequest' when calling rootPost(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void status_get_should_throw_when_server_correlation_id_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.statusServerCorrelationIdGet(
                    null,
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    PARTNER_NAME,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'serverCorrelationId' when calling"
                    + " statusServerCorrelationIdGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void status_get_should_throw_when_version_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.statusServerCorrelationIdGet(
                    "some-correlation-id",
                    null,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    PARTNER_NAME,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'version' when calling"
                    + " statusServerCorrelationIdGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void status_get_should_throw_when_x_correlation_id_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.statusServerCorrelationIdGet(
                    "some-correlation-id",
                    API_VERSION,
                    null,
                    IDENTIFIER,
                    PARTNER_NAME,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'xCorrelationID' when calling"
                    + " statusServerCorrelationIdGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void status_get_should_throw_when_user_account_identifier_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.statusServerCorrelationIdGet(
                    "some-correlation-id",
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    null,
                    PARTNER_NAME,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'userAccountIdentifier' when calling"
                    + " statusServerCorrelationIdGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void status_get_should_throw_when_partner_name_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.statusServerCorrelationIdGet(
                    "some-correlation-id",
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    null,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'partnerName' when calling"
                    + " statusServerCorrelationIdGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void status_get_should_throw_when_cache_control_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.statusServerCorrelationIdGet(
                    "some-correlation-id",
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    PARTNER_NAME,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'cacheControl' when calling"
                    + " statusServerCorrelationIdGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void transaction_get_should_throw_when_transaction_reference_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.transactionReferenceGet(
                    null,
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'transactionReference' when calling"
                    + " transactionReferenceGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void transaction_get_should_throw_when_version_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.transactionReferenceGet(
                    "641235",
                    null,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'version' when calling"
                    + " transactionReferenceGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void transaction_get_should_throw_when_x_correlation_id_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.transactionReferenceGet(
                    "641235",
                    API_VERSION,
                    null,
                    IDENTIFIER,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'xCorrelationID' when calling"
                    + " transactionReferenceGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void transaction_get_should_throw_when_user_account_identifier_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.transactionReferenceGet(
                    "641235",
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    null,
                    CACHE_CONTROL,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'userAccountIdentifier' when calling"
                    + " transactionReferenceGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }

  @Test
  void transaction_get_should_throw_when_cache_control_is_null() {
    final ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                defaultApi.transactionReferenceGet(
                    "641235",
                    API_VERSION,
                    UUID.randomUUID().toString(),
                    IDENTIFIER,
                    null,
                    null,
                    null,
                    null,
                    null));

    assertEquals(0, ex.getCode());
    assertTrue(
        ex.getMessage()
            .contains(
                "Missing the required parameter 'cacheControl' when calling"
                    + " transactionReferenceGet(Async)"),
        String.format("Unexpected message: %s", ex.getMessage()));
  }
}
