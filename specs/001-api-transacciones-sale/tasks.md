---
description: "Tareas de implementacion MVP - API Financiera de Transacciones SALE"
---

# Tareas: API Financiera de Transacciones SALE (MVP)

**Entrada**: `/specs/001-api-transacciones-sale/spec.md`, `plan.md`, `data-model.md`, `contracts/openapi.yaml`  
**Prerrequisitos**: research.md, plan.md, data-model.md, contracts/openapi.yaml

## Formato: `[ID] [P?] [Story] Descripcion`

- **[P]**: Ejecutable en paralelo
- **[Story]**: US1, US2, US3, US4

---

## Fase 1 - Configuracion inicial

- [X] T001 [US1] Configurar `pom.xml` con Java 21, Spring Boot 3, Maven, dependencias de seguridad, MongoDB, Resilience4j, OpenTelemetry, SpringDoc, pruebas y calidad
- [X] T002 [P] [US1] Crear estructura de paquetes Clean Architecture en `src/main/java/com/ecommerce/sale/{domain,application,infrastructure,presentation}` y espejo en `src/test/java/com/ecommerce/sale`
- [X] T003 [P] [US1] Crear `src/main/resources/application.yml` con propiedades externas para security, mongo, switch, observabilidad y resiliencia
- [X] T004 [P] [US1] Crear `src/main/resources/application-local.yml` para entorno local (mongo, wiremock, logs)
- [X] T005 [P] [US1] Crear `src/main/java/com/ecommerce/sale/SaleApiApplication.java`
- [X] T006 [P] [US3] Crear `src/main/resources/logback-spring.xml` con logs JSON estructurados

---

## Fase 2 - Fundacional bloqueante

