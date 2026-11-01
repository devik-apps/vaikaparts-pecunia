package com.devikapps.vaikaparts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.model.PapiPaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import com.devikapps.vaikaparts.model.classifier.PaymentCurrency;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.repository.PapiPaymentRepository;
import com.devikapps.vaikaparts.repository.model.JPapiPayment;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PapiPaymentStatusServiceTest {
  @Mock private PapiPaymentRepository repository;
  @InjectMocks private PapiPaymentStatusService service;
  private JPapiPayment payment;

  @BeforeEach
  void setUp() {
    payment = new JPapiPayment();
    payment.setMerchantReference("ref-1");
    payment.setPaymentMethod(PapiPaymentMethod.MVOLA.name());
    payment.setAmount(BigDecimal.valueOf(3000));
    payment.setCurrency(PaymentCurrency.MGA);
    payment.setStatus(VerificationStatus.PENDING);
    when(repository.findByMerchantReferenceForUpdate("ref-1")).thenReturn(Optional.of(payment));
    when(repository.save(any(JPapiPayment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void should_persist_papi_fee_and_total_including_zero_fee() {
    PapiPaymentResponse response = response();
    response.setPayerFee(BigDecimal.ZERO);
    response.setTotalAmount(BigDecimal.valueOf(3000));

    JPapiPayment result = service.applyProviderResponse("ref-1", response);

    assertThat(result.getPayerFee()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getTotalAmount()).isEqualByComparingTo("3000");
    verify(repository).save(payment);
  }

  @Test
  void should_reject_provider_totals_that_do_not_match_the_base_amount() {
    PapiPaymentResponse response = response();
    response.setPayerFee(BigDecimal.valueOf(300));
    response.setTotalAmount(BigDecimal.valueOf(3000));

    assertThatThrownBy(() -> service.applyProviderResponse("ref-1", response))
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessage("Papi response contains invalid fee totals.");
  }

  @Test
  void should_keep_historical_payment_compatible_when_provider_omits_fee_fields() {
    JPapiPayment result = service.applyProviderResponse("ref-1", response());

    assertThat(result.getPayerFee()).isNull();
    assertThat(result.getTotalAmount()).isNull();
  }

  private PapiPaymentResponse response() {
    PapiPaymentResponse response = new PapiPaymentResponse();
    response.setTransactionId("ref-1");
    response.setAmount(BigDecimal.valueOf(3000));
    response.setPaymentMethod(PapiPaymentMethod.MVOLA);
    response.setCurrency(PaymentCurrency.MGA);
    response.setStatus(PaymentStatus.PENDING);
    return response;
  }
}
