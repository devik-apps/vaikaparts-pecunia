package com.devikapps.vaikaparts.client.papi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PapiInitiatePaymentRequest(
    String paymentMethod,
    BigDecimal amount,
    String currency,
    String reference,
    String description,
    String notificationUrl,
    String clientName,
    String payerEmail,
    String phoneNumber,
    String temporaryCode,
    @JsonProperty("isTestMode") Boolean testMode) {}
