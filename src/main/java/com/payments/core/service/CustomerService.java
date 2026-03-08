package com.payments.core.service;

import com.payments.core.dto.CustomerDTOs;
import com.payments.core.model.Customer;
import com.payments.core.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // CREATE
    @Transactional
    public CustomerDTOs.CustomerResponse createCustomer(CustomerDTOs.CustomerRequest request) {
        log.info("Creating customer with email: {}", request.getEmail());

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Customer with email already exists: " + request.getEmail());
        }

        Customer customer = Customer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        Customer saved = customerRepository.save(customer);
        return mapToResponse(saved);
    }

    // GET ALL
    @Transactional(readOnly = true)
    public List<CustomerDTOs.CustomerResponse> getAllCustomers() {
        log.info("Fetching all customers");
        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // GET BY ID
    @Transactional(readOnly = true)
    public CustomerDTOs.CustomerResponse getCustomerById(Long id) {
        log.info("Fetching customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        return mapToResponse(customer);
    }

    // UPDATE
    @Transactional
    public CustomerDTOs.CustomerResponse updateCustomer(Long id, CustomerDTOs.CustomerRequest request) {
        log.info("Updating customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());

        Customer updated = customerRepository.save(customer);
        return mapToResponse(updated);
    }

    // DELETE
    @Transactional
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with id: {}", id);
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found with id: " + id);
        }
        customerRepository.deleteById(id);
    }

    // HELPER — Entity to DTO
    private CustomerDTOs.CustomerResponse mapToResponse(Customer customer) {
        return CustomerDTOs.CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}