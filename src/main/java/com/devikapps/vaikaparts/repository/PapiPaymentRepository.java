package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.model.JPapiPayment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PapiPaymentRepository extends JpaRepository<JPapiPayment, String> {
  Optional<JPapiPayment> findByMerchantReference(String merchantReference);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select payment from JPapiPayment payment where payment.merchantReference = :reference")
  Optional<JPapiPayment> findByMerchantReferenceForUpdate(@Param("reference") String reference);
}
