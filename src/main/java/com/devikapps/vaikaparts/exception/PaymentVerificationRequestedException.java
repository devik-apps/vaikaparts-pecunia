package com.devikapps.vaikaparts.exception;

public class PaymentVerificationRequestedException extends RuntimeException {
  public PaymentVerificationRequestedException(String message) {
    super(message);
  }

  public PaymentVerificationRequestedException(String message, Throwable cause) {
    super(message, cause);
  }
}
