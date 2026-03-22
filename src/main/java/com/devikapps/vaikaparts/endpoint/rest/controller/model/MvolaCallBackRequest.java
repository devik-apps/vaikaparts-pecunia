package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class MvolaCallBackRequest {
  @JsonProperty("transactionStatus")
  private String transactionStatus;

  @JsonProperty("serverCorrelationId")
  private String serverCorrelationId;

  @JsonProperty("transactionReference")
  private String transactionReference;

  @JsonProperty("requestDate")
  private String requestDate;

  @JsonProperty("debitParty")
  private List<MvolaPartyEntry> debitParty;

  @JsonProperty("creditParty")
  private List<MvolaPartyEntry> creditParty;

  @JsonProperty("fees")
  private List<MvolaFeeEntry> fees;

  @JsonProperty("metadata")
  private List<MvolaPartyEntry> metadata;

  public record MvolaPartyEntry(
      @JsonProperty("key") String key, @JsonProperty("value") String value) {}

  public record MvolaFeeEntry(@JsonProperty("feeAmount") String feeAmount) {}
}
