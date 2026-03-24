package com.devikapps.vaikaparts.gateway.airtelmoney;

import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.AIRTEL_MONEY;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.config.AirtelMoneyConf;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.gateway.PaymentGateway;
import com.devikapps.vaikaparts.mapper.AirtelMoneyTransactionStatusAdapter;
import com.devikapps.vaikaparts.model.AirtelMoneyPaymentRequest;
import com.devikapps.vaikaparts.model.AirtelMoneyPaymentResponse;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.service.AirtelMoneyTokenService;
import com.devikapps.vaikaparts.validator.PaymentRequestValidator;
import dev.razafindratelo.airtel_money_client.api.CollectionApi;
import dev.razafindratelo.airtel_money_client.api.TransactionApi;
import dev.razafindratelo.airtel_money_client.model.PaymentInitiationResponse;
import dev.razafindratelo.airtel_money_client.model.PaymentSubscriber;
import dev.razafindratelo.airtel_money_client.model.TransactionEnquiryResponse;
import dev.razafindratelo.airtel_money_client.model.TransactionEnquiryTransactionData;
import dev.razafindratelo.airtel_money_client.model.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AirtelMoneyPaymentGateway implements PaymentGateway {

  private static final String CONTENT_TYPE = "application/json";
  private static final String ACCEPT = "*/*";
  private static final String BEARER_PREFIX = "Bearer ";

  private final PaymentRequestValidator validator;
  private final AirtelMoneyConf conf;
  private final AirtelMoneyTokenService tokenService;
  private final AirtelMoneyTransactionStatusAdapter statusAdapter;
  private final CollectionApi collectionApi;
  private final TransactionApi transactionApi;

  @Override
  public PaymentProvider getProvider() {
    return AIRTEL_MONEY;
  }

  @Override
  public PaymentResponse initiatePayment(PaymentRequest request) {
    validator.validate(request);

    log.info(
        "Initiating Airtel Money payment. Payer={}, Amount={}",
        forJava(request.getPayer().getPhoneNumber()),
        request.getAmount());

    AirtelMoneyPaymentRequest airtelRequest = (AirtelMoneyPaymentRequest) request;
    String token = bearerToken();

    PaymentSubscriber subscriber = new PaymentSubscriber();
    subscriber.setMsisdn(normalizeMsisdn(request.getPayer().getPhoneNumber()));
    subscriber.setCountry(conf.getCountry());
    subscriber.setCurrency(conf.getCurrency());

    dev.razafindratelo.airtel_money_client.model.PaymentRequest apiRequest =
        getPaymentRequest(request, airtelRequest, subscriber);

    try {
      PaymentInitiationResponse response =
          collectionApi.initiatePayment(
              ACCEPT, CONTENT_TYPE, conf.getCountry(), conf.getCurrency(), token, apiRequest);

      log.info(
          "Airtel Money payment initiated. TransactionId={}, ApiStatus={}",
          forJava(request.getTransactionId()),
          response.getData().getTransaction() != null
              ? response.getData().getTransaction().getStatus()
              : "unknown");

      return buildPaymentResponse(request.getTransactionId(), PaymentStatus.PENDING);

    } catch (RestClientResponseException e) {
      log.error(
          "Airtel Money initiatePayment failed. TransactionId={}, HttpStatus={}",
          forJava(request.getTransactionId()),
          e.getStatusCode().value(),
          e);
      throw new PaymentGatewayException(
          format(
              "Airtel Money initiatePayment failed with HTTP %d. Error= %s",
              e.getStatusCode().value(), e));
    }
  }

  private dev.razafindratelo.airtel_money_client.model.@NonNull PaymentRequest getPaymentRequest(
      PaymentRequest request,
      AirtelMoneyPaymentRequest airtelRequest,
      PaymentSubscriber subscriber) {
    dev.razafindratelo.airtel_money_client.model.PaymentTransaction transaction =
        new dev.razafindratelo.airtel_money_client.model.PaymentTransaction();
    transaction.setId(request.getTransactionId());
    transaction.setAmount(request.getAmount());
    transaction.setCountry(conf.getCountry());
    transaction.setCurrency(conf.getCurrency());

    dev.razafindratelo.airtel_money_client.model.PaymentRequest apiRequest =
        new dev.razafindratelo.airtel_money_client.model.PaymentRequest();
    apiRequest.setReference(airtelRequest.getReference());
    apiRequest.setSubscriber(subscriber);
    apiRequest.setTransaction(transaction);
    return apiRequest;
  }

  @Override
  public PaymentResponse getPaymentStatus(String transactionId) {
    if (transactionId == null || transactionId.isBlank()) {
      throw new PaymentValidationException("transactionId must not be blank for getPaymentStatus");
    }

    log.info("Querying Airtel Money transaction status. TransactionId={}", forJava(transactionId));

    String token = bearerToken();

    try {
      TransactionEnquiryResponse response =
          transactionApi.getTransactionStatus(
              transactionId, ACCEPT, conf.getCountry(), conf.getCurrency(), token);

      TransactionEnquiryTransactionData txData = response.getData().getTransaction();

      assert txData != null;
      TransactionStatus airtelStatus = txData.getStatus();
      PaymentStatus paymentStatus = statusAdapter.toPaymentStatus(airtelStatus);

      log.info(
          "Airtel Money transaction status retrieved. TransactionId={}, AirtelStatus={},"
              + " MappedStatus={}",
          forJava(transactionId),
          airtelStatus,
          paymentStatus);

      AirtelMoneyPaymentResponse paymentResponse =
          (AirtelMoneyPaymentResponse) buildPaymentResponse(transactionId, paymentStatus);
      paymentResponse.setAirtelMoneyId(txData.getAirtelMoneyId());
      paymentResponse.setMessage(txData.getMessage());
      return paymentResponse;

    } catch (RestClientResponseException e) {
      log.error(
          "Airtel Money getPaymentStatus failed. TransactionId={}, HttpStatus={}",
          forJava(transactionId),
          e.getStatusCode().value(),
          e);
      throw new PaymentGatewayException(
          format(
              "Airtel Money getPaymentStatus failed with HTTP %d. Error= %s",
              e.getStatusCode().value(), e));
    }
  }

  @Override
  public PaymentResponse getPaymentDetails(String transactionId) {
    // Airtel Money does not provide a separate payment details endpoint beyond
    // the transaction status enquiry. Delegate to getPaymentStatus.
    log.info(
        "Airtel Money getPaymentDetails delegating to getPaymentStatus. TransactionId={}",
        forJava(transactionId));
    return getPaymentStatus(transactionId);
  }

  /**
   * Strips the Madagascar country code prefix from an MSISDN if present. Airtel Money requires the
   * local number without any country code prefix. Examples: +261331234567 → 331234567 261331234567
   * → 331234567 0331234567 → 331234567 331234567 → 331234567 (unchanged)
   */
  private String normalizeMsisdn(String msisdn) {
    if (msisdn == null) return null;
    String normalized = msisdn.trim();
    if (normalized.startsWith("+261")) normalized = normalized.substring(4);
    else if (normalized.startsWith("261")) normalized = normalized.substring(3);
    else if (normalized.startsWith("0")) normalized = normalized.substring(1);
    return normalized;
  }

  private String bearerToken() {
    return format("%s%s", BEARER_PREFIX, tokenService.getToken());
  }

  private PaymentResponse buildPaymentResponse(String transactionId, PaymentStatus status) {
    return AirtelMoneyPaymentResponse.builder()
        .transactionId(transactionId)
        .status(status)
        .provider(AIRTEL_MONEY)
        .respondedAt(now())
        .build();
  }
}
