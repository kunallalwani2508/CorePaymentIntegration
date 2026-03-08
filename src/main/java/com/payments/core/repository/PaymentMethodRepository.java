package com.payments.core.repository;

import com.payments.core.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // Get all payment methods for a customer
    List<PaymentMethod> findByCustomerId(Long customerId);

    // Get default payment method for a customer
    Optional<PaymentMethod> findByCustomerIdAndIsDefaultTrue(Long customerId);

    // Get active methods for a customer
    List<PaymentMethod> findByCustomerIdAndStatus(Long customerId, PaymentMethod.MethodStatus status);
}