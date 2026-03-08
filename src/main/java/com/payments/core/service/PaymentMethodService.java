package com.payments.core.service;

import com.payments.core.dto.PaymentMethodDTOs;
import com.payments.core.model.Customer;
import com.payments.core.model.PaymentMethod;
import com.payments.core.repository.CustomerRepository;
import com.payments.core.repository.PaymentMethodRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final CustomerRepository customerRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository,
                                CustomerRepository customerRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.customerRepository = customerRepository;
    }

    // ADD PAYMENT METHOD
    @Transactional
    public PaymentMethodDTOs.PaymentMethodResponse addPaymentMethod(
            PaymentMethodDTOs.PaymentMethodRequest request) {

        log.info("Adding payment method for customerId: {}", request.getCustomerId());

        // Validate customer exists
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found with id: " + request.getCustomerId()));

        // If this is set as default, unset previous default first
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            paymentMethodRepository
                    .findByCustomerIdAndIsDefaultTrue(request.getCustomerId())
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        paymentMethodRepository.save(existing);
                    });
        }

        PaymentMethod method = PaymentMethod.builder()
                .customer(customer)
                .methodType(PaymentMethod.MethodType.valueOf(request.getMethodType().toUpperCase()))
                .provider(request.getProvider())
                .maskedAccount(request.getMaskedAccount())
                .accountHolderName(request.getAccountHolderName())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .status(PaymentMethod.MethodStatus.ACTIVE)
                .build();

        PaymentMethod saved = paymentMethodRepository.save(method);
        log.info("Payment method added with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    // GET ALL METHODS FOR A CUSTOMER
    @Transactional(readOnly = true)
    public List<PaymentMethodDTOs.PaymentMethodResponse> getMethodsByCustomer(Long customerId) {
        log.info("Fetching payment methods for customerId: {}", customerId);

        if (!customerRepository.existsById(customerId)) {
            throw new RuntimeException("Customer not found with id: " + customerId);
        }

        return paymentMethodRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // GET BY ID
    @Transactional(readOnly = true)
    public PaymentMethodDTOs.PaymentMethodResponse getMethodById(Long id) {
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found with id: " + id));
        return mapToResponse(method);
    }

    // SET AS DEFAULT
    @Transactional
    public PaymentMethodDTOs.PaymentMethodResponse setAsDefault(Long id) {
        log.info("Setting payment method {} as default", id);

        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found with id: " + id));

        // Unset current default
        paymentMethodRepository
                .findByCustomerIdAndIsDefaultTrue(method.getCustomer().getId())
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    paymentMethodRepository.save(existing);
                });

        // Set new default
        method.setIsDefault(true);
        return mapToResponse(paymentMethodRepository.save(method));
    }

    // DELETE (soft delete — set INACTIVE)
    @Transactional
    public String removePaymentMethod(Long id) {
        log.info("Removing payment method with id: {}", id);
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found with id: " + id));

        method.setStatus(PaymentMethod.MethodStatus.INACTIVE);
        paymentMethodRepository.save(method);
        return "Payment method removed successfully";
    }

    // HELPER
    private PaymentMethodDTOs.PaymentMethodResponse mapToResponse(PaymentMethod method) {
        return PaymentMethodDTOs.PaymentMethodResponse.builder()
                .id(method.getId())
                .customerId(method.getCustomer().getId())
                .customerName(method.getCustomer().getName())
                .methodType(method.getMethodType().name())
                .provider(method.getProvider())
                .maskedAccount(method.getMaskedAccount())
                .accountHolderName(method.getAccountHolderName())
                .isDefault(method.getIsDefault())
                .status(method.getStatus().name())
                .createdAt(method.getCreatedAt())
                .build();
    }
}