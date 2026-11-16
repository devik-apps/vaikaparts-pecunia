package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.event.model.VerificationStatus.FAILED;
import static com.devikapps.vaikaparts.model.classifier.Country.MADAGASCAR;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.MGA;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.PAPI;
import static com.devikapps.vaikaparts.model.classifier.PaymentType.PROFILE_UNLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.client.papi.PapiClient;
import com.devikapps.vaikaparts.client.papi.model.PapiFeeQuoteResponse;
import com.devikapps.vaikaparts.config.PapiConf;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.RPapiPaymentRequest;
import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.gateway.PaymentGateway;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.mapper.PapiPaymentMapper;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.PapiPayment;
import com.devikapps.vaikaparts.model.PapiPaymentResponse;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.PaymentRequest;
import com.devikapps.vaikaparts.model.classifier.PapiPaymentMethod;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.repository.PapiPaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.model.JPapiPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import com.devikapps.vaikaparts.service.papi.PapiSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PapiPaymentServiceTest {
  @Mock private PapiConf conf;
  @Mock private PapiClient papiClient;
  @Mock private PaymentGatewayFactory gatewayFactory;
  @Mock private PapiPaymentRepository papiPaymentRepository;
  @Mock private PaymentPartyRepository paymentPartyRepository;
  @Mock private PaymentPartyMapper paymentPartyMapper;
  @Mock private PapiPaymentMapper papiPaymentMapper;
  @Mock private PapiPaymentStatusService papiPaymentStatusService;
  @Mock private PapiSignatureVerifier signatureVerifier;
  @Mock private ObjectMapper objectMapper;
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

  @Test
  void should_keep_domain_payer_phone_in_local_payment_and_provider_request() {
    String reference = "unlock-reference";
    String payerPhone = "0345551234";
    var payerParty =
        JPaymentParty.builder()
            .id("payer-id")
            .name("Researcher")
            .phoneNumber(payerPhone)
            .country(MADAGASCAR)
            .build();
    var payeeParty =
        JPaymentParty.builder()
            .id("payee-id")
            .name("Merchant")
            .phoneNumber("0345554321")
            .country(MADAGASCAR)
            .build();
    var savedPayment = new AtomicReference<JPapiPayment>();
    var gateway = org.mockito.Mockito.mock(PaymentGateway.class);
    var providerResponse =
        PapiPaymentResponse.builder()
            .transactionId(reference)
            .status(PaymentStatus.PENDING)
            .amount(BigDecimal.valueOf(3000))
            .currency(MGA)
            .provider(PAPI)
            .paymentMethod(PapiPaymentMethod.MVOLA)
            .papiPaymentReference("papi-reference")
            .notificationToken("notification-token")
            .payerFee(BigDecimal.ZERO)
            .totalAmount(BigDecimal.valueOf(3000))
            .build();

    when(conf.getPartnerName()).thenReturn("Merchant");
    when(conf.getPartnerMsisdn()).thenReturn("0345554321");
    when(papiPaymentRepository.findByMerchantReference(reference)).thenReturn(Optional.empty());
    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber(payerPhone))
        .thenReturn(Optional.of(payerParty));
    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber("0345554321"))
        .thenReturn(Optional.of(payeeParty));
    when(papiPaymentRepository.save(any(JPapiPayment.class)))
        .thenAnswer(
            invocation -> {
              JPapiPayment payment = invocation.getArgument(0);
              savedPayment.set(payment);
              return payment;
            });
    when(gatewayFactory.getGateway(PAPI)).thenReturn(gateway);
    when(gateway.initiatePayment(any(PaymentRequest.class))).thenReturn(providerResponse);
    when(papiPaymentStatusService.applyProviderResponse(eq(reference), any()))
        .thenAnswer(invocation -> savedPayment.get());

    service.initiatePayment(
        RPapiPaymentRequest.builder()
            .amount(BigDecimal.valueOf(3000))
            .type(PROFILE_UNLOCK)
            .description("Contact unlock")
            .reference(reference)
            .paymentMethod(PapiPaymentMethod.MVOLA)
            .payer(PaymentParty.builder().name("Researcher").phoneNumber(payerPhone).build())
            .build());

    assertThat(savedPayment.get().getPayer().getPhoneNumber()).isEqualTo(payerPhone);
    var request = ArgumentCaptor.forClass(PaymentRequest.class);
    org.mockito.Mockito.verify(gateway).initiatePayment(request.capture());
    assertThat(request.getValue().getPayer().getPhoneNumber()).isEqualTo(payerPhone);
  }

  @Test
  void should_load_payment_parties_before_mapping_status_response() {
    String reference = "unlock-reference";
    var payment =
        JPapiPayment.builder()
            .id("payment-id")
            .merchantReference(reference)
            .status(FAILED)
            .payer(JPaymentParty.builder().id("payer-id").build())
            .payee(JPaymentParty.builder().id("payee-id").build())
            .build();
    var expected = PapiPayment.builder().build();
    when(papiPaymentRepository.findByMerchantReference(reference)).thenReturn(Optional.of(payment));
    when(papiPaymentRepository.findWithPartiesByMerchantReference(reference))
        .thenReturn(Optional.of(payment));
    when(papiPaymentMapper.toModel(payment)).thenReturn(expected);

    assertThat(service.getPayment(reference)).isSameAs(expected);

    verify(papiPaymentRepository).findWithPartiesByMerchantReference(reference);
    verify(papiPaymentMapper).toModel(payment);
  }
}
