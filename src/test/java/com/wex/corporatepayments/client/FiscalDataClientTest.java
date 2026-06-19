package com.wex.corporatepayments.client;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.corporatepayments.dto.ExchangeRateResponse;
import com.wex.corporatepayments.dto.ExchangeRateResponse.ExchangeRateRecord;

@RestClientTest(FiscalDataClient.class)
class FiscalDataClientTest {

    @Autowired
    private FiscalDataClient fiscalDataClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fetchExchangeRates_ShouldReturnResponse_WhenApiCallIsSuccessful() throws Exception {
        // Arrange
        String currency = "Canada-Dollar";
        LocalDate purchaseDate = LocalDate.of(2026, 6, 18);

        ExchangeRateRecord record = new ExchangeRateRecord("2026-03-31", "Canada", "Dollar", "1.35");
        ExchangeRateResponse mockResponse = new ExchangeRateResponse(List.of(record));
        String mockResponseJson = objectMapper.writeValueAsString(mockResponse);

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("od/rates_of_exchange")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        // Act
        ExchangeRateResponse response = fiscalDataClient.fetchExchangeRates(currency, purchaseDate);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.data().size());
        assertEquals("1.35", response.data().get(0).exchangeRate());
        assertEquals("2026-03-31", response.data().get(0).recordDate());
        mockServer.verify();
    }

    @Test
    void fetchExchangeRates_ShouldThrowRuntimeException_WhenApiCallFails() {
        // Arrange
        String currency = "Canada-Dollar";
        LocalDate purchaseDate = LocalDate.of(2026, 6, 18);

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("od/rates_of_exchange")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withServerError());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                fiscalDataClient.fetchExchangeRates(currency, purchaseDate)
        );

        assertEquals("Failed to pull currency conversion schemas from US Treasury gateway", exception.getMessage());
        mockServer.verify();
    }
}
