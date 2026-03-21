package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.conf.EnvConf.MVOLA_MSISDN;
import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
import static com.devikapps.vaikaparts.model.classifier.PaymentStatus.PENDING;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.PaymentVerificationRequested;
import com.devikapps.vaikaparts.event.model.VerificationStatus;
import com.devikapps.vaikaparts.exception.PaymentGatewayException;
import com.devikapps.vaikaparts.gateway.PaymentGatewayFactory;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentGateway;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentRequest;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentResponse;
import com.devikapps.vaikaparts.mapper.MvolaPaymentMapper;
import com.devikapps.vaikaparts.mapper.PaymentPartyMapper;
import com.devikapps.vaikaparts.model.MvolaPayment;
import com.devikapps.vaikaparts.model.PaymentParty;
import com.devikapps.vaikaparts.model.PaymentResponse;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.PaymentRequestedRepository;
import com.devikapps.vaikaparts.repository.model.JMvolaPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class MvolaPaymentServiceTest {

  @Mock private PaymentGatewayFactory gatewayFactory;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentRequestedRepository paymentRequestedRepository;
  @Mock private EventProducer<PaymentVerificationRequested> eventProducer;
  @Mock private PaymentPartyMapper paymentPartyMapper;
  @Mock private MvolaPaymentMapper mvolaPaymentMapper;
  @Mock private MvolaPaymentGateway mvolaGateway;

  private MvolaPaymentService service;

  @BeforeEach
  void set_up() {
    TransactionSynchronizationManager.initSynchronization();
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            paymentRequestedRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);
  }

  @AfterEach
  void tear_down() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void should_save_payment_with_pending_status_on_first_save() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
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
    stubMappers(request);
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
    stubMappers(request);
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
    stubMappers(request);
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
    stubMappers(request);
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
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    verify(paymentRepository, atLeast(2)).save(any(JMvolaPayment.class));
  }

  @Test
  void should_save_payment_verification_requested_log_before_gateway_call() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
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
    stubMappers(request);
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
    stubMappers(request);
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
    stubMappers(request);
    final JMvolaPayment savedPayment = stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    final ArgumentCaptor<JPaymentVerificationRequested> captor =
        ArgumentCaptor.forClass(JPaymentVerificationRequested.class);
    verify(paymentRequestedRepository).save(captor.capture());
    assertEquals(savedPayment.getId(), captor.getValue().getPayment().getId());
  }

  @Test
  void should_publish_event_after_commit_with_correct_payment_id() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
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
    stubMappers(request);
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
    stubMappers(request);
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
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);
    flushSynchronizations();

    final ArgumentCaptor<List<PaymentVerificationRequested>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    final String eventId = captor.getValue().getFirst().getId();
    assertFalse(eventId == null || eventId.isBlank());
  }

  @Test
  void should_not_publish_event_before_commit() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    verify(eventProducer, never()).accept(any());
  }

  @Test
  void should_delegate_to_mvola_gateway_on_initiate_payment() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(randomUUID().toString());

    service.initiatePayment(request);

    verify(mvolaGateway, times(1)).initiatePayment(request);
  }

  @Test
  void should_return_response_with_transaction_id_equal_to_server_correlation_id() {
    final MvolaPaymentRequest request = buildValidRequest();
    final String correlationId = randomUUID().toString();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(correlationId);

    final PaymentResponse result = service.initiatePayment(request);

    assertEquals(
        correlationId,
        result.getTransactionId(),
        "response transactionId must be overwritten with serverCorrelationId");
  }

  @Test
  void should_propagate_payment_gateway_exception_from_gateway() {
    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
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

    final var result = service.getPayment(transactionId);

    assertSame(expected, result);
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

  private void flushSynchronizations() {
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
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

  private void stubMappers(final MvolaPaymentRequest request) {
    when(paymentPartyMapper.toPersistence(request.getPayer())).thenReturn(new JPaymentParty());
    when(paymentPartyMapper.toPersistence(request.getPayee())).thenReturn(new JPaymentParty());
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
}
