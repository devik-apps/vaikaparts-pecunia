package com.devikapps.vaikaparts.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AirtelIntegrationApiContractTest {

  @Test
  void should_keep_airtel_available_only_through_papi() throws IOException {
    String api = Files.readString(Path.of("doc/api.yaml"));

    assertThat(api).doesNotContain("/v1/payments/airtel-money");
    assertThat(api).contains("enum: [MVOLA, AIRTEL_MONEY, ORANGE_MONEY]");
  }
}
