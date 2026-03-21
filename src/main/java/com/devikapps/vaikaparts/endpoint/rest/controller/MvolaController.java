package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.gateway.mvola.MvolaPaymentRequest;
import com.devikapps.vaikaparts.model.MvolaPayment;
import com.devikapps.vaikaparts.service.MvolaPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/v1/payments/mvola")
@RequiredArgsConstructor
public class MvolaController {

  private final MvolaPaymentService mvolaPaymentService;

  @PostMapping
  public ResponseEntity<MvolaPayment> initiatePayment(@Valid @NotNull MvolaPaymentRequest request) {
    log.info("MVola Payment initiation at POST /v1/payments/mvola");
    return new ResponseEntity<>(
        (MvolaPayment) mvolaPaymentService.initiatePayment(request), HttpStatus.CREATED);
  }

  @GetMapping("/{transactionId}")
  public MvolaPayment getPayment(@PathVariable @NotNull String transactionId) {
    log.info("MVola Payment get at GET /v1/payments/mvola/{}", forJava(transactionId));
    return (MvolaPayment) mvolaPaymentService.getPayment(transactionId);
  }
}
