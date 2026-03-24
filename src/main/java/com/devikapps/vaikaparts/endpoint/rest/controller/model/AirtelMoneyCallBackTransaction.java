package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
public class AirtelMoneyCallBackTransaction {

  /** Partner unique transaction ID — the same ID sent in the initiation request. */
  @JsonProperty("id")
  @NotBlank
  private String id;

  /** Human-readable description of the transaction outcome. */
  @JsonProperty("message")
  private String message;

  /** Airtel transaction status code. Possible values: TS (success), TF (failed). */
  @JsonProperty("status_code")
  @NotBlank
  private String statusCode;

  /** Airtel Money system-generated transaction ID. */
  @JsonProperty("airtel_money_id")
  private String airtelMoneyId;
}
