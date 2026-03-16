package com.devikapps.vaikaparts.repository.model;

import com.devikapps.vaikaparts.event.model.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_verification_requested")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode
public class JPaymentVerificationRequested {
  @Id private String id;

  @OneToOne
  @JoinColumn(name = "payment_id")
  private JPayment payment;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private VerificationStatus status;

  @Column(name = "attempt_nb")
  private int attemptNb;

  @Column(name = "max_verification_attemp_nb")
  private int maxVerificationAttemptNb;

  @Column(name = "failed_attempt_nb")
  private int failedAttemptNb;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "last_verified_at")
  private LocalDateTime lastVerifiedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;
}
