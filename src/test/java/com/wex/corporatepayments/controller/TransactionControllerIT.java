package com.wex.corporatepayments.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.corporatepayments.dto.TransactionRequest;
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
}