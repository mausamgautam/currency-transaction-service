# Corporate Transactions & Multi-Currency Conversion Service

A production-ready, highly portable Spring Boot 3.x microservice built to ingest, persist, and retrieve corporate purchase transactions with automated multi-currency conversion capabilities powered by the US Treasury Reporting Rates of Exchange API.

## Requirements
Requirement #1: Store a Purchase Transaction
Your application must be able to accept and store (i.e., persist) a purchase transaction with a description, transaction
date, and a purchase amount in United States dollars. When the transaction is stored, it will be assigned a unique
identifier.
Field requirements

● Description: must not exceed 50 characters
● Transaction date: must be a valid date format
● Purchase amount: must be a valid positive amount rounded to the nearest cent
● Unique identifier: must uniquely identify the purchase



Requirement #2: Retrieve a Purchase Transaction in a Specified Country’s Currency
Based upon purchase transactions previously submitted and stored, your application must provide a way to retrieve the
stored purchase transactions converted to currencies supported by the Treasury Reporting Rates of Exchange API based
upon the exchange rate active for the date of the purchase.
https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange
The retrieved purchase should include the identifier, the description, the transaction date, the original US dollar purchase
amount, the exchange rate used, and the converted amount based upon the specified currency’s exchange rate for the
date of the purchase.

Currency conversion requirements
When converting between currencies, you do not need an exact date match, but must use a currency conversion
rate less than or equal to the purchase date from within the last 6 months.
If no currency conversion rate is available within 6 months equal to or before the purchase date, an error should
be returned stating the purchase cannot be converted to the target currency.
The converted purchase amount to the target currency should be rounded to two decimal places (i.e., cent).


---

## 🏛️ Architectural Highlights & Design DNA

This service was designed from the ground up using clean architecture and domain-driven design patterns, prioritizing financial precision, high performance, and robust error safety:

* **Financial Precision-First:** To prevent floating-point rounding issues common to binary representations (`double` or `float`), all transaction amounts and currency exchange operations strictly utilize `java.math.BigDecimal` with explicit scale alignments (`2` decimal places) and clean `RoundingMode.HALF_UP` configurations.
* **Separation of Concerns:** Implements a clean Controller-Service-Client architecture:
    * `controller`: Encapsulates REST API ingress boundaries, Swagger/OpenAPI exposure, request validation, and global HTTP exception mappings.
    * `service`: Houses core business rule validation (such as enforcing the 6-month historical rate boundary constraint).
    * `client`: Abstracts downstream gateway calls using Spring Boot 3's modern, fluent `RestClient`.
* **API Gateway Throughput Optimization:** Rather than downloading large multi-megabyte historical XML/JSON lists from the federal gateway, the `FiscalDataClient` applies aggressive server-side filtering (`?filter=...`) to download *only* the specific data bracket needed for the target currency and transaction window.
* **Zero-Dependency Portability:** Uses an embedded, in-memory H2 database managed via Spring Data JPA. The application requires zero local database setups, external credentials, or container provisioning to run out of the box.

---

## 🔄 End-to-End Execution Flow

The sequence diagram below details the data flow and boundary validations for storing a transaction and querying it with historical multi-currency conversions:

```mermaid
sequenceDiagram
    autonumber
    actor User as Client (Postman/Swagger UI)
    participant Ctrl as TransactionController
    participant Repo as PurchaseTransactionRepository
    participant DB as H2 Database
    participant Service as CurrencyConversionService
    participant Client as FiscalDataClient
    participant Treasury as US Treasury API

    %% POST Flow
    Note over User, DB: Scenario A: Store Purchase Transaction
    User->>Ctrl: POST /api/v1/transactions (TransactionRequest)
    Note over Ctrl: Validate fields (description <= 50 chars,<br/>positive amount, valid date)
    Ctrl->>Repo: save(PurchaseTransaction)
    Repo->>DB: INSERT INTO purchase_transactions
    DB-->>Repo: Saved entity
    Repo-->>Ctrl: Saved entity (UUID id)
    Ctrl-->>User: 201 Created (PurchaseTransaction JSON)

    %% GET Flow
    Note over User, Treasury: Scenario B: Retrieve Converted Transaction
    User->>Ctrl: GET /api/v1/transactions/{id}?targetCurrency={currency}
    Ctrl->>Repo: findById(id)
    Repo->>DB: SELECT * FROM purchase_transactions WHERE id = ?
    DB-->>Repo: Result row
    alt Transaction not found
        Repo-->>Ctrl: Optional.empty()
        Ctrl-->>User: 404 Not Found (TransactionNotFoundException)
    else Transaction exists
        Repo-->>Ctrl: PurchaseTransaction entity
        Ctrl->>Service: calculateConversion(amount, date, targetCurrency)
        Service->>Client: fetchExchangeRates(targetCurrency, date)
        Note over Client: Calculate start date parameter<br/>(purchaseDate - 12 months)
        Client->>Treasury: GET /rates_of_exchange (filtered & sorted)
        Treasury-->>Client: ExchangeRateResponse (JSON data list)
        Client-->>Service: ExchangeRateResponse
        alt Response empty or null
            Service-->>Ctrl: throw CurrencyRateUnavailableException
            Ctrl-->>User: 400 Bad Request
        else Rate date is before (purchaseDate - 6 months)
            Service-->>Ctrl: throw CurrencyRateUnavailableException
            Ctrl-->>User: 400 Bad Request
        else Rate is valid (within 6 months)
            Note over Service: Select closest rate (index 0)<br/>Multiply amount * rate<br/>Round to 2 decimal places HALF_UP
            Service-->>Ctrl: ConvertedAmountDetails
            Ctrl->>User: 200 OK (ConvertedTransactionResponse JSON)
        end
    end
```

---

## 🛠️ Prerequisites & Local Environment

* **Java:** JDK 17 or 21
* **Build Tool:** Maven (Wrapper included in repository)

---

## 🚀 Getting Started (Build & Run)

To build the application, execute the comprehensive test suite, and launch the server locally on port `8080`, run the following commands:

```bash
# Build the project and execute the unit and integration test suite
./mvnw clean test

# Launch the microservice application locally
./mvnw spring-boot:run
```

Once started:
* **Interactive UI Playground:** Access the Swagger UI dashboard at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) to interact with and test all endpoint requests directly.
* **OpenAPI Specs:** View the generated raw JSON OpenAPI specifications at [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).
* **Database Console:** Access the H2 in-memory database explorer at [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:transactionsdb`, Username: `sa`, Password: `[blank]`).

---

## 🧪 Testing Scope & Coverage (Jacoco)

The test suite covers REST endpoint integrations, domain services, client mock integrations, and structural DTOs. 

To run tests and check the coverage percentage:
```bash
./mvnw clean test
```
The test coverage reports are automatically generated by the Jacoco plugin and can be opened locally at:
`target/site/jacoco/index.html`

### Current Metrics
* **Total Instruction Coverage:** **98%**
* **Total Branch Coverage:** **91%**

| Package | Instruction Cov. | Branch Cov. | Targeted Component Verification |
| :--- | :---: | :---: | :--- |
| `controller` | **100%** | n/a | REST API boundaries, validation filters, exception mappings |
| `client` | **100%** | n/a | Downstream query construction & network gateway mock servers |
| `service` | **100%** | **87%** | Precision calculations & 6-month threshold validation rules |
| `model` | **100%** | **100%** | Entity model constructs and scale alignments |
| `dto` | **100%** | n/a | Request and Response JSON serialization mappings |
| `exception` | **100%** | n/a | Business validation exceptions |
