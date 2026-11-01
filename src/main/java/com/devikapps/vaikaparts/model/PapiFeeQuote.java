package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import java.math.BigDecimal;

public record PapiFeeQuote(
    PapiPaymentMethod paymentMethod,
    BigDecimal amount,
    BigDecimal payerFee,
    BigDecimal totalAmount,
    String currency) {}
