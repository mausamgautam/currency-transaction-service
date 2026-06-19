package com.wex.corporatepayments.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import com.wex.corporatepayments.client.FiscalDataClient;
import com.wex.corporatepayments.dto.ExchangeRateResponse;
import com.wex.corporatepayments.dto.ExchangeRateResponse.ExchangeRateRecord;
import com.wex.corporatepayments.exception.CurrencyRateUnavailableException;

class CurrencyConversionServiceTest {

    private FiscalDataClient fiscalDataClient;
    private CurrencyConversionService conversionService;

    @BeforeEach
    void setUp() {
        fiscalDataClient = Mockito.mock(FiscalDataClient.class);
        conversionService = new CurrencyConversionService(fiscalDataClient);
    }

    @Test
    void calculateConversion_ShouldSucceed_WhenExactDateMatchFound() {
        // Arrange
        LocalDate purchaseDate = LocalDate.of(2026, 3, 31);
        BigDecimal amountUsd = new BigDecimal("100.00");
        String currency = "Canada-Dollar";

        ExchangeRateRecord record = new ExchangeRateRecord("2026-03-31", "Canada", "Dollar", "1.35");
        ExchangeRateResponse response = new ExchangeRateResponse(List.of(record));

        when(fiscalDataClient.fetchExchangeRates(currency, purchaseDate)).thenReturn(response);

        // Act
        CurrencyConversionService.ConvertedAmountDetails result = 
                conversionService.calculateConversion(amountUsd, purchaseDate, currency);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("1.35"), result.exchangeRate());
        assertEquals(new BigDecimal("135.00"), result.convertedAmount()); // 100.00 * 1.35
    }

    @Test
    void calculateConversion_ShouldSucceed_WhenRateIsWithinSixMonths() {
        // Arrange
        LocalDate purchaseDate = LocalDate.of(2026, 6, 18); // Purchase date
        LocalDate rateDate = LocalDate.of(2026, 3, 31);    // Rate date (~2.5 months prior)
        BigDecimal amountUsd = new BigDecimal("10.00");
        String currency = "Euro Zone-Euro";

        ExchangeRateRecord record = new ExchangeRateRecord(rateDate.toString(), "Euro Zone", "Euro", "0.92");
        ExchangeRateResponse response = new ExchangeRateResponse(List.of(record));

        when(fiscalDataClient.fetchExchangeRates(currency, purchaseDate)).thenReturn(response);

        // Act
        CurrencyConversionService.ConvertedAmountDetails result = 
                conversionService.calculateConversion(amountUsd, purchaseDate, currency);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("9.20"), result.convertedAmount());
    }

    @Test
    void calculateConversion_ShouldThrowException_WhenRateIsOlderThanSixMonths() {
        // Arrange
        LocalDate purchaseDate = LocalDate.of(2026, 6, 18);
        LocalDate oldRateDate = LocalDate.of(2025, 11, 15); // Out of bounds (> 6 months)
        BigDecimal amountUsd = new BigDecimal("100.00");
        String currency = "Canada-Dollar";

        ExchangeRateRecord record = new ExchangeRateRecord(oldRateDate.toString(), "Canada", "Dollar", "1.35");
        ExchangeRateResponse response = new ExchangeRateResponse(List.of(record));

        when(fiscalDataClient.fetchExchangeRates(currency, purchaseDate)).thenReturn(response);

        // Act & Assert
        CurrencyRateUnavailableException exception = assertThrows(CurrencyRateUnavailableException.class, () -> 
                conversionService.calculateConversion(amountUsd, purchaseDate, currency)
        );

        assertTrue(exception.getMessage().contains("No currency conversion rate is available within 6 months"));
    }

    @Test
    void calculateConversion_ShouldThrowException_WhenResponseIsEmpty() {
        // Arrange
        LocalDate purchaseDate = LocalDate.of(2026, 6, 18);
        BigDecimal amountUsd = new BigDecimal("100.00");
        String currency = "Canada-Dollar";

        ExchangeRateResponse response = new ExchangeRateResponse(List.of());

        when(fiscalDataClient.fetchExchangeRates(currency, purchaseDate)).thenReturn(response);

        // Act & Assert
        CurrencyRateUnavailableException exception = assertThrows(CurrencyRateUnavailableException.class, () -> 
                conversionService.calculateConversion(amountUsd, purchaseDate, currency)
        );

        assertTrue(exception.getMessage().contains("No currency conversion rate is available within 6 months"));
    }

    @Test
    void calculateConversion_ShouldThrowException_WhenResponseIsNull() {
        // Arrange
        LocalDate purchaseDate = LocalDate.of(2026, 6, 18);
        BigDecimal amountUsd = new BigDecimal("100.00");
        String currency = "Canada-Dollar";

        when(fiscalDataClient.fetchExchangeRates(currency, purchaseDate)).thenReturn(null);

        // Act & Assert
        CurrencyRateUnavailableException exception = assertThrows(CurrencyRateUnavailableException.class, () -> 
                conversionService.calculateConversion(amountUsd, purchaseDate, currency)
        );

        assertTrue(exception.getMessage().contains("No currency conversion rate is available within 6 months"));
    }
}