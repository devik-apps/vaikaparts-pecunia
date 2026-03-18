package com.devikapps.vaikaparts.gateway.mvola;

import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.gateway.AbstractPaymentGateway;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.service.MvolaTokenService;
import com.devikapps.vaikaparts.validator.PaymentRequestValidator;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.wso2.client.api.ApiClient;
import org.wso2.client.api.ApiException;
import org.wso2.client.api.MVOLA_Merchant_Pay_API.DefaultApi;
import org.wso2.client.model.MVOLA_Merchant_Pay_API.PostRequest;
import org.wso2.client.model.MVOLA_Merchant_Pay_API.PostRequestDebitPartyInner;

@Slf4j
@Service
public class MvolaPaymentGateway extends AbstractPaymentGateway {

  private static final String API_VERSION = "1.0";
  private static final String CACHE_CONTROL = "no-cache";

  private final MvolaProperties properties;
  private final MvolaTokenService tokenService;
  private final DefaultApi defaultApi;

  public MvolaPaymentGateway(
      PaymentRequestValidator validator,
      MvolaProperties properties,
      MvolaTokenService tokenService) {
    super(validator);
    this.properties = properties;
    this.tokenService = tokenService;
    this.defaultApi = buildDefaultApi();
  }

  @Override
  public PaymentProvider getProvider() {
    return MVOLA;
  }

  @Override
  protected PaymentResponse doInitiatePayment(PaymentRequest request) {
    var mvolaRequest = (MvolaPaymentRequest) request;

    configureApiClientToken();

    PostRequest postRequest = buildPostRequest(mvolaRequest);
    String correlationId = resolveCorrelationId(mvolaRequest);
    String callbackUrl = resolveCallbackUrl(mvolaRequest);

    try {
      defaultApi.rootPost(
          API_VERSION,
          correlationId,
          CACHE_CONTROL,
          postRequest,
          null,
          null,
          null,
          null,
          null,
          null,
          callbackUrl);

      return MvolaPaymentResponse.builder()
          .transactionId(request.getTransactionId())
          .status(PaymentStatus.PENDING)
          .amount(request.getAmount())
          .currency(request.getCurrency())
          .provider(MVOLA)
          .respondedAt(now())
          .build();

    } catch (ApiException e) {
      log.error(
          "MVola initiatePayment failed: HTTP {}, body: {}",
          e.getCode(),
          forJava(e.getResponseBody()),
          e);
      throw new PaymentGatewayException(
          format(
              "MVola initiatePayment failed: %s, errorCode=%s", e.getResponseBody(), e.getCode()));
    }
  }

  @Override
  protected PaymentResponse doGetPaymentStatus(String serverCorrelationId) {
    configureApiClientToken();

    try {
      defaultApi.statusServerCorrelationIdGet(
          serverCorrelationId,
          API_VERSION,
          randomUUID().toString(),
          format("msisdn;%s", properties.getPartnerMsisdn()),
          properties.getPartnerName(),
          CACHE_CONTROL,
          null,
          null,
          null,
          null);

      return MvolaPaymentResponse.builder()
          .serverCorrelationId(serverCorrelationId)
          .status(PaymentStatus.PENDING)
          .provider(MVOLA)
          .respondedAt(now())
          .build();

    } catch (ApiException e) {
      log.error(
          "MVola getPaymentStatus failed: HTTP {}, body: {}",
          e.getCode(),
          forJava(e.getResponseBody()),
          e);
      throw new PaymentGatewayException(
          format(
              "MVola getPaymentStatus failed: %s, errorCode=%s", e.getResponseBody(), e.getCode()));
    }
  }

  @Override
  protected PaymentResponse doGetPaymentDetails(final String transactionReference) {
    configureApiClientToken();

    try {
      defaultApi.transactionReferenceGet(
          transactionReference,
          API_VERSION,
          randomUUID().toString(),
          format("msisdn;%s", properties.getPartnerMsisdn()),
          CACHE_CONTROL,
          null,
          null,
          null,
          null);

      return MvolaPaymentResponse.builder()
          .transactionId(transactionReference)
          .status(PaymentStatus.PENDING)
          .provider(MVOLA)
          .respondedAt(now())
          .build();

    } catch (ApiException e) {
      log.error(
          "MVola getPaymentDetails failed: HTTP {}, body: {}",
          e.getCode(),
          forJava(e.getResponseBody()),
          e);
      throw new PaymentGatewayException(
          format(
              "MVola getPaymentDetails failed: %s, errorCode=%s",
              e.getResponseBody(), e.getCode()));
    }
  }

  private void configureApiClientToken() {
    defaultApi.getApiClient().setAccessToken(tokenService.getToken());
  }

  private PostRequest buildPostRequest(final MvolaPaymentRequest request) {
    return new PostRequest()
        .amount(request.getAmount().toBigInteger().toString())
        .currency("Ar")
        .descriptionText(request.getDescription())
        .requestDate(now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")))
        .requestingOrganisationTransactionReference(request.getTransactionId())
        .originalTransactionReference("")
        .addDebitPartyItem(buildParty("msisdn", request.getPayer().getPhoneNumber()))
        .addCreditPartyItem(buildParty("msisdn", request.getPayee().getPhoneNumber()))
        .addMetadataItem(buildParty("partnerName", properties.getPartnerName()))
        .addMetadataItem(buildParty("fc", "USD"))
        .addMetadataItem(buildParty("amountFc", "1"));
  }

  private PostRequestDebitPartyInner buildParty(final String key, final String value) {
    return new PostRequestDebitPartyInner().key(key).value(value);
  }

  private String resolveCorrelationId(final MvolaPaymentRequest request) {
    return request.getCorrelationId() != null
        ? request.getCorrelationId()
        : randomUUID().toString();
  }

  private String resolveCallbackUrl(final MvolaPaymentRequest request) {
    return request.getCallbackUrl() != null
        ? request.getCallbackUrl()
        : properties.getCallbackUrl();
  }

  private DefaultApi buildDefaultApi() {
    var client = new ApiClient();
    client.setBasePath(properties.getBaseUrl());
    client.setVerifyingSsl(true);
    client.addDefaultHeader("UserLanguage", "FR");
    client.addDefaultHeader(
        "UserAccountIdentifier", format("msisdn;%s", properties.getPartnerMsisdn()));
    client.addDefaultHeader("partnerName", properties.getPartnerName());

    DefaultApi api = new DefaultApi(client);
    api.setCustomBaseUrl(properties.getBaseUrl());

    return api;
  }
}
