package com.devikapps.vaikaparts.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "airtel_money_payments")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class JAirtelMoneyPayment extends JPayment {

  /**
   * Airtel Money system-generated transaction ID returned once the transaction reaches TS
   * (Transaction Success).
   */
  @Column(name = "airtel_money_id")
  private String airtelMoneyId;
}
