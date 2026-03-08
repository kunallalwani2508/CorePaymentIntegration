# 💳 Core Payment Integration System

A secure, scalable payment processing backend built with **Java & Spring Boot**.
Designed with real-world FinTech practices — idempotent transactions, callback handling, and a clean layered architecture.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Server | Apache Tomcat (embedded) |
| Database | MySQL / H2 (dev) |
| ORM | Spring Data JPA + Hibernate |
| Build Tool | Maven |
| Utilities | Lombok |

---

## 📁 Project Structure

```
src/
└── main/
    └── java/com/payments/core/
        ├── controller/        # REST API endpoints
        ├── service/           # Business logic
        ├── repository/        # Database operations
        ├── model/             # JPA entities
        ├── dto/               # Request/Response objects
        ├── exception/         # Global error handling
        └── util/              # Signature & helper utils
```

---

## ⚙️ Setup & Run Locally

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+ (or use H2 for quick start)

### 1. Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/core-payment-system.git
cd core-payment-system
```

### 2. Configure the database

**Option A — MySQL**

Create the database:
```sql
CREATE DATABASE payment_db;
```

Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/payment_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

**Option B — H2 (no setup needed)**

The project runs out of the box with H2 in-memory database for local development.

### 3. Run the application
```bash
mvn spring-boot:run
```

App starts at: `http://localhost:8080`

---

## 📌 API Reference

### 💰 Payments `/api/payments`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments/initiate` | Initiate a new payment |
| POST | `/api/payments/callback` | Handle gateway callback |
| GET | `/api/payments/status/{transactionId}` | Get payment status |
| GET | `/api/payments/all` | Get all payments |

**Initiate Payment — Request Body:**
```json
{
  "merchantId": "M001",
  "orderId": "ORD001",
  "amount": 500.00,
  "currency": "INR",
  "callbackUrl": "http://localhost:8080/api/payments/callback"
}
```

**Initiate Payment — Response:**
```json
{
  "transactionId": "TXN-ABC123",
  "paymentUrl": "http://mock-gateway.com/checkout?txn=TXN-ABC123",
  "status": "PENDING",
  "message": "Payment initiated. Redirect user to paymentUrl to complete payment."
}
```

---

### 👤 Customers `/api/customers`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/customers` | Create a customer |
| GET | `/api/customers` | Get all customers |
| GET | `/api/customers/{id}` | Get customer by ID |
| PUT | `/api/customers/{id}` | Update customer |
| DELETE | `/api/customers/{id}` | Delete customer |

**Create Customer — Request Body:**
```json
{
  "name": "Rahul Sharma",
  "email": "rahul@example.com",
  "phone": "9876543210",
  "address": "Mumbai, India"
}
```

---

### 🏪 Merchants `/api/merchants`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/merchants` | Register a merchant |
| GET | `/api/merchants` | Get all merchants |
| GET | `/api/merchants/{id}` | Get merchant by ID |
| PUT | `/api/merchants/{id}` | Update merchant |
| PATCH | `/api/merchants/{id}/activate` | Activate merchant |
| PATCH | `/api/merchants/{id}/deactivate` | Deactivate merchant |

**Register Merchant — Request Body:**
```json
{
  "name": "TechMart Pvt Ltd",
  "email": "techmart@example.com",
  "phone": "9876543210",
  "businessType": "E-Commerce",
  "gstin": "27AAPFU0939F1ZV"
}
```

> API Key is **auto-generated** on registration.

---

### 💳 Payment Methods `/api/payment-methods`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payment-methods` | Add a payment method |
| GET | `/api/payment-methods/customer/{customerId}` | Get methods by customer |
| GET | `/api/payment-methods/{id}` | Get by ID |
| PATCH | `/api/payment-methods/{id}/set-default` | Set as default |
| DELETE | `/api/payment-methods/{id}` | Remove method |

**Supported Types:** `UPI`, `CARD`, `NET_BANKING`, `WALLET`

**Add Payment Method — Request Body:**
```json
{
  "customerId": 1,
  "methodType": "UPI",
  "provider": "GooglePay",
  "maskedAccount": "rahul@okicici",
  "accountHolderName": "Rahul Sharma",
  "isDefault": true
}
```

---

## 🔑 Key Design Concepts

### Idempotency
Duplicate payment requests for the same `merchantId + orderId` are automatically rejected, preventing double charges.

### Callback Signature Verification
Every gateway callback is verified using HMAC signature validation before updating any transaction status.

### Soft Delete
Merchants and Payment Methods use soft delete (status → `INACTIVE`) instead of hard delete, preserving data integrity across payment history.

### Layered Architecture
```
Controller  →  Service  →  Repository  →  Database
(HTTP)        (Logic)      (JPA)          (MySQL)
```

---

## 🗄️ Database Schema

### payments
| Column | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| transaction_id | VARCHAR | Unique transaction ID |
| merchant_id | VARCHAR | Merchant reference |
| order_id | VARCHAR | Order reference |
| amount | DECIMAL | Payment amount |
| currency | VARCHAR | Currency code |
| status | ENUM | PENDING / SUCCESS / FAILED |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |

### customers
| Column | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| name | VARCHAR | Customer name |
| email | VARCHAR | Unique email |
| phone | VARCHAR | Phone number |
| address | VARCHAR | Address |

### merchants
| Column | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| name | VARCHAR | Business name |
| email | VARCHAR | Unique email |
| api_key | VARCHAR | Auto-generated API key |
| business_type | VARCHAR | Type of business |
| gstin | VARCHAR | GST number |
| status | ENUM | ACTIVE / INACTIVE |

### payment_methods
| Column | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| customer_id | BIGINT | FK → customers |
| method_type | ENUM | UPI / CARD / NET_BANKING / WALLET |
| provider | VARCHAR | GooglePay, HDFC etc. |
| masked_account | VARCHAR | Masked sensitive details |
| is_default | BOOLEAN | Default method flag |
| status | ENUM | ACTIVE / INACTIVE |

---

## 🔮 Future Enhancements

- [ ] Swagger UI for interactive API docs
- [ ] Order management module
- [ ] Refund & settlement module
- [ ] Event-driven architecture with Kafka
- [ ] Integration with real PSPs (Razorpay, Stripe, PayU)
- [ ] Merchant dashboard

---

## 👨‍💻 Author

Built as a personal FinTech project to demonstrate Spring Boot, REST API design, and real-world payment processing concepts.
