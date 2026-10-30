package com.devikapps.vaikaparts.gateway.papi;

import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.MGA;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.PAPI;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.client.papi.PapiClient;
import com.devikapps.vaikaparts.client.papi.model.PapiInitiatePaymentRequest;
import com.devikapps.vaikaparts.client.papi.model.PapiPaymentData;
import com.devikapps.vaikaparts.config.PapiConf;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.gateway.AbstractPaymentGateway;
import com.devikapps.vaikaparts.model.PapiPaymentRequest;
import com.devikapps.vaikaparts.model.PapiPaymentResponse;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.validator.PaymentRequestValidator;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PapiPaymentGateway extends AbstractPaymentGateway {
  private final PapiClient client;
  private final PapiConf conf;

  public PapiPaymentGateway(
      PaymentRequestValidator validator, PapiClient client, PapiConf conf) {
    super(validator);
    this.client = client;
    this.conf = conf;
  }

  @Override
  public PaymentProvider getProvider() {
    return PAPI;
  }

  @Override
  protected PaymentResponse doInitiatePayment(PaymentRequest paymentRequest) {
    PapiPaymentRequest request = requirePapiRequest(paymentRequest);
    validatePapiFields(request);

    PapiInitiatePaymentRequest apiRequest =
        new PapiInitiatePaymentRequest(
            request.getPaymentMethod().name(),
            request.getAmount().setScale(0),
            MGA.name(),
            request.getReference(),
            request.getDescription(),
            blankToNull(conf.getNotificationUrl()),
            request.getPayer().getName(),
            request.getPayerEmail(),
            request.getPayer().getPhoneNumber(),
            request.getPaymentMethod() == PapiPaymentMethod.ORANGE_MONEY
                ? request.getTemporaryCode()
                : null,
            conf.isTestMode());

    log.info(
        "Initiating Papi payment. Reference={}, method={}, amount={}",
        forJava(request.getReference()),
        request.getPaymentMethod(),
        request.getAmount());

    return toPaymentResponse(client.initiate(apiRequest), request.getReference());
  }

  @Override
  protected PaymentResponse doGetPaymentStatus(String reference) {
    return toPaymentResponse(client.getPayment(reference), reference);
  }

  @Override
  protected PaymentResponse doGetPaymentDetails(String reference) {
    return doGetPaymentStatus(reference);
  }

  private PapiPaymentRequest requirePapiRequest(PaymentRequest request) {
    if (!(request instanceof PapiPaymentRequest papiRequest)) {
      throw new PaymentValidationException("Papi gateway requires a PapiPaymentRequest.");
    }
    return papiRequest;
  }

  private void validatePapiFields(PapiPaymentRequest request) {
    BigDecimal amount = request.getAmount();
    if (amount == null || amount.compareTo(BigDecimal.valueOf(300)) < 0) {
      throw new PaymentValidationException("Papi amount must be at least 300 MGA.");
    }
    if (amount.stripTrailingZeros().scale() > 0) {
      throw new PaymentValidationException("Papi amount must be an integer number of MGA.");
    }
    if (request.getCurrency() != MGA) {
      throw new PaymentValidationException("Papi only supports MGA currency.");
    }
    if (request.getPaymentMethod() == null) {
      throw new PaymentValidationException("Papi paymentMethod is required.");
    }
    if (request.getReference() == null || request.getReference().isBlank()) {
      throw new PaymentValidationException("Papi reference is required.");
    }
    if (request.getReference().length() > 255) {
      throw new PaymentValidationException("Papi reference must not exceed 255 characters.");
    }
    if (request.getPayer().getName() == null || request.getPayer().getName().isBlank()) {
      throw new PaymentValidationException("Papi clientName is required.");
    }
    if (request.getDescription() == null || request.getDescription().isBlank()) {
      throw new PaymentValidationException("Papi description is required.");
    }
    if (request.getPaymentMethod() == PapiPaymentMethod.ORANGE_MONEY
        && (request.getTemporaryCode() == null || request.getTemporaryCode().isBlank())) {
      throw new PaymentValidationException(
          "Papi temporaryCode is required for Orange Money payments.");
    }
  }

  private PapiPaymentResponse toPaymentResponse(
      com.devikapps.vaikaparts.client.papi.model.PapiPaymentResponse apiResponse,
      String merchantReference) {
    PapiPaymentData data = apiResponse.data();
    if (data == null) {
      String message =
          apiResponse.error() == null ? "unknown Papi error" : apiResponse.error().message();
      throw new PaymentGatewayException(format("Papi returned no payment data: %s", message));
    }
    if (!merchantReference.equals(data.reference())) {
      throw new PaymentGatewayException("Papi response reference does not match the request.");
    }

    if (data.amount() == null || data.currency() == null || data.paymentMethod() == null) {
      throw new PaymentGatewayException("Papi response is missing payment details.");
    }

    if (!MGA.name().equals(data.currency())) {
      throw new PaymentGatewayException("Papi returned an unsupported payment currency.");
    }

    PaymentStatus status =
        switch (data.paymentStatus() == null ? "" : data.paymentStatus()) {
          case "PENDING" -> PaymentStatus.PENDING;
          case "SUCCESS" -> PaymentStatus.COMPLETED;
          case "FAILED" -> PaymentStatus.FAILED;
          default ->
              throw new PaymentGatewayException(
                  format("Papi returned unsupported payment status: %s", data.paymentStatus()));
        };

    PapiPaymentResponse response = new PapiPaymentResponse();
    response.setTransactionId(merchantReference);
    response.setPapiPaymentReference(data.papiPaymentReference());
    try {
      response.setPaymentMethod(PapiPaymentMethod.valueOf(data.paymentMethod()));
    } catch (IllegalArgumentException e) {
      throw new PaymentGatewayException("Papi returned an unsupported payment method.");
    }
    response.setNotificationToken(data.notificationToken());
    response.setStatus(status);
    response.setAmount(data.amount());
    response.setCurrency(MGA);
    response.setProvider(PAPI);
    response.setRespondedAt(now());
    if (data.error() != null) response.setErrorDescription(data.error().message());
    return response;
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
