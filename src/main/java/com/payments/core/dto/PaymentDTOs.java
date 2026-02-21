package com.payments.core.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * DTO = Data Transfer Object
 * ============================================================
 * DTOs are simple classes used to carry data between layers.
 *
 * WHY NOT USE THE ENTITY (Payment.java) DIRECTLY?
 * - The entity has DB-specific fields (id, createdAt etc.)
 * - We don't want to expose raw DB structure to the outside world
 * - DTOs let us control exactly what goes in and what comes out
 * - Separation of concerns — good software design practice
 *
 * We define multiple DTOs as inner static classes in one file
 * for convenience. In large projects, each would be its own file.
 * ============================================================
 */
public class PaymentDTOs {

    // ============================================================
    // REQUEST DTO: What the merchant sends us to START a payment
    // ============================================================
    @Data                   // Lombok: generates getters, setters, toString etc.
    @NoArgsConstructor      // Lombok: generates empty constructor (needed for JSON parsing)
    @AllArgsConstructor     // Lombok: generates constructor with all fields
    @Builder                // Lombok: lets us use builder pattern
    public static class PaymentInitiateRequest {

        /**
         * @NotBlank = field must not be null AND must contain non-whitespace chars
         * message = the error message shown if validation fails
         */
        @NotBlank(message = "Merchant ID is required")
        private String merchantId;

        @NotBlank(message = "Order ID is required")
        private String orderId;

        /**
         * @NotNull = field must not be null
         * @DecimalMin = minimum allowed value (inclusive)
         * @Digits = controls max digits before and after decimal
         */
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Amount format invalid")
        private BigDecimal amount;

        /**
         * @Size = string length constraints
         * @Pattern = validates against a regex
         * Currency should be a 3-letter code like INR, USD, EUR
         */
        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code (e.g. INR, USD)")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be uppercase letters only (e.g. INR)")
        private String currency;

        @NotBlank(message = "Callback URL is required")
        private String callbackUrl;
    }


    // ============================================================
    // RESPONSE DTO: What we send BACK to the merchant after initiation
    // ============================================================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentInitiateResponse {

        private String transactionId;  // Our unique ID for tracking
        private String paymentUrl;     // URL where user completes payment
        private String status;         // Always "PENDING" at this stage
        private String message;        // Human-readable info
    }


    // ============================================================
    // CALLBACK DTO: What the payment gateway sends us after processing
    // ============================================================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentCallbackRequest {

        @NotBlank(message = "Transaction ID is required")
        private String transactionId;

        @NotBlank(message = "Status is required")
        private String status;           // "SUCCESS" or "FAILED"

        @NotBlank(message = "Signature is required")
        private String signature;        // Security: proves callback is genuine
    }


    // ============================================================
    // STATUS DTO: Response when someone checks a payment's status
    // ============================================================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentStatusResponse {

        private String transactionId;
        private String paymentStatus;      // PENDING / SUCCESS / FAILED
        private BigDecimal amount;
        private String currency;
        private String merchantId;
        private String orderId;
        private LocalDateTime lastUpdatedTime;
    }


    // ============================================================
    // ERROR RESPONSE DTO: Consistent error format for all API errors
    // ============================================================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorResponse {

        private int statusCode;       // HTTP status code e.g. 400, 404, 500
        private String errorCode;     // Our custom code e.g. "PAYMENT_NOT_FOUND"
        private String message;       // Human-readable error description
        private LocalDateTime timestamp;
    }
}
