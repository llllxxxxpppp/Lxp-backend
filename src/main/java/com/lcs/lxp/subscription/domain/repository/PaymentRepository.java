package com.lcs.lxp.subscription.domain.repository;

import com.lcs.lxp.subscription.domain.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
