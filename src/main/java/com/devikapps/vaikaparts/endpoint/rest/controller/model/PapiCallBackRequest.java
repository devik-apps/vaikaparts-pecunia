package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PapiCallBackRequest(
    String merchantPaymentReference,
    String paymentReference,
    String paymentStatus,
    String paymentMethod,
    String notificationToken,
    BigDecimal amount,
    BigDecimal fee,
    String message) {}
