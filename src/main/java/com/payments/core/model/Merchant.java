package com.payments.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    // Business Details
    private String businessType;

    private String gstin;

    // API Key (auto-generated on registration)
    @Column(nullable = false, unique = true)
    private String apiKey;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum MerchantStatus {
        ACTIVE, INACTIVE
    }
}