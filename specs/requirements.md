# WEX Corporate Payments - Product Requirements Specification

This document details the functional specifications and field validations required for the Transaction & Multi-Currency Conversion Service.

---

## 📋 Requirement #1: Store a Purchase Transaction

The application must accept and store (i.e., persist) a purchase transaction with a description, transaction date, and a purchase amount in United States dollars (USD). When the transaction is stored, it must be assigned a unique identifier.

### Field Validation Rules
* **Description:** Must not exceed `50` characters. Required.
* **Transaction Date:** Must be a valid date format (e.g., `YYYY-MM-DD`). Required.
* **Purchase Amount:** Must be a valid positive amount (greater than or equal to `0.01`) rounded to the nearest cent. Required.
* **Unique Identifier:** Must uniquely identify the purchase (automatically generated UUID).

---

## 📋 Requirement #2: Retrieve a Purchase Transaction in a Specified Country's Currency

The application must provide a REST API endpoint to retrieve stored purchase transactions converted into any target currency supported by the US Treasury Reporting Rates of Exchange API. The conversion must be based upon the exchange rate active on the date of the purchase.

### Returned Payload Properties
The response JSON must contain:
1. `id`: The unique transaction identifier (UUID).
2. `description`: The purchase description.
3. `transactionDate`: The purchase transaction date.
4. `originalAmountUsd`: The original purchase amount in USD.
5. `exchangeRate`: The exchange rate used.
6. `convertedAmount`: The converted purchase amount in the target currency.
7. `targetCurrency`: The name of the target currency.

### Currency Conversion Logic & Constraints
* **Approximate Date Matching:** If no exact exchange rate record exists for the transaction date, the application must use the closest currency conversion rate *less than or equal to* the purchase date.
* **6-Month Threshold:** The closest historical exchange rate must be from within the last **6 months** preceding the purchase date.
* **Error Handling:** If no currency conversion rate is available within 6 months equal to or before the purchase date, a graceful error must be returned stating:
  > `"No currency conversion rate is available within 6 months equal to or before the purchase date."`
* **Precision:** The final converted purchase amount in the target currency must be rounded strictly to **two decimal places** (nearest cent) using a financial rounding strategy (e.g., `HALF_UP`).
