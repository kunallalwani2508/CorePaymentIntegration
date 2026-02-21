package com.payments.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================
 * MAIN ENTRY POINT
 * ============================================================
 * This is where your Spring Boot application starts.
 *
 * @SpringBootApplication is actually 3 annotations in one:
 *   1. @Configuration      - This class can define Spring beans
 *   2. @EnableAutoConfiguration - Spring Boot auto-configures things for us
 *   3. @ComponentScan      - Scans this package for @Service, @Controller etc.
 *
 * When you run this class, Spring Boot:
 *   - Starts an embedded Tomcat server on port 8080
 *   - Connects to the database
 *   - Registers all your REST endpoints
 * ============================================================
 */
@SpringBootApplication
public class CorePaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorePaymentApplication.class, args);
        System.out.println("\n✅ Core Payment System is running!");
        System.out.println("📡 API available at: http://localhost:8080/api/payments");
    }
}