- [X] T007 [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/SecurityConfig.java` con JWT Bearer Entra ID
- [X] T008 [P] [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/security/JwtAuthenticationConverter.java`
- [X] T009 [P] [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/security/JwtClaimsValidator.java` (issuer, audience, expiration, signature, tenant)
- [X] T010 [P] [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/security/AuthorizationConfig.java` para scopes/roles
- [X] T011 [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/MongoConfig.java` con indices: `idx_transactionId_unique`, `idx_createdAt_ttl`, `idx_duplicate_validation`
- [X] T012 [P] [US2] Crear indice compuesto de duplicados `idx_duplicate_validation` por `terminalId`, `invoice`, `totalAmount`, `accountNumber`, `transactionType`
- [X] T013 [P] [US4] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/ResilienceConfig.java`
- [X] T014 [P] [US3] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/OpenTelemetryConfig.java`
- [X] T015 [P] [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/KeyVaultConfig.java`
- [X] T016 [P] [US3] Crear `src/main/java/com/ecommerce/sale/infrastructure/filter/CorrelationIdFilter.java`
- [X] T017 [P] [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/SwaggerConfig.java`
- [X] T018 [P] [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/adapter/security/KeyVaultAdapter.java`
- [X] T019 [P] [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/SwitchProperties.java`

---

## Fase 3 - US1 Procesamiento SALE (MVP)

- [X] T020 [P] [US1] Crear enums de dominio en `src/main/java/com/ecommerce/sale/domain/model/`: `TransactionStatus`, `TransactionType`, `AuthorizationSource`, `CardBrand`
- [X] T021 [US1] Crear `src/main/java/com/ecommerce/sale/domain/model/AuthorizationResponse.java`
- [X] T022 [US1] Crear `src/main/java/com/ecommerce/sale/domain/model/SaleTransaction.java` con `totalAmount` Long (centavos)
- [X] T023 [P] [US1] Crear excepciones de dominio en `src/main/java/com/ecommerce/sale/domain/exception/`
- [X] T024 [P] [US1] Crear puertos de salida: `TransactionRepositoryPort`, `AuthorizationSwitchPort`, `SwitchAuthenticationPort`
- [X] T025 [P] [US1] Crear comandos de entrada: `ProcessSaleCommand`, `AuthenticationInformationCommand`, `TokenizationInformationCommand`, `ProcessingInformationCommand`
- [X] T026 [US1] Crear `GenerateTransactionIdUseCase` en `src/main/java/com/ecommerce/sale/application/usecase/`
- [X] T027 [US1] Crear `GetSwitchAccessTokenUseCase`
- [X] T028 [US1] Crear `AuthorizeTransactionUseCase` 
Responsabilidades:
- Obtener access token mediante GetSwitchAccessTokenUseCase
- Invocar AuthorizationSwitchPort.authorize(ProcessSaleCommand)
- Interpretar responseCode
- Determinar TransactionStatus
- [X] T029 [US1] Crear `PersistTransactionUseCase`
- [X] T030 [US1] Crear `ProcessSaleUseCase` (sin duplicados aun)
- [X] T031 [US1] Crear `SaleTransactionDocument` en `src/main/java/com/ecommerce/sale/infrastructure/persistence/document/` sin persistir `securityCodeEntry`
- [X] T032 [US1] Crear `MongoSaleTransactionRepository`
- [X] T033 [US1] Crear `MongoTransactionAdapter`
- [X] T034 [US1] Crear `SaleTransactionMapper` con enmascaramiento PAN
- [X] T035 [US1] Crear DTOs externos `SwitchAuthorizationRequest` y `SwitchAuthorizationResponse`
- [X] T036 [US1] Crear `SwitchRequestMapper`
- [X] T037 [US1] Crear `SwitchOAuthAdapter` (client credentials + cache en memoria)
- [X] T038 [US1] Crear `SwitchApiAdapter` (Bearer token + resiliencia + propagacion de correlationId) y construcción del payload mediante SwitchRequestMapper No construir payload manualmente. Utilizar obligatoriamente SwitchRequestMapper.
- [X] T039 [US1] Crear DTOs de API: `SaleRequest`, `AuthenticationInformationDto`, `TokenizationInformationDto`, `ProcessingInformationDto`, `SaleResponse`
- [X] T040 [US1] Crear `SaleMapper`
- [X] T041 [US1] Crear `GlobalExceptionHandler` con RFC9457
- [X] T042 [US1] Crear `SaleController` con `POST /api/v1/sales`

---

## Fase 4 - US2 Validacion integral y duplicados

- [X] T043 [US2] Crear `ValidateSaleRequestUseCase` validando payload completo de `SaleRequest`
- [X] T044 [P] [US2] Crear `DuplicateDetectionService` con clave de negocio (`terminalId`, `invoice`, `totalAmount`, `accountNumber`, `transactionType`)
- [X] T045 [US2] Actualizar `ProcessSaleUseCase` para ejecutar validacion y deteccion de duplicados antes de invocar API Switch
- [X] T046 [P] [US2] Actualizar `MongoSaleTransactionRepository` con busqueda de duplicados por clave de negocio
- [X] T047 [P] [US2] Crear `PanMaskingService`

---

## Fase 5 - US3 Trazabilidad operativa

- [X] T048 [US3] Crear `ApplicationInsightsAdapter`
- [X] T049 [P] [US3] Agregar eventos estructurados en `ProcessSaleUseCase`
- [X] T050 [P] [US3] Agregar eventos estructurados en `SwitchApiAdapter`
- [X] T051 [P] [US3] Agregar eventos estructurados en `MongoTransactionAdapter`
- [X] T052 [P] [US3] Publicar metricas en `GrafanaMetricsConfig`
- [X] T053 [US3] Verificar propagacion E2E de `X-Correlation-Id`

---

## Fase 6 - US4 Resiliencia

- [X] T054 [US4] Aplicar `@CircuitBreaker`, `@Retry`, `@Bulkhead`, `@TimeLimiter`, `@RateLimiter` en `SwitchApiAdapter`
- [X] T055 [P] [US4] Aplicar resiliencia equivalente en `MongoTransactionAdapter`
- [X] T056 [P] [US4] Ajustar parametros de resiliencia en `application.yml`
- [X] T057 [P] [US4] Agregar `Retry-After` en respuestas 503 desde `GlobalExceptionHandler`
- [X] T058 [US4] Exponer estado de circuit breakers en `/actuator/health`

---

## Fase 7 - OpenAPI y Swagger

- [X] T059 [US1] Sincronizar anotaciones OpenAPI en `SaleController` con `contracts/openapi.yaml`
- [X] T060 [US1] Verificar que solo existan paths MVP en contrato (`POST /sales` y `GET /actuator/health`)

---

## Fase 8 - Pruebas

- [X] T061 [P] [US1] Unit tests dominio y casos de uso (JUnit 5 + Mockito)
- [X] T062 [P] [US2] Unit tests de validacion y duplicados
- [X] T063 [P] [US3] Integration tests con Testcontainers para MongoDB
- [X] T064 [P] [US1] Contract tests con WireMock para API Switch
- [X] T065 [P] [US4] Performance tests (P95/P99/error rate)
- [X] T066 [P] [US1] ArchUnit tests de reglas de capas y restricciones
- [X] T071 [US1] Mejorar `GlobalExceptionHandler` para usar `type` RFC9457 explícito por categoría y actualizar pruebas unitarias afectadas
- [X] T072 [US3] Implementar exportación de logs JSON hacia Azure Application Insights con OpenTelemetry y validar consulta `AppTraces`
- [X] T073 [US1] Externalizar configuración de AppConnector (`SWITCH_BASE_URL`, `SWITCH_API_KEY`) y alinear contrato `POST /api/v1/payments` con logging PCI

---

## Fase 9 - Despliegue Azure

- [X] T067 [US1] Crear `src/main/resources/application-azure.yml`
- [X] T068 [P] [US1] Preparar `docker/docker-compose.local.yml` para entorno local
- [X] T069 [P] [US1] Definir variables y secretos operativos para App Service/Key Vault/Managed Identity
- [X] T070 [P] [US1] Crear `README.md` Contenido:
- ejecución local
- variables de entorno
- perfiles Spring
- WireMock
- MongoDB
- Swagger
- Azure App Service

---

## Dependencias explicitas y orden recomendado

1. T001 -> T019 (setup + fundacional)
2. T020 -> T042 (MVP funcional de negocio)
3. T043 -> T047 (validacion integral + duplicados)
4. T048 -> T053 (trazabilidad y observabilidad)
5. T054 -> T058 (resiliencia)
6. T059 -> T060 (contrato OpenAPI)
7. T061 -> T066 (pruebas)
8. T067 -> T069 (despliegue)

Dependencias clave:
- T030 depende de T026, T027, T028 y T029.
- T036 depende de T025 y T035
- T037 depende de T018 y T019.
- T038 depende de T035, T036 y T037.
- T042 depende de T030, T039, T040 y T041.
- T045 depende de T043, T044 y T046.
- T060 depende de T059 y `contracts/openapi.yaml`.

---

## Alcance MVP y exclusiones

**Incluido**:
- Endpoint de negocio `POST /api/v1/sales`
- Duplicados por clave de negocio
- transactionId generado por API
- totalAmount como Long (centavos)
- CorrelationId + OpenTelemetry + logs + App Insights

**Fuera de alcance MVP**:
- `TransactionAudit` como entidad separada
- `AuditRepositoryPort`
- `RecordAuditUseCase`
- Endpoints de consulta funcional de transacciones
