package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.client.airtel.AbstractAirtelMoneyTestBase.SAMPLE_MSISDN;
import static com.devikapps.vaikaparts.client.airtel.AbstractAirtelMoneyTestBase.TEST_MSISDN;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.MGA;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.AIRTEL_MONEY;
import static com.devikapps.vaikaparts.service.util.Paginator.PAGE_FIELD;
import static com.devikapps.vaikaparts.service.util.Paginator.SIZE_FIELD;
import static dev.razafindratelo.airtel_money_client.model.TransactionStatus.TS;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.AirtelMoneyCallBackRequest;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.AirtelMoneyCallBackTransaction;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.PaymentVerificationRequested;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.gateway.airtelmoney.AirtelMoneyPaymentGateway;
import com.devikapps.vaikaparts.mapper.AirtelMoneyPaymentMapper;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.AirtelMoneyPayment;
import com.devikapps.vaikaparts.model.AirtelMoneyPaymentRequest;
import com.devikapps.vaikaparts.model.AirtelMoneyPaymentResponse;
import com.devikapps.vaikaparts.model.Payment;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JAirtelMoneyPayment;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import com.devikapps.vaikaparts.service.util.Paginator;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AirtelMoneyPaymentServiceTest {

  private static final String PARTNER_TRANSACTION_ID = "airtel-txn-001";
  @Mock private PaymentGatewayFactory gatewayFactory;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentRequestedRepository paymentRequestedRepository;
  @Mock private EventProducer<PaymentVerificationRequested> eventProducer;
  @Mock private PaymentPartyMapper paymentPartyMapper;
  @Mock private AirtelMoneyPaymentMapper airtelMoneyPaymentMapper;
  @Mock private PaymentPartyRepository paymentPartyRepository;
  @Mock private Paginator paginator;
  @Mock private AirtelMoneyPaymentGateway airtelGateway;
  private AirtelMoneyPaymentService subject;

  @BeforeEach
  void set_up() {
    TransactionSynchronizationManager.initSynchronization();
    subject =
        new AirtelMoneyPaymentService(
            gatewayFactory,
            paymentRepository,
            paymentRequestedRepository,
            eventProducer,
            paymentPartyMapper,
            airtelMoneyPaymentMapper,
            paymentPartyRepository,
            paginator);
  }

  @AfterEach
  void tear_down() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void should_save_payment_with_pending_status_on_first_save() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();

    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    ArgumentCaptor<JAirtelMoneyPayment> captor = ArgumentCaptor.forClass(JAirtelMoneyPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());

    JAirtelMoneyPayment firstSave = captor.getAllValues().getFirst();
    assertEquals(VerificationStatus.PENDING, firstSave.getStatus());
    assertEquals(AIRTEL_MONEY, firstSave.getProvider());
    assertEquals(MGA, firstSave.getCurrency());
    assertEquals(request.getAmount(), firstSave.getAmount());
    assertEquals(request.getDescription(), firstSave.getDescription());
  }

  @Test
  void should_save_payment_with_correct_type() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    ArgumentCaptor<JAirtelMoneyPayment> captor = ArgumentCaptor.forClass(JAirtelMoneyPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    assertEquals(request.getType(), captor.getAllValues().getFirst().getType());
  }

  @Test
  void should_set_created_at_and_updated_at_on_payment_creation() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();

    var before = now().minusSeconds(1);
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);
    var after = now().plusSeconds(1);

    ArgumentCaptor<JAirtelMoneyPayment> captor = ArgumentCaptor.forClass(JAirtelMoneyPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    JAirtelMoneyPayment payment = captor.getAllValues().getFirst();

    assertTrue(payment.getCreatedAt().isAfter(before) && payment.getCreatedAt().isBefore(after));
    assertTrue(payment.getUpdatedAt().isAfter(before) && payment.getUpdatedAt().isBefore(after));
  }

  @Test
  void should_update_transaction_id_from_gateway_response_on_last_save() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    ArgumentCaptor<JAirtelMoneyPayment> captor = ArgumentCaptor.forClass(JAirtelMoneyPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    assertEquals(PARTNER_TRANSACTION_ID, captor.getAllValues().getLast().getTransactionId());
  }

  @Test
  void should_save_payment_at_least_twice_on_successful_initiation() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    verify(paymentRepository, atLeast(2)).save(any(JAirtelMoneyPayment.class));
  }

  @Test
  void should_reuse_existing_payer_when_phone_number_already_exists() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    JPaymentParty existingPayer = buildJPaymentParty(request.getPayer().getPhoneNumber());

    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber(request.getPayer().getPhoneNumber()))
        .thenReturn(Optional.of(existingPayer));
    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber(request.getPayee().getPhoneNumber()))
        .thenReturn(Optional.empty());
    when(paymentPartyMapper.toPersistence(request.getPayee())).thenReturn(new JPaymentParty());
    when(paymentPartyRepository.save(any(JPaymentParty.class))).thenReturn(new JPaymentParty());
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    verify(paymentPartyMapper, never()).toPersistence(request.getPayer());
  }

  @Test
  void should_create_new_payer_when_phone_number_does_not_exist() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    verify(paymentPartyMapper, times(1)).toPersistence(request.getPayer());
    verify(paymentPartyMapper, times(1)).toPersistence(request.getPayee());
    verify(paymentPartyRepository, times(2)).save(any(JPaymentParty.class));
  }

  @Test
  void should_save_verification_log_with_pending_status() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    ArgumentCaptor<JPaymentVerificationRequested> captor =
        ArgumentCaptor.forClass(JPaymentVerificationRequested.class);
    verify(paymentRequestedRepository).save(captor.capture());
    assertEquals(VerificationStatus.PENDING, captor.getValue().getStatus());
  }

  @Test
  void should_save_verification_log_with_max_attempt_nb_5() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    ArgumentCaptor<JPaymentVerificationRequested> captor =
        ArgumentCaptor.forClass(JPaymentVerificationRequested.class);
    verify(paymentRequestedRepository).save(captor.capture());
    assertEquals(5, captor.getValue().getMaxVerificationAttemptNb());
  }

  @Test
  void should_save_verification_log_linked_to_correct_payment() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    JAirtelMoneyPayment savedPayment = stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    ArgumentCaptor<JPaymentVerificationRequested> captor =
        ArgumentCaptor.forClass(JPaymentVerificationRequested.class);
    verify(paymentRequestedRepository).save(captor.capture());
    assertEquals(savedPayment.getId(), captor.getValue().getPayment().getId());
  }

  @Test
  void should_save_verification_log_before_calling_gateway() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    InOrder order = inOrder(paymentRequestedRepository, airtelGateway);
    order.verify(paymentRequestedRepository).save(any(JPaymentVerificationRequested.class));
    order.verify(airtelGateway).initiatePayment(any());
  }

  @Test
  void should_not_publish_event_before_commit() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    verify(eventProducer, never()).accept(any());
  }

  @Test
  void should_publish_event_after_commit_with_correct_payment_id() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    JAirtelMoneyPayment savedPayment = stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);
    flushSynchronizations();

    ArgumentCaptor<List<PaymentVerificationRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals(savedPayment.getId(), captor.getValue().getFirst().getPaymentId());
  }

  @Test
  void should_publish_exactly_one_event_after_commit() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);
    flushSynchronizations();

    ArgumentCaptor<List<PaymentVerificationRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals(1, captor.getValue().size());
  }

  @Test
  void should_publish_event_with_non_blank_id_after_commit() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);
    flushSynchronizations();

    ArgumentCaptor<List<PaymentVerificationRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    String eventId = captor.getValue().getFirst().getId();
    assertFalse(eventId == null || eventId.isBlank());
  }

  @Test
  void should_publish_event_with_max_attempt_nb_5_after_commit() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);
    flushSynchronizations();

    ArgumentCaptor<List<PaymentVerificationRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals(5, captor.getValue().getFirst().getMaxVerificationAttemptNb());
  }

  @Test
  void should_delegate_to_airtel_money_gateway_on_initiation() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate();
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    subject.initiatePayment(request);

    verify(airtelGateway, times(1)).initiatePayment(request);
  }

  @Test
  void should_return_mapped_payment_from_initiation() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    JAirtelMoneyPayment savedPayment = stubPaymentSave();
    stubGatewayInitiate();

    AirtelMoneyPayment expected = AirtelMoneyPayment.builder().build();
    when(airtelMoneyPaymentMapper.toModel(savedPayment)).thenReturn(expected);
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    Payment result = subject.initiatePayment(request);

    assertSame(expected, result);
  }

  @Test
  void should_propagate_payment_gateway_exception_from_gateway() {
    AirtelMoneyPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    when(airtelGateway.initiatePayment(any()))
        .thenThrow(new PaymentGatewayException("Gateway error"));
    when(gatewayFactory.getGateway(AIRTEL_MONEY)).thenReturn(airtelGateway);
    assertThrows(PaymentGatewayException.class, () -> subject.initiatePayment(request));
  }

  @Test
  void should_return_mapped_payment_on_get_payment() {
    JAirtelMoneyPayment jPayment = buildJPayment();
    AirtelMoneyPayment expected = AirtelMoneyPayment.builder().build();

    when(paymentRepository.findJPaymentByTransactionId(PARTNER_TRANSACTION_ID))
        .thenReturn(Optional.of(jPayment));
    when(airtelMoneyPaymentMapper.toModel(jPayment)).thenReturn(expected);

    assertSame(expected, subject.getPayment(PARTNER_TRANSACTION_ID));
  }

  @Test
  void should_throw_entity_not_found_when_payment_does_not_exist() {
    when(paymentRepository.findJPaymentByTransactionId(any())).thenReturn(Optional.empty());

    final var nonExistendId = "non-existent-id";

    EntityNotFoundException ex =
        assertThrows(EntityNotFoundException.class, () -> subject.getPayment(nonExistendId));

    assertTrue(ex.getMessage().contains(nonExistendId));
  }

  @Test
  void should_query_repository_with_exact_transaction_id() {
    JAirtelMoneyPayment jPayment = buildJPayment();
    when(paymentRepository.findJPaymentByTransactionId(PARTNER_TRANSACTION_ID))
        .thenReturn(Optional.of(jPayment));
    when(airtelMoneyPaymentMapper.toModel(jPayment))
        .thenReturn(AirtelMoneyPayment.builder().build());

    subject.getPayment(PARTNER_TRANSACTION_ID);

    verify(paymentRepository, times(1)).findJPaymentByTransactionId(PARTNER_TRANSACTION_ID);
  }

  @Test
  void should_delegate_to_mapper_on_get_payment() {
    JAirtelMoneyPayment jPayment = buildJPayment();
    when(paymentRepository.findJPaymentByTransactionId(PARTNER_TRANSACTION_ID))
        .thenReturn(Optional.of(jPayment));
    when(airtelMoneyPaymentMapper.toModel(jPayment))
        .thenReturn(AirtelMoneyPayment.builder().build());

    subject.getPayment(PARTNER_TRANSACTION_ID);

    verify(airtelMoneyPaymentMapper, times(1)).toModel(jPayment);
  }

  @Test
  void should_update_payment_status_to_success_on_ts_callback() {
    JAirtelMoneyPayment payment = buildJPayment();
    when(paymentRepository.findJPaymentByTransactionId(PARTNER_TRANSACTION_ID))
        .thenReturn(Optional.of(payment));

    subject.handleCallBack(
        buildCallbackRequest(PARTNER_TRANSACTION_ID, TS.toString(), "AIRTEL-123"));

    ArgumentCaptor<JAirtelMoneyPayment> captor = ArgumentCaptor.forClass(JAirtelMoneyPayment.class);
    verify(paymentRepository).save(captor.capture());
    assertEquals(VerificationStatus.SUCCESS, captor.getValue().getStatus());
  }

  @Test
  void should_update_payment_status_to_failed_on_tf_callback() {
    JAirtelMoneyPayment payment = buildJPayment();
    when(paymentRepository.findJPaymentByTransactionId(PARTNER_TRANSACTION_ID))
        .thenReturn(Optional.of(payment));

    subject.handleCallBack(buildCallbackRequest(PARTNER_TRANSACTION_ID, "TF", null));

    ArgumentCaptor<JAirtelMoneyPayment> captor = ArgumentCaptor.forClass(JAirtelMoneyPayment.class);
    verify(paymentRepository).save(captor.capture());
    assertEquals(VerificationStatus.FAILED, captor.getValue().getStatus());
  }

  @Test
  void should_update_payment_status_to_pending_on_unknown_status_callback() {
    JAirtelMoneyPayment payment = buildJPayment();
    when(paymentRepository.findJPaymentByTransactionId(PARTNER_TRANSACTION_ID))
        .thenReturn(Optional.of(payment));

    subject.handleCallBack(buildCallbackRequest(PARTNER_TRANSACTION_ID, "UNKNOWN", null));

    ArgumentCaptor<JAirtelMoneyPayment> captor = ArgumentCaptor.forClass(JAirtelMoneyPayment.class);
    verify(paymentRepository).save(captor.capture());
    assertEquals(VerificationStatus.PENDING, captor.getValue().getStatus());
  }

  @Test
  void should_store_airtel_money_id_from_callback() {
    JAirtelMoneyPayment payment = buildJPayment();
    when(paymentRepository.findJPaymentByTransactionId(PARTNER_TRANSACTION_ID))
        .thenReturn(Optional.of(payment));

    subject.handleCallBack(
        buildCallbackRequest(PARTNER_TRANSACTION_ID, TS.toString(), "AIRTEL-XYZ-999"));

    ArgumentCaptor<JAirtelMoneyPayment> captor = ArgumentCaptor.forClass(JAirtelMoneyPayment.class);
    verify(paymentRepository).save(captor.capture());
    assertEquals("AIRTEL-XYZ-999", captor.getValue().getAirtelMoneyId());
  }

  @Test
  void should_set_updated_at_on_callback() {
    JAirtelMoneyPayment payment = buildJPayment();
    when(paymentRepository.findJPaymentByTransactionId(PARTNER_TRANSACTION_ID))
        .thenReturn(Optional.of(payment));

    var before = now().minusSeconds(1);
    subject.handleCallBack(
        buildCallbackRequest(PARTNER_TRANSACTION_ID, TS.toString(), "AIRTEL-123"));
    var after = now().plusSeconds(1);

    ArgumentCaptor<JAirtelMoneyPayment> captor = ArgumentCaptor.forClass(JAirtelMoneyPayment.class);
    verify(paymentRepository).save(captor.capture());
    assertTrue(
        captor.getValue().getUpdatedAt().isAfter(before)
            && captor.getValue().getUpdatedAt().isBefore(after));
  }

  @Test
  void should_throw_entity_not_found_on_callback_when_payment_does_not_exist() {
    when(paymentRepository.findJPaymentByTransactionId(any())).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () -> subject.handleCallBack(buildCallbackRequest("non-existent-id", TS.toString(), null)));
  }

  @Test
  void should_return_page_of_airtel_money_payments_for_given_msisdn() {
    JAirtelMoneyPayment jPayment = buildJPayment();
    AirtelMoneyPayment expected = AirtelMoneyPayment.builder().build();
    Page<JPayment> page = new PageImpl<>(List.of(jPayment));

    stubPaginator();
    when(paymentRepository.findAllByPayer_PhoneNumberAndProvider(
            eq(TEST_MSISDN), eq(AIRTEL_MONEY), any()))
        .thenReturn(page);
    when(airtelMoneyPaymentMapper.toModel(jPayment)).thenReturn(expected);

    Page<AirtelMoneyPayment> result = subject.findPaymentsByPaymentPartyMsisdn(TEST_MSISDN, 0, 10);

    assertEquals(1, result.getTotalElements());
    assertSame(expected, result.getContent().getFirst());
  }

  @Test
  void should_return_empty_page_when_no_payments_found_for_msisdn() {
    stubPaginator();
    when(paymentRepository.findAllByPayer_PhoneNumberAndProvider(any(), eq(AIRTEL_MONEY), any()))
        .thenReturn(Page.empty());

    Page<AirtelMoneyPayment> result = subject.findPaymentsByPaymentPartyMsisdn("000000000", 0, 10);

    assertTrue(result.isEmpty());
  }

  @Test
  void should_delegate_to_repository_with_airtel_money_provider() {
    stubPaginator();
    when(paymentRepository.findAllByPayer_PhoneNumberAndProvider(
            eq(TEST_MSISDN), eq(AIRTEL_MONEY), any()))
        .thenReturn(Page.empty());

    subject.findPaymentsByPaymentPartyMsisdn(TEST_MSISDN, 0, 10);

    verify(paymentRepository, times(1))
        .findAllByPayer_PhoneNumberAndProvider(eq(TEST_MSISDN), eq(AIRTEL_MONEY), any());
  }

  @Test
  void should_sort_payments_by_created_at_descending() {
    stubPaginator();
    when(paymentRepository.findAllByPayer_PhoneNumberAndProvider(any(), any(), any()))
        .thenReturn(Page.empty());

    subject.findPaymentsByPaymentPartyMsisdn(TEST_MSISDN, 0, 10);

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(paymentRepository).findAllByPayer_PhoneNumberAndProvider(any(), any(), captor.capture());
    assertEquals(Sort.by("createdAt").descending(), captor.getValue().getSort());
  }

  private void flushSynchronizations() {
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
  }

  private void stubPaginator() {
    when(paginator.apply(anyInt(), anyInt())).thenReturn(Map.of(PAGE_FIELD, 0, SIZE_FIELD, 10));
  }

  private AirtelMoneyPaymentRequest buildValidRequest() {
    return AirtelMoneyPaymentRequest.builder()
        .transactionId(PARTNER_TRANSACTION_ID)
        .amount(new BigDecimal("1000"))
        .currency(MGA)
        .description("Test Airtel Money payment")
        .provider(AIRTEL_MONEY)
        .type(PaymentType.PROFILE_UNLOCK)
        .payer(PaymentParty.builder().phoneNumber(TEST_MSISDN).build())
        .payee(PaymentParty.builder().phoneNumber(SAMPLE_MSISDN).build())
        .reference("Test reference")
        .build();
  }

  private JAirtelMoneyPayment stubPaymentSave() {
    JAirtelMoneyPayment payment =
        JAirtelMoneyPayment.builder()
            .id(randomUUID().toString())
            .status(VerificationStatus.PENDING)
            .provider(AIRTEL_MONEY)
            .currency(MGA)
            .amount(new BigDecimal("1000"))
            .description("Test Airtel Money payment")
            .type(PaymentType.PROFILE_UNLOCK)
            .createdAt(now())
            .updatedAt(now())
            .build();
    when(paymentRepository.save(any(JAirtelMoneyPayment.class))).thenReturn(payment);
    return payment;
  }

  private void stubPartyResolution(AirtelMoneyPaymentRequest request) {
    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber(request.getPayer().getPhoneNumber()))
        .thenReturn(Optional.empty());
    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber(request.getPayee().getPhoneNumber()))
        .thenReturn(Optional.empty());
    when(paymentPartyMapper.toPersistence(request.getPayer())).thenReturn(new JPaymentParty());
    when(paymentPartyMapper.toPersistence(request.getPayee())).thenReturn(new JPaymentParty());
    when(paymentPartyRepository.save(any(JPaymentParty.class))).thenReturn(new JPaymentParty());
  }

  private void stubGatewayInitiate() {
    AirtelMoneyPaymentResponse response = new AirtelMoneyPaymentResponse();
    response.setTransactionId(AirtelMoneyPaymentServiceTest.PARTNER_TRANSACTION_ID);
    response.setStatus(PaymentStatus.PENDING);
    response.setProvider(AIRTEL_MONEY);
    response.setRespondedAt(now());
    when(airtelGateway.initiatePayment(any())).thenReturn(response);
  }

  private JAirtelMoneyPayment buildJPayment() {
    return JAirtelMoneyPayment.builder()
        .id(randomUUID().toString())
        .transactionId(AirtelMoneyPaymentServiceTest.PARTNER_TRANSACTION_ID)
        .status(VerificationStatus.PENDING)
        .provider(AIRTEL_MONEY)
        .build();
  }

  private JPaymentParty buildJPaymentParty(String phoneNumber) {
    return JPaymentParty.builder().id(randomUUID().toString()).phoneNumber(phoneNumber).build();
  }

  private AirtelMoneyCallBackRequest buildCallbackRequest(
      String transactionId, String statusCode, String airtelMoneyId) {
    AirtelMoneyCallBackTransaction tx =
        AirtelMoneyCallBackTransaction.builder()
            .id(transactionId)
            .statusCode(statusCode)
            .airtelMoneyId(airtelMoneyId)
            .message("Test callback message")
            .build();
    return AirtelMoneyCallBackRequest.builder().transaction(tx).build();
  }
}
