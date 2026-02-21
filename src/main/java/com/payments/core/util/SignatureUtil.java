package com.payments.core.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * ============================================================
 * SIGNATURE UTILITY
 * ============================================================
 * This class handles generating and verifying "signatures."
 *
 * WHAT IS A SIGNATURE?
 * When a payment gateway sends us a callback, how do we know
 * it's REALLY from the gateway and not from a hacker trying
 * to fake a SUCCESS status?
 *
 * The gateway signs the callback using a shared secret key.
 * We verify the signature matches what we'd compute ourselves.
 * If they don't match → reject the callback immediately.
 *
 * HOW IT WORKS (HMAC-SHA256 simplified):
 * 1. Take the transaction ID + status
 * 2. Combine with a secret key
 * 3. Run through SHA-256 hash algorithm
 * 4. Result is a fixed-length hex string (the "signature")
 *
 * Even a tiny change in inputs → completely different signature.
 * A hacker without the secret key can't produce a valid signature.
 *
 * @Component → Registers this class as a Spring Bean.
 *   We can then inject it with @Autowired or constructor injection.
 *
 * @Value → Reads a value from application.properties.
 *   The format is: @Value("${property.key}")
 * ============================================================
 */
@Component
@Slf4j
public class SignatureUtil {

    /**
     * Our secret key — shared between us and the payment gateway.
     * In a real project, NEVER hardcode this. Use environment variables.
     * We default to "mock-secret-key" here for development.
     */
    @Value("${payment.gateway.secret-key:mock-secret-key}")
    private String secretKey;

    /**
     * Generates a signature for a given transaction.
     *
     * In production: use proper HMAC-SHA256.
     * Here we use a simplified hash for learning purposes.
     *
     * @param transactionId - the unique transaction ID
     * @param status        - the payment status (SUCCESS/FAILED)
     * @return hex string signature
     */
    public String generateSignature(String transactionId, String status) {
        try {
            // Combine all data into one string — order must match gateway's format
            String dataToSign = transactionId + "|" + status + "|" + secretKey;

            // SHA-256 is a cryptographic hash function
            // MessageDigest is Java's built-in crypto utility
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert string to bytes, then hash them
            byte[] hashBytes = digest.digest(dataToSign.getBytes(StandardCharsets.UTF_8));

            // Convert raw bytes to readable hex string
            // Example: [0x4A, 0x3F] → "4a3f"
            return HexFormat.of().formatHex(hashBytes);

        } catch (Exception e) {
            log.error("Failed to generate signature", e);
            throw new RuntimeException("Signature generation failed");
        }
    }

    /**
     * Verifies that the signature in a callback matches what we'd generate.
     *
     * @param transactionId - transaction ID from callback
     * @param status        - status from callback
     * @param receivedSignature - signature sent by gateway
     * @return true if valid, false if tampered
     */
    public boolean isValidSignature(String transactionId, String status, String receivedSignature) {
        String expectedSignature = generateSignature(transactionId, status);

        // .equals() compares actual content of the strings
        boolean isValid = expectedSignature.equals(receivedSignature);

        if (!isValid) {
            log.warn("Signature mismatch! Expected: {}, Received: {}",
                    expectedSignature, receivedSignature);
        }

        return isValid;
    }
}
