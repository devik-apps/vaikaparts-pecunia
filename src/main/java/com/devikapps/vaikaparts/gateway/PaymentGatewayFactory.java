package com.devikapps.vaikaparts.gateway;

import static java.lang.String.format;

import com.devikapps.vaikaparts.exception.PaymentProviderUnsupportedException;
import com.devikapps.vaikaparts.model.classifier.PaymentProvider;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactory {

  private final Map<PaymentProvider, PaymentGateway> gatewayRegistry;

  public PaymentGatewayFactory(final List<PaymentGateway> gateways) {
    this.gatewayRegistry =
        gateways.stream()
            .collect(
                Collectors.toUnmodifiableMap(PaymentGateway::getProvider, Function.identity()));
  }

  public PaymentGateway getGateway(final PaymentProvider provider) {
    final PaymentGateway gateway = gatewayRegistry.get(provider);

    if (gateway == null)
      throw new PaymentProviderUnsupportedException(
          format("Payment Provider not supported : %s", provider));

    return gateway;
  }
}
