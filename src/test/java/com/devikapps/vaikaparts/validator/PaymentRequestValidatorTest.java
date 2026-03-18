package com.devikapps.vaikaparts.validator;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.classifier.PaymentCurrency;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentRequestValidatorTest {

  private final PaymentRequestValidator subject = new PaymentRequestValidator();

  @Test
  void should_return_payment_request_as_validated_type() {
    assertEquals(PaymentRequest.class, subject.getValidatedType());
  }

  @Test
  void should_not_throw_when_request_is_fully_valid() {
    assertDoesNotThrow(() -> subject.validate(buildValidRequest()));
  }

  @Test
  void should_throw_when_transaction_id_is_null() {
    final TestPaymentRequest request = buildValidRequest();
    request.setTransactionId(null);

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'transactionId' must not be null or blank.", ex.getMessage());
  }

  @Test
  void should_throw_when_transaction_id_is_blank() {
    final TestPaymentRequest request = buildValidRequest();
    request.setTransactionId("   ");

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'transactionId' must not be null or blank.", ex.getMessage());
  }

  @Test
  void should_throw_when_transaction_id_is_empty() {
    final TestPaymentRequest request = buildValidRequest();
    request.setTransactionId("");

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'transactionId' must not be null or blank.", ex.getMessage());
  }

  @Test
  void should_throw_when_amount_is_null() {
    final TestPaymentRequest request = buildValidRequest();
    request.setAmount(null);

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'amount' must not be null.", ex.getMessage());
  }

  @Test
  void should_throw_when_amount_is_zero() {
    final TestPaymentRequest request = buildValidRequest();
    request.setAmount(BigDecimal.ZERO);

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Payment amount must be a positive value.", ex.getMessage());
  }

  @Test
  void should_throw_when_amount_is_negative() {
    final TestPaymentRequest request = buildValidRequest();
    request.setAmount(new BigDecimal("-1.00"));

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Payment amount must be a positive value.", ex.getMessage());
  }

  @Test
  void should_not_throw_when_amount_is_minimal_positive_value() {
    final TestPaymentRequest request = buildValidRequest();
    request.setAmount(new BigDecimal("0.01"));

    assertDoesNotThrow(() -> subject.validate(request));
  }

  @Test
  void should_throw_when_currency_is_null() {
    final TestPaymentRequest request = buildValidRequest();
    request.setCurrency(null);

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'currency' must not be null.", ex.getMessage());
  }

  @Test
  void should_throw_when_type_is_null() {
    final TestPaymentRequest request = buildValidRequest();
    request.setType(null);

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'type' must not be null.", ex.getMessage());
  }

  @Test
  void should_throw_when_provider_is_null() {
    final TestPaymentRequest request = buildValidRequest();
    request.setProvider(null);

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'provider' must not be null.", ex.getMessage());
  }

  @Test
  void should_throw_when_payer_is_null() {
    final TestPaymentRequest request = buildValidRequest();
    request.setPayer(null);

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'payer' must not be null.", ex.getMessage());
  }

  @Test
  void should_throw_when_payer_phone_number_is_null() {
    final TestPaymentRequest request = buildValidRequest();
    request.setPayer(PaymentParty.builder().phoneNumber(null).build());

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'payer.phoneNumber' must not be null or blank.", ex.getMessage());
  }

  @Test
  void should_throw_when_payer_phone_number_is_blank() {
    final TestPaymentRequest request = buildValidRequest();
    request.setPayer(PaymentParty.builder().phoneNumber("   ").build());

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'payer.phoneNumber' must not be null or blank.", ex.getMessage());
  }

  @Test
  void should_throw_when_payer_phone_number_is_empty() {
    final TestPaymentRequest request = buildValidRequest();
    request.setPayer(PaymentParty.builder().phoneNumber("").build());

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'payer.phoneNumber' must not be null or blank.", ex.getMessage());
  }

  @Test
  void should_throw_when_payee_is_null() {
    final TestPaymentRequest request = buildValidRequest();
    request.setPayee(null);

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'payee' must not be null.", ex.getMessage());
  }

  @Test
  void should_throw_when_payee_phone_number_is_null() {
    final TestPaymentRequest request = buildValidRequest();
    request.setPayee(PaymentParty.builder().phoneNumber(null).build());

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'payee.phoneNumber' must not be null or blank.", ex.getMessage());
  }

  @Test
  void should_throw_when_payee_phone_number_is_blank() {
    final TestPaymentRequest request = buildValidRequest();
    request.setPayee(PaymentParty.builder().phoneNumber("   ").build());

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'payee.phoneNumber' must not be null or blank.", ex.getMessage());
  }

  @Test
  void should_throw_when_payee_phone_number_is_empty() {
    final TestPaymentRequest request = buildValidRequest();
    request.setPayee(PaymentParty.builder().phoneNumber("").build());

    final PaymentValidationException ex =
        assertThrows(PaymentValidationException.class, () -> subject.validate(request));

    assertEquals("Field 'payee.phoneNumber' must not be null or blank.", ex.getMessage());
  }

  @Test
  void should_throw_payment_validation_exception_and_not_a_generic_runtime_exception() {
    final TestPaymentRequest request = buildValidRequest();
    request.setTransactionId(null);

    assertThrows(PaymentValidationException.class, () -> subject.validate(request));
  }

  private TestPaymentRequest buildValidRequest() {
    final TestPaymentRequest request = new TestPaymentRequest();
    request.setTransactionId(randomUUID().toString());
    request.setAmount(new BigDecimal("5000"));
    request.setCurrency(PaymentCurrency.AR);
    request.setDescription("Test payment");
    request.setProvider(PaymentProvider.MVOLA);
    request.setType(PaymentType.PROFILE_UNLOCK);
    request.setPayer(PaymentParty.builder().phoneNumber("0341234567").build());
    request.setPayee(PaymentParty.builder().phoneNumber("0340017983").build());
    return request;
  }

  private static class TestPaymentRequest extends PaymentRequest {}
}
