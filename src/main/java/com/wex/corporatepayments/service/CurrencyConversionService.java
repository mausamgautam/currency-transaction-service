package com.wex.corporatepayments.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.wex.corporatepayments.client.FiscalDataClient;
import com.wex.corporatepayments.dto.ExchangeRateResponse;
import com.wex.corporatepayments.dto.ExchangeRateResponse.ExchangeRateRecord;

@Service
public class CurrencyConversionService {

    private final FiscalDataClient fiscalDataClient;

    public CurrencyConversionService(FiscalDataClient fiscalDataClient) {
        this.fiscalDataClient = fiscalDataClient;
    }

    public ConvertedAmountDetails calculateConversion(BigDecimal usdAmount, LocalDate purchaseDate, String targetCurrency) {
        // 1. Fetch rates within the filtered historical window
        ExchangeRateResponse response = fiscalDataClient.fetchExchangeRates(targetCurrency, purchaseDate);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalArgumentException("No currency conversion rate is available within 6 months equal to or before the purchase date.");
        }

        // 2. Extract the closest applicable rate (since we sorted descending by record_date, the first element is closest)
        ExchangeRateRecord closestRecord = response.data().get(0);
        LocalDate rateDate = LocalDate.parse(closestRecord.recordDate());
        
        // 3. Strict defensive check: Enforce the 6-month threshold constraint
        if (rateDate.isBefore(purchaseDate.minusMonths(6))) {
            throw new IllegalArgumentException("No currency conversion rate is available within 6 months equal to or before the purchase date.");
        }

        BigDecimal exchangeRate = new BigDecimal(closestRecord.exchangeRate());

        // 4. Compute conversion using multi-digit currency scaling and round cleanly to 2 decimal places
        BigDecimal convertedAmount = usdAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        return new ConvertedAmountDetails(exchangeRate, convertedAmount);
    }

    // Inner record pattern to group our return values cleanly
    public record ConvertedAmountDetails(BigDecimal exchangeRate, BigDecimal convertedAmount) {}
}