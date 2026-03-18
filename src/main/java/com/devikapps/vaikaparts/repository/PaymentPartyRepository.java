package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.model.JPaymentParty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentPartyRepository extends JpaRepository<JPaymentParty, String> {
  Optional<JPaymentParty> findJPaymentPartyByPhoneNumber(
      @NotNull
          @Pattern(
              regexp = "^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$",
              message = "Invalid phone number format")
          String phoneNumber);
}
