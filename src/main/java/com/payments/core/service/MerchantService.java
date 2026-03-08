package com.payments.core.service;

import com.payments.core.dto.MerchantDTOs;
import com.payments.core.model.Merchant;
import com.payments.core.repository.MerchantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    // CREATE
    @Transactional
    public MerchantDTOs.MerchantResponse registerMerchant(MerchantDTOs.MerchantRequest request) {
        log.info("Registering merchant with email: {}", request.getEmail());

        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Merchant already exists with email: " + request.getEmail());
        }

        // Auto-generate a unique API key for this merchant
        String apiKey = "MK-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        Merchant merchant = Merchant.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .businessType(request.getBusinessType())
                .gstin(request.getGstin())
                .apiKey(apiKey)
                .status(Merchant.MerchantStatus.ACTIVE) // default ACTIVE on registration
                .build();

        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant registered with apiKey: {}", saved.getApiKey());
        return mapToResponse(saved);
    }

    // GET ALL
    @Transactional(readOnly = true)
    public List<MerchantDTOs.MerchantResponse> getAllMerchants() {
        log.info("Fetching all merchants");
        return merchantRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // GET BY ID
    @Transactional(readOnly = true)
    public MerchantDTOs.MerchantResponse getMerchantById(Long id) {
        log.info("Fetching merchant with id: {}", id);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found with id: " + id));
        return mapToResponse(merchant);
    }

    // UPDATE
    @Transactional
    public MerchantDTOs.MerchantResponse updateMerchant(Long id, MerchantDTOs.MerchantRequest request) {
        log.info("Updating merchant with id: {}", id);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found with id: " + id));

        merchant.setName(request.getName());
        merchant.setPhone(request.getPhone());
        merchant.setBusinessType(request.getBusinessType());
        merchant.setGstin(request.getGstin());

        Merchant updated = merchantRepository.save(merchant);
        return mapToResponse(updated);
    }

    // DEACTIVATE (instead of hard delete for merchants)
    @Transactional
    public String deactivateMerchant(Long id) {
        log.info("Deactivating merchant with id: {}", id);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found with id: " + id));

        merchant.setStatus(Merchant.MerchantStatus.INACTIVE);
        merchantRepository.save(merchant);
        return "Merchant deactivated successfully";
    }

    // ACTIVATE
    @Transactional
    public String activateMerchant(Long id) {
        log.info("Activating merchant with id: {}", id);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found with id: " + id));

        merchant.setStatus(Merchant.MerchantStatus.ACTIVE);
        merchantRepository.save(merchant);
        return "Merchant activated successfully";
    }

    // HELPER
    private MerchantDTOs.MerchantResponse mapToResponse(Merchant merchant) {
        return MerchantDTOs.MerchantResponse.builder()
                .id(merchant.getId())
                .name(merchant.getName())
                .email(merchant.getEmail())
                .phone(merchant.getPhone())
                .businessType(merchant.getBusinessType())
                .gstin(merchant.getGstin())
                .apiKey(merchant.getApiKey())
                .status(merchant.getStatus().name())
                .createdAt(merchant.getCreatedAt())
                .build();
    }
}