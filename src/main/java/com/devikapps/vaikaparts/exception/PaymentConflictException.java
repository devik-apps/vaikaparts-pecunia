package com.devikapps.vaikaparts.exception;

public class PaymentConflictException extends PaymentGatewayException {
  public PaymentConflictException(String message) {
    super(message);
  }
}
