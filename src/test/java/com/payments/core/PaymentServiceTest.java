package com.payments.core;

import com.payments.core.dto.PaymentDTOs;
import com.payments.core.exception.PaymentExceptions;
import com.payments.core.model.Payment;
import com.payments.core.repository.PaymentRepository;
import com.payments.core.service.PaymentService;
import com.payments.core.util.SignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 * UNIT TESTS FOR PaymentService
 * ============================================================
 * Unit tests verify that each METHOD works correctly in ISOLATION.
 * We don't use a real database — we use MOCKS instead.
 *
 * KEY CONCEPTS:
 *
 * @ExtendWith(MockitoExtension.class)
 *   → Tells JUnit to use Mockito for managing mocks.
 *
 * @Mock → Creates a FAKE version of a class.
 *   A mock records method calls and returns fake data.
 *   We control what it returns using 'when(...).thenReturn(...)'.
 *
 * @InjectMocks → Creates a REAL instance of the class under test
 *   and automatically injects the @Mock objects into it.
 *   So: PaymentService gets the mock PaymentRepository and mock SignatureUtil.
 *
 * when(repo.save(any())).thenReturn(fakePayment)
 *   → "When save() is called with any argument, pretend it returned fakePayment"
 *
 * verify(repo, times(1)).save(any())
 *   → "Assert that save() was called exactly once"
 *
 * assertThat(result).isNotNull()
 *   → AssertJ's fluent assertion style (more readable than JUnit's assertEquals)
 *
 * WHY MOCK THE DATABASE?
 * - Tests run instantly (no DB connection needed)
 * - Tests are isolated (one test doesn't affect another)
 * - We test only the service logic, not the database
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    // Mocks — fake/controlled versions of dependencies
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SignatureUtil signatureUtil;

    // The class we're actually testing — gets mocks injected into it
    @InjectMocks
    private PaymentService paymentService;

    // Test data we'll reuse across tests
    private PaymentDTOs.PaymentInitiateRequest validRequest;
    private Payment mockPayment;

    /**
     * @BeforeEach → Runs BEFORE every single test method.
     * We use this to set up fresh test data for each test.
     */
    @BeforeEach
    void setUp() {
        // Build a valid payment request
        validRequest = PaymentDTOs.PaymentInitiateRequest.builder()
                .merchantId("MERCHANT_001")
                .orderId("ORDER_12345")
                .amount(new BigDecimal("499.99"))
                .currency("INR")
                .callbackUrl("https://myshop.com/callback")
                .build();

        // Build a mock Payment that represents what the DB would return
        mockPayment = Payment.builder()
                .id(1L)
                .transactionId("TXN-MOCKID123")
                .merchantId("MERCHANT_001")
                .orderId("ORDER_12345")
                .amount(new BigDecimal("499.99"))
                .currency("INR")
                .status(Payment.PaymentStatus.PENDING)
                .callbackUrl("https://myshop.com/callback")
                .paymentUrl("http://mock-gateway.com/checkout?txn=TXN-MOCKID123")
                .updatedAt(LocalDateTime.now())
                .build();
    }


    // ============================================================
    // TEST GROUP 1: initiatePayment()
    // ============================================================

    @Test
    @DisplayName("Should successfully initiate payment and return transactionId + paymentUrl")
    void initiatePayment_ShouldReturnResponse_WhenRequestIsValid() {

        // ARRANGE — set up what our mocks should do
        when(paymentRepository.existsByMerchantIdAndOrderId(anyString(), anyString()))
                .thenReturn(false);   // No duplicate

        when(signatureUtil.generateSignature(anyString(), anyString()))
                .thenReturn("mock-signature-abc123");

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(mockPayment);   // Pretend DB save returns our mock payment

        // ACT — call the actual method we're testing
        PaymentDTOs.PaymentInitiateResponse response = paymentService.initiatePayment(validRequest);

        // ASSERT — verify the response is what we expect
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo("TXN-MOCKID123");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getPaymentUrl()).isNotBlank();

        // Also verify save() was called exactly once
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw DuplicatePaymentException when same merchant+order already exists")
    void initiatePayment_ShouldThrowException_WhenDuplicatePaymentDetected() {

        // ARRANGE — simulate that a payment already exists
        when(paymentRepository.existsByMerchantIdAndOrderId("MERCHANT_001", "ORDER_12345"))
                .thenReturn(true);  // Duplicate!

        // ACT + ASSERT — verify exception is thrown
        // assertThatThrownBy: runs the lambda and expects it to throw
        assertThatThrownBy(() -> paymentService.initiatePayment(validRequest))
                .isInstanceOf(PaymentExceptions.DuplicatePaymentException.class)
                .hasMessageContaining("MERCHANT_001");

        // Verify save was NEVER called (we should have stopped before that)
        verify(paymentRepository, never()).save(any());
    }


    // ============================================================
    // TEST GROUP 2: processCallback()
    // ============================================================

    @Test
    @DisplayName("Should update payment to SUCCESS when callback is valid")
    void processCallback_ShouldUpdateStatus_WhenCallbackIsValid() {

        // Build a valid callback request
        PaymentDTOs.PaymentCallbackRequest callbackRequest = PaymentDTOs.PaymentCallbackRequest.builder()
                .transactionId("TXN-MOCKID123")
                .status("SUCCESS")
                .signature("valid-signature")
                .build();

        // Signature is valid
        when(signatureUtil.isValidSignature("TXN-MOCKID123", "SUCCESS", "valid-signature"))
                .thenReturn(true);

        // Payment exists in DB
        when(paymentRepository.findByTransactionId("TXN-MOCKID123"))
                .thenReturn(Optional.of(mockPayment));

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(mockPayment);

        // ACT
        String result = paymentService.processCallback(callbackRequest);

        // ASSERT
        assertThat(result).contains("SUCCESS");
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw InvalidSignatureException when callback signature is wrong")
    void processCallback_ShouldThrowException_WhenSignatureIsInvalid() {

        PaymentDTOs.PaymentCallbackRequest callbackRequest = PaymentDTOs.PaymentCallbackRequest.builder()
                .transactionId("TXN-FAKE")
                .status("SUCCESS")
                .signature("hacked-signature")
                .build();

        // Signature check FAILS
        when(signatureUtil.isValidSignature(anyString(), anyString(), anyString()))
                .thenReturn(false);

        assertThatThrownBy(() -> paymentService.processCallback(callbackRequest))
                .isInstanceOf(PaymentExceptions.InvalidSignatureException.class)
                .hasMessageContaining("invalid");

        // DB should NOT be touched at all
        verify(paymentRepository, never()).findByTransactionId(anyString());
    }

    @Test
    @DisplayName("Should return already-processed message for duplicate callback")
    void processCallback_ShouldIgnore_WhenPaymentAlreadyProcessed() {

        // Payment is already SUCCESS (not PENDING)
        mockPayment.setStatus(Payment.PaymentStatus.SUCCESS);

        PaymentDTOs.PaymentCallbackRequest callbackRequest = PaymentDTOs.PaymentCallbackRequest.builder()
                .transactionId("TXN-MOCKID123")
                .status("SUCCESS")
                .signature("valid-sig")
                .build();

        when(signatureUtil.isValidSignature(anyString(), anyString(), anyString()))
                .thenReturn(true);

        when(paymentRepository.findByTransactionId("TXN-MOCKID123"))
                .thenReturn(Optional.of(mockPayment));

        // ACT
        String result = paymentService.processCallback(callbackRequest);

        // ASSERT — should acknowledge without doing anything
        assertThat(result).contains("Already processed");
        // Crucially: save() was never called (no DB write)
        verify(paymentRepository, never()).save(any());
    }


    // ============================================================
    // TEST GROUP 3: getPaymentStatus()
    // ============================================================

    @Test
    @DisplayName("Should return payment status when transaction exists")
    void getPaymentStatus_ShouldReturnStatus_WhenTransactionExists() {

        when(paymentRepository.findByTransactionId("TXN-MOCKID123"))
                .thenReturn(Optional.of(mockPayment));

        // ACT
        PaymentDTOs.PaymentStatusResponse response = paymentService.getPaymentStatus("TXN-MOCKID123");

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo("TXN-MOCKID123");
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("499.99"));
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when transactionId does not exist")
    void getPaymentStatus_ShouldThrowException_WhenNotFound() {

        // Empty Optional = not found in DB
        when(paymentRepository.findByTransactionId("INVALID-TXN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus("INVALID-TXN"))
                .isInstanceOf(PaymentExceptions.PaymentNotFoundException.class)
                .hasMessageContaining("INVALID-TXN");
    }
}
