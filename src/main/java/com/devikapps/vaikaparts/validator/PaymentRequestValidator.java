package com.devikapps.vaikaparts.validator;

import static java.lang.String.format;

import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.model.PaymentRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
public class PaymentRequestValidator implements Validator<PaymentRequest> {

  @Override
  public void validate(final PaymentRequest input) {
    if (input == null) throw new PaymentValidationException("PaymentRequest cannot be null");

    validateNotNull(input.getAmount(), "amount");
    validatePositiveAmount(input.getAmount());
    validateNotNull(input.getCurrency(), "currency");
    validateNotNull(input.getType(), "type");
    validateNotNull(input.getProvider(), "provider");
    validateNotNull(input.getPayer(), "payer");
    validateNotBlank(input.getPayer().getPhoneNumber(), "payer.phoneNumber");
    validateNotNull(input.getPayee(), "payee");
    validateNotBlank(input.getPayee().getPhoneNumber(), "payee.phoneNumber");
  }

  @Override
  public Class<PaymentRequest> getValidatedType() {
    return PaymentRequest.class;
  }

  public void validateNotBlank(final String value, final String fieldName) {
    if (value == null || value.isBlank())
      throw new PaymentValidationException(
          format("Field '%s' must not be null or blank.", fieldName));
  }

  private void validateNotNull(final Object value, final String fieldName) {
    if (value == null)
      throw new PaymentValidationException(format("Field '%s' must not be null.", fieldName));
  }

  private void validatePositiveAmount(final BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0)
      throw new PaymentValidationException("Payment amount must be a positive value.");
  }
}
