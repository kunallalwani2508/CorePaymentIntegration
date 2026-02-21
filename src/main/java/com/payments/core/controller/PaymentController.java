package com.payments.core.controller;

import com.payments.core.dto.PaymentDTOs;
import com.payments.core.service.PaymentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 * CONTROLLER LAYER — The API Entry Point (Front Door)
 * ============================================================
 * The Controller's ONLY job is to:
 *   1. Receive HTTP requests
 *   2. Validate the request format (using @Valid)
 *   3. Call the Service layer to do the real work
 *   4. Return the HTTP response
 *
 * Controllers should be THIN — no business logic here!
 * All logic lives in the Service layer.
 *
 * KEY ANNOTATIONS:
 *
 * @RestController → Combines @Controller + @ResponseBody.
 *   Means: every method's return value is automatically
 *   converted to JSON and sent in the HTTP response body.
 *
 * @RequestMapping("/api/payments") → Base URL prefix for all
 *   endpoints in this controller.
 *   Full URL example: http://localhost:8080/api/payments/initiate
 *
 * @Slf4j → Creates a 'log' variable for logging.
 *
 * ResponseEntity<T> → Wraps your response body AND lets you
 *   set the HTTP status code explicitly (200, 201, 400 etc.)
 * ============================================================
 */
@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    /**
     * We inject the service via constructor injection.
     * 'final' means this reference cannot be changed after construction.
     */
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }


    // ============================================================
    // ENDPOINT 1: POST /api/payments/initiate
    // ============================================================

    /**
     * Initiates a new payment.
     * The merchant calls this to start the payment process.
     *
     * @PostMapping → Maps HTTP POST requests to this method.
     *
     * @RequestBody → Tells Spring to read the HTTP request body
     *   and convert the JSON into a PaymentInitiateRequest object.
     *
     * @Valid → Triggers validation on the request object.
     *   If @NotBlank, @NotNull etc. fail → Spring throws
     *   MethodArgumentNotValidException → caught by GlobalExceptionHandler.
     *
     * HTTP 201 CREATED is the correct status for resource creation.
     * (201 is more specific than 200 OK — use it for POST that creates data)
     *
     * EXAMPLE REQUEST BODY:
     * {
     *   "merchantId": "MERCHANT_001",
     *   "orderId": "ORDER_12345",
     *   "amount": 499.99,
     *   "currency": "INR",
     *   "callbackUrl": "https://myshop.com/payment/callback"
     * }
     *
     * EXAMPLE RESPONSE:
     * {
     *   "transactionId": "TXN-ABC123...",
     *   "paymentUrl": "http://mock-gateway.com/checkout?txn=...",
     *   "status": "PENDING",
     *   "message": "Payment initiated..."
     * }
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentDTOs.PaymentInitiateResponse> initiatePayment(
            @RequestBody @Valid PaymentDTOs.PaymentInitiateRequest request) {

        log.info("POST /api/payments/initiate called for merchant: {}", request.getMerchantId());

        PaymentDTOs.PaymentInitiateResponse response = paymentService.initiatePayment(request);

        // ResponseEntity.status(201).body(response) — returns 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // ============================================================
    // ENDPOINT 2: POST /api/payments/callback
    // ============================================================

    /**
     * Receives payment result from the gateway (asynchronous callback).
     * The payment gateway calls this when a user completes payment.
     *
     * This endpoint is called by the GATEWAY, not the merchant/user.
     *
     * EXAMPLE REQUEST BODY (sent by gateway):
     * {
     *   "transactionId": "TXN-ABC123...",
     *   "status": "SUCCESS",
     *   "signature": "a3f4b2c1d9e8..."
     * }
     */
    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestBody @Valid PaymentDTOs.PaymentCallbackRequest callbackRequest) {

        log.info("POST /api/payments/callback received for txn: {}",
                callbackRequest.getTransactionId());

        String result = paymentService.processCallback(callbackRequest);

        // 200 OK is fine here — we're confirming receipt of callback
        return ResponseEntity.ok(result);
    }


    // ============================================================
    // ENDPOINT 3: GET /api/payments/status/{transactionId}
    // ============================================================

    /**
     * Checks the current status of a payment.
     * Merchants or users can call this to see if payment went through.
     *
     * @GetMapping → Maps HTTP GET requests to this method.
     *
     * @PathVariable → Extracts a value from the URL path.
     *   URL: /api/payments/status/TXN-ABC123
     *   → transactionId = "TXN-ABC123"
     *
     * EXAMPLE RESPONSE:
     * {
     *   "transactionId": "TXN-ABC123...",
     *   "paymentStatus": "SUCCESS",
     *   "amount": 499.99,
     *   "currency": "INR",
     *   "merchantId": "MERCHANT_001",
     *   "orderId": "ORDER_12345",
     *   "lastUpdatedTime": "2024-01-15T10:30:00"
     * }
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<PaymentDTOs.PaymentStatusResponse> getPaymentStatus(
            @PathVariable String transactionId) {

        log.info("GET /api/payments/status/{} called", transactionId);

        PaymentDTOs.PaymentStatusResponse response = paymentService.getPaymentStatus(transactionId);

        return ResponseEntity.ok(response);  // 200 OK
    }


    // ============================================================
    // HEALTH CHECK ENDPOINT
    // ============================================================

    /**
     * Simple endpoint to verify the app is running.
     * Useful in production to check if the service is alive.
     * URL: GET /api/payments/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("✅ Core Payment System is UP and running!");
    }
}
