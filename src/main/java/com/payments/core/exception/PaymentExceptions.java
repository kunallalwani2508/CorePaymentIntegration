package com.payments.core.exception;

/**
 * ============================================================
 * CUSTOM EXCEPTIONS
 * ============================================================
 * In Java, an Exception is an event that disrupts the normal
 * flow of the program. We create our OWN custom exceptions
 * so we can give meaningful names and messages to errors.
 *
 * WHY CUSTOM EXCEPTIONS?
 * - "PaymentNotFoundException" is much clearer than generic "RuntimeException"
 * - We can attach HTTP status codes to them
 * - Our GlobalExceptionHandler can catch them specifically
 *
 * RuntimeException = "unchecked" exception.
 * We don't have to declare it with "throws" keyword everywhere.
 * Most custom exceptions in Spring Boot extend RuntimeException.
 * ============================================================
 */
public class PaymentExceptions {

    /**
     * Thrown when we look up a transaction ID that doesn't exist in DB.
     * Will result in HTTP 404 Not Found.
     */
    public static class PaymentNotFoundException extends RuntimeException {

        // 'super(message)' calls the parent class (RuntimeException) constructor
        // This sets the exception's message which appears in logs
        public PaymentNotFoundException(String transactionId) {
            super("Payment not found for transaction ID: " + transactionId);
        }
    }

    /**
     * Thrown when we detect a DUPLICATE payment request.
     * This is our idempotency guard in action.
     * Will result in HTTP 409 Conflict.
     */
    public static class DuplicatePaymentException extends RuntimeException {

        public DuplicatePaymentException(String merchantId, String orderId) {
            super("Payment already exists for Merchant: " + merchantId + ", Order: " + orderId);
        }
    }

    /**
     * Thrown when a callback's signature doesn't match what we expect.
     * This protects us from fake/malicious callbacks.
     * Will result in HTTP 400 Bad Request.
     */
    public static class InvalidSignatureException extends RuntimeException {

        public InvalidSignatureException() {
            super("Callback signature is invalid. Request may be tampered.");
        }
    }

    /**
     * Thrown when trying to update a payment that is already in a final state.
     * Example: trying to update a FAILED payment to SUCCESS.
     * Will result in HTTP 422 Unprocessable Entity.
     */
    public static class InvalidPaymentStatusException extends RuntimeException {

        public InvalidPaymentStatusException(String currentStatus) {
            super("Cannot update payment. Already in final status: " + currentStatus);
        }
    }
}
