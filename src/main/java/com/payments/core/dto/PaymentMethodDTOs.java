package com.payments.core.dto;

import lombok.*;
import java.time.LocalDateTime;

public class PaymentMethodDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodRequest {
        private Long customerId;
        private String methodType;      // UPI / CARD / NET_BANKING / WALLET
        private String provider;        // GooglePay, HDFC, PhonePe etc.
        private String maskedAccount;   // XXXX@okicici or XXXX-4242
        private String accountHolderName;
        private Boolean isDefault;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodResponse {
        private Long id;
        private Long customerId;
        private String customerName;
        private String methodType;
        private String provider;
        private String maskedAccount;
        private String accountHolderName;
        private Boolean isDefault;
        private String status;
        private LocalDateTime createdAt;
    }
}