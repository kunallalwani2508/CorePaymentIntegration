package com.payments.core.dto;

import lombok.*;

import java.time.LocalDateTime;

public class CustomerDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerRequest {
        private String name;
        private String email;
        private String phone;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerResponse {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String address;
        private LocalDateTime createdAt;
    }
}