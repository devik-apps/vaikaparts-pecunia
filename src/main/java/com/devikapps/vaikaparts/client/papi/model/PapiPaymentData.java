package com.devikapps.vaikaparts.client.papi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PapiPaymentData(
    String papiPaymentReference,
    String reference,
    String paymentMethod,
    String paymentStatus,
    BigDecimal amount,
    BigDecimal payerFee,
    BigDecimal totalAmount,
    String currency,
    String description,
    String clientName,
    String payerEmail,
    String phoneNumber,
    String notificationUrl,
    String notificationToken,
    @JsonProperty("isTestMode") Boolean testMode,
    PapiErrorResponse error,
    Long createdAt,
    Long updatedAt) {}
