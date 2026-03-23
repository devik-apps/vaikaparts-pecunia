package com.devikapps.vaikaparts.client.airtel;

import static java.util.UUID.randomUUID;

import dev.razafindratelo.airtel_money_client.invoker.ApiClient;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class AbstractAirtelMoneyTestBase {
  protected static final String DUMMY_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.dummyToken";
  protected static final String CUSTOMER_MSISDN = "330000001";
  protected static final String AIRTEL_MONEY_ID = "C36xxxxxxx67";
  protected static final String PARTNER_TRANSACTION_ID = "test-txn-001";
  protected static final String CLIENT_ID = randomUUID().toString();
  protected static final String CLIENT_SECRET = randomUUID().toString();
  protected static final String X_COUNTRY = "MG";
  protected static final String X_CURRENCY = "MGA";
  protected static final String BEARER_PREFIX = "Bearer ";
  protected static final String JSON_MIME_TYPE = "application/json";
  protected static final String ACCEPT_ALL_MIME_TYPE = "*/*";

  protected MockWebServer mockWebServer;
  protected ApiClient apiClient;

  @BeforeEach
  void set_up_server() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    apiClient = new ApiClient();
    apiClient.setBasePath(mockWebServer.url("/").toString().replaceAll("/$", ""));
  }

  @AfterEach
  void tear_down_server() throws IOException {
    mockWebServer.shutdown();
  }

  protected MockResponse jsonResponse(final int code, final String body) {
    return new MockResponse()
        .setResponseCode(code)
        .setBody(body)
        .addHeader("Content-Type", "application/json");
  }
}
