package com.devikapps.vaikaparts.service.util;

import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;

import com.devikapps.vaikaparts.model.MvolaPaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class MvolaResponseParser {

  private static final String STATUS_COMPLETED = "completed";
  private static final String STATUS_FAILED = "failed";

  public final Function<String, MvolaPaymentResponse> initiatePaymentParser =
      rawBody -> {
        final JsonObject json = JsonParser.parseString(rawBody).getAsJsonObject();

        return MvolaPaymentResponse.builder()
            .status(resolveStatus(getStringOrNull(json, "status")))
            .serverCorrelationId(getStringOrNull(json, "serverCorrelationId"))
            .notificationMethod(getStringOrNull(json, "notificationMethod"))
            .respondedAt(LocalDateTime.now())
            .build();
      };

  public final Function<String, MvolaPaymentResponse> paymentStatusParser =
      rawBody -> {
        final JsonObject json = JsonParser.parseString(rawBody).getAsJsonObject();

        return MvolaPaymentResponse.builder()
            .status(resolveStatus(getStringOrNull(json, "status")))
            .serverCorrelationId(getStringOrNull(json, "serverCorrelationId"))
            .notificationMethod(getStringOrNull(json, "notificationMethod"))
            .objectReference(getStringOrNull(json, "objectReference"))
            .respondedAt(LocalDateTime.now())
            .build();
      };

  public final Function<String, MvolaPaymentResponse> paymentDetailsParser =
      rawBody -> {
        final JsonObject json = JsonParser.parseString(rawBody).getAsJsonObject();

        var amountField = "amount";

        final BigDecimal amount =
            json.has(amountField) && !json.get(amountField).isJsonNull()
                ? new BigDecimal(json.get(amountField).getAsString())
                : null;

        return MvolaPaymentResponse.builder()
            .transactionId(getStringOrNull(json, "transactionReference"))
            .status(resolveStatus(getStringOrNull(json, "transactionStatus")))
            .amount(amount)
            .currency(AR)
            .provider(MVOLA)
            .respondedAt(LocalDateTime.now())
            .build();
      };

  private PaymentStatus resolveStatus(final String mvolaStatus) {
    if (mvolaStatus == null) return PaymentStatus.PENDING;

    return switch (mvolaStatus.toLowerCase()) {
      case STATUS_COMPLETED -> PaymentStatus.COMPLETED;
      case STATUS_FAILED -> PaymentStatus.FAILED;
      default -> PaymentStatus.PENDING;
    };
  }

  private String getStringOrNull(final JsonObject json, final String key) {
    return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
  }
}
