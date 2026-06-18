package com.wex.corporatepayments.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExchangeRateResponse(
    @JsonProperty("data") List<ExchangeRateRecord> data
) {
    public record ExchangeRateRecord(
        @JsonProperty("record_date") String recordDate,
        @JsonProperty("country") String country,
        @JsonProperty("currency") String currency,
        @JsonProperty("exchange_rate") String exchangeRate
    ) {}
}