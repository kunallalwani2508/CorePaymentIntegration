package com.payments.core.controller;

import com.payments.core.dto.MerchantDTOs;
import com.payments.core.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ResponseEntity<MerchantDTOs.MerchantResponse> registerMerchant(
            @RequestBody MerchantDTOs.MerchantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(merchantService.registerMerchant(request));
    }

    @GetMapping
    public ResponseEntity<List<MerchantDTOs.MerchantResponse>> getAllMerchants() {
        return ResponseEntity.ok(merchantService.getAllMerchants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MerchantDTOs.MerchantResponse> getMerchantById(@PathVariable Long id) {
        return ResponseEntity.ok(merchantService.getMerchantById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MerchantDTOs.MerchantResponse> updateMerchant(
            @PathVariable Long id,
            @RequestBody MerchantDTOs.MerchantRequest request) {
        return ResponseEntity.ok(merchantService.updateMerchant(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateMerchant(@PathVariable Long id) {
        return ResponseEntity.ok(merchantService.deactivateMerchant(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<String> activateMerchant(@PathVariable Long id) {
        return ResponseEntity.ok(merchantService.activateMerchant(id));
    }
}