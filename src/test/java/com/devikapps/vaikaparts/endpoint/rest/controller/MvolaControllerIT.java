package com.devikapps.vaikaparts.endpoint.rest.controller;

import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_MSISDN;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentRequest;
import com.devikapps.vaikaparts.model.MvolaPayment;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.Country;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.service.MvolaPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class MvolaControllerIT extends FacadeIT {

  private static final String BASE_URL = "/v1/payments/mvola";
  private static final String TRANSACTION_ID = randomUUID().toString();
  private static final String CORRELATION_ID = randomUUID().toString();
  private static final String CUSTOMER_MSISDN = "0343500003";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private MvolaPaymentService mvolaPaymentService;

  @Test
  void should_return_201_with_payment_body_on_successful_initiation() throws Exception {
    when(mvolaPaymentService.initiatePayment(any(MvolaPaymentRequest.class)))
        .thenReturn(buildMvolaPayment());

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildValidRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.transaction_id").value(TRANSACTION_ID))
        .andExpect(jsonPath("$.server_correlation_id").value(CORRELATION_ID))
        .andExpect(jsonPath("$.provider").value("MVOLA"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void should_delegate_to_service_exactly_once_on_initiate_payment() throws Exception {
    when(mvolaPaymentService.initiatePayment(any(MvolaPaymentRequest.class)))
        .thenReturn(buildMvolaPayment());

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildValidRequest())))
        .andExpect(status().isCreated());

    verify(mvolaPaymentService, times(1)).initiatePayment(any(MvolaPaymentRequest.class));
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
  void should_propagate_gateway_exception_as_5xx_on_initiate_payment() throws Exception {
    when(mvolaPaymentService.initiatePayment(any(MvolaPaymentRequest.class)))
        .thenThrow(new PaymentGatewayException("MVola gateway error"));

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildValidRequest())))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void should_return_200_with_correct_payment_body_on_get_payment() throws Exception {
    when(mvolaPaymentService.getPayment(TRANSACTION_ID)).thenReturn(buildMvolaPayment());

    mockMvc
        .perform(get(BASE_URL + "/{transactionId}", TRANSACTION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transaction_id").value(TRANSACTION_ID))
        .andExpect(jsonPath("$.server_correlation_id").value(CORRELATION_ID))
        .andExpect(jsonPath("$.provider").value("MVOLA"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void should_delegate_to_service_with_exact_transaction_id_on_get_payment() throws Exception {
    when(mvolaPaymentService.getPayment(TRANSACTION_ID)).thenReturn(buildMvolaPayment());

    mockMvc.perform(get(BASE_URL + "/{transactionId}", TRANSACTION_ID)).andExpect(status().isOk());

    verify(mvolaPaymentService, times(1)).getPayment(TRANSACTION_ID);
  }

  @Test
  void should_return_404_when_payment_not_found() throws Exception {
    when(mvolaPaymentService.getPayment(any()))
        .thenThrow(new EntityNotFoundException("No payment found"));

    mockMvc
        .perform(get(BASE_URL + "/{transactionId}", "non-existent-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_200_on_valid_completed_callback() throws Exception {
    doNothing().when(mvolaPaymentService).handleCallBack(any(MvolaCallBackRequest.class));

    mockMvc
        .perform(
            put(BASE_URL + "/callback")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCompletedCallbackRequest())))
        .andExpect(status().isOk());
  }

  @Test
  void should_return_200_on_valid_failed_callback() throws Exception {
    doNothing().when(mvolaPaymentService).handleCallBack(any(MvolaCallBackRequest.class));

    mockMvc
        .perform(
            put(BASE_URL + "/callback")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildFailedCallbackRequest())))
        .andExpect(status().isOk());
  }

  @Test
  void should_delegate_to_service_exactly_once_on_callback() throws Exception {
    doNothing().when(mvolaPaymentService).handleCallBack(any(MvolaCallBackRequest.class));

    mockMvc
        .perform(
            put(BASE_URL + "/callback")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCompletedCallbackRequest())))
        .andExpect(status().isOk());

    verify(mvolaPaymentService, times(1)).handleCallBack(any(MvolaCallBackRequest.class));
  }

  @Test
  void should_return_400_when_callback_body_is_absent() throws Exception {
    mockMvc
        .perform(put(BASE_URL + "/callback").contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_empty_body_on_successful_callback() throws Exception {
    doNothing().when(mvolaPaymentService).handleCallBack(any(MvolaCallBackRequest.class));

    mockMvc
        .perform(
            put(BASE_URL + "/callback")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCompletedCallbackRequest())))
        .andExpect(status().isOk())
        .andExpect(content().string(""));
  }

  private MvolaPaymentRequest buildValidRequest() {
    return MvolaPaymentRequest.builder()
        .transactionId(randomUUID().toString())
        .amount(new BigDecimal("100"))
        .currency(AR)
        .description("Test payment")
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
        .correlationId(randomUUID().toString())
        .build();
  }

  private MvolaPayment buildMvolaPayment() {
    return MvolaPayment.builder()
        .transactionId(TRANSACTION_ID)
        .serverCorrelationId(CORRELATION_ID)
        .provider(MVOLA)
        .status(VerificationStatus.PENDING)
        .amount(new BigDecimal("100"))
        .currency(AR)
        .build();
  }

  private MvolaCallBackRequest buildCompletedCallbackRequest() {
    final MvolaCallBackRequest request = new MvolaCallBackRequest();
    request.setServerCorrelationId(CORRELATION_ID);
    request.setTransactionStatus("completed");
    request.setTransactionReference("641235");
    request.setRequestDate("2021-02-24T03:28:00.567Z");
    request.setDebitParty(
        List.of(new MvolaCallBackRequest.MvolaPartyEntry("msisdn", CUSTOMER_MSISDN)));
    request.setCreditParty(
        List.of(new MvolaCallBackRequest.MvolaPartyEntry("msisdn", MVOLA_MSISDN)));
    request.setFees(List.of(new MvolaCallBackRequest.MvolaFeeEntry("5.46")));
    return request;
  }

  private MvolaCallBackRequest buildFailedCallbackRequest() {
    final MvolaCallBackRequest request = new MvolaCallBackRequest();
    request.setServerCorrelationId(CORRELATION_ID);
    request.setTransactionStatus("failed");
    request.setTransactionReference("641235");
    request.setRequestDate("2021-02-24T03:28:00.567Z");
    request.setDebitParty(
        List.of(new MvolaCallBackRequest.MvolaPartyEntry("msisdn", CUSTOMER_MSISDN)));
    request.setCreditParty(
        List.of(new MvolaCallBackRequest.MvolaPartyEntry("msisdn", MVOLA_MSISDN)));
    return request;
  }
}
