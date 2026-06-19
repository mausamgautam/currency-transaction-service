package com.wex.corporatepayments.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class PurchaseTransactionTest {

    @Test
    void testGettersAndSetters() {
        PurchaseTransaction transaction = new PurchaseTransaction();
        
        UUID id = UUID.randomUUID();
        transaction.setId(id);
        assertEquals(id, transaction.getId());

        transaction.setDescription("Test Description");
        assertEquals("Test Description", transaction.getDescription());

        LocalDate date = LocalDate.of(2026, 6, 19);
        transaction.setTransactionDate(date);
        assertEquals(date, transaction.getTransactionDate());

        BigDecimal amount = new BigDecimal("100.567");
        transaction.setPurchaseAmount(amount);
        assertEquals(new BigDecimal("100.57"), transaction.getPurchaseAmount()); // 100.567 rounded to 2 decimals HALF_UP
    }

    @Test
    void testConstructorRounding() {
        PurchaseTransaction transaction = new PurchaseTransaction(
                "Constructor Test",
                LocalDate.of(2026, 6, 19),
                new BigDecimal("200.123")
        );

        assertEquals("Constructor Test", transaction.getDescription());
        assertEquals(LocalDate.of(2026, 6, 19), transaction.getTransactionDate());
        assertEquals(new BigDecimal("200.12"), transaction.getPurchaseAmount()); // 200.123 rounded to 2 decimals HALF_UP
    }

    @Test
    void testNullAmountHandling() {
        PurchaseTransaction transaction = new PurchaseTransaction("Null Amount Test", LocalDate.now(), null);
        assertNull(transaction.getPurchaseAmount());

        transaction.setPurchaseAmount(null);
        assertNull(transaction.getPurchaseAmount());
    }
}
