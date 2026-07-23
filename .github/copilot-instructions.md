# repo-hackaton-ecommerce Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-07-22

## Active Technologies

- (001-api-transacciones-sale) Java 21 · Spring Boot 3.3.x · Spring Security 6 · Spring Data MongoDB · Maven
- Resilience4j 2.x (CircuitBreaker, Retry, Bulkhead, TimeLimiter, RateLimiter)
- OpenTelemetry Java SDK · Azure Application Insights · Azure Monitor · Grafana
- MongoDB Atlas / Azure Cosmos DB API MongoDB
- Microsoft Entra ID JWT Bearer Authentication
- Azure Key Vault · Managed Identity
- SpringDoc OpenAPI 3.x · Swagger UI
- Lombok · MapStruct
- JUnit 5 · Mockito · TestContainers · WireMock · ArchUnit · Pact / Spring Cloud Contract

## Project Structure

```text
src/
├── main/java/com/ecommerce/sale/
│   ├── domain/           (entities, value objects, domain rules — NO Spring annotations)
│   ├── application/      (use cases, ports/interfaces — NO framework code)
│   ├── infrastructure/   (adapters, MongoDB, Spring config, Azure clients)
│   └── presentation/     (REST controllers, DTOs, exception handlers)
└── test/java/com/ecommerce/sale/
    ├── architecture/     (ArchUnit — enforces layer dependency rules)
    ├── domain/
    ├── application/
    ├── infrastructure/   (TestContainers)
    ├── presentation/     (WebMvcTest)
    ├── contract/         (WireMock / Pact)
    ├── integration/
    └── performance/
```

## Architecture Rules (enforced by ArchUnit)

- `domain.*` MUST NOT import from `application.*`, `infrastructure.*`, or `presentation.*`
- `application.*` MUST NOT import from `infrastructure.*` or `presentation.*`
- Spring annotations (`@Component`, `@Repository`, `@Service`) MUST NOT appear in `domain.*` or `application.*`
- `HttpClient`, `RestTemplate`, `WebClient` MUST NOT appear in `domain.*` or `application.*`
- Monetary amounts MUST use `BigDecimal` — `double` and `float` are PROHIBITED
- Domain methods MUST NOT return `null` — use `Optional<T>` or typed results
- Each use case class MUST have exactly ONE public method (`execute`)

## Code Style

Java 21 — use `record` for Value Objects and DTOs, `sealed interface` for typed results,
pattern matching for switch expressions. Lombok `@Value` + `@Builder` only in `infrastructure`
and `presentation` layers. Methods ≤ 20 lines, Classes ≤ 200 lines, CC ≤ 10.
No magic numbers or hardcoded strings. Self-documenting names required.

## Commands

```bash
# Build and run locally
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# All tests
./mvnw verify

# Unit tests only
./mvnw test -P unit-tests

# Coverage report (target/site/jacoco/index.html)
./mvnw verify jacoco:report

# ArchUnit verification
./mvnw test -Dtest=ArchitectureRulesTest
```

## API Endpoint

- `POST /api/v1/sales` — Process SALE transaction
- `GET /api/v1/sales/{transactionId}` — Query transaction by ID
- `GET /api/v1/sales?merchantId=&from=&to=` — Query by merchant + date range
- `GET /actuator/health` — Health check (circuit breaker states)
- `GET /swagger-ui.html` — Swagger UI
- `GET /v3/api-docs` — OpenAPI JSON spec

## Recent Changes

- 001-api-transacciones-sale: Added Clean Architecture API for SALE transaction processing
  with Entra ID JWT auth, MongoDB (TTL 548d, unique transactionId), Resilience4j,
  OpenTelemetry, OAuth 2.0 Client Credentials for API Switch integration

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->

