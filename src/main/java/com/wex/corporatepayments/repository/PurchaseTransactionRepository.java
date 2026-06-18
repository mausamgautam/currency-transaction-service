package com.wex.corporatepayments.repository;

import com.wex.corporatepayments.model.PurchaseTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseTransactionRepository extends JpaRepository<PurchaseTransaction, UUID> {
    // Standard CRUD operations (save, findById, findAll) are inherited automatically
}