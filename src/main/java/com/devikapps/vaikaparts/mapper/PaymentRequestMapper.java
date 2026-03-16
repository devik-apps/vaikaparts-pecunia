package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.model.PaymentRequest;

public interface PaymentRequestMapper<T> {

  T toProviderRequest(PaymentRequest request);
}
