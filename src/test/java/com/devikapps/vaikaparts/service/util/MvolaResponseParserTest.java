package com.devikapps.vaikaparts.service.util;

import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MvolaResponseParserTest {

  private final MvolaResponseParser subject = new MvolaResponseParser();

  @Test
  void initiate_parser_should_map_pending_status() {
    final String rawBody =
        """
        {
          "status": "pending",
          "serverCorrelationId": "421a22a2-ef1d-42bc-9452-f4939a3d5cdf",
          "notificationMethod": "polling"
        }
        """;

    final MvolaPaymentResponse response = subject.initiatePaymentParser.apply(rawBody);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
    assertEquals("421a22a2-ef1d-42bc-9452-f4939a3d5cdf", response.getServerCorrelationId());
    assertEquals("polling", response.getNotificationMethod());
    assertNotNull(response.getRespondedAt());
  }

  @Test
  void initiate_parser_should_map_callback_notification_method() {
    final String rawBody =
        """
        {
          "status": "pending",
          "serverCorrelationId": "abc-123",
          "notificationMethod": "callback"
        }
        """;

    final MvolaPaymentResponse response = subject.initiatePaymentParser.apply(rawBody);

    assertEquals("callback", response.getNotificationMethod());
  }

  @Test
  void initiate_parser_should_handle_null_notification_method() {
    final String rawBody =
        """
        {
          "status": "pending",
          "serverCorrelationId": "abc-123",
          "notificationMethod": null
        }
        """;

    final MvolaPaymentResponse response = subject.initiatePaymentParser.apply(rawBody);

    assertNull(response.getNotificationMethod());
  }

  @Test
  void initiate_parser_should_handle_missing_fields() {
    final String rawBody =
        """
        {
          "status": "pending"
        }
        """;

    final MvolaPaymentResponse response = subject.initiatePaymentParser.apply(rawBody);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
    assertNull(response.getServerCorrelationId());
    assertNull(response.getNotificationMethod());
  }

  @Test
  void status_parser_should_map_pending_status() {
    final String rawBody =
        """
        {
          "status": "pending",
          "serverCorrelationId": "421a22a2-ef1d-42bc-9452-f4939a3d5cdf",
          "notificationMethod": "polling",
          "objectReference": null
        }
        """;

    final MvolaPaymentResponse response = subject.paymentStatusParser.apply(rawBody);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
    assertEquals("421a22a2-ef1d-42bc-9452-f4939a3d5cdf", response.getServerCorrelationId());
    assertEquals("polling", response.getNotificationMethod());
    assertNull(response.getObjectReference());
  }

  @Test
  void status_parser_should_map_completed_status_with_object_reference() {
    final String rawBody =
        """
        {
          "status": "completed",
          "serverCorrelationId": "421a22a2-ef1d-42bc-9452-f4939a3d5cdf",
          "notificationMethod": "polling",
          "objectReference": "641235"
        }
        """;

    final MvolaPaymentResponse response = subject.paymentStatusParser.apply(rawBody);

    assertEquals(PaymentStatus.COMPLETED, response.getStatus());
    assertEquals("641235", response.getObjectReference());
  }

  @Test
  void status_parser_should_map_failed_status() {
    final String rawBody =
        """
        {
          "status": "failed",
          "serverCorrelationId": "421a22a2-ef1d-42bc-9452-f4939a3d5cdf",
          "notificationMethod": "polling",
          "objectReference": null
        }
        """;

    final MvolaPaymentResponse response = subject.paymentStatusParser.apply(rawBody);

    assertEquals(PaymentStatus.FAILED, response.getStatus());
  }

  @Test
  void status_parser_should_default_to_pending_on_unknown_status() {
    final String rawBody =
        """
        {
          "status": "unknown_value",
          "serverCorrelationId": "abc-123",
          "notificationMethod": "polling"
        }
        """;

    final MvolaPaymentResponse response = subject.paymentStatusParser.apply(rawBody);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
  }

  @Test
  void status_parser_should_default_to_pending_on_null_status() {
    final String rawBody =
        """
        {
          "status": null,
          "serverCorrelationId": "abc-123",
          "notificationMethod": "polling"
        }
        """;

    final MvolaPaymentResponse response = subject.paymentStatusParser.apply(rawBody);

    assertEquals(PaymentStatus.PENDING, response.getStatus());
  }

  @Test
  void details_parser_should_map_transaction_reference_and_amount() {
    final String rawBody =
        """
        {
          "amount": "5000",
          "currency": "Ar",
          "transactionReference": "641235",
          "transactionStatus": "completed",
          "createDate": "2026-03-16T10:00:00.000Z",
          "debitParty": [{"key": "msisdn", "value": "0341234567"}],
          "creditParty": [{"key": "msisdn", "value": "0340017983"}]
        }
        """;

    final MvolaPaymentResponse response = subject.paymentDetailsParser.apply(rawBody);

    assertEquals("641235", response.getTransactionId());
    assertEquals(PaymentStatus.COMPLETED, response.getStatus());
    assertEquals(new BigDecimal("5000"), response.getAmount());
    assertEquals(AR, response.getCurrency());
    assertEquals(MVOLA, response.getProvider());
    assertNotNull(response.getRespondedAt());
  }

  @Test
  void details_parser_should_map_failed_transaction_status() {
    final String rawBody =
        """
        {
          "amount": "5000",
          "currency": "Ar",
          "transactionReference": "641235",
          "transactionStatus": "failed"
        }
        """;

    final MvolaPaymentResponse response = subject.paymentDetailsParser.apply(rawBody);

    assertEquals(PaymentStatus.FAILED, response.getStatus());
  }

  @Test
  void details_parser_should_handle_null_amount() {
    final String rawBody =
        """
        {
          "amount": null,
          "transactionReference": "641235",
          "transactionStatus": "completed"
        }
        """;

    final MvolaPaymentResponse response = subject.paymentDetailsParser.apply(rawBody);

    assertNull(response.getAmount());
    assertEquals(PaymentStatus.COMPLETED, response.getStatus());
  }

  @Test
  void details_parser_should_handle_missing_transaction_reference() {
    final String rawBody =
        """
        {
          "amount": "5000",
          "transactionStatus": "completed"
        }
        """;

    final MvolaPaymentResponse response = subject.paymentDetailsParser.apply(rawBody);

    assertNull(response.getTransactionId());
  }

  @Test
  void details_parser_should_always_set_provider_to_mvola() {
    final String rawBody =
        """
        {
          "amount": "1000",
          "transactionReference": "123",
          "transactionStatus": "pending"
        }
        """;

    final MvolaPaymentResponse response = subject.paymentDetailsParser.apply(rawBody);

    assertEquals(MVOLA, response.getProvider());
  }

  @Test
  void details_parser_should_always_set_currency_to_ar() {
    final String rawBody =
        """
        {
          "amount": "1000",
          "transactionReference": "123",
          "transactionStatus": "completed"
        }
        """;

    final MvolaPaymentResponse response = subject.paymentDetailsParser.apply(rawBody);

    assertEquals(AR, response.getCurrency());
  }
}
