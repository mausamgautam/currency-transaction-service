package com.wex.corporatepayments.controller;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.wex.corporatepayments.exception.CurrencyRateUnavailableException;
import com.wex.corporatepayments.exception.TransactionNotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleCurrencyRateUnavailableException_ShouldReturnBadRequest() {
        CurrencyRateUnavailableException ex = new CurrencyRateUnavailableException("No rate available");
        ResponseEntity<Map<String, String>> response = handler.handleCurrencyRateUnavailableException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No rate available", response.getBody().get("error"));
    }

    @Test
    void handleTransactionNotFoundException_ShouldReturnNotFound() {
        TransactionNotFoundException ex = new TransactionNotFoundException("Transaction missing");
        ResponseEntity<Map<String, String>> response = handler.handleTransactionNotFoundException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Transaction missing", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgumentException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid argument", response.getBody().get("error"));
    }
}
