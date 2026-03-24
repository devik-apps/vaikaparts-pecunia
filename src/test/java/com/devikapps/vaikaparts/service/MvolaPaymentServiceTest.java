package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.client.mvola.MvolaApiTestBase.PARTNER_NAME;
import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_BASE_URL;
import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_BASE_URL_TOKEN;
import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_MSISDN;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static com.devikapps.vaikaparts.model.classifier.PaymentStatus.PENDING;
import static com.devikapps.vaikaparts.service.util.Paginator.PAGE_FIELD;
import static com.devikapps.vaikaparts.service.util.Paginator.SIZE_FIELD;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.config.MvolaConf;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.MvolaCallBackRequest;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.PaymentVerificationRequested;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentGateway;
import com.devikapps.vaikaparts.mapper.MvolaPaymentMapper;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.MvolaPayment;
import com.devikapps.vaikaparts.model.MvolaPaymentRequest;
import com.devikapps.vaikaparts.model.MvolaPaymentResponse;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.repository.PaymentPartyRepository;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JMvolaPayment;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import com.devikapps.vaikaparts.service.util.Paginator;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@ExtendWith(MockitoExtension.class)
class MvolaPaymentServiceTest {

  @Mock private PaymentGatewayFactory gatewayFactory;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentRequestedRepository paymentRequestedRepository;
  @Mock private EventProducer<PaymentVerificationRequested> eventProducer;
  @Mock private PaymentPartyMapper paymentPartyMapper;
  @Mock private MvolaPaymentMapper mvolaPaymentMapper;
  @Mock private PaymentPartyRepository paymentPartyRepository;
  @Mock private Paginator paginator;
  @Mock private MvolaPaymentGateway mvolaGateway;

  private MvolaPaymentService service;

  @BeforeEach
  void set_up() {
    TransactionSynchronizationManager.initSynchronization();

    MvolaConf conf =
        MvolaConf.builder()
            .baseUrl(MVOLA_BASE_URL)
            .callbackUrl("https://example.com/callback")
            .partnerMsisdn(MVOLA_MSISDN)
            .partnerName(PARTNER_NAME)
            .consumerKey(randomUUID().toString())
            .consumerSecret(randomUUID().toString())
            .tokenUrl(MVOLA_BASE_URL_TOKEN)
            .build();

    service =
        new MvolaPaymentService(
            conf,
            gatewayFactory,
            paymentRepository,
            paymentRequestedRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper,
            paymentPartyRepository,
            paginator);
  }

