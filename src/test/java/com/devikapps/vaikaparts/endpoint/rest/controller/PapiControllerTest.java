package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.model.PapiFeeQuote;
import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import com.devikapps.vaikaparts.service.PapiPaymentService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PapiControllerTest {
  private final PapiPaymentService service = org.mockito.Mockito.mock(PapiPaymentService.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new PapiController(service)).build();
  }

  @Test
  void should_expose_the_provider_fee_quote() throws Exception {
    when(service.getFees(PapiPaymentMethod.MVOLA, BigDecimal.valueOf(3000)))
        .thenReturn(
            new PapiFeeQuote(
                PapiPaymentMethod.MVOLA,
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(3250),
                "MGA"));

    mockMvc
        .perform(
            get("/v1/payments/papi/fees").param("paymentMethod", "MVOLA").param("amount", "3000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payerFee").value(250))
        .andExpect(jsonPath("$.totalAmount").value(3250));

    verify(service).getFees(PapiPaymentMethod.MVOLA, BigDecimal.valueOf(3000));
  }

  @Test
  void should_reject_unknown_payment_method() throws Exception {
    mockMvc
        .perform(
            get("/v1/payments/papi/fees").param("paymentMethod", "CARD").param("amount", "3000"))
        .andExpect(status().isBadRequest());
  }
}
