package com.payments.core.dto;

import lombok.*;
import java.time.LocalDateTime;

public class MerchantDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantRequest {
        private String name;
        private String email;
        private String phone;
        private String businessType;
        private String gstin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantResponse {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String businessType;
        private String gstin;
        private String apiKey;
        private String status;
        private LocalDateTime createdAt;
    }
}