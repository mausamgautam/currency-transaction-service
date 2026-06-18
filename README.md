# WEX Corporate Payments - Currency & Transaction Management Service

A production-ready, highly portable Spring Boot 3.x microservice built to ingest, persist, and retrieve corporate purchase transactions with automated multi-currency conversion capabilities powered by the US Treasury Reporting Rates of Exchange API.

---

## 🏛️ Architectural Highlights & Design DNA

This service was designed from the ground up using clean architecture principles, prioritizing financial precision, performance optimization, and developer experience:

* **Financial Rigor (Precision-First):** To prevent floating-point rounding errors native to binary types like `double` or `float`, all transaction amounts and currency exchange calculations strictly utilize `java.math.BigDecimal` with explicit `RoundingMode.HALF_UP` configurations.
* **Decoupled & Clean Architecture:** Implements a strict separation of concerns across layers:
    * `controller`: Encapsulates REST ingress boundaries, payload validation, and request/response mapping.
    * `service`: Enforces domain business logic (such as the mandatory 6-month historical rate boundary checks).
    * `client`: Abstracts downstream network communication using Spring Boot 3's modern, fluent `RestClient`.
* **Egress Throughput Optimization:** Instead of downloading massive multi-megabyte historical rate files from the federal gateway into memory, the `FiscalDataClient` applies aggressive server-side URL parameter filtering (`?filter=...`). It downloads *only* the data bracket required for the target currency and transaction window.
* **Zero-Dependency Portability (Plug & Play):** Leverages an embedded, in-memory H2 database managed via Spring Data JPA. The application requires zero local database setups, external credentials, or container provisioning to run out of the box.

---

## 🛠️ Prerequisites & Local Environment

* **Java:** JDK 17 or 21
* **Build Tool:** Maven (Wrapper included in repository)

---

## 🚀 Getting Started (Run & Compile)

To build the application, execute all verification tests, and launch the server locally on port `8080`, run the following commands in your terminal:

```bash
# Build the project and execute the comprehensive test suite
./mvnw clean test

# Launch the microservice application locally
./mvnw spring-boot:run