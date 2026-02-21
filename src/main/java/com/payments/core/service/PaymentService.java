package com.payments.core.service;

import com.payments.core.dto.PaymentDTOs;
import com.payments.core.exception.PaymentExceptions;
import com.payments.core.model.Payment;
import com.payments.core.repository.PaymentRepository;
import com.payments.core.util.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ============================================================
 * SERVICE LAYER — The Brain of the Application
 * ============================================================
 * The Service layer contains the BUSINESS LOGIC.
 * It sits between the Controller (handles HTTP) and
 * the Repository (handles database).
 *
 * RESPONSIBILITIES:
 * - Validate business rules (not just input format)
 * - Coordinate between repository and other utilities
 * - Handle idempotency (duplicate payment prevention)
 * - Manage transaction boundaries
 *
 * @Service → Marks this as a Spring-managed Service bean.
 *   Spring creates ONE instance of this class (Singleton pattern)
 *   and injects it wherever needed.
 *
 * @Slf4j → Lombok creates a 'log' logger for us.
 *   Use: log.info(), log.warn(), log.error(), log.debug()
 *
 * DEPENDENCY INJECTION:
 * Instead of doing 'new PaymentRepository()', Spring injects
 * the repository for us via the constructor.
 * This is called Constructor Injection — the preferred way in Spring.
 * ============================================================
 */
@Service
@Slf4j
public class PaymentService {

    // These are injected by Spring — we don't create them ourselves
    private final PaymentRepository paymentRepository;
    private final SignatureUtil signatureUtil;

    /**
     * The mock gateway base URL. In a real project this would
     * point to Razorpay/Stripe/PayU etc.
     * Reads from application.properties, defaults to "http://mock-gateway.com"
     */
    @Value("${payment.gateway.url:http://mock-gateway.com}")
    private String gatewayUrl;

    /**
     * CONSTRUCTOR INJECTION
     * Spring automatically passes the required beans here.
     * Using constructor injection is recommended because:
     *   - Makes dependencies explicit
     *   - Makes testing easier (we can pass mock objects)
     *   - Fields can be final (immutable)
     */
    public PaymentService(PaymentRepository paymentRepository, SignatureUtil signatureUtil) {
        this.paymentRepository = paymentRepository;
        this.signatureUtil = signatureUtil;
    }

    // ============================================================
    // 1. INITIATE PAYMENT
    // ============================================================

    /**
     * Creates a new payment record and returns a checkout URL.
     *
     * STEP-BY-STEP FLOW:
     * 1. Check if this merchant+order combo was already paid (idempotency)
     * 2. Generate a unique transaction ID
     * 3. Build a checkout URL (would be real gateway URL in production)
     * 4. Save payment to database
     * 5. Return response to controller
     *
     * @Transactional → Wraps the method in a database transaction.
     * If ANY line in this method throws an exception → entire transaction ROLLS BACK.
     * This ensures we never have half-saved data in the database.
     *
     * @param request - the merchant's payment initiation data
     * @return PaymentInitiateResponse with transactionId and paymentUrl
     */
    @Transactional
    public PaymentDTOs.PaymentInitiateResponse initiatePayment(PaymentDTOs.PaymentInitiateRequest request) {

        log.info("Initiating payment for Merchant: {}, Order: {}", request.getMerchantId(), request.getOrderId());

        // -------------------------------------------------------
        // STEP 1: IDEMPOTENCY CHECK
        // Has this merchant already initiated payment for this order?
        // If yes → throw an exception instead of creating a duplicate.
        // -------------------------------------------------------
        boolean alreadyExists = paymentRepository.existsByMerchantIdAndOrderId(
                request.getMerchantId(), request.getOrderId()
        );

        if (alreadyExists) {
            log.warn("Duplicate payment detected for Merchant: {}, Order: {}",
                    request.getMerchantId(), request.getOrderId());
            throw new PaymentExceptions.DuplicatePaymentException(
                    request.getMerchantId(), request.getOrderId()
            );
        }

        // -------------------------------------------------------
        // STEP 2: GENERATE UNIQUE TRANSACTION ID
        // UUID = Universally Unique Identifier
        // Example: "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        // Probability of collision is astronomically low.
        // We remove the dashes and add a prefix for readability.
        // -------------------------------------------------------
        String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        log.debug("Generated transaction ID: {}", transactionId);

        // -------------------------------------------------------
        // STEP 3: GENERATE PAYMENT URL
        // In production: call Razorpay/Stripe API here to get a real URL.
        // For this project: we construct a mock URL for demonstration.
        // -------------------------------------------------------
        String paymentUrl = gatewayUrl + "/checkout?txn=" + transactionId
                + "&amount=" + request.getAmount()
                + "&currency=" + request.getCurrency();

        // Pre-compute the signature we'll verify when callback comes in
        String expectedSignature = signatureUtil.generateSignature(transactionId, "SUCCESS");

        // -------------------------------------------------------
        // STEP 4: BUILD & SAVE THE PAYMENT ENTITY
        // We use the Builder pattern (provided by @Builder in Payment.java)
        // This is cleaner than a constructor with 10 parameters.
        // -------------------------------------------------------
        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .merchantId(request.getMerchantId())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(Payment.PaymentStatus.PENDING)
                .callbackUrl(request.getCallbackUrl())
                .paymentUrl(paymentUrl)
                .gatewaySignature(expectedSignature)
                .build();

        // .save() → runs an INSERT SQL statement and returns the saved entity
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment saved to DB with ID: {}", savedPayment.getId());

        // -------------------------------------------------------
        // STEP 5: BUILD AND RETURN RESPONSE DTO
        // We return a DTO, NOT the entity.
        // This hides internal DB details from the API caller.
        // -------------------------------------------------------
        return PaymentDTOs.PaymentInitiateResponse.builder()
                .transactionId(savedPayment.getTransactionId())
                .paymentUrl(savedPayment.getPaymentUrl())
                .status(savedPayment.getStatus().name())
                .message("Payment initiated. Redirect user to paymentUrl to complete payment.")
                .build();
    }


