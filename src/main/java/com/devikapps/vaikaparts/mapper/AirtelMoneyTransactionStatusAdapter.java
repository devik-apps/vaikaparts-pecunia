package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import dev.razafindratelo.airtel_money_client.model.TransactionStatus;
import org.springframework.stereotype.Component;

/**
 * Maps Airtel Money transaction status codes to the domain {@link PaymentStatus}.
 *
 * <p>Airtel status codes: TS — Transaction Success → COMPLETED TF — Transaction Failed → FAILED TIP
 * — Transaction In Progress → PENDING TA — Transaction Ambiguous → PENDING (retry enquiry) TE —
 * Transaction Expired → FAILED
 *
 * <p>Important: status.success in Airtel responses may be false even on HTTP 200. The transaction
 * status code is the authoritative source of truth, not the status envelope.
 */
@Component
public class AirtelMoneyTransactionStatusAdapter {

  public PaymentStatus toPaymentStatus(TransactionStatus airtelStatus) {
    if (airtelStatus == null) return PaymentStatus.PENDING;

    return switch (airtelStatus) {
      case TS -> PaymentStatus.COMPLETED;
      case TF, TE -> PaymentStatus.FAILED;
      case TIP, TA -> PaymentStatus.PENDING;
    };
  }
}
