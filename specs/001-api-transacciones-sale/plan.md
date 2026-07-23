# Plan de Implementación: API Financiera de Transacciones SALE

**Rama**: `001-api-transacciones-sale` | **Fecha**: 2026-07-22 | **Spec**: [spec.md](spec.md)  
**Entrada**: Especificación de feature en `/specs/001-api-transacciones-sale/spec.md`

---

## Resumen

API REST financiera para el procesamiento de transacciones SALE de comercios afiliados. La solución valida integralmente cada solicitud, genera automáticamente un `transactionId` único en formato UUID v4 de 36 caracteres para fines de trazabilidad y auditoría, delega la autorización financiera a un API Switch transaccional externo (que internamente enruta a AS400 o Cybersource), persiste la transacción completa en MongoDB y retorna una respuesta estandarizada al comercio. Todo el flujo es trazable mediante `CorrelationId` y `OpenTelemetry`. La arquitectura sigue Clean Architecture con Ports and Adapters, Spring Boot 3, Java 21, Resilience4j, Microsoft Entra ID JWT y Azure Key Vault.

---

## Contexto Técnico

**Lenguaje/Versión**: Java 21 (LTS) — usar records, sealed classes, pattern matching  
**Dependencias Principales**: Spring Boot 3.3.x, Spring Security 6, Spring Data MongoDB, Resilience4j 2.x, OpenTelemetry Java SDK, SpringDoc OpenAPI 3, Azure Identity SDK, Azure Key Vault Secrets SDK, Lombok, MapStruct  
**Almacenamiento**: MongoDB Atlas / Azure Cosmos DB API MongoDB — colección `sale_transactions`, índice único `transactionId`, TTL index `createdAt` (548 días)  
**Pruebas**: JUnit 5, Mockito, TestContainers (MongoDB), WireMock, ArchUnit, Pact / Spring Cloud Contract, JMeter / Gatling  
**Plataforma Destino**: Azure App Service (Linux, Java 21 runtime)  
**Tipo de Proyecto**: REST web-service (API backend standalone)  
**Objetivos de Rendimiento**: P95 < 3 segundos end-to-end, P99 < 5 segundos, throughput sostenido de transacciones financieras  
**Restricciones**: Latencia total incluye llamada al API Switch externo (timeout ≤ 5s), retención MongoDB 18 meses, idempotencia obligatoria, cero pérdida de datos transaccionales  
**CorrelationId**:
  - Toda solicitud deberá incluir un encabezado X-Correlation-Id.
  - Si el consumidor no envía X-Correlation-Id, la API generará automáticamente un CorrelationId.
  - El CorrelationId deberá propagarse a todas las llamadas internas y externas.
  - El CorrelationId deberá estar presente en logs, métricas y trazas distribuidas.
**Idempotencia**:
  - El transactionId será generado automáticamente por la API y no será utilizado para identificar reintentos provenientes del consumidor.
  - La detección de operaciones duplicadas se realizará mediante reglas de negocio definidas por la organización.
  - Las reglas podrán considerar combinaciones de datos tales como:
      - terminalId
      - invoice
      - totalAmount
      - accountNumber tokenizado o enmascarado
      - transactionType
  - Cuando una operación sea identificada como previamente procesada, el sistema deberá retornar el resultado existente sin volver a invocar el API Switch.
  - La información utilizada para identificar duplicados deberá persistirse para fines de auditoría y trazabilidad.
**Escala/Alcance**: Comercios afiliados múltiples, escalamiento horizontal en Azure App Service, disponibilidad ≥ 99.9%

---

## Verificación de Constitución

*COMPUERTA: Debe pasar antes de la investigación de la Fase 0. Re-verificar tras el diseño de la Fase 1.*

