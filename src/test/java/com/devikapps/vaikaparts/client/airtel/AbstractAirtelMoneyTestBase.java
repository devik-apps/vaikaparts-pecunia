package com.devikapps.vaikaparts.client.airtel;

import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.MGA;
import static java.util.UUID.randomUUID;

import dev.razafindratelo.airtel_money_client.invoker.ApiClient;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class AbstractAirtelMoneyTestBase {
  public static final String DUMMY_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.dummyToken";
  public static final String CUSTOMER_MSISDN = "330000001";
  public static final String AIRTEL_MONEY_ID = "C36xxxxxxx67";
  public static final String PARTNER_TRANSACTION_ID = "test-txn-001";
  public static final String CLIENT_ID = randomUUID().toString();
  public static final String CLIENT_SECRET = randomUUID().toString();
  public static final String X_COUNTRY = "MG";
  public static final String X_CURRENCY = MGA.toString();
  public static final String BEARER_PREFIX = "Bearer ";
  public static final String JSON_MIME_TYPE = "application/json";
  public static final String ACCEPT_ALL_MIME_TYPE = "*/*";
  public static final String TEST_MSISDN = "331233567";
  public static final String SAMPLE_MSISDN = "330000001";
  public static final String SUCCESS_MESSAGE = "success";

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

  public MockResponse jsonResponse(final int code, final String body) {
    return new MockResponse()
        .setResponseCode(code)
        .setBody(body)
        .addHeader("Content-Type", "application/json");
  }
}
