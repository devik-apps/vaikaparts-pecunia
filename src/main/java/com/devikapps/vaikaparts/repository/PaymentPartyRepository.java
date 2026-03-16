package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentPartyRepository extends JpaRepository<JPaymentParty, String> {}