| Principio Constitución | Estado | Evidencia en este Plan |
|------------------------|--------|------------------------|
| I — Arquitectura Limpia | ✅ PASA | Estructura de paquetes `domain/application/infrastructure/presentation` definida; ArchUnit valida dependencias en CI |
| II — SOLID y Clean Code | ✅ PASA | Un caso de uso por clase, un método `execute()`, Lombok para inmutabilidad, complejidad CC ≤ 10 |
| III — Seguridad Entra ID / Key Vault | ✅ PASA | JWT Entra ID en todo endpoint, secretos exclusivos en Key Vault vía Managed Identity |
| IV — Validación Integral | ✅ PASA | Bean Validation en Presentation, invariantes de dominio en Domain, `ValidateSaleRequestUseCase` en Application |
| V — Idempotencia / transactionId | ✅ PASA | UUID v4 generado automáticamente para identificación y trazabilidad de la transacción, índice único en MongoDB, GenerateTransactionIdUseCase. La prevención de operaciones duplicadas se implementa mediante reglas de negocio definidas por la organización antes de iniciar el procesamiento de la transacción. |
| VI — Persistencia MongoDB 18 meses | ✅ PASA | TTL index `expireAfterSeconds: 47347200`, índice único `transactionId`, campos de auditoría completos |
| VII — Integración Servicio Transaccional | ✅ PASA | `AuthorizationSwitchPort` + `SwitchApiAdapter`; sin lógica de selección de autorizador en esta API |
| VIII — Resiliencia Resilience4j | ✅ PASA | CircuitBreaker + Retry + Bulkhead + TimeLimiter en `SwitchApiAdapter` y `MongoTransactionAdapter` |
| IX — Observabilidad / Testing | ✅ PASA | OpenTelemetry + CorrelationId propagado, cobertura ≥ 80%, SonarQube A, OWASP DC |
| X — SLA | ✅ PASA | P95 < 3s objetivo, alertas en Grafana, pruebas de rendimiento antes de cada release |
| XI — OpenAPI 3.x / Swagger | ✅ PASA | SpringDoc OpenAPI auto-genera spec, 100% endpoints documentados, seguridad JWT declarada |
| XII — Azure App Service | ✅ PASA | Despliegue en App Service, Key Vault, Monitor, Application Insights, Managed Identity |

**Resultado de Compuerta**: ✅ TODOS LOS PRINCIPIOS PASAN — Aprobado para proceder a Fase 0

---

## Estructura del Proyecto

### Documentación de esta Feature

```text
specs/001-api-transacciones-sale/
├── plan.md              # Este archivo (salida del comando /speckit.plan)
├── research.md          # Salida de Fase 0 (/speckit.plan)
├── data-model.md        # Salida de Fase 1 (/speckit.plan)
├── quickstart.md        # Salida de Fase 1 (/speckit.plan)
├── contracts/           # Salida de Fase 1 (/speckit.plan)
│   └── openapi.yaml
└── tasks.md             # Salida de Fase 2 (/speckit.tasks — NO creado por /speckit.plan)
```

### Código Fuente (raíz del repositorio)

