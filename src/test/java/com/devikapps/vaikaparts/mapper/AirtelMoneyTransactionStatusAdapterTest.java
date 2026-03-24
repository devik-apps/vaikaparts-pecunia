package com.devikapps.vaikaparts.mapper;

import static com.devikapps.vaikaparts.model.classifier.PaymentStatus.COMPLETED;
import static com.devikapps.vaikaparts.model.classifier.PaymentStatus.FAILED;
import static com.devikapps.vaikaparts.model.classifier.PaymentStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.razafindratelo.airtel_money_client.model.TransactionStatus;
import org.junit.jupiter.api.Test;

class AirtelMoneyTransactionStatusAdapterTest {

  private final AirtelMoneyTransactionStatusAdapter subject =
      new AirtelMoneyTransactionStatusAdapter();

  @Test
  void should_map_ts_to_completed() {
    assertEquals(COMPLETED, subject.toPaymentStatus(TransactionStatus.TS));
  }

  @Test
  void should_map_tf_to_failed() {
    assertEquals(FAILED, subject.toPaymentStatus(TransactionStatus.TF));
  }

  @Test
  void should_map_te_to_failed() {
    assertEquals(FAILED, subject.toPaymentStatus(TransactionStatus.TE));
  }

  @Test
  void should_map_tip_to_pending() {
    assertEquals(PENDING, subject.toPaymentStatus(TransactionStatus.TIP));
  }

  @Test
  void should_map_ta_to_pending() {
    assertEquals(PENDING, subject.toPaymentStatus(TransactionStatus.TA));
  }

  @Test
  void should_map_null_to_pending() {
    assertEquals(PENDING, subject.toPaymentStatus(null));
  }
}
