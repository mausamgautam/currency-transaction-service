package com.wex.corporatepayments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConvertedTransactionResponse(
    UUID id,
    String description,
    LocalDate transactionDate,
    BigDecimal originalAmountUsd,
    BigDecimal exchangeRate,
    BigDecimal convertedAmount,
    String targetCurrency
) {}