package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.PaymentCurrency.AR;
import static com.devikapps.vaikaparts.model.classifier.PaymentProvider.MVOLA;
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
import com.devikapps.vaikaparts.model.classifier.PaymentStatus;
import com.devikapps.vaikaparts.model.classifier.PaymentType;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import com.devikapps.vaikaparts.repository.model.JMvolaPayment;
import com.devikapps.vaikaparts.repository.model.JPayment;
import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MvolaPaymentServiceTest {

  @Mock private PaymentGatewayFactory gatewayFactory;
  @Mock private PaymentRepository paymentRepository;
  @Mock private EventProducer<PaymentVerificationRequested> eventProducer;
  @Mock private PaymentPartyMapper paymentPartyMapper;
  @Mock private MvolaPaymentMapper mvolaPaymentMapper;
  @Mock private MvolaPaymentGateway mvolaGateway;

  private MvolaPaymentService service;

  @Test
  void should_save_payment_with_pending_status_before_calling_gateway() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    service.initiatePayment(request);

    final ArgumentCaptor<JPayment> captor = ArgumentCaptor.forClass(JPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());

    final JPayment firstSave = captor.getAllValues().getFirst();
    assertEquals(VerificationStatus.PENDING, firstSave.getStatus());
    assertEquals(MVOLA, firstSave.getProvider());
    assertEquals(AR, firstSave.getCurrency());
    assertEquals(request.getAmount(), firstSave.getAmount());
    assertEquals(request.getDescription(), firstSave.getDescription());
  }

  @Test
  void should_save_payment_with_correct_type() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    service.initiatePayment(request);

    final ArgumentCaptor<JPayment> captor = ArgumentCaptor.forClass(JPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    assertEquals(request.getType(), captor.getAllValues().getFirst().getType());
  }

  @Test
  void should_set_created_at_and_updated_at_on_payment_creation() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    final LocalDateTime before = now().minusSeconds(1);
    service.initiatePayment(request);
    final LocalDateTime after = now().plusSeconds(1);

    final ArgumentCaptor<JPayment> captor = ArgumentCaptor.forClass(JPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    final JPayment payment = captor.getAllValues().getFirst();

    assertTrue(payment.getCreatedAt().isAfter(before) && payment.getCreatedAt().isBefore(after));
    assertTrue(payment.getUpdatedAt().isAfter(before) && payment.getUpdatedAt().isBefore(after));
  }

  @Test
  void should_update_payment_transaction_id_from_gateway_response() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    final String serverCorrelationId = randomUUID().toString();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(serverCorrelationId);

    service.initiatePayment(request);

    final ArgumentCaptor<JPayment> captor = ArgumentCaptor.forClass(JPayment.class);
    verify(paymentRepository, atLeastOnce()).save(captor.capture());
    final JPayment lastSave = captor.getAllValues().getLast();
    assertEquals(serverCorrelationId, lastSave.getTransactionId());
  }

  @Test
  void should_save_payment_at_least_twice_on_successful_initiate() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    service.initiatePayment(request);

    // First save: initial persist. Second save: update with transactionId from gateway.
    verify(paymentRepository, atLeast(2)).save(any(JPayment.class));
  }

  @Test
  void should_publish_payment_verification_requested_event_exactly_once() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    service.initiatePayment(request);

    verify(eventProducer, times(1)).accept(any());
  }

  @Test
  void should_publish_event_with_correct_payment_id() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    final JPayment savedPayment = stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    service.initiatePayment(request);

    final ArgumentCaptor<List<PaymentVerificationRequested>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());

    assertEquals(savedPayment.getId(), captor.getValue().getFirst().getPaymentId());
  }

  @Test
  void should_publish_event_with_max_verification_attempt_nb_of_5() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    service.initiatePayment(request);

    final ArgumentCaptor<List<PaymentVerificationRequested>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals(5, captor.getValue().getFirst().getMaxVerificationAttemptNb());
  }

  @Test
  void should_publish_a_list_containing_exactly_one_event() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    service.initiatePayment(request);

    final ArgumentCaptor<List<PaymentVerificationRequested>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals(1, captor.getValue().size());
  }

  @Test
  void should_publish_event_with_non_blank_id() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    service.initiatePayment(request);

    final ArgumentCaptor<List<PaymentVerificationRequested>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    final String eventId = captor.getValue().getFirst().getId();
    assertFalse(eventId == null || eventId.isBlank());
  }

  @Test
  void should_delegate_to_mvola_gateway_on_initiate_payment() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    stubGatewayInitiate(request.getTransactionId());

    service.initiatePayment(request);

    verify(mvolaGateway, times(1)).initiatePayment(request);
  }

  @Test
  void should_return_gateway_response_from_initiate_payment() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    final MvolaPaymentResponse expected = buildPendingResponse(request.getTransactionId());
    stubMappers(request);
    stubPaymentSave();
    when(mvolaGateway.initiatePayment(request)).thenReturn(expected);

    final PaymentResponse result = service.initiatePayment(request);

    assertSame(expected, result);
  }

  @Test
  void should_propagate_payment_gateway_exception_from_gateway() {
    when(gatewayFactory.getGateway(MVOLA)).thenReturn(mvolaGateway);
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final MvolaPaymentRequest request = buildValidRequest();
    stubMappers(request);
    stubPaymentSave();
    when(mvolaGateway.initiatePayment(any()))
        .thenThrow(new PaymentGatewayException("MVola initiatePayment failed"));

    assertThrows(PaymentGatewayException.class, () -> service.initiatePayment(request));
  }

  @Test
  void should_return_mvola_payment_mapped_from_persistence() {
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

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
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    when(paymentRepository.findJPaymentByTransactionId(any())).thenReturn(Optional.empty());

    final EntityNotFoundException ex =
        assertThrows(EntityNotFoundException.class, () -> service.getPayment("non-existent-id"));

    assertTrue(ex.getMessage().contains("non-existent-id"));
  }

  @Test
  void should_query_repository_with_exact_transaction_id_on_get_payment() {
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

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
    service =
        new MvolaPaymentService(
            gatewayFactory,
            paymentRepository,
            eventProducer,
            paymentPartyMapper,
            mvolaPaymentMapper);

    final String transactionId = randomUUID().toString();
    final JMvolaPayment jPayment = buildJMvolaPayment(transactionId);
    when(paymentRepository.findJPaymentByTransactionId(transactionId))
        .thenReturn(Optional.of(jPayment));
    when(mvolaPaymentMapper.toModel(jPayment)).thenReturn(MvolaPayment.builder().build());

    service.getPayment(transactionId);

    verify(mvolaPaymentMapper, times(1)).toModel(jPayment);
  }

  private MvolaPaymentRequest buildValidRequest() {
    final MvolaPaymentRequest request = new MvolaPaymentRequest();
    request.setTransactionId(randomUUID().toString());
    request.setAmount(new BigDecimal("5000"));
    request.setCurrency(AR);
    request.setDescription("Test payment");
    request.setProvider(MVOLA);
    request.setType(PaymentType.PROFILE_UNLOCK);
    request.setPayer(PaymentParty.builder().phoneNumber("0343500003").build());
    request.setPayee(PaymentParty.builder().phoneNumber("0343500004").build());
    return request;
  }

  private JPayment stubPaymentSave() {
    final JPayment payment =
        JPayment.builder()
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
    when(paymentRepository.save(any(JPayment.class))).thenReturn(payment);
    return payment;
  }

  private void stubMappers(final MvolaPaymentRequest request) {
    when(paymentPartyMapper.toPersistence(request.getPayer())).thenReturn(new JPaymentParty());
    when(paymentPartyMapper.toPersistence(request.getPayee())).thenReturn(new JPaymentParty());
  }

  private void stubGatewayInitiate(final String transactionId) {
    when(mvolaGateway.initiatePayment(any())).thenReturn(buildPendingResponse(transactionId));
  }

  private MvolaPaymentResponse buildPendingResponse(final String transactionId) {
    final MvolaPaymentResponse response = new MvolaPaymentResponse();
    response.setTransactionId(transactionId);
    response.setStatus(PaymentStatus.PENDING);
    response.setProvider(MVOLA);
    response.setServerCorrelationId(randomUUID().toString());
    response.setNotificationMethod("polling");
    response.setRespondedAt(now());
    return response;
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
