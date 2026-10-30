package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.RPapiPaymentRequest;
import com.devikapps.vaikaparts.exception.PaymentValidationException;
import com.devikapps.vaikaparts.exception.PapiWebhookSecurityException;
import com.devikapps.vaikaparts.model.PapiPayment;
import com.devikapps.vaikaparts.service.PapiPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/v1/payments/papi")
@RequiredArgsConstructor
public class PapiController {
  private final PapiPaymentService papiPaymentService;

  @PostMapping
  public ResponseEntity<PapiPayment> initiatePayment(
      @Valid @NotNull @RequestBody RPapiPaymentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(papiPaymentService.initiatePayment(request));
  }

  @GetMapping("/{reference}")
  public PapiPayment getPayment(@PathVariable @NotBlank String reference) {
    log.info("Papi payment lookup for reference={}", forJava(reference));
    return papiPaymentService.getPayment(reference);
  }

  @PostMapping("/callback")
  public ResponseEntity<Void> handleCallback(
      @RequestBody byte[] rawBody,
      @RequestHeader(value = "X-Papi-Signature", required = false) String signatureHeader) {
    papiPaymentService.handleCallBack(rawBody, signatureHeader);
    return ResponseEntity.ok().build();
  }

  @ExceptionHandler(PapiWebhookSecurityException.class)
  public ResponseEntity<Void> handleInvalidCallback(PapiWebhookSecurityException ex) {
    log.warn("Rejected Papi callback: {}", forJava(ex.getMessage()));
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  @ExceptionHandler(PaymentValidationException.class)
  public ResponseEntity<Void> handleInvalidPapiRequest(PaymentValidationException ex) {
    log.warn("Rejected Papi payment request: {}", forJava(ex.getMessage()));
    return ResponseEntity.badRequest().build();
  }
}
