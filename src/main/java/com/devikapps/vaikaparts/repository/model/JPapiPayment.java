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
@Table(name = "papi_payments")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class JPapiPayment extends JPayment {
  @Column(name = "papi_payment_reference")
  private String papiPaymentReference;

  @Column(name = "merchant_reference", nullable = false, unique = true)
  private String merchantReference;

  @Column(name = "payment_method", nullable = false)
  private String paymentMethod;

  @Column(name = "notification_token")
  private String notificationToken;
}
