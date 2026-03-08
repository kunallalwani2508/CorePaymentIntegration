package com.payments.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_methods")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Linked to Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MethodType methodType;

    private String provider;       // e.g. HDFC, ICICI, GooglePay, PhonePe

    // Masked details only — never store raw sensitive data
    private String maskedAccount;  // e.g. XXXX-XXXX-XXXX-4242 for card, XXXX@okicici for UPI

    private String accountHolderName;

    @Column(nullable = false)
    private Boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MethodStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum MethodType {
        UPI, CARD, NET_BANKING, WALLET
    }

    public enum MethodStatus {
        ACTIVE, INACTIVE
    }
}