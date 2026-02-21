package com.payments.core.exception;

import com.payments.core.dto.PaymentDTOs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * GLOBAL EXCEPTION HANDLER
 * ============================================================
 * Without this class, whenever an exception is thrown,
 * Spring would return an ugly HTML error page or a raw Java
 * stack trace to the API caller. That's bad!
 *
 * This class INTERCEPTS all exceptions thrown anywhere in the app
 * and converts them into clean, consistent JSON error responses.
 *
 * @RestControllerAdvice → Applies to ALL controllers in the app.
 *   It's a combination of @ControllerAdvice + @ResponseBody.
 *   Think of it as a global "catch block" for your entire app.
 *
 * @ExceptionHandler → Tells Spring: "When THIS type of exception
 *   occurs, run THIS method to handle it."
 *
 * @Slf4j → Lombok annotation that creates a 'log' variable for us.
 *   We can then do: log.error("Something went wrong")
 *   Logs appear in your console when running the app.
 * ============================================================
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles: Payment not found in database
     * HTTP Status: 404 Not Found
     */
    @ExceptionHandler(PaymentExceptions.PaymentNotFoundException.class)
    public ResponseEntity<PaymentDTOs.ErrorResponse> handlePaymentNotFound(
            PaymentExceptions.PaymentNotFoundException ex) {

        // Log the error — useful for debugging in production
        log.error("Payment not found: {}", ex.getMessage());

        PaymentDTOs.ErrorResponse error = PaymentDTOs.ErrorResponse.builder()
                .statusCode(HttpStatus.NOT_FOUND.value())   // 404
                .errorCode("PAYMENT_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        // ResponseEntity wraps our body AND sets the HTTP status code
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles: Duplicate payment attempt
     * HTTP Status: 409 Conflict
     */
    @ExceptionHandler(PaymentExceptions.DuplicatePaymentException.class)
    public ResponseEntity<PaymentDTOs.ErrorResponse> handleDuplicatePayment(
            PaymentExceptions.DuplicatePaymentException ex) {

        log.warn("Duplicate payment attempt: {}", ex.getMessage());

        PaymentDTOs.ErrorResponse error = PaymentDTOs.ErrorResponse.builder()
                .statusCode(HttpStatus.CONFLICT.value())    // 409
                .errorCode("DUPLICATE_PAYMENT")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles: Fake/invalid callback signature
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(PaymentExceptions.InvalidSignatureException.class)
    public ResponseEntity<PaymentDTOs.ErrorResponse> handleInvalidSignature(
            PaymentExceptions.InvalidSignatureException ex) {

        log.error("Security alert - Invalid callback signature: {}", ex.getMessage());

        PaymentDTOs.ErrorResponse error = PaymentDTOs.ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value()) // 400
                .errorCode("INVALID_SIGNATURE")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles: Trying to update a payment that's already finalised
     * HTTP Status: 422 Unprocessable Entity
     */
    @ExceptionHandler(PaymentExceptions.InvalidPaymentStatusException.class)
    public ResponseEntity<PaymentDTOs.ErrorResponse> handleInvalidStatus(
            PaymentExceptions.InvalidPaymentStatusException ex) {

        log.warn("Invalid status transition: {}", ex.getMessage());

        PaymentDTOs.ErrorResponse error = PaymentDTOs.ErrorResponse.builder()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value()) // 422
                .errorCode("INVALID_STATUS_TRANSITION")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    /**
     * Handles: @Valid annotation failures on request DTOs
     * Example: missing merchantId, invalid amount format, etc.
     * HTTP Status: 400 Bad Request
     *
     * Spring throws MethodArgumentNotValidException automatically
     * when a @Valid annotated parameter fails validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Collect all field errors into a map: { "fieldName": "error message" }
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 400);
        response.put("errorCode", "VALIDATION_FAILED");
        response.put("message", "Request validation failed. Check the 'errors' field.");
        response.put("errors", fieldErrors);
        response.put("timestamp", LocalDateTime.now());

        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles: Any other unexpected exception we didn't specifically handle
     * HTTP Status: 500 Internal Server Error
     *
     * This is the "catch-all" — the safety net for unknown errors.
     * IMPORTANT: Don't expose internal details (like stack traces) to callers.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<PaymentDTOs.ErrorResponse> handleGenericException(Exception ex) {

        // Log full stack trace for internal debugging
        log.error("Unexpected error occurred", ex);

        PaymentDTOs.ErrorResponse error = PaymentDTOs.ErrorResponse.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.internalServerError().body(error);
    }
}
