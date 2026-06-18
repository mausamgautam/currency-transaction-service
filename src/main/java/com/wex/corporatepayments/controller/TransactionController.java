package com.wex.corporatepayments.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wex.corporatepayments.dto.ConvertedTransactionResponse;
import com.wex.corporatepayments.dto.TransactionRequest;
import com.wex.corporatepayments.model.PurchaseTransaction;
import com.wex.corporatepayments.repository.PurchaseTransactionRepository;
import com.wex.corporatepayments.service.CurrencyConversionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    // Inject the CurrencyConversionService into your existing constructor
    private  final CurrencyConversionService conversionService;
    private  final PurchaseTransactionRepository repository;

    public TransactionController(
        PurchaseTransactionRepository repository, 
        CurrencyConversionService conversionService) {
    this.repository = repository;
    this.conversionService = conversionService;
    }


    @PostMapping
    public ResponseEntity<PurchaseTransaction> createTransaction(@Valid @RequestBody TransactionRequest request) {
        // Map DTO to the persistent Entity block
        PurchaseTransaction transaction = new PurchaseTransaction(
                request.getDescription(),
                request.getTransactionDate(),
                request.getPurchaseAmount()
        );

        // Commit transaction to H2 database
        PurchaseTransaction savedTransaction = repository.save(transaction);

        return new ResponseEntity<>(savedTransaction, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConvertedTransactionResponse> getConvertedTransaction(
        @PathVariable UUID id,
        @RequestParam String targetCurrency) {

    // 1. Fetch transaction from database, throwing a 404 if not found
    PurchaseTransaction transaction = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + id));

    // 2. Compute the multi-currency conversion details via the service layer
    CurrencyConversionService.ConvertedAmountDetails details = conversionService.calculateConversion(
            transaction.getPurchaseAmount(),
            transaction.getTransactionDate(),
            targetCurrency
    );

    // 3. Map everything into the finalized response payload
    ConvertedTransactionResponse response = new ConvertedTransactionResponse(
            transaction.getId(),
            transaction.getDescription(),
            transaction.getTransactionDate(),
            transaction.getPurchaseAmount(),
            details.exchangeRate(),
            details.convertedAmount(),
            targetCurrency
    );

    return ResponseEntity.ok(response);
}
}