# Plan de Implementacion: API Financiera de Transacciones SALE

**Rama**: `001-api-transacciones-sale` | **Fecha**: 2026-07-22 | **Spec**: [spec.md](spec.md)  
**Entrada**: Especificacion funcional en `/specs/001-api-transacciones-sale/spec.md`

---

## Resumen

El MVP implementa una API REST para procesar transacciones SALE en un unico endpoint de negocio (`POST /api/v1/sales`). La API valida la entrada, genera `transactionId` (UUID v4), detecta duplicados por clave de negocio (`terminalId`, `invoice`, `totalAmount`, `accountNumber`, `transactionType`), delega autorizacion al API Switch, persiste el resultado en MongoDB y retorna una respuesta estandarizada. La trazabilidad se garantiza con `X-Correlation-Id`, OpenTelemetry, logs estructurados y Application Insights.

---

## Contexto Tecnico

**Lenguaje/Version**: Java 21 (LTS)  
**Dependencias Principales**: Spring Boot 3.3.x, Spring Security 6, Spring Data MongoDB, Resilience4j 2.x, OpenTelemetry Java SDK, SpringDoc OpenAPI 3, Azure Identity SDK, Azure Key Vault Secrets SDK, Lombok, MapStruct  
**Almacenamiento**: MongoDB Atlas / Azure Cosmos DB API MongoDB  
**Modelo Monetario**: `totalAmount` como `Long` en unidad monetaria minima (centavos). Ejemplo: 56.33 USD = 5633  
**Pruebas**: JUnit 5, Mockito, TestContainers, WireMock, ArchUnit, performance testing  
**Plataforma**: Azure App Service + Azure Key Vault + Application Insights + Azure Monitor  
**Tipo de Proyecto**: REST web-service (backend standalone)

**Endpoints MVP aprobados**:
- `POST /api/v1/sales` (negocio)
- `GET /actuator/health` (operacional)

**CorrelationId**:
- Toda solicitud puede incluir `X-Correlation-Id`.
- Si no llega, la API lo genera y lo propaga en logs, trazas y llamadas externas.

**Deteccion de duplicados**:
- Se implementa por clave de negocio: `terminalId`, `invoice`, `totalAmount`, `accountNumber`, `transactionType`.
- `transactionId` no se utiliza para detectar duplicados.
- `transactionId` se usa exclusivamente para identificacion y trazabilidad.

---

## Verificacion de Constitucion

| Principio Constitucion | Estado | Evidencia en este Plan |
|------------------------|--------|------------------------|
| I - Arquitectura Limpia | PASA | Capas `domain/application/infrastructure/presentation`, dependencias hacia adentro |
| II - SOLID y Clean Code | PASA | Casos de uso con unico metodo publico `execute` |
| III - Seguridad Entra ID / Key Vault | PASA | JWT Entra ID + secretos en Key Vault via Managed Identity |
| IV - Validacion Integral | PASA | Validacion de payload completo antes de invocar API Switch |
| V - Idempotencia y trazabilidad | PASA | Duplicados por clave de negocio; `transactionId` solo trazabilidad |
| VI - Persistencia MongoDB 18 meses | PASA | TTL 548 dias + indice unico de `transactionId` |
| VII - Integracion servicio transaccional | PASA | `AuthorizationSwitchPort` + `SwitchApiAdapter`; sin seleccion de autorizador local |
| VIII - Resiliencia Resilience4j | PASA | CircuitBreaker + Retry + Bulkhead + TimeLimiter + RateLimiter |
| IX - Observabilidad / Testing | PASA | OpenTelemetry + logs JSON + Application Insights + pruebas automatizadas |
| X - SLA | PASA | Objetivo P95 < 3s, P99 < 5s |
| XI - OpenAPI / Swagger | PASA | Contrato unico y consistente con payload de MVP |
| XII - Azure App Service | PASA | Ejecucion y observabilidad en Azure |

**Resultado de Compuerta**: PASA

---

## Estructura del Proyecto

### Documentacion

```text
specs/001-api-transacciones-sale/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md
```

### Codigo Fuente (planificado)

```text
src/main/java/com/ecommerce/sale/
├── domain/
│   ├── model/
│   │   ├── SaleTransaction.java
│   │   ├── AuthorizationResponse.java
│   │   ├── TransactionStatus.java
│   │   ├── TransactionType.java
│   │   ├── AuthorizationSource.java
│   │   └── CardBrand.java
│   └── exception/
├── application/
│   ├── port/in/
│   ├── port/out/
│   └── usecase/
├── infrastructure/
│   ├── adapter/persistence/
│   ├── adapter/switch_api/
│   ├── adapter/observability/
│   ├── adapter/security/
│   ├── config/
│   └── filter/
└── presentation/
    ├── controller/
    ├── dto/
    ├── mapper/
    └── exception/
```

---

## Alcance MVP y Fuera de Alcance

**Incluido en MVP**:
- Procesamiento SALE
- Deteccion de duplicados por clave de negocio
- Persistencia MongoDB con TTL
- Resiliencia y seguridad
- Observabilidad operacional

**Fuera de alcance MVP**:
- Entidad dedicada `TransactionAudit`
- `AuditRepositoryPort`
- `RecordAuditUseCase`
- Endpoints de consulta de transacciones para backoffice

---

## Fases de Implementacion

### Fase 0 - Investigacion
Completada en `research.md`.

### Fase 1 - Diseno
Completada en `data-model.md` y `contracts/openapi.yaml`.

### Fase 2 - Implementacion
Pendiente; se ejecuta con `tasks.md`.

---

## Seguimiento de Complejidad

Sin excepciones a la constitucion para el MVP actual.
