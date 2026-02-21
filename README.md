# 💳 Core Payment Integration System

A **beginner-friendly** backend payment processing system built with **Java + Spring Boot**.
Designed as a portfolio/learning project for entry-level Java developers.

---

## 🎯 What This Project Does

This system simulates a real-world payment backend — the kind used by companies like Razorpay, PayU, or CCAvenue under the hood.

**The flow:**
```
Merchant App ──POST──▶ /api/payments/initiate ──▶ Returns checkout URL
                                                          │
User visits checkout URL, completes payment               │
                                                          ▼
Payment Gateway ──POST──▶ /api/payments/callback ──▶ Updates status in DB
                                                          │
Merchant checks ──GET──▶ /api/payments/status/{id} ──▶ Returns SUCCESS/FAILED
```

---

## 🏗️ Project Structure (Layered Architecture)

```
src/main/java/com/payments/core/
│
├── CorePaymentApplication.java     ← App entry point (main method)
│
├── controller/
│   └── PaymentController.java      ← Handles HTTP requests/responses
│
├── service/
│   └── PaymentService.java         ← Business logic (the brain)
│
├── repository/
│   └── PaymentRepository.java      ← Database operations (JPA)
│
├── model/
│   └── Payment.java                ← Database entity (maps to MySQL table)
│
├── dto/
│   └── PaymentDTOs.java            ← Request/Response data transfer objects
│
├── exception/
│   ├── PaymentExceptions.java      ← Custom exception classes
│   └── GlobalExceptionHandler.java ← Centralized error handling
│
└── util/
    └── SignatureUtil.java          ← Callback signature verification
```

### Why Layered Architecture?
| Layer | Role | Talks To |
|-------|------|----------|
| Controller | Receive HTTP, return JSON | Service |
| Service | Business logic, validation | Repository, Utils |
| Repository | Read/write database | MySQL |
| Model | Java ↔ DB mapping | — |
| DTO | Data shape for API | — |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8+
- An IDE (IntelliJ IDEA Community Edition — free and recommended)

### Step 1: Clone the project
```bash
git clone https://github.com/YOUR_USERNAME/core-payment-system.git
cd core-payment-system
```

### Step 2: Set up MySQL database
```sql
-- Run this in MySQL Workbench or terminal
CREATE DATABASE payment_db;
```

### Step 3: Configure your database credentials
Open `src/main/resources/application.properties` and update:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/payment_db
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 4: Run the application
```bash
# Using Maven
mvn spring-boot:run

# Or compile and run the JAR
mvn clean package
java -jar target/core-payment-system-1.0.0.jar
```

The app starts at: **http://localhost:8080**

### Step 5: Run tests
```bash
mvn test
```
> Tests use H2 (in-memory database) — no MySQL needed!

---

## 📡 API Reference

### 1. Initiate Payment
```http
POST /api/payments/initiate
Content-Type: application/json

{
  "merchantId": "MERCHANT_001",
  "orderId": "ORDER_12345",
  "amount": 499.99,
  "currency": "INR",
  "callbackUrl": "https://myshop.com/payment/callback"
}
```
**Response (201 Created):**
```json
{
  "transactionId": "TXN-A1B2C3D4E5F6...",
  "paymentUrl": "http://mock-gateway.com/checkout?txn=TXN-...",
  "status": "PENDING",
  "message": "Payment initiated. Redirect user to paymentUrl to complete payment."
}
```

---

### 2. Payment Callback (called by gateway)
```http
POST /api/payments/callback
Content-Type: application/json

{
  "transactionId": "TXN-A1B2C3D4E5F6...",
  "status": "SUCCESS",
  "signature": "a3f4b2c1..."
}
```
**Response (200 OK):**
```
Callback processed successfully. Status updated to: SUCCESS
```

---

### 3. Check Payment Status
```http
GET /api/payments/status/TXN-A1B2C3D4E5F6...
```
**Response (200 OK):**
```json
{
  "transactionId": "TXN-A1B2C3D4E5F6...",
  "paymentStatus": "SUCCESS",
  "amount": 499.99,
  "currency": "INR",
  "merchantId": "MERCHANT_001",
  "orderId": "ORDER_12345",
  "lastUpdatedTime": "2024-01-15T10:30:00"
}
```

