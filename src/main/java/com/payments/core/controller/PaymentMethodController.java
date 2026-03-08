package com.payments.core.controller;

import com.payments.core.dto.PaymentMethodDTOs;
import com.payments.core.service.PaymentMethodService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PostMapping
    public ResponseEntity<PaymentMethodDTOs.PaymentMethodResponse> addPaymentMethod(
            @RequestBody PaymentMethodDTOs.PaymentMethodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentMethodService.addPaymentMethod(request));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<PaymentMethodDTOs.PaymentMethodResponse>> getMethodsByCustomer(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(paymentMethodService.getMethodsByCustomer(customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethodDTOs.PaymentMethodResponse> getMethodById(
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentMethodService.getMethodById(id));
    }

    @PatchMapping("/{id}/set-default")
    public ResponseEntity<PaymentMethodDTOs.PaymentMethodResponse> setAsDefault(
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentMethodService.setAsDefault(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> removePaymentMethod(@PathVariable Long id) {
        return ResponseEntity.ok(paymentMethodService.removePaymentMethod(id));
    }
}