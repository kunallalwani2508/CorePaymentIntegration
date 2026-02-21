package com.payments.core.repository;

import com.payments.core.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * ============================================================
 * REPOSITORY LAYER
 * ============================================================
 * The Repository is responsible for all database operations.
 * Think of it as the "database access layer."
 *
 * MAGIC OF JpaRepository:
 * By extending JpaRepository<Payment, Long>, Spring Data JPA
 * automatically gives us these methods FOR FREE — no SQL needed!
 *
 *   save(entity)         → INSERT or UPDATE
 *   findById(id)         → SELECT WHERE id = ?
 *   findAll()            → SELECT * FROM payments
 *   delete(entity)       → DELETE
 *   count()              → SELECT COUNT(*)
 *   existsById(id)       → SELECT COUNT(*) > 0
 *
 * The two generics: <Payment, Long>
 *   Payment = the Entity this repo manages
 *   Long    = the data type of the primary key (@Id)
 *
 * @Repository → Marks this as a Spring-managed bean.
 *               Also translates database exceptions into Spring exceptions.
 * ============================================================
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * SPRING DATA MAGIC — METHOD NAME QUERIES
     * ========================================
     * Spring reads the method name and auto-generates the SQL.
     * "findBy" + "TransactionId" → SELECT * FROM payments WHERE transaction_id = ?
     *
     * Optional<Payment> means: the result might or might not exist.
     * We use Optional to avoid NullPointerExceptions.
     * caller can do: repo.findByTransactionId("xyz").orElseThrow(...)
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Generated SQL: SELECT * FROM payments WHERE merchant_id = ?
     * Returns a List because a merchant can have many payments.
     */
    List<Payment> findByMerchantId(String merchantId);

    /**
     * Generated SQL: SELECT * FROM payments WHERE merchant_id = ? AND order_id = ?
     * Useful for idempotency check — has this order been paid before?
     */
    Optional<Payment> findByMerchantIdAndOrderId(String merchantId, String orderId);

    /**
     * CUSTOM QUERY using JPQL (Java Persistence Query Language)
     * JPQL looks like SQL but uses CLASS names and FIELD names, not table/column names.
     *
     * @Query → lets us write our own query when method name magic isn't enough
     * @Param → binds the method parameter to the :status placeholder in the query
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status")
    List<Payment> findAllByStatus(@Param("status") Payment.PaymentStatus status);

    /**
     * Checks if a transaction ID already exists.
     * Used in idempotency validation — "have we seen this transaction before?"
     * Generated SQL: SELECT COUNT(*) > 0 FROM payments WHERE transaction_id = ?
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Checks if this exact merchant+order combination exists.
     * Prevents a merchant from paying for the same order twice.
     */
    boolean existsByMerchantIdAndOrderId(String merchantId, String orderId);
}
