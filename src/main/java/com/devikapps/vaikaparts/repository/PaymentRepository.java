package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.model.JPayment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<JPayment, String> {

  Optional<JPayment> findJPaymentByTransactionId(String transactionId);
}
