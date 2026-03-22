package com.devikapps.vaikaparts.endpoint.rest.controller;

import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_MSISDN;
import static com.devikapps.vaikaparts.event.model.VerificationStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.MvolaCallBackRequest;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentRequest;
import com.devikapps.vaikaparts.model.MvolaPayment;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.Country;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.service.MvolaPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

class MvolaControllerIT extends FacadeIT {

  public static final String COMPLETED_STATUS = "completed";
  private static final String BASE_URL = "/v1/payments/mvola";
  private static final String CUSTOMER_MSISDN = "0343500003";
  private static final long CONSUMER_WAIT_MS = 5_000L;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private MvolaPaymentService mvolaPaymentService;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private PaymentRequestedRepository paymentRequestedRepository;
  @Autowired private PaymentPartyRepository paymentPartyRepository;

  @AfterEach
  void clean_up() {
    paymentRequestedRepository.deleteAll();
    paymentRepository.deleteAll();
    paymentPartyRepository.deleteAll();
  }

  @Test
  void should_return_201_with_payment_body_on_successful_initiation() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildValidRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.server_correlation_id").isNotEmpty())
        .andExpect(jsonPath("$.transaction_id").isNotEmpty())
        .andExpect(jsonPath("$.provider").value(MVOLA.toString()))
        .andExpect(jsonPath("$.status").value(PENDING.toString()));
  }

  @Test
  void should_persist_payment_after_initiate_via_controller() throws Exception {
    final var result =
        mockMvc
            .perform(
                post(BASE_URL)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildValidRequest())))
            .andExpect(status().isCreated())
            .andReturn();

    final MvolaPayment response =
        objectMapper.readValue(result.getResponse().getContentAsString(), MvolaPayment.class);

    Thread.sleep(CONSUMER_WAIT_MS);

    assertTrue(
        paymentRepository.findJPaymentByTransactionId(response.getTransactionId()).isPresent());
  }

  @Test
  void should_return_400_when_request_body_is_absent() throws Exception {
    mockMvc
        .perform(post(BASE_URL).contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_400_when_payer_is_null() throws Exception {
    final MvolaPaymentRequest request = buildValidRequest();
    request.setPayer(null);

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_400_when_amount_is_null() throws Exception {
    final MvolaPaymentRequest request = buildValidRequest();
    request.setAmount(null);

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Transactional
  void should_return_200_with_correct_payment_body_on_get_payment() throws Exception {
    final MvolaPayment initiated =
        (MvolaPayment) mvolaPaymentService.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    mockMvc
        .perform(get(format("%s/{transactionId}", BASE_URL), initiated.getTransactionId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transaction_id").value(initiated.getTransactionId()))
        .andExpect(jsonPath("$.server_correlation_id").value(initiated.getServerCorrelationId()))
        .andExpect(jsonPath("$.provider").value(MVOLA.toString()))
        .andExpect(jsonPath("$.status").value(PENDING.toString()));
  }

  @Test
  void should_return_404_when_payment_not_found() throws Exception {
    mockMvc
        .perform(get(format("%s/{transactionId}", BASE_URL), "non-existent-tx-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_200_on_valid_completed_callback() throws Exception {
    final MvolaPayment initiated =
        (MvolaPayment) mvolaPaymentService.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    mockMvc
        .perform(
            put(format("%s/callback", BASE_URL))
                .contentType(APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        buildCallbackRequest(
                            initiated.getServerCorrelationId(), COMPLETED_STATUS, "TX-REF-001"))))
        .andExpect(status().isOk())
        .andExpect(content().string(""));
  }

  @Test
  void should_update_payment_status_to_success_after_completed_callback() throws Exception {
    final MvolaPayment initiated =
        (MvolaPayment) mvolaPaymentService.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    mockMvc
        .perform(
            put(format("%s/callback", BASE_URL))
                .contentType(APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        buildCallbackRequest(
                            initiated.getServerCorrelationId(), COMPLETED_STATUS, "TX-REF-001"))))
        .andExpect(status().isOk());

    final JPayment updated =
        paymentRepository
            .findJPaymentByTransactionId(initiated.getTransactionId())
            .orElseThrow(() -> new AssertionError("Payment not found"));

    assertEquals(VerificationStatus.SUCCESS, updated.getStatus());
  }

  @Test
  void should_update_payment_status_to_failed_after_failed_callback() throws Exception {
    final MvolaPayment initiated =
        (MvolaPayment) mvolaPaymentService.initiatePayment(buildValidRequest());
    Thread.sleep(CONSUMER_WAIT_MS);

    mockMvc
        .perform(
            put(format("%s/callback", BASE_URL))
                .contentType(APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        buildCallbackRequest(
                            initiated.getServerCorrelationId(), "failed", "TX-REF-002"))))
        .andExpect(status().isOk());

    final JPayment updated =
        paymentRepository
            .findJPaymentByTransactionId(initiated.getTransactionId())
            .orElseThrow(() -> new AssertionError("Payment not found"));

    assertEquals(VerificationStatus.FAILED, updated.getStatus());
  }

  @Test
  void should_return_400_when_callback_body_is_absent() throws Exception {
    mockMvc
        .perform(put(format("%s/callback", BASE_URL)).contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_404_when_callback_references_unknown_payment() throws Exception {
    mockMvc
        .perform(
            put(format("%s/callback", BASE_URL))
                .contentType(APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        buildCallbackRequest(
                            "non-existent-correlation-id", COMPLETED_STATUS, "TX123"))))
        .andExpect(status().isNotFound());
  }

  private MvolaPaymentRequest buildValidRequest() {
    return MvolaPaymentRequest.builder()
        .transactionId(randomUUID().toString())
        .amount(new BigDecimal("100"))
        .currency(AR)
        .description("Integration test payment")
        .provider(MVOLA)
        .type(PaymentType.PROFILE_UNLOCK)
        .payer(
            PaymentParty.builder()
                .phoneNumber("0343500003")
                .name("Test Customer")
                .country(Country.MADAGASCAR)
                .build())
        .payee(
            PaymentParty.builder()
                .phoneNumber(MVOLA_MSISDN)
                .name("TestMVola")
                .country(Country.MADAGASCAR)
                .build())
        .build();
  }

  private MvolaCallBackRequest buildCallbackRequest(
      final String serverCorrelationId, final String status, final String transactionReference) {
    final MvolaCallBackRequest request = new MvolaCallBackRequest();
    request.setServerCorrelationId(serverCorrelationId);
    request.setTransactionStatus(status);
    request.setTransactionReference(transactionReference);
    request.setRequestDate("2026-03-22T03:28:00.567Z");
    request.setDebitParty(
        List.of(new MvolaCallBackRequest.MvolaPartyEntry("msisdn", CUSTOMER_MSISDN)));
    request.setCreditParty(
        List.of(new MvolaCallBackRequest.MvolaPartyEntry("msisdn", MVOLA_MSISDN)));
    return request;
  }
}
