package com.devikapps.vaikaparts.client.papi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.devikapps.vaikaparts.client.papi.model.PapiFeeQuoteResponse;
import com.devikapps.vaikaparts.config.PapiConf;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class PapiClientTest {
  private final PapiConf conf = new PapiConf();
  private final RestTemplate restTemplate = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
  private PapiClient client;

  @BeforeEach
  void setUp() {
    conf.setApiKey("sandbox-key");
    conf.setBaseUrl("https://sandbox.papi.mg/engine/api/");
    client = new PapiClient(conf, restTemplate, new ObjectMapper());
  }

  @Test
  void should_get_fee_quote_for_each_supported_method_and_accept_zero_fee() {
    for (PapiPaymentMethod paymentMethod : PapiPaymentMethod.values()) {
      server
          .expect(
              requestTo(
                  org.hamcrest.Matchers.containsString(
                      "https://sandbox.papi.mg/engine/api/v1/payments/fees")))
          .andExpect(method(HttpMethod.GET))
          .andExpect(queryParam("paymentMethod", paymentMethod.name()))
          .andExpect(queryParam("amount", "3000"))
          .andExpect(header("Token", "sandbox-key"))
          .andRespond(
              withSuccess(
                  """
                  {"data":{"paymentMethod":"%s","amount":3000,"payerFee":0,"totalAmount":3000,"currency":"MGA"}}
                  """
                      .formatted(paymentMethod.name()),
                  MediaType.APPLICATION_JSON));

      PapiFeeQuoteResponse.Data quote = client.getFees(paymentMethod, BigDecimal.valueOf(3000));

      assertThat(quote.payerFee()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(quote.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }
    server.verify();
  }

  @Test
  void should_reject_inconsistent_fee_quote() {
    server
        .expect(requestTo(org.hamcrest.Matchers.containsString("/v1/payments/fees")))
        .andRespond(
            withSuccess(
                """
                {"data":{"paymentMethod":"MVOLA","amount":3000,"payerFee":250,"totalAmount":3000,"currency":"MGA"}}
                """,
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.getFees(PapiPaymentMethod.MVOLA, BigDecimal.valueOf(3000)))
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessage("Papi returned an invalid fee quote.");
    server.verify();
  }

  @Test
  void should_reject_fee_quote_when_api_key_is_missing() {
    conf.setApiKey(" ");

    assertThatThrownBy(() -> client.getFees(PapiPaymentMethod.MVOLA, BigDecimal.valueOf(3000)))
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessage("Papi API key is not configured.");
  }

  @Test
  void should_hide_upstream_details_on_provider_errors() {
    server
        .expect(requestTo(org.hamcrest.Matchers.containsString("/v1/payments/fees")))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    assertThatThrownBy(() -> client.getFees(PapiPaymentMethod.MVOLA, BigDecimal.valueOf(3000)))
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessage("Papi request failed with HTTP 401.")
        .hasMessageNotContaining("sandbox-key");
    server.verify();
  }

  @Test
  void should_hide_upstream_details_on_network_errors() {
    server
        .expect(requestTo(org.hamcrest.Matchers.containsString("/v1/payments/fees")))
        .andRespond(
            request -> {
              throw new ResourceAccessException("network failure with sandbox-key");
            });

    assertThatThrownBy(() -> client.getFees(PapiPaymentMethod.MVOLA, BigDecimal.valueOf(3000)))
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessage("Papi request failed due to a network or response error.")
        .hasMessageNotContaining("sandbox-key");
    server.verify();
  }
}
