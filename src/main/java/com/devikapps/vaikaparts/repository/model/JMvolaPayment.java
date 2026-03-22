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
@Table(name = "mvola_payments")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class JMvolaPayment extends JPayment {

  @Column(name = "server_correlation_id")
  private String serverCorrelationId;

  @Column(name = "mvola_transaction_id")
  private String mvolaTransactionId;

  @Column(name = "notification_method")
  private String notificationMethod;
}