  @AfterEach
  void tear_down() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void should_save_payment_with_pending_status_on_first_save() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());

    final JMvolaPayment firstSave = captor.getAllValues().getFirst();
    assertEquals(VerificationStatus.PENDING, firstSave.getStatus());
    assertEquals(MVOLA, firstSave.getProvider());
    assertEquals(AR, firstSave.getCurrency());
    assertEquals(request.getAmount(), firstSave.getAmount());
    assertEquals(request.getDescription(), firstSave.getDescription());
  }

  @Test
  void should_save_payment_with_correct_type() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    assertEquals(request.getType(), captor.getAllValues().getFirst().getType());
  }

  @Test
  void should_set_created_at_and_updated_at_on_payment_creation() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    final LocalDateTime before = now().minusSeconds(1);
    service.initiatePayment(request);
    final LocalDateTime after = now().plusSeconds(1);

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    final JMvolaPayment payment = captor.getAllValues().getFirst();

    assertTrue(payment.getCreatedAt().isAfter(before) && payment.getCreatedAt().isBefore(after));
    assertTrue(payment.getUpdatedAt().isAfter(before) && payment.getUpdatedAt().isBefore(after));
  }

  @Test
  void should_update_payment_transaction_id_to_server_correlation_id_on_last_save() {
    final MvolaPaymentRequest request = buildValidRequest();
    final String correlationId = randomUUID().toString();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(correlationId);

    service.initiatePayment(request);

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    assertEquals(correlationId, captor.getAllValues().getLast().getTransactionId());
  }

  @Test
  void should_update_server_correlation_id_on_last_save() {
    final MvolaPaymentRequest request = buildValidRequest();
    final String correlationId = randomUUID().toString();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(correlationId);

    service.initiatePayment(request);

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    assertEquals(correlationId, captor.getAllValues().getLast().getServerCorrelationId());
  }

  @Test
  void should_save_payment_at_least_twice_on_successful_initiate() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    verify(paymentRepository, atLeast(2)).save(any(JMvolaPayment.class));
  }

  @Test
  void should_reuse_existing_payer_when_phone_number_already_exists() {
    final MvolaPaymentRequest request = buildValidRequest();
    final JPaymentParty existingPayer = buildJPaymentParty(request.getPayer().getPhoneNumber());

    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber(request.getPayer().getPhoneNumber()))
        .thenReturn(Optional.of(existingPayer));
    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber(MVOLA_MSISDN))
        .thenReturn(Optional.empty());
    when(paymentPartyMapper.toPersistence(argThat(p -> MVOLA_MSISDN.equals(p.getPhoneNumber()))))
        .thenReturn(new JPaymentParty());
    when(paymentPartyRepository.save(any(JPaymentParty.class))).thenReturn(new JPaymentParty());
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    // payer already exists — must not be saved again
    verify(paymentPartyMapper, never()).toPersistence(request.getPayer());
  }

  @Test
  void should_create_new_payer_when_phone_number_does_not_exist() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    // both parties not found — both must be mapped and saved
    verify(paymentPartyMapper, times(1)).toPersistence(request.getPayer());
    verify(paymentPartyMapper, times(1))
        .toPersistence(argThat(p -> MVOLA_MSISDN.equals(p.getPhoneNumber())));
    verify(paymentPartyRepository, times(2)).save(any(JPaymentParty.class));
  }

  @Test
  void should_save_payment_verification_requested_log_before_gateway_call() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    final InOrder order = inOrder(paymentRequestedRepository, mvolaGateway);
    order.verify(paymentRequestedRepository).save(any(JPaymentVerificationRequested.class));
    order.verify(mvolaGateway).initiatePayment(any());
  }

  @Test
  void should_save_payment_verification_requested_log_with_pending_status() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    final ArgumentCaptor<JPaymentVerificationRequested> captor =
        ArgumentCaptor.forClass(JPaymentVerificationRequested.class);
    verify(paymentRequestedRepository).save(captor.capture());
    assertEquals(VerificationStatus.PENDING, captor.getValue().getStatus());
  }

  @Test
  void should_save_payment_verification_requested_log_with_max_attempt_nb_5() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    final ArgumentCaptor<JPaymentVerificationRequested> captor =
        ArgumentCaptor.forClass(JPaymentVerificationRequested.class);
    verify(paymentRequestedRepository).save(captor.capture());
    assertEquals(5, captor.getValue().getMaxVerificationAttemptNb());
  }

  @Test
  void should_save_payment_verification_requested_log_linked_to_payment() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    final JMvolaPayment savedPayment = stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    final ArgumentCaptor<JPaymentVerificationRequested> captor =
        ArgumentCaptor.forClass(JPaymentVerificationRequested.class);
    verify(paymentRequestedRepository).save(captor.capture());
    assertEquals(savedPayment.getId(), captor.getValue().getPayment().getId());
  }

  @Test
  void should_not_publish_event_before_commit() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    verify(eventProducer, never()).accept(any());
  }

  @Test
  void should_publish_event_after_commit_with_correct_payment_id() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    final JMvolaPayment savedPayment = stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);
    flushSynchronizations();

    final ArgumentCaptor<List<PaymentVerificationRequested>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals(savedPayment.getId(), captor.getValue().getFirst().getPaymentId());
  }

  @Test
  void should_publish_event_with_max_verification_attempt_nb_of_5() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);
    flushSynchronizations();

    final ArgumentCaptor<List<PaymentVerificationRequested>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals(5, captor.getValue().getFirst().getMaxVerificationAttemptNb());
  }

  @Test
  void should_publish_a_list_containing_exactly_one_event() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);
    flushSynchronizations();

    final ArgumentCaptor<List<PaymentVerificationRequested>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals(1, captor.getValue().size());
  }

  @Test
  void should_publish_event_with_non_blank_id() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);
    flushSynchronizations();

    final ArgumentCaptor<List<PaymentVerificationRequested>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertFalse(
        captor.getValue().getFirst().getId() == null
            || captor.getValue().getFirst().getId().isBlank());
  }

  @Test
  void should_delegate_to_mvola_gateway_on_initiate_payment() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    verify(mvolaGateway, times(1)).initiatePayment(request);
  }

  @Test
  void should_return_mapped_payment_from_initiate_payment() {
    final MvolaPaymentRequest request = buildValidRequest();
    final String correlationId = randomUUID().toString();
    stubPartyResolution(request);
    stubPaymentSave();
    stubGatewayInitiate(correlationId);
    when(mvolaPaymentMapper.toModel(any(JMvolaPayment.class)))
        .thenReturn(MvolaPayment.builder().transactionId(correlationId).build());

    final var result = service.initiatePayment(request);

    assertEquals(correlationId, result.getTransactionId());
  }

  @Test
  void should_propagate_payment_gateway_exception_from_gateway() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubPartyResolution(request);
    stubPaymentSave();
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    when(mvolaGateway.initiatePayment(any()))
        .thenThrow(new PaymentGatewayException("MVola initiatePayment failed"));

    assertThrows(PaymentGatewayException.class, () -> service.initiatePayment(request));
  }

  @Test
  void should_return_mvola_payment_mapped_from_persistence() {
    final String transactionId = randomUUID().toString();
    final JMvolaPayment jPayment = buildJMvolaPayment(transactionId);
    final MvolaPayment expected = MvolaPayment.builder().build();

    when(paymentRepository.findJPaymentByTransactionId(transactionId))
        .thenReturn(Optional.of(jPayment));
    when(mvolaPaymentMapper.toModel(jPayment)).thenReturn(expected);

    assertSame(expected, service.getPayment(transactionId));
  }

  @Test
  void should_throw_entity_not_found_when_payment_does_not_exist() {
    when(paymentRepository.findJPaymentByTransactionId(any())).thenReturn(Optional.empty());

    final EntityNotFoundException ex =
        assertThrows(EntityNotFoundException.class, () -> service.getPayment("non-existent-id"));

    assertTrue(ex.getMessage().contains("non-existent-id"));
  }

  @Test
  void should_query_repository_with_exact_transaction_id_on_get_payment() {
    final String transactionId = "tx-abc-123";
    final JMvolaPayment jPayment = buildJMvolaPayment(transactionId);
    when(paymentRepository.findJPaymentByTransactionId(transactionId))
        .thenReturn(Optional.of(jPayment));
    when(mvolaPaymentMapper.toModel(jPayment)).thenReturn(MvolaPayment.builder().build());

    service.getPayment(transactionId);

    verify(paymentRepository, times(1)).findJPaymentByTransactionId(transactionId);
  }

  @Test
  void should_delegate_to_mvola_payment_mapper_on_get_payment() {
    final String transactionId = randomUUID().toString();
    final JMvolaPayment jPayment = buildJMvolaPayment(transactionId);
    when(paymentRepository.findJPaymentByTransactionId(transactionId))
        .thenReturn(Optional.of(jPayment));
    when(mvolaPaymentMapper.toModel(jPayment)).thenReturn(MvolaPayment.builder().build());

    service.getPayment(transactionId);

    verify(mvolaPaymentMapper, times(1)).toModel(jPayment);
  }

  @Test
  void should_update_payment_status_to_success_on_completed_callback() {
    final JMvolaPayment payment = buildJMvolaPayment(randomUUID().toString());
    when(paymentRepository.findJPaymentByTransactionId(payment.getTransactionId()))
        .thenReturn(Optional.of(payment));

    service.handleCallBack(buildCallbackRequest(payment.getTransactionId(), "completed", "TX123"));

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
    verify(paymentRepository).save(captor.capture());
    assertEquals(VerificationStatus.SUCCESS, captor.getValue().getStatus());
  }

  @Test
  void should_update_payment_status_to_failed_on_failed_callback() {
    final JMvolaPayment payment = buildJMvolaPayment(randomUUID().toString());
    when(paymentRepository.findJPaymentByTransactionId(payment.getTransactionId()))
        .thenReturn(Optional.of(payment));

    service.handleCallBack(buildCallbackRequest(payment.getTransactionId(), "failed", "TX123"));

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
    verify(paymentRepository).save(captor.capture());
    assertEquals(VerificationStatus.FAILED, captor.getValue().getStatus());
  }

  @Test
  void should_update_payment_status_to_pending_on_unknown_callback_status() {
    final JMvolaPayment payment = buildJMvolaPayment(randomUUID().toString());
    when(paymentRepository.findJPaymentByTransactionId(payment.getTransactionId()))
        .thenReturn(Optional.of(payment));

    service.handleCallBack(buildCallbackRequest(payment.getTransactionId(), "unknown", "TX123"));

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
    verify(paymentRepository).save(captor.capture());
    assertEquals(VerificationStatus.PENDING, captor.getValue().getStatus());
  }

  @Test
  void should_store_mvola_transaction_id_on_callback() {
    final JMvolaPayment payment = buildJMvolaPayment(randomUUID().toString());
    when(paymentRepository.findJPaymentByTransactionId(payment.getTransactionId()))
        .thenReturn(Optional.of(payment));

    service.handleCallBack(
        buildCallbackRequest(payment.getTransactionId(), "completed", "TX-REF-999"));

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
    verify(paymentRepository).save(captor.capture());
    assertEquals("TX-REF-999", captor.getValue().getMvolaTransactionId());
  }

  @Test
  void should_set_updated_at_on_callback() {
    final JMvolaPayment payment = buildJMvolaPayment(randomUUID().toString());
    when(paymentRepository.findJPaymentByTransactionId(payment.getTransactionId()))
        .thenReturn(Optional.of(payment));

    final LocalDateTime before = now().minusSeconds(1);
    service.handleCallBack(buildCallbackRequest(payment.getTransactionId(), "completed", "TX123"));
    final LocalDateTime after = now().plusSeconds(1);

    final ArgumentCaptor<JMvolaPayment> captor = ArgumentCaptor.forClass(JMvolaPayment.class);
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
        () ->
            service.handleCallBack(
                buildCallbackRequest("non-existent-correlation-id", "completed", "TX123")));
  }

  @Test
  void should_return_page_of_mvola_payments_for_given_msisdn() {
    final String msisdn = "0343500003";
    final JMvolaPayment jPayment = buildJMvolaPayment(randomUUID().toString());
    final MvolaPayment expected = MvolaPayment.builder().build();
    final Page<JPayment> page = new PageImpl<>(List.of(jPayment));

    stubPaginator();
    when(paymentRepository.findAllByPayer_PhoneNumberAndProvider(eq(msisdn), eq(MVOLA), any()))
        .thenReturn(page);
    when(mvolaPaymentMapper.toModel(jPayment)).thenReturn(expected);

    final Page<MvolaPayment> result = service.findPaymentsByPaymentPartyMsisdn(msisdn, 0, 10);

    assertEquals(1, result.getTotalElements());
    assertSame(expected, result.getContent().getFirst());
  }

  @Test
  void should_return_empty_page_when_no_payments_found_for_msisdn() {
    stubPaginator();
    when(paymentRepository.findAllByPayer_PhoneNumberAndProvider(any(), eq(MVOLA), any()))
        .thenReturn(Page.empty());

    final Page<MvolaPayment> result = service.findPaymentsByPaymentPartyMsisdn("0343500099", 0, 10);

    assertTrue(result.isEmpty());
  }

  @Test
  void should_delegate_to_repository_with_mvola_provider_on_find_by_msisdn() {
    final String msisdn = "0343500003";
    stubPaginator();
    when(paymentRepository.findAllByPayer_PhoneNumberAndProvider(eq(msisdn), eq(MVOLA), any()))
        .thenReturn(Page.empty());

    service.findPaymentsByPaymentPartyMsisdn(msisdn, 0, 10);

    verify(paymentRepository, times(1))
        .findAllByPayer_PhoneNumberAndProvider(eq(msisdn), eq(MVOLA), any());
  }

  @Test
  void should_sort_payments_by_created_at_descending() {
    stubPaginator();
    when(paymentRepository.findAllByPayer_PhoneNumberAndProvider(any(), any(), any()))
        .thenReturn(Page.empty());

    service.findPaymentsByPaymentPartyMsisdn("0343500003", 0, 10);

    final ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
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

  private MvolaPaymentRequest buildValidRequest() {
    return MvolaPaymentRequest.builder()
        .transactionId(randomUUID().toString())
        .amount(new BigDecimal("5000"))
        .currency(AR)
        .description("Test payment")
        .provider(MVOLA)
        .type(PaymentType.PROFILE_UNLOCK)
        .payer(PaymentParty.builder().phoneNumber("0343500003").build())
        .payee(PaymentParty.builder().phoneNumber(MVOLA_MSISDN).build())
        .build();
  }

  private JMvolaPayment stubPaymentSave() {
    final JMvolaPayment payment =
        JMvolaPayment.builder()
            .id(randomUUID().toString())
            .status(VerificationStatus.PENDING)
            .provider(MVOLA)
            .currency(AR)
            .amount(new BigDecimal("5000"))
            .description("Test payment")
            .type(PaymentType.PROFILE_UNLOCK)
            .createdAt(now())
            .updatedAt(now())
            .build();
    when(paymentRepository.save(any(JMvolaPayment.class))).thenReturn(payment);
    return payment;
  }

  private void stubPartyResolution(final MvolaPaymentRequest request) {
    // parties not found — service creates new ones
    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber(request.getPayer().getPhoneNumber()))
        .thenReturn(Optional.empty());
    when(paymentPartyRepository.findJPaymentPartyByPhoneNumber(MVOLA_MSISDN))
        .thenReturn(Optional.empty());
    when(paymentPartyMapper.toPersistence(request.getPayer())).thenReturn(new JPaymentParty());
    when(paymentPartyMapper.toPersistence(argThat(p -> MVOLA_MSISDN.equals(p.getPhoneNumber()))))
        .thenReturn(new JPaymentParty());
    when(paymentPartyRepository.save(any(JPaymentParty.class))).thenReturn(new JPaymentParty());
  }

  private void stubGatewayInitiate(final String serverCorrelationId) {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    when(mvolaGateway.initiatePayment(any())).thenReturn(buildPendingResponse(serverCorrelationId));
  }

  private MvolaPaymentResponse buildPendingResponse(final String serverCorrelationId) {
    return MvolaPaymentResponse.builder()
        .transactionId(serverCorrelationId)
        .status(PENDING)
        .provider(MVOLA)
        .serverCorrelationId(serverCorrelationId)
        .notificationMethod("polling")
        .respondedAt(now())
        .build();
  }

  private JMvolaPayment buildJMvolaPayment(final String transactionId) {
    return JMvolaPayment.builder()
        .id(randomUUID().toString())
        .transactionId(transactionId)
        .status(VerificationStatus.PENDING)
        .provider(MVOLA)
        .build();
  }

  private JPaymentParty buildJPaymentParty(final String phoneNumber) {
    return JPaymentParty.builder().id(randomUUID().toString()).phoneNumber(phoneNumber).build();
  }

  private MvolaCallBackRequest buildCallbackRequest(
      final String serverCorrelationId, final String status, final String transactionReference) {
    final MvolaCallBackRequest request = new MvolaCallBackRequest();
    request.setServerCorrelationId(serverCorrelationId);
    request.setTransactionStatus(status);
    request.setTransactionReference(transactionReference);
    return request;
  }
}
