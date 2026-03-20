package com.devikapps.vaikaparts.gateway.mvola;

import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.config.MvolaConf;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.gateway.AbstractPaymentGateway;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.service.MvolaTokenService;
import com.devikapps.vaikaparts.service.util.MvolaResponseParser;
import com.devikapps.vaikaparts.validator.PaymentRequestValidator;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

  private final MvolaConf properties;
  private final MvolaTokenService tokenService;
  private final MvolaResponseParser responseParser;
  private DefaultApi defaultApi;

  public MvolaPaymentGateway(
      final PaymentRequestValidator validator,
      final MvolaConf properties,
      final MvolaTokenService tokenService,
      final MvolaResponseParser responseParser) {
    super(validator);
    this.properties = properties;
    this.tokenService = tokenService;
    this.responseParser = responseParser;
  }

  @PostConstruct
  public void init() {
    defaultApi = buildDefaultApi();
  }

  @Override
  public PaymentProvider getProvider() {
    return MVOLA;
  }

  @Override
  protected PaymentResponse doInitiatePayment(final PaymentRequest request) {
    final MvolaPaymentRequest mvolaRequest = (MvolaPaymentRequest) request;
    log.info(
        "Initiate MVOLA payment. Payer={}, Description={}",
        mvolaRequest.getPayer().getPhoneNumber(),
        mvolaRequest.getDescription());
    configureApiClientToken();

    try {
      final okhttp3.Call call =
          defaultApi.rootPostCall(
              API_VERSION,
              resolveCorrelationId(mvolaRequest),
              CACHE_CONTROL,
              buildPostRequest(mvolaRequest),
              null,
              null,
              null,
              null,
              null,
              null,
              resolveCallbackUrl(mvolaRequest),
              null);

      final String rawBody = executeAndReadBody(call);
      final MvolaPaymentResponse response = responseParser.initiatePaymentParser.apply(rawBody);

      response.setTransactionId(request.getTransactionId());
      response.setAmount(request.getAmount());
      response.setCurrency(request.getCurrency());
      response.setProvider(MVOLA);

      return response;

    } catch (ApiException e) {
      log.error(
          "MVola initiatePayment failed: HTTP {}, body: {}",
          e.getCode(),
          forJava(e.getResponseBody()),
          e);
      throw new PaymentGatewayException(
          format(
              "MVola initiatePayment failed: %s. ErrorCode=%d", e.getResponseBody(), e.getCode()));
    } catch (IOException e) {
      log.error("MVola initiatePayment I/O error.", e);
      throw new PaymentGatewayException("MVola initiatePayment I/O error.");
    }
  }

  @Override
  protected PaymentResponse doGetPaymentStatus(final String serverCorrelationId) {
    configureApiClientToken();

    try {
      final okhttp3.Call call =
          defaultApi.statusServerCorrelationIdGetCall(
              serverCorrelationId,
              API_VERSION,
              randomUUID().toString(),
              null,
              null,
              CACHE_CONTROL,
              null,
              null,
              null,
              null,
              null);

      final String rawBody = executeAndReadBody(call);
      final MvolaPaymentResponse response = responseParser.paymentStatusParser.apply(rawBody);

      response.setProvider(MVOLA);
      return response;

    } catch (ApiException e) {
      log.error(
          "MVola getPaymentStatus failed: HTTP {}, body: {}",
          e.getCode(),
          forJava(e.getResponseBody()),
          e);
      throw new PaymentGatewayException(
          format("MVola getPaymentStatus failed: %s", e.getResponseBody()));
    } catch (IOException e) {
      log.error("MVola getPaymentStatus I/O error.", e);
      throw new PaymentGatewayException("MVola getPaymentStatus I/O error.");
    }
  }

  @Override
  protected PaymentResponse doGetPaymentDetails(final String transactionReference) {
    log.info(
        "MVOLA: Get payment status detail of payment of transactionId={}", transactionReference);
    configureApiClientToken();

    try {
      final okhttp3.Call call =
          defaultApi.transactionReferenceGetCall(
              transactionReference,
              API_VERSION,
              randomUUID().toString(),
              null,
              CACHE_CONTROL,
              null,
              null,
              "application/json",
              "UTF-8",
              null);

      final String rawBody = executeAndReadBody(call);
      final MvolaPaymentResponse response = responseParser.paymentDetailsParser.apply(rawBody);

      response.setProvider(MVOLA);
      return response;

    } catch (ApiException e) {
      log.error(
          "MVola getPaymentDetails failed: HTTP {}, body: {}",
          e.getCode(),
          forJava(e.getResponseBody()),
          e);
      throw new PaymentGatewayException(
          format(
              "MVola getPaymentDetails failed: %s. ErrCode=%s", e.getResponseBody(), e.getCode()));
    } catch (IOException e) {
      log.error("MVola getPaymentDetails I/O error.", e);
      throw new PaymentGatewayException("MVola getPaymentDetails I/O error.");
    }
  }

  private String executeAndReadBody(final okhttp3.Call call) throws IOException, ApiException {
    try (final okhttp3.Response response = call.execute()) {
      final String rawBody = response.body().string();

      if (!response.isSuccessful())
        throw new ApiException(
            response.message(), response.code(), response.headers().toMultimap(), rawBody);

      return rawBody;
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
        .requestDate(
            OffsetDateTime.now(ZoneOffset.UTC).format(ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")) + "Z")
        .requestingOrganisationTransactionReference(request.getTransactionId())
        .originalTransactionReference(request.getTransactionId())
        .addDebitPartyItem(
            buildParty("msisdn", normalizeMsisdn(request.getPayer().getPhoneNumber())))
        .addCreditPartyItem(
            buildParty("msisdn", normalizeMsisdn(request.getPayee().getPhoneNumber())))
        .addMetadataItem(buildParty("partnerName", properties.getPartnerName()))
        .addMetadataItem(buildParty("fc", "USD"))
        .addMetadataItem(buildParty("amountFc", "1"));
  }

  private PostRequestDebitPartyInner buildParty(final String key, final String value) {
    return new PostRequestDebitPartyInner().key(key).value(value);
  }

  private String normalizeMsisdn(final String phoneNumber) {
    if (phoneNumber == null) return null;

    if (phoneNumber.startsWith("+261")) return format("0%s", phoneNumber.substring(4));

    if (phoneNumber.startsWith("261")) return format("0%s", phoneNumber.substring(3));

    return phoneNumber;
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
    final var client = new ApiClient();

    client.setBasePath(properties.getBaseUrl());
    client.setVerifyingSsl(true);
    client.addDefaultHeader("UserLanguage", "FR");
    client.addDefaultHeader(
        "UserAccountIdentifier", format("msisdn;%s", properties.getPartnerMsisdn()));
    client.addDefaultHeader("partnerName", properties.getPartnerName());

    final var api = new DefaultApi(client);
    api.setCustomBaseUrl(properties.getBaseUrl());
    return api;
  }
}
