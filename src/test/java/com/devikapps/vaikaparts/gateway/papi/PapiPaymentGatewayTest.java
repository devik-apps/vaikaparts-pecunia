package com.devikapps.vaikaparts.gateway.papi;

import static com.devikapps.vaikaparts.model.classifier.Country.MADAGASCAR;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.MGA;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.PAPI;
import static com.devikapps.vaikaparts.model.classifier.PaymentType.PROFILE_UNLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.client.papi.PapiClient;
import com.devikapps.vaikaparts.client.papi.model.PapiInitiatePaymentRequest;
import com.devikapps.vaikaparts.client.papi.model.PapiPaymentData;
import com.devikapps.vaikaparts.client.papi.model.PapiPaymentResponse;
import com.devikapps.vaikaparts.config.PapiConf;
import com.devikapps.vaikaparts.model.PapiPaymentRequest;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.validator.PaymentRequestValidator;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PapiPaymentGatewayTest {

  private static final String PAYER_PHONE = "0345551234";
  private static final String ORANGE_CODE = "873421";

  @Test
  void testModeUsesSuccessPhoneForEachOperatorAndFixedOrangeCode() {
    var papiClient = mock(PapiClient.class);
    var conf = new PapiConf();
    conf.setTestMode(true);
    when(papiClient.initiate(any())).thenAnswer(invocation -> response(invocation.getArgument(0)));
    var gateway = new PapiPaymentGateway(new PaymentRequestValidator(), papiClient, conf);
    var phones =
        Map.of(
            PapiPaymentMethod.MVOLA, "0341230001",
            PapiPaymentMethod.AIRTEL_MONEY, "331230001",
            PapiPaymentMethod.ORANGE_MONEY, "0321230001");

    phones.forEach(
        (method, expectedPhone) -> {
          var request = request(method);
          var captor = ArgumentCaptor.forClass(PapiInitiatePaymentRequest.class);

          gateway.initiatePayment(request);
          org.mockito.Mockito.verify(papiClient, org.mockito.Mockito.atLeastOnce())
              .initiate(captor.capture());

          var sent = captor.getValue();
          assertThat(sent.phoneNumber()).isEqualTo(expectedPhone);
          assertThat(sent.testMode()).isTrue();
          if (method == PapiPaymentMethod.ORANGE_MONEY) {
            assertThat(sent.temporaryCode()).isEqualTo("101001");
          }
          assertThat(request.getPayer().getPhoneNumber()).isEqualTo(PAYER_PHONE);
        });
  }

  @Test
  void productionModePreservesPayerPhoneAndOrangeCode() {
    var papiClient = mock(PapiClient.class);
    var conf = new PapiConf();
    conf.setTestMode(false);
    when(papiClient.initiate(any())).thenAnswer(invocation -> response(invocation.getArgument(0)));
    var gateway = new PapiPaymentGateway(new PaymentRequestValidator(), papiClient, conf);
    var captor = ArgumentCaptor.forClass(PapiInitiatePaymentRequest.class);
    var request = request(PapiPaymentMethod.ORANGE_MONEY);

    gateway.initiatePayment(request);
    org.mockito.Mockito.verify(papiClient).initiate(captor.capture());

    assertThat(captor.getValue().phoneNumber()).isEqualTo(PAYER_PHONE);
    assertThat(captor.getValue().temporaryCode()).isEqualTo(ORANGE_CODE);
    assertThat(captor.getValue().testMode()).isFalse();
  }

  private PapiPaymentRequest request(PapiPaymentMethod method) {
    return PapiPaymentRequest.builder()
        .transactionId("unlock-reference")
        .amount(BigDecimal.valueOf(3000))
        .currency(MGA)
        .description("Contact unlock")
        .payer(PaymentParty.builder().name("Researcher").phoneNumber(PAYER_PHONE).build())
        .payee(
            PaymentParty.builder()
                .name("Merchant")
                .phoneNumber("0345550000")
                .country(MADAGASCAR)
                .build())
        .provider(PAPI)
        .type(PROFILE_UNLOCK)
        .reference("unlock-reference")
        .paymentMethod(method)
        .temporaryCode(ORANGE_CODE)
        .build();
  }

  private PapiPaymentResponse response(PapiInitiatePaymentRequest request) {
    return new PapiPaymentResponse(
        new PapiPaymentData(
            "papi-reference",
            request.reference(),
            request.paymentMethod(),
            PaymentStatus.PENDING.name(),
            request.amount(),
            BigDecimal.ZERO,
            request.amount(),
            MGA.name(),
            request.description(),
            request.clientName(),
            request.payerEmail(),
            request.phoneNumber(),
            request.notificationUrl(),
            "notification-token",
            request.testMode(),
            null,
            1L,
            1L),
        null);
  }
}
