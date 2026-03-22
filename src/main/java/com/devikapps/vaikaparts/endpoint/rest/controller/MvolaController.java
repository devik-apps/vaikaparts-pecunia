package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.MvolaCallBackRequest;
import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentRequest;
import com.devikapps.vaikaparts.model.MvolaPayment;
import com.devikapps.vaikaparts.service.MvolaPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/v1/payments/mvola")
@RequiredArgsConstructor
public class MvolaController {

  private final MvolaPaymentService mvolaPaymentService;

  @PostMapping
  public ResponseEntity<MvolaPayment> initiatePayment(
      @Valid @NotNull @RequestBody MvolaPaymentRequest request) {
    log.info("MVola Payment initiation at POST /v1/payments/mvola");
    return new ResponseEntity<>(
        (MvolaPayment) mvolaPaymentService.initiatePayment(request), HttpStatus.CREATED);
  }

  @GetMapping("/{customer-msisdn}")
  public Page<MvolaPayment> getPaymentsByCustomerMsisdn(
      @PathVariable(name = "customer-msisdn") @NotNull String customerMsisdn,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    log.info("Mvola Payment get by customer msisdn at GET /v1/payments/mvola/{}", customerMsisdn);
    return mvolaPaymentService.findPaymentsByPaymentPartyMsisdn(customerMsisdn, page, size);
  }

  @GetMapping("/{transactionId}")
  public MvolaPayment getPayment(@PathVariable @NotNull String transactionId) {
    log.info("MVola Payment get at GET /v1/payments/mvola/{}", forJava(transactionId));
    return (MvolaPayment) mvolaPaymentService.getPayment(transactionId);
  }

  @PutMapping("/callback")
  public ResponseEntity<Void> handleCallBack(
      @RequestBody @NotNull final MvolaCallBackRequest request) {
    log.info(
        "PUT /v1/payments/mvola/callback — serverCorrelationId={}, status={}",
        forJava(request.getServerCorrelationId()),
        forJava(request.getTransactionReference()));

    mvolaPaymentService.handleCallBack(request);
    return ResponseEntity.ok().build();
  }
}
