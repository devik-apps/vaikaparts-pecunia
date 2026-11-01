package com.devikapps.vaikaparts.conf;

import static java.util.UUID.randomUUID;

import com.devikapps.vaikaparts.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

@InfraGenerated
@TestConfiguration
public class EnvConf {

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("app.security.api-key", () -> randomUUID().toString());
  }
}
