package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.classifier.Country;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class PaymentParty {
  private UUID id;
  private String phoneNumber;
  private String name;
  private Country country;
}