```text
src/
├── main/
│   └── java/
│       └── com/ecommerce/sale/
│           ├── domain/
│           │   ├── model/
│           │   │   ├── SaleTransaction.java          # Entidad raíz de dominio
│           │   │   ├── AuthorizationResponse.java    # Value object
│           │   │   └── TransactionStatus.java        # Enum de estado
│           │   │   
│           │   ├── event/
│           │   │   └── TransactionProcessedEvent.java
│           │   └── exception/
│           │       ├── DuplicateTransactionException.java
│           │       ├── InvalidTransactionException.java
│           │       └── AuthorizationException.java
│           ├── application/
│           │   ├── port/
│           │   │   ├── in/
│           │   │   │   └── ProcessSaleCommand.java   # Puerto de entrada (use case)
│           │   │   └── out/
│           │   │       ├── TransactionRepositoryPort.java
│           │   │       ├── AuthorizationSwitchPort.java
│           │   │       └── SwitchAuthenticationPort.java
│           │   └── usecase/
│           │       ├── ProcessSaleUseCase.java
│           │       ├── ValidateSaleRequestUseCase.java
│           │       ├── GenerateTransactionIdUseCase.java
│           │       ├── GetSwitchAccessTokenUseCase.java
│           │       ├── AuthorizeTransactionUseCase.java
│           │       └── PersistTransactionUseCase.java
│           │       
│           ├── infrastructure/
│           │   ├── adapter/
│           │   │   ├── persistence/
│           │   │   │   └── MongoTransactionAdapter.java
│           │   │   ├── switch_api/
│           │   │   │   ├── SwitchApiAdapter.java
│           │   │   │   └── SwitchOAuthAdapter.java
│           │   │   ├── observability/
│           │   │   │   └── ApplicationInsightsAdapter.java
│           │   │   └── security/
│           │   │       └── KeyVaultAdapter.java
│           │   ├── config/
│           │   │   ├── SecurityConfig.java
│           │   │   ├── ResilienceConfig.java
│           │   │   ├── MongoConfig.java
│           │   │   ├── OpenTelemetryConfig.java
│           │   │   └── SwaggerConfig.java
│           │   ├── persistence/
│           │   │   ├── document/
│           │   │   │   └── SaleTransactionDocument.java
│           │   │   └── repository/
│           │   │       └── MongoSaleTransactionRepository.java
│           │   └── client/
│           │       ├── SwitchApiClient.java
│           │       └── dto/
│           │           ├── SwitchAuthorizationRequest.java
│           │           └── SwitchAuthorizationResponse.java
│           └── presentation/
│               ├── controller/
│               │   └── SaleController.java
│               ├── dto/
│               │   ├── SaleRequest.java
│               │   └── SaleResponse.java
│               ├── mapper/
│               │   └── SaleMapper.java
│               └── exception/
│                   └── GlobalExceptionHandler.java
└── test/
    └── java/
        └── com/ecommerce/sale/
            ├── architecture/
            │   └── ArchitectureRulesTest.java        # ArchUnit
            ├── domain/
            │   ├── model/
            │   └── usecase/
            ├── application/
            │   └── usecase/
            ├── infrastructure/
            │   ├── adapter/
            │   └── persistence/                      # TestContainers
            ├── presentation/
            │   └── controller/                       # WebMvcTest
            ├── contract/
            │   └── SwitchApiContractTest.java        # WireMock / Pact
            ├── integration/
            │   └── SaleFlowIntegrationTest.java
            └── performance/
                └── SaleLoadTest.java                 # JMeter / Gatling

pom.xml
README.md
```

**Decisión de Estructura**: Proyecto único tipo web-service siguiendo Clean Architecture. Paquetes organizados por capa (`domain`, `application`, `infrastructure`, `presentation`). Tests espejados por capa más suites especializadas (architecture, contract, integration, performance).

---

## Seguimiento de Complejidad

> Sin violaciones a la Constitución que requieran justificación adicional. Todas las compuertas pasaron.

---

## Fases de Implementación (referencia para /speckit.tasks)

### Fase 0 — Investigación (completada — ver research.md)

- Patrones de Clean Architecture con Spring Boot 3 y Java 21
- Resilience4j 2.x: configuración de CircuitBreaker, Retry, Bulkhead, TimeLimiter
- OAuth 2.0 Client Credentials con renovación automática de token
- OpenTelemetry con Azure Application Insights
- MongoDB TTL index y unique index patterns
- Spring Security 6 con Entra ID JWT Bearer

### Fase 1 — Diseño y Contratos (completada — ver data-model.md, contracts/, quickstart.md)

- Modelo de datos: `SaleTransaction`, `AuthorizationResponse`
- Puertos: `TransactionRepositoryPort`, `AuthorizationSwitchPort`, `SwitchAuthenticationPort`
- Contrato OpenAPI: `POST /api/v1/sales`
- Quickstart de desarrollo local

### Fase 2 — Tareas de Implementación (pendiente — ejecutar /speckit.tasks)

Las tareas de implementación se generarán con `/speckit.tasks` basándose en este plan.
