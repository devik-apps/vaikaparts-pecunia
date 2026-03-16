package com.devikapps.vaikaparts.repository.model;

import com.devikapps.vaikaparts.model.classifier.Country;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_parties")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
public class JPaymentParty {
  @Id private String id;

  @Pattern(
      regexp = "^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$",
      message = "Invalid phone number format")
  @Column(name = "phone_number", unique = true, nullable = false)
  private String phoneNumber;

  private String name;
  private Country country;

  @OneToMany(mappedBy = "payment_party", fetch = FetchType.LAZY)
  @Builder.Default
  private List<JPayment> payments = new ArrayList<>();
}
