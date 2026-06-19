package com.wex.corporatepayments.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.corporatepayments.client.FiscalDataClient;
import com.wex.corporatepayments.dto.ExchangeRateResponse;
import com.wex.corporatepayments.dto.TransactionRequest;
import com.wex.corporatepayments.model.PurchaseTransaction;
import com.wex.corporatepayments.repository.PurchaseTransactionRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseTransactionRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FiscalDataClient fiscalDataClient;

    @Test
    void createTransaction_ShouldPersistRecord_WhenPayloadIsValid() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setDescription("Integration Test Purchase");
        request.setTransactionDate(LocalDate.of(2026, 6, 18));
        request.setPurchaseAmount(new BigDecimal("250.50"));

        long initialCount = repository.count();

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Integration Test Purchase"))
                .andExpect(jsonPath("$.purchaseAmount").value(250.50));

        // Verify database actually incremented a row
        assertEquals(initialCount + 1, repository.count());
    }

    @Test
    void createTransaction_ShouldReturnBadRequest_WhenDescriptionExceedsFiftyChars() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        // 51 characters long string
        request.setDescription("This description string is purposefully way too long extension");
        request.setTransactionDate(LocalDate.of(2026, 6, 18));
        request.setPurchaseAmount(new BigDecimal("10.00"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getConvertedTransaction_ShouldSucceed_WhenRateIsWithinSixMonths() throws Exception {
        // Arrange
        PurchaseTransaction transaction = new PurchaseTransaction(
                "Test Purchase",
                LocalDate.of(2026, 6, 18),
                new BigDecimal("100.00")
        );
        transaction = repository.save(transaction);

        ExchangeRateResponse.ExchangeRateRecord record = 
                new ExchangeRateResponse.ExchangeRateRecord("2026-03-31", "Canada", "Dollar", "1.35");
        ExchangeRateResponse response = new ExchangeRateResponse(List.of(record));

        when(fiscalDataClient.fetchExchangeRates("Canada-Dollar", transaction.getTransactionDate()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/" + transaction.getId())
                        .param("targetCurrency", "Canada-Dollar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()))
                .andExpect(jsonPath("$.description").value("Test Purchase"))
                .andExpect(jsonPath("$.originalAmountUsd").value(100.00))
                .andExpect(jsonPath("$.exchangeRate").value(1.35))
                .andExpect(jsonPath("$.convertedAmount").value(135.00))
                .andExpect(jsonPath("$.targetCurrency").value("Canada-Dollar"));
    }

    @Test
    void getConvertedTransaction_ShouldFail_WhenRateIsOlderThanSixMonths() throws Exception {
        // Arrange
        PurchaseTransaction transaction = new PurchaseTransaction(
                "Test Old Purchase",
                LocalDate.of(2026, 6, 18),
                new BigDecimal("100.00")
        );
        transaction = repository.save(transaction);

        ExchangeRateResponse.ExchangeRateRecord record = 
                new ExchangeRateResponse.ExchangeRateRecord("2025-11-15", "Canada", "Dollar", "1.35"); // > 6 months
        ExchangeRateResponse response = new ExchangeRateResponse(List.of(record));

        when(fiscalDataClient.fetchExchangeRates("Canada-Dollar", transaction.getTransactionDate()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/" + transaction.getId())
                        .param("targetCurrency", "Canada-Dollar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No currency conversion rate is available within 6 months equal to or before the purchase date."));
    }

    @Test
    void getConvertedTransaction_ShouldReturnNotFound_WhenTransactionDoesNotExist() throws Exception {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/" + nonExistentId)
                        .param("targetCurrency", "Canada-Dollar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Transaction not found with ID: " + nonExistentId));
    }
}