    // ============================================================
    // 2. PROCESS CALLBACK
    // ============================================================

    /**
     * Called when the payment gateway notifies us of a payment result.
     *
     * FLOW:
     * 1. Verify callback signature (security check)
     * 2. Find the payment record in DB
     * 3. Check it's still PENDING (idempotency — ignore if already done)
     * 4. Update the status to SUCCESS or FAILED
     * 5. Save and confirm
     *
     * @Transactional ensures status update + save are atomic.
     */
    @Transactional
    public String processCallback(PaymentDTOs.PaymentCallbackRequest callbackRequest) {

        log.info("Received callback for Transaction: {}, Status: {}",
                callbackRequest.getTransactionId(), callbackRequest.getStatus());

        // -------------------------------------------------------
        // STEP 1: VERIFY SIGNATURE
        // Is this callback really from our payment gateway?
        // If signature is wrong → reject immediately.
        // -------------------------------------------------------
        boolean isValid = signatureUtil.isValidSignature(
                callbackRequest.getTransactionId(),
                callbackRequest.getStatus(),
                callbackRequest.getSignature()
        );

        if (!isValid) {
            log.error("Invalid signature for callback on transaction: {}",
                    callbackRequest.getTransactionId());
            throw new PaymentExceptions.InvalidSignatureException();
        }

        // -------------------------------------------------------
        // STEP 2: FETCH THE PAYMENT RECORD
        // .orElseThrow() = if not found, throw the given exception
        // -------------------------------------------------------
        Payment payment = paymentRepository
                .findByTransactionId(callbackRequest.getTransactionId())
                .orElseThrow(() -> new PaymentExceptions.PaymentNotFoundException(
                        callbackRequest.getTransactionId()
                ));

        // -------------------------------------------------------
        // STEP 3: IDEMPOTENCY CHECK — IS IT STILL PENDING?
        // If this callback was already processed (status is not PENDING),
        // we just acknowledge it without doing anything.
        // This handles: gateway retrying a callback we already processed.
        // -------------------------------------------------------
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            log.warn("Callback already processed for transaction: {}. Current status: {}",
                    payment.getTransactionId(), payment.getStatus());
            return "Already processed. Current status: " + payment.getStatus();
        }

        // -------------------------------------------------------
        // STEP 4: UPDATE STATUS
        // Convert the string status from callback to our Enum
        // -------------------------------------------------------
        Payment.PaymentStatus newStatus;
        try {
            // Enum.valueOf() throws IllegalArgumentException if value not found
            newStatus = Payment.PaymentStatus.valueOf(callbackRequest.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Unknown status received in callback: {}", callbackRequest.getStatus());
            throw new PaymentExceptions.InvalidPaymentStatusException(callbackRequest.getStatus());
        }

        payment.setStatus(newStatus);

        // -------------------------------------------------------
        // STEP 5: SAVE UPDATED PAYMENT
        // .save() on an existing entity → runs UPDATE SQL
        // -------------------------------------------------------
        paymentRepository.save(payment);

        log.info("Payment {} updated to status: {}", payment.getTransactionId(), newStatus);
        return "Callback processed successfully. Status updated to: " + newStatus;
    }


    // ============================================================
    // 3. GET PAYMENT STATUS
    // ============================================================

    /**
     * Looks up the current status of a payment by transactionId.
     * This is a READ-ONLY operation — no database changes.
     *
     * @Transactional(readOnly = true) → Performance optimization.
     * Tells Spring this transaction won't write data.
     * Hibernate can skip certain overhead (like dirty checking).
     */
    @Transactional(readOnly = true)
    public PaymentDTOs.PaymentStatusResponse getPaymentStatus(String transactionId) {

        log.info("Fetching status for transaction: {}", transactionId);

        // Find or throw a 404 exception
        Payment payment = paymentRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentExceptions.PaymentNotFoundException(transactionId));

        // Map the entity fields to our response DTO
        return PaymentDTOs.PaymentStatusResponse.builder()
                .transactionId(payment.getTransactionId())
                .paymentStatus(payment.getStatus().name())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .merchantId(payment.getMerchantId())
                .orderId(payment.getOrderId())
                .lastUpdatedTime(payment.getUpdatedAt())
                .build();
    }
}
