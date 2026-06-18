package com.wex.corporatepayments.client;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.wex.corporatepayments.dto.ExchangeRateResponse;

@Component
public class FiscalDataClient {

    private final RestClient restClient;
    private final String endpoint;

    public FiscalDataClient(
            RestClient.Builder restClientBuilder,
            @Value("${integrations.us-treasury.base-url}") String baseUrl,
            @Value("${integrations.us-treasury.endpoint}") String endpoint) {
        
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.endpoint = endpoint;
    }

    public ExchangeRateResponse fetchExchangeRates(String targetCurrency, LocalDate purchaseDate) {
        // Enforce a generous 12-month retrieval query bound to ensure index coverage, 
        // our Service layer will handle the precise 6-month drop constraint validation.
        LocalDate calculationBufferStart = purchaseDate.minusMonths(12);

        // Realignment: target 'country_currency_desc' which maps strings like "Canada-Dollar" or "Euro Zone-Euro"
        String filterParam = String.format("record_date:gte:%s,record_date:lte:%s,country_currency_desc:eq:%s",
                calculationBufferStart, purchaseDate, targetCurrency);

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("filter", filterParam)
                            .queryParam("sort", "-record_date") // Highest date at index 0
                            .queryParam("page[size]", "10")     // Restrict payload to optimization targets
                            .build())
                    .retrieve()
                    .body(ExchangeRateResponse.class);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to pull currency conversion schemas from US Treasury gateway", ex);
        }
    }
}