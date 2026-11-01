package com.devikapps.vaikaparts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.client.papi.PapiClient;
import com.devikapps.vaikaparts.client.papi.model.PapiFeeQuoteResponse;
import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PapiPaymentServiceTest {
  @Mock private PapiClient papiClient;
  @InjectMocks private PapiPaymentService service;

  @Test
  void should_return_exact_fee_quote_from_papi() {
    when(papiClient.getFees(PapiPaymentMethod.MVOLA, BigDecimal.valueOf(3000)))
        .thenReturn(
            new PapiFeeQuoteResponse.Data(
                "MVOLA",
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(300),
                BigDecimal.valueOf(3300),
                "MGA"));

    var quote = service.getFees(PapiPaymentMethod.MVOLA, BigDecimal.valueOf(3000));

    assertThat(quote.payerFee()).isEqualByComparingTo("300");
    assertThat(quote.totalAmount()).isEqualByComparingTo("3300");
  }

  @Test
  void should_reject_amount_below_papi_minimum_without_calling_provider() {
    assertThatThrownBy(() -> service.getFees(PapiPaymentMethod.MVOLA, BigDecimal.valueOf(299)))
        .isInstanceOf(PaymentValidationException.class);

    verifyNoInteractions(papiClient);
  }

  @Test
  void should_reject_fractional_amount_without_calling_provider() {
    assertThatThrownBy(() -> service.getFees(PapiPaymentMethod.MVOLA, new BigDecimal("3000.5")))
        .isInstanceOf(PaymentValidationException.class);

    verifyNoInteractions(papiClient);
  }
}
