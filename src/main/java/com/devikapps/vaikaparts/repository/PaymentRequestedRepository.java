package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.model.JPaymentVerificationRequested;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRequestedRepository
    extends JpaRepository<JPaymentVerificationRequested, String> {}
