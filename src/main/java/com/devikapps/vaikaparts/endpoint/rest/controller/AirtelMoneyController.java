package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.AirtelMoneyCallBackRequest;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.RPaymentRequest;
import com.devikapps.vaikaparts.mapper.PaymentRequestMapper;
import com.devikapps.vaikaparts.model.AirtelMoneyPayment;
import com.devikapps.vaikaparts.service.AirtelMoneyPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/payments/airtel-money")
public class AirtelMoneyController {
  private final AirtelMoneyPaymentService airtelMoneyPaymentService;
  private final PaymentRequestMapper paymentRequestMapper;

  @PostMapping
  public ResponseEntity<AirtelMoneyPayment> initiatePayment(
      @Valid @NotNull RPaymentRequest request) {
    log.info("Airtel Money Payment initiation at POST /v1/payments/airtel-money");

    var airtelMoneyPaymentReq = paymentRequestMapper.toAirtelMoneyPaymentRequest(request);

    return new ResponseEntity<>(
        (AirtelMoneyPayment) airtelMoneyPaymentService.initiatePayment(airtelMoneyPaymentReq),
        HttpStatus.CREATED);
  }

  @GetMapping("/{transactionId}")
  public AirtelMoneyPayment getPayment(@PathVariable @NotNull String transactionId) {
    log.info(
        "Airtel Money Payment get at GET /v1/payments/airtel-money/{}", forJava(transactionId));
    return (AirtelMoneyPayment) airtelMoneyPaymentService.getPayment(transactionId);
  }

  @GetMapping("/customer/{customer-msisdn}")
  public Page<AirtelMoneyPayment> getPaymentsByCustomerMsisdn(
      @PathVariable(name = "customer-msisdn") String customerMsisdn,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    log.info(
        "Airtel Money Payment gt by customer msisdn at GET /v1/payments/airtel-money/customer/{}",
        forJava(customerMsisdn));

    return airtelMoneyPaymentService.findPaymentsByPaymentPartyMsisdn(customerMsisdn, page, size);
  }

  @PutMapping("/callback")
  public ResponseEntity<Void> handleAirtelMoneyCallBack(
      @RequestBody @NotNull final AirtelMoneyCallBackRequest request) {
    log.info("Airtel Money Payment callback at PUT /v1/payments/airtel-money/callback");

    airtelMoneyPaymentService.handleCallBack(request);
    return ResponseEntity.ok().build();
  }
}
