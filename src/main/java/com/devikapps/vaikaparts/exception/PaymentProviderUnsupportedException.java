package com.devikapps.vaikaparts.exception;

public class PaymentProviderUnsupportedException extends RuntimeException {
  public PaymentProviderUnsupportedException(String message) {
    super(message);
  }
}
