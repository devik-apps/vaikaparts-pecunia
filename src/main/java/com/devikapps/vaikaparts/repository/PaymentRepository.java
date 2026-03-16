package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.model.JPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<JPayment, String> {}
