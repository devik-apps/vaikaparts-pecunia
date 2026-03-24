package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AirtelMoneyCallBackRequest {

  @NotNull private AirtelMoneyCallBackTransaction transaction;

  /**
   * HMAC hash sent by Airtel when callback authentication is enabled. Null when callback
   * authentication is disabled.
   */
  private String hash;
}