---

### 4. Health Check
```http
GET /api/payments/health
```

---

## 🛡️ Key Concepts Implemented

### Idempotency (Duplicate Prevention)
If a merchant sends the same `merchantId + orderId` twice, the system returns `409 Conflict` instead of creating a duplicate payment. This is critical in payment systems to prevent double-charging.

### Callback Signature Verification
When the payment gateway sends a callback, we verify its `signature` using SHA-256 hashing with a shared secret key. If the signature doesn't match → request rejected immediately. This prevents hackers from faking a `SUCCESS` status.

### Global Exception Handling
All exceptions are caught by `GlobalExceptionHandler` and converted to consistent JSON error responses — no raw stack traces exposed to API callers.

### @Transactional
Critical operations are wrapped in database transactions. If anything fails mid-way, the entire operation rolls back — preventing partial/corrupted data.

---

## ❌ Error Responses

All errors follow a consistent format:
```json
{
  "statusCode": 404,
  "errorCode": "PAYMENT_NOT_FOUND",
  "message": "Payment not found for transaction ID: TXN-XYZ",
  "timestamp": "2024-01-15T10:30:00"
}
```

| Scenario | HTTP Status | Error Code |
|----------|-------------|------------|
| Transaction not found | 404 | `PAYMENT_NOT_FOUND` |
| Duplicate payment | 409 | `DUPLICATE_PAYMENT` |
| Invalid signature | 400 | `INVALID_SIGNATURE` |
| Invalid status | 422 | `INVALID_STATUS_TRANSITION` |
| Validation failure | 400 | `VALIDATION_FAILED` |
| Server error | 500 | `INTERNAL_SERVER_ERROR` |

---

## 🧪 Testing with Postman

Import and test manually:

1. **Health check:** `GET http://localhost:8080/api/payments/health`
2. **Initiate payment:** Use the request body from the API Reference above
3. **Copy the `transactionId`** from the response
4. **Generate a signature** (for testing, the signature = SHA-256 of `transactionId|SUCCESS|mock-secret-key`)
5. **Send callback** with the transactionId, status, and signature
6. **Check status:** `GET http://localhost:8080/api/payments/status/{transactionId}`

---

## 🗄️ Database Table

Auto-created by Hibernate (`spring.jpa.hibernate.ddl-auto=update`):

```sql
CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  VARCHAR(255) NOT NULL UNIQUE,
    merchant_id     VARCHAR(255) NOT NULL,
    order_id        VARCHAR(255) NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    callback_url    VARCHAR(255),
    payment_url     VARCHAR(500),
    gateway_signature VARCHAR(255),
    created_at      DATETIME,
    updated_at      DATETIME
);
```

---

## 🔮 Future Enhancements (Ideas for your next version)

- [ ] Integrate with **Razorpay sandbox API** (free to sign up)
- [ ] Add **Spring Security** for API key authentication
- [ ] Add **Swagger/OpenAPI** UI for interactive API docs
- [ ] Implement **Kafka** for async event-driven callbacks
- [ ] Add **retry mechanism** for failed callbacks
- [ ] Build a **React/Angular dashboard** for merchants

---

## 📚 Spring Boot Concepts You'll Learn

| Concept | Where in Code |
|---------|--------------|
| `@RestController` | PaymentController.java |
| `@Service` | PaymentService.java |
| `@Repository` + JPA | PaymentRepository.java |
| `@Entity` + `@Table` | Payment.java |
| `@Transactional` | PaymentService.java |
| `@ControllerAdvice` | GlobalExceptionHandler.java |
| `@Valid` + Validation | PaymentController + DTOs |
| Custom Exceptions | PaymentExceptions.java |
| Mockito Unit Testing | PaymentServiceTest.java |
| Builder Pattern (Lombok) | All DTO/Entity classes |

---

## 👤 Author

**[Your Name]**
- GitHub: [@your-username](https://github.com/your-username)
- LinkedIn: [Your LinkedIn](https://linkedin.com/in/your-profile)

---

## 📄 License

MIT License — free to use for learning and portfolio purposes.
