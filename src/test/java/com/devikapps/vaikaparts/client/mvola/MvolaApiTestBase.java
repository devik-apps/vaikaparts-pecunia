package com.devikapps.vaikaparts.client.mvola;

import static java.lang.String.format;

import java.io.IOException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.wso2.client.api.ApiClient;
import org.wso2.client.api.MVOLA_Merchant_Pay_API.DefaultApi;
import org.wso2.client.model.MVOLA_Merchant_Pay_API.PostRequest;
import org.wso2.client.model.MVOLA_Merchant_Pay_API.PostRequestDebitPartyInner;

public abstract class MvolaApiTestBase {

  public static final String API_VERSION = "1.0";
  public static final String CACHE_CONTROL = "no-cache";
  public static final String USER_LANGUAGE = "MG";
  public static final String PARTNER_MSISDN = "0340017983";
  public static final String CUSTOMER_MSISDN = "0341234567";
  public static final String PARTNER_NAME = "TestPartner";
  public static final String DUMMY_ACCESS_TOKEN = "dummyAccessToken.test";
  public static final String MERCHANT_PAY_BASE_PATH =
      "/mvola/mm/transactions/type/merchantpay/1.0.0";

  public MockWebServer mockWebServer;
  public DefaultApi defaultApi;
  public ApiClient apiClient;

  static PostRequest buildValidPostRequest() {
    return new PostRequest()
        .amount("5000")
        .currency("Ar")
        .descriptionText("Test payment")
        .requestDate("2026-03-16T10:00:00.000+0300")
        .requestingOrganisationTransactionReference("REF-001")
        .originalTransactionReference("")
        .addDebitPartyItem(buildParty("msisdn", CUSTOMER_MSISDN))
        .addCreditPartyItem(buildParty("msisdn", PARTNER_MSISDN))
        .addMetadataItem(buildParty("partnerName", PARTNER_NAME))
        .addMetadataItem(buildParty("fc", "USD"))
        .addMetadataItem(buildParty("amountFc", "1"));
  }

  static PostRequestDebitPartyInner buildParty(final String key, final String value) {
    return new PostRequestDebitPartyInner().key(key).value(value);
  }

  @BeforeEach
  void setUpBase() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    final String baseUrl = mockWebServer.url(MERCHANT_PAY_BASE_PATH).toString();

    apiClient = new ApiClient();
    apiClient.setBasePath(baseUrl);
    apiClient.setVerifyingSsl(false);
    apiClient.setAccessToken(DUMMY_ACCESS_TOKEN);
    apiClient.addDefaultHeader("UserLanguage", USER_LANGUAGE);
    apiClient.addDefaultHeader("UserAccountIdentifier", format("msisdn;%s", PARTNER_MSISDN));
    apiClient.addDefaultHeader("partnerName", PARTNER_NAME);

    defaultApi = new DefaultApi(apiClient);
    defaultApi.setCustomBaseUrl(baseUrl);
  }

  @AfterEach
  void tearDownBase() throws IOException {
    mockWebServer.shutdown();
  }
}
