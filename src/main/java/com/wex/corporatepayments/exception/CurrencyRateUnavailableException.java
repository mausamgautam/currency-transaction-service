package com.wex.corporatepayments.exception;

public class CurrencyRateUnavailableException extends RuntimeException {
    public CurrencyRateUnavailableException(String message) {
        super(message);
    }
}
