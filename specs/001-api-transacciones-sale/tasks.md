---
description: "Lista de tareas de implementación — API Financiera de Transacciones SALE"
---

# Tareas: API Financiera de Transacciones SALE

**Entrada**: Documentos de diseño en `/specs/001-api-transacciones-sale/`  
**Prerequisitos**: plan.md ✅ · spec.md ✅ · data-model.md ✅ · contracts/openapi.yaml ✅ · research.md ✅

## Formato: `[ID] [P?] [Story?] Descripción con ruta de archivo`

- **[P]**: Puede ejecutarse en paralelo (archivos distintos, sin dependencias incompletas)
- **[Story]**: Historia de usuario a la que pertenece la tarea (US1, US2, US3, US4)
- Se incluyen rutas exactas de archivos en cada descripción

## Convenciones de Rutas

- Código fuente: `src/main/java/com/ecommerce/sale/`
- Tests: `src/test/java/com/ecommerce/sale/`
- Recursos: `src/main/resources/`
- Tests recursos: `src/test/resources/`

---

## Fase 1: Setup — Configuración Inicial del Proyecto

**Propósito**: Inicializar el proyecto Maven con todas las dependencias, estructura de carpetas y configuración base.

- [ ] T001 Crear `pom.xml` con Java 21, Spring Boot 3.3.x, dependencias de Spring Security, Spring Data MongoDB, Resilience4j 2.x, OpenTelemetry SDK, SpringDoc OpenAPI, Azure Identity, Azure Key Vault Secrets, Lombok, MapStruct, JUnit 5, Mockito, TestContainers, WireMock, ArchUnit
- [ ] T002 [P] Crear estructura de paquetes Clean Architecture: `src/main/java/com/ecommerce/sale/{domain,application,infrastructure,presentation}/` y espejo en `src/test/`
- [ ] T003 [P] Crear `src/main/resources/application.yml` con configuración base:
  - server.port
  - MongoDB URI placeholder
  - security.jwt.issuer-uri
- security.jwt.audience
- security.jwt.tenant-id
- switch.api.base-url
- switch.oauth.token-url
- otel.service-name
- mongo.uri
- Resilience4j defaults
- OpenTelemetry service name
- switch.api.base-url
- switch.oauth.token-url
Todos los valores deberán obtenerse desde variables de entorno o configuración externa.
- [ ] T004 [P] Crear `src/main/resources/application-local.yml` con valores de desarrollo local (MongoDB local, WireMock URL, logs DEBUG, OTel deshabilitar exportación remota)
- [ ] T005 [P] Crear clase principal `src/main/java/com/ecommerce/sale/SaleApiApplication.java` con `@SpringBootApplication`
- [ ] T006 [P] Crear `src/main/resources/logback-spring.xml` configurado para salida JSON estructurada con campos: timestamp, level, service, correlationId, transactionId, traceId, spanId

---

## Fase 2: Fundacional — Infraestructura Bloqueante

**Propósito**: Seguridad JWT, configuración de MongoDB, Resilience4j, OpenTelemetry y propagación de CorrelationId. Ninguna historia de usuario puede iniciarse hasta que esta fase esté completa.

**⚠️ CRÍTICO**: Ningún trabajo de historia de usuario puede comenzar hasta que esta fase esté completa.

- [ ] T007 Crear `src/main/java/com/ecommerce/sale/infrastructure/config/SecurityConfig.java` — `SecurityFilterChain` con JWT Bearer Entra ID, validación de issuer/audience/expiry/signature, todos los endpoints autenticados, stateless
- [ ] T007A [P] Crear
  src/main/java/com/ecommerce/sale/infrastructure/security/JwtAuthenticationConverter.java

  Responsabilidades:
  - Extraer claims del JWT emitido por Microsoft Entra ID.
  - Mapear roles/scopes a GrantedAuthority.
  - Validar estructura mínima requerida del token.
- [ ] T007B [P] Crear
  src/main/java/com/ecommerce/sale/infrastructure/security/JwtClaimsValidator.java

  Validar:

  - issuer
  - audience
  - expiration
  - notBefore
  - signature
  - tenantId esperado

  Rechazar cualquier token inválido con HTTP 401.

 - [ ] T007C [P] Crear `src/main/java/com/ecommerce/sale/infrastructure/security/AuthorizationConfig.java`
Configurar autorización por scopes y/o app roles.
- [ ] T008 [P] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/MongoConfig.java` — configuración de MongoDB con TLS, creación de índices al arranque: `idx_transactionId_unique` (unique), `idx_createdAt_ttl` (TTL 47347200s), `idx_merchantId_createdAt` (compuesto), `idx_status`, `idx_duplicate_validation`
- [ ] T008A [P] Crear índice compuesto `idx_duplicate_validation` para optimizar la detección de operaciones duplicadas utilizando:
{
  terminalId: 1,
  invoice: 1,
  totalAmount: 1,
  accountNumber: 1,
  transactionType: 1
}
El índice será utilizado por TransactionRepositoryPort.findDuplicate(...).
- [ ] T009 [P] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/ResilienceConfig.java` — beans de CircuitBreaker, Retry, Bulkhead y TimeLimiter para `switchApi` y `mongoDb` con parámetros de research.md
- [ ] T010 [P] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/OpenTelemetryConfig.java` — configuración del SDK, propagación de `X-Correlation-Id`, exportador a Azure Application Insights vía OTLP
- [ ] T011 [P] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/KeyVaultConfig.java` — cliente Azure Key Vault utilizando Managed Identity (`DefaultAzureCredential`) para obtención segura de secretos en tiempo de ejecución.
- [ ] T012 [P] Crear `src/main/java/com/ecommerce/sale/infrastructure/filter/CorrelationIdFilter.java` — filtro Servlet que extrae `X-Correlation-Id` del header (o genera UUID v4 si ausente), lo almacena en `MDC` y lo propaga en la respuesta
- [ ] T013 [P] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/SwaggerConfig.java` — SpringDoc OpenAPI con seguridad JWT (`bearerAuth`), título, versión, `X-Correlation-Id` como header global
- [ ] T014 [P] Crear
`src/main/java/com/ecommerce/sale/infrastructure/adapter/security/KeyVaultAdapter.java`
Responsabilidades:
- Obtener switch-client-id
- Obtener switch-client-secret
Los secretos nunca deberán registrarse en logs ni exponerse en respuestas.
- [ ] T014A [P] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/SwitchProperties.java` — clase `@ConfigurationProperties(prefix = "switch")` para configuración externa de:
  - switch.api.base-url
  - switch.oauth.token-url

  Las URLs no podrán estar hardcodeadas en el código fuente y deberán ser obtenidas desde variables de entorno o configuración externa.

**Checkpoint**: Seguridad, MongoDB, Resilience4j, OTel y CorrelationId operativos — las historias de usuario pueden comenzar.

---

## Fase 3: Historia de Usuario 1 — Procesamiento de Autorización SALE (P1) 🎯 MVP

**Objetivo**: Un comercio autenticado puede enviar una solicitud SALE, recibir autorización del API Switch y obtener una respuesta en menos de 3 segundos (P95). La transacción queda registrada en MongoDB.

**Prueba Independiente**: `POST /api/v1/sales` con JWT válido retorna HTTP 200 con `status: AUTHORIZED` o `DECLINED` y la transacción existe en MongoDB.

### Modelo de Dominio

- [ ] T015 [P] [US1] Crear `src/main/java/com/ecommerce/sale/domain/model/TransactionStatus.java` — enum `PENDING | AUTHORIZED | DECLINED | ERROR`
- [ ] T016 [P] [US1] Crear `src/main/java/com/ecommerce/sale/domain/model/TransactionType.java` — enum `SALE`
- [ ] T017 [P] [US1] Crear `src/main/java/com/ecommerce/sale/domain/model/AuthorizationSource.java` — enum `AS400 | CYBERSOURCE | UNKNOWN`
- [ ] T018 [P] [US1] Crear `src/main/java/com/ecommerce/sale/domain/model/CardBrand.java` — enum `VISA | MASTERCARD | AMEX | DISCOVER | OTHER` con lógica de detección desde BIN
- [ ] T019 [P] [US1] Crear `src/main/java/com/ecommerce/sale/domain/model/AuthorizationResponse.java` — record con: `authorizationSource`, `authorizationNumber`, `responseCode`, `responseDescription`, `referenceNumber`, `hostDate`, `hostTime`; invariante: `authorizationSource` nunca null
- [ ] T020 [US1] Crear `src/main/java/com/ecommerce/sale/domain/model/SaleTransaction.java` — record con todos los campos del data-model.md (`transactionId`, `correlationId`, `merchantId`, `terminalId`, `transactionType`, `totalAmount` Long, `accountNumber`, `cardBrand`, `expirationDate`, `invoice` Long, `securityCodeEntry`, `securityValidationResponse`, `binValidate`, `status`, `authorizationResult`, `createdAt`, `processingDateTime`, `updatedAt`); invariantes: `totalAmount` > 0, `transactionId` no null, `transactionType` = SALE, `accountNumber` tokenizado/enmascarado, `securityCodeEntry` se utilizará únicamente durante el procesamiento transaccional y nunca será persistido en MongoDB.
- [ ] T021 [P] [US1] Crear `src/main/java/com/ecommerce/sale/domain/exception/DuplicateTransactionException.java`, `InvalidTransactionException.java`, `AuthorizationException.java`

### Puertos (Application)

- [ ] T022 [P] [US1] Crear `src/main/java/com/ecommerce/sale/application/port/out/TransactionRepositoryPort.java` — métodos: `save(SaleTransaction)`, `findByTransactionId(String)`, `findDuplicate(String terminalId, Long invoice, Long totalAmount, String accountNumber, String transactionType)`, `findByMerchantId(...)`, `updateStatus(...)`
- [ ] T023 [P] [US1] Crear `src/main/java/com/ecommerce/sale/application/port/out/AuthorizationSwitchPort.java` — método: `authorize(SaleTransaction): AuthorizationResponse`
- [ ] T024 [P] [US1] Crear `src/main/java/com/ecommerce/sale/application/port/out/SwitchAuthenticationPort.java` — método: `getAccessToken(): String`
- [ ] T025 [P] [US1] Crear `src/main/java/com/ecommerce/sale/application/port/in/ProcessSaleCommand.java` — record de comando de entrada con todos los campos de `SaleRequest`

### Casos de Uso (Application)

- [ ] T026 [US1] Crear `src/main/java/com/ecommerce/sale/application/usecase/GenerateTransactionIdUseCase.java` — genera UUID v4 de 36 caracteres con `UUID.randomUUID().toString()`; único método público `execute(): String`
- [ ] T027 [US1] Crear `src/main/java/com/ecommerce/sale/application/usecase/GetSwitchAccessTokenUseCase.java` — delega a `SwitchAuthenticationPort.getAccessToken()`; único método público `execute(): String`
- [ ] T028 [US1] Crear `src/main/java/com/ecommerce/sale/application/usecase/AuthorizeTransactionUseCase.java` — delega a `AuthorizationSwitchPort.authorize(transaction)`, obtiene token OAuth vía `GetSwitchAccessTokenUseCase`, determina `TransactionStatus` según `responseCode`; único método `execute(SaleTransaction): AuthorizationResponse`
- [ ] T029 [US1] Crear `src/main/java/com/ecommerce/sale/application/usecase/PersistTransactionUseCase.java` — delega a `TransactionRepositoryPort.save()` y `updateStatus()`; único método `execute(SaleTransaction): void`
- [ ] T030 [US1] Crear `src/main/java/com/ecommerce/sale/application/usecase/ProcessSaleUseCase.java` — orquesta: `ValidateSaleRequestUseCase` → `GenerateTransactionIdUseCase` → `findDuplicate` → `AuthorizeTransactionUseCase` → `PersistTransactionUseCase`; único método `execute(ProcessSaleCommand): SaleTransaction`

### Adaptadores de Infraestructura

- [ ] T031 [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/persistence/document/SaleTransactionDocument.java` — documento MongoDB con `@Document("sale_transactions")`, todos los campos del esquema, `@Id` en `_id`, `securityCodeEntry` **EXCLUIDO** del documento (no persistir).El campo `securityCodeEntry` deberá excluirse completamente del esquema MongoDB por razones de seguridad y cumplimiento.
- [ ] T032 [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/persistence/repository/MongoSaleTransactionRepository.java` — `MongoRepository<SaleTransactionDocument, String>` con métodos de consulta Spring Data
- [ ] T033 [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/adapter/persistence/MongoTransactionAdapter.java` — implementa `TransactionRepositoryPort`, usa `MongoSaleTransactionRepository`, decorado con Resilience4j CircuitBreaker + Retry + TimeLimiter para `mongoDb`
- [ ] T034 [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/adapter/persistence/SaleTransactionMapper.java` — mapea entre `SaleTransaction` (domain) y `SaleTransactionDocument` (infra); aplica enmascaramiento de PAN (BIN 8 dígitos para VISA/MC, BIN 6 para otros; últimos 4 siempre visibles); `securityCodeEntry` nunca incluido en document
- [ ] T035 [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/client/dto/SwitchAuthorizationRequest.java` y `SwitchAuthorizationResponse.java` — DTOs para comunicación con API Switch
- [ ] T036 [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/adapter/switch_api/SwitchOAuthAdapter.java` — implementa `SwitchAuthenticationPort`.

  Responsabilidades:
  - Obtener client_id desde KeyVaultAdapter.
  - Obtener client_secret desde KeyVaultAdapter.
  - Obtener tokenUrl desde SwitchProperties.
  - Solicitar access token mediante OAuth 2.0 Client Credentials.
  - Cachear el token mientras sea válido.
  - Renovar automáticamente 30 segundos antes de la expiración.
  - Nunca registrar credenciales ni tokens en logs.
  - El access token deberá almacenarse únicamente en memoria.
  - No deberá persistirse ni registrarse en logs.
- [ ] T037 [US1] Crear `src/main/java/com/ecommerce/sale/infrastructure/adapter/switch_api/SwitchApiAdapter.java` — implementa `AuthorizationSwitchPort`.

  Responsabilidades:
  - Obtener baseUrl desde SwitchProperties.
  - Construir WebClient utilizando configuración externa.
  - Obtener Bearer Token desde SwitchOAuthAdapter.
  - Invocar API Switch REST.
  - Propagar X-Correlation-Id a servicios externos.
  - Aplicar CircuitBreaker, Retry, Bulkhead y TimeLimiter.
  - No permitir URLs hardcodeadas.

### Capa de Presentación

- [ ] T038 [US1] Crear `src/main/java/com/ecommerce/sale/presentation/dto/SaleRequest.java` — record con Bean Validation: `@NotBlank merchantId`, `@NotBlank terminalId`, `@NotNull transactionType`, `@Positive totalAmount` (Long), `@NotBlank accountNumber`, `@NotBlank expirationDate` con patrón `^(\\d{2})(0[1-9]|1[0-2])$`, `@NotNull invoice`, `securityCodeEntry` (opcional), `securityValidationResponse` (opcional), `binValidate` (opcional)
- [ ] T039 [US1] Crear `src/main/java/com/ecommerce/sale/presentation/dto/SaleResponse.java` — record con: `transactionId`, `correlationId`, `status`, `merchantId`, `totalAmount`, `authorization` (AuthorizationResultDto), `processingDateTime`, `createdAt`
- [ ] T040 [US1] Crear `src/main/java/com/ecommerce/sale/presentation/mapper/SaleMapper.java` — mapea `SaleRequest` → `ProcessSaleCommand` y `SaleTransaction` → `SaleResponse`; `securityCodeEntry` NO incluido en `SaleResponse`
- [ ] T041 [US1] Crear `src/main/java/com/ecommerce/sale/presentation/exception/GlobalExceptionHandler.java` — `@RestControllerAdvice` que mapea excepciones a `ProblemDetail` (RFC 9457): `InvalidTransactionException` → 400, `DuplicateTransactionException` → 200 (respuesta existente), `AuthorizationException` → 503, `MethodArgumentNotValidException` → 400 con lista de `FieldError`, `Exception` → 500; incluye `correlationId` en todas las respuestas
- [ ] T042 [US1] Crear `src/main/java/com/ecommerce/sale/presentation/controller/SaleController.java` — `POST /api/v1/sales` con `@Valid @RequestBody SaleRequest`, extrae `correlationId` del `MDC`, delega a `ProcessSaleUseCase`; anotaciones OpenAPI `@Operation`, `@ApiResponse`, `@Parameter` para Swagger; 

---

## Fase 4: Historia de Usuario 2 — Validación Integral y Detección de Duplicados (P2)

**Objetivo**: Toda solicitud es validada antes del procesamiento. Operaciones duplicadas (misma combinación de `terminalId + invoice + totalAmount + accountNumber + transactionType`) retornan el resultado existente sin invocar el API Switch.

**Prueba Independiente**: Solicitudes inválidas retornan HTTP 400 con detalle específico del campo. Reenvío con misma combinación de negocio retorna resultado anterior sin nueva llamada al API Switch.

- [ ] T043 [US1] Crear `src/main/java/com/ecommerce/sale/application/usecase/ValidateSaleRequestUseCase.java` — valida: `totalAmount` > 0, `transactionType` = SALE, formato `expirationDate` (patrón YYMM), `accountNumber` no es PAN en claro (debe ser token/enmascarado), `invoice` no null; lanza `InvalidTransactionException` con mensaje específico por campo inválido; único método `execute(ProcessSaleCommand): void`
- [ ] T044 [P] [US2] Crear `src/main/java/com/ecommerce/sale/domain/service/DuplicateDetectionService.java` — lógica de dominio para determinar si una transacción es duplicada basada en la combinación: `terminalId + invoice + totalAmount + accountNumber + transactionType`; sin Spring annotations (puro dominio)
- [ ] T045 [US2] Actualizar `src/main/java/com/ecommerce/sale/application/usecase/ProcessSaleUseCase.java` — agregar paso de detección de duplicados usando `TransactionRepositoryPort.findDuplicate(...)` antes de invocar `AuthorizeTransactionUseCase`; si se detecta duplicado, retornar la transacción existente sin procesar nuevamente
- [ ] T046 [P] [US2] Actualizar `src/main/java/com/ecommerce/sale/infrastructure/persistence/repository/MongoSaleTransactionRepository.java` —método para búsqueda de duplicados por reglas de negocio `findByTerminalIdAndInvoiceAndTotalAmountAndAccountNumberAndTransactionTypeAndStatusIn(...)` para consulta de duplicados. La consulta deberá considerar únicamente transacciones persistidas válidas y evitar falsos positivos provenientes de transacciones fallidas o incompletas.
- [ ] T047 [P] [US2] Crear `src/main/java/com/ecommerce/sale/domain/model/PanMaskingService.java` — servicio de dominio para enmascaramiento de PAN: VISA/MC (BIN 8 + `****` + últimos 4), otros esquemas (BIN 6 + `******` + últimos 4); detecta longitud de BIN desde `CardBrand`

---

## Fase 5: Historia de Usuario 3 — Trazabilidad Completa y Auditoría (P2)

**Objetivo**: Toda transacción procesada es completamente trazable mediante `correlationId`, OpenTelemetry, logs estructurados JSON y permite consulta posterior para auditoría y conciliación.

**Prueba Independiente**: Para cualquier transacción, los logs estructurados contienen todos los campos de auditoría con el mismo `correlationId` en cada entrada. Las trazas de OpenTelemetry muestran el flujo completo.

- [ ] T048 [US3] Crear `src/main/java/com/ecommerce/sale/infrastructure/adapter/observability/ApplicationInsightsAdapter.java` — instrumentación con OpenTelemetry SDK: spans para `ProcessSaleUseCase`, `AuthorizeTransactionUseCase`, `PersistTransactionUseCase`, `SwitchApiAdapter`; atributos de span: `transactionId`, `correlationId`, `merchantId`, `status`, `authorizationSource`
- [ ] T049 [P] [US3] Actualizar `src/main/resources/logback-spring.xml` — asegurar propagación de `%X{correlationId}` y `%X{transactionId}` en cada entrada de log; formato JSON con campos: `timestamp`, `level`, `service`, `correlationId`, `transactionId`, `traceId`, `spanId`, `message`; `securityCodeEntry` NUNCA en logs
- [ ] T050 [P] [US3] Agregar logging estructurado en `ProcessSaleUseCase.java` — evento `REQUEST_RECEIVED` al inicio, `VALIDATION_PASSED/FAILED`, `DUPLICATE_DETECTED`, `SWITCH_REQUEST_SENT`, `AUTHORIZATION_RECEIVED`, `PERSISTENCE_COMPLETED`, `RESPONSE_SENT`; enmascarar PAN en todos los mensajes de log
- [ ] T051 [P] [US3] Agregar logging estructurado en `SwitchApiAdapter.java` — log de inicio de llamada, respuesta recibida (sin datos sensibles), timeout, circuit breaker state change; propagar `X-Correlation-Id` como header HTTP de salida al API Switch
- [ ] T052 [P] [US3] Agregar logging estructurado en `MongoTransactionAdapter.java` — log de operaciones de persistencia con resultado y duración; enmascarar `accountNumber` en todos los mensajes
- [ ] T053 [US3] Crear `src/main/java/com/ecommerce/sale/infrastructure/config/GrafanaMetricsConfig.java` — registrar métricas personalizadas con Micrometer + OTel: `sale.transactions.total` (por status, merchantId), `sale.transactions.duration` (histograma para P50/P95/P99), `sale.switch.duration` (por authorizationSource), `sale.circuitbreaker.state` (por instancia); exportar a Azure Monitor/Grafana

---

## Fase 6: Historia de Usuario 4 — Disponibilidad y Resiliencia (P3)

**Objetivo**: El sistema continúa operando de forma controlada ante fallos del API Switch o MongoDB, con CircuitBreaker, Retry y degradación controlada.

**Prueba Independiente**: Cuando el API Switch devuelve 503 repetidamente, el CircuitBreaker se abre y las solicitudes subsecuentes retornan HTTP 503 inmediatamente sin invocar el servicio.

- [ ] T054 [US4] Actualizar `src/main/java/com/ecommerce/sale/infrastructure/adapter/switch_api/SwitchApiAdapter.java` — verificar que todas las anotaciones `@CircuitBreaker`, `@Retry`, `@Bulkhead`, `@TimeLimiter` de Resilience4j están aplicadas correctamente con nombres de instancia `switchApi`; implementar fallback que lanza `AuthorizationException` con motivo descriptivo
- [ ] T055 [P] [US4] Actualizar `src/main/java/com/ecommerce/sale/infrastructure/adapter/persistence/MongoTransactionAdapter.java` — verificar que `@CircuitBreaker`, `@Retry`, `@TimeLimiter` están aplicadas con instancia `mongoDb`; fallback que lanza excepción con motivo descriptivo y log de alerta
- [ ] T056 [P] [US4] Actualizar `src/main/resources/application.yml` — asegurar configuración completa de Resilience4j para ambas instancias (`switchApi` y `mongoDb`) según research.md: `failureRateThreshold: 50`, `waitDurationInOpenState: 10s`, `slidingWindowSize: 10`, Retry `maxAttempts: 3` con backoff exponencial, `timeoutDuration: 5s` para switchApi y `3s` para mongoDb
- [ ] T057 [P] [US4] Actualizar `src/main/java/com/ecommerce/sale/presentation/exception/GlobalExceptionHandler.java` — agregar `Retry-After` header en respuestas 503 con valor del `waitDurationInOpenState` del CircuitBreaker abierto
- [ ] T058 [P] [US4] Actualizar `src/main/java/com/ecommerce/sale/infrastructure/config/ResilienceConfig.java` — registrar `CircuitBreakerRegistry.EventConsumer` para publicar eventos (OPEN/CLOSED/HALF_OPEN) como logs estructurados y métricas OTel; exponer estados en `/actuator/health`

---

## Fase Final: Polish y Aspectos Transversales

**Propósito**: Pruebas completas, documentación, validación de arquitectura, rendimiento y preparación para despliegue en Azure.

### ArchUnit (Reglas de Arquitectura)

- [ ] T059 [P] Crear `src/test/java/com/ecommerce/sale/architecture/ArchitectureRulesTest.java` — reglas ArchUnit: (1) `domain.*` no importa `application.*`, `infrastructure.*`, `presentation.*`; (2) `application.*` no importa `infrastructure.*`, `presentation.*`; (3) `@Repository`, `@Service`, `@Component` prohibidos en `domain.*` y `application.*`; (4) `HttpClient`, `RestTemplate`, `WebClient` prohibidos en `domain.*` y `application.*`; (5) `double`/`float` prohibidos en clases con `Amount` o `Money` en nombre; (6) Cada clase con `UseCase` en nombre tiene exactamente un método público

### Pruebas Unitarias — Dominio y Application

- [ ] T060 [P] Crear `src/test/java/com/ecommerce/sale/domain/model/SaleTransactionTest.java` — prueba todos los invariantes: `totalAmount` ≤ 0 lanza excepción, `transactionId` null lanza excepción, transición de estado terminal no permitida, `securityCodeEntry` no serializado
- [ ] T061 [P] Crear `src/test/java/com/ecommerce/sale/domain/model/AuthorizationResponseTest.java` — prueba: `authorizationSource` nunca null, responseCode "00" mapea a AUTHORIZED, responseCode ≠ "00" mapea a DECLINED
- [ ] T062 [P] Crear `src/test/java/com/ecommerce/sale/domain/model/PanMaskingServiceTest.java` — prueba enmascaramiento VISA (BIN 8 visible), MC (BIN 8 visible), AMEX (BIN 6 visible), valores límite (PAN corto, PAN exacto de 16 dígitos)
- [ ] T063 [P] Crear `src/test/java/com/ecommerce/sale/application/usecase/GenerateTransactionIdUseCaseTest.java` — prueba: ID generado es UUID v4, longitud 36, no null, dos llamadas producen IDs diferentes
- [ ] T064 [P] Crear `src/test/java/com/ecommerce/sale/application/usecase/ValidateSaleRequestUseCaseTest.java` — prueba todos los escenarios de la US2: `terminalId` vacío → excepción, `totalAmount` ≤ 0 → excepción, `transactionType` ≠ SALE → excepción, `expirationDate` formato inválido → excepción, solicitud válida → sin excepción
- [ ] T065 [P] Crear `src/test/java/com/ecommerce/sale/application/usecase/ProcessSaleUseCaseTest.java` — prueba con Mockito: flujo completo exitoso (AUTHORIZED), flujo con duplicado detectado (retorna existente sin llamar Switch), flujo con DECLINED, flujo con ERROR (Switch lanza excepción)
- [ ] T066 [P] Crear `src/test/java/com/ecommerce/sale/application/usecase/AuthorizeTransactionUseCaseTest.java` — prueba: responseCode "00" → AUTHORIZED, responseCode "51" → DECLINED, timeout → AuthorizationException
- [ ] T067 [P] Crear `src/test/java/com/ecommerce/sale/application/usecase/DuplicateDetectionTest.java` — prueba: misma combinación terminalId+invoice+totalAmount+accountNumber+transactionType → duplicado detectado; diferencia en cualquier campo → no duplicado

### Pruebas de Infraestructura (TestContainers)

- [ ] T068 Crear `src/test/java/com/ecommerce/sale/infrastructure/persistence/MongoTransactionAdapterTest.java` — prueba de integración con TestContainers MongoDB: `save()` persiste documento, `findByTransactionId()` retorna transacción, `findDuplicate()` detecta duplicado, `updateStatus()` actualiza estado terminal, `securityCodeEntry` no presente en documento, índice único rechaza duplicado de transactionId
- [ ] T069 [P] Crear `src/test/java/com/ecommerce/sale/infrastructure/persistence/SaleTransactionMapperTest.java` — prueba mapeo domain→document y document→domain; verifica enmascaramiento PAN correcto por CardBrand; verifica `securityCodeEntry` nunca en documento
- [ ] T070 [P] Crear `src/test/java/com/ecommerce/sale/infrastructure/adapter/switch_api/SwitchOAuthAdapterTest.java` — prueba: token obtenido correctamente, token cacheado en segunda llamada, token renovado al expirar, margen de 30s antes de expiración
- [ ] T070A [P] Crear `src/test/java/com/ecommerce/sale/infrastructure/config/SwitchPropertiesTest.java`
Validar:
- carga de configuración
- variables obligatorias
- URLs no vacías
- [ ] T070B [P] Crear  `src/test/java/com/ecommerce/sale/infrastructure/adapter/security/KeyVaultAdapterTest.java`
Validar:
- lectura correcta de secretos
- secreto inexistente
- excepción de acceso
- nunca registrar secretos en logs
- [ ] T071 [P] Crear `src/test/java/com/ecommerce/sale/infrastructure/adapter/switch_api/SwitchApiAdapterTest.java` — prueba con Mockito de Resilience4j: timeout activa CircuitBreaker, 3 reintentos con backoff, Bulkhead limita concurrencia, respuesta aprobada mapea correctamente

### Pruebas de Presentación (WebMvcTest)

- [ ] T072 [P] Crear `src/test/java/com/ecommerce/sale/presentation/controller/SaleControllerTest.java` con `@WebMvcTest` — prueba: HTTP 200 con JWT válido y body válido, HTTP 400 con campo faltante (validación Bean Validation), HTTP 401 sin JWT, HTTP 400 con `totalAmount` ≤ 0, HTTP 503 cuando ProcessSaleUseCase lanza AuthorizationException, respuesta incluye `X-Correlation-Id` header, body sigue esquema de SaleResponse
- [ ] T072A [P]
Crear: `src/test/java/com/ecommerce/sale/presentation/security/JwtSecurityTest.java`
Casos:
  - JWT válido → 200
  - JWT expirado → 401
  - JWT con audience incorrecta → 401
  - JWT con issuer incorrecto → 401
  - JWT sin scope requerido → 403
  - Sin JWT → 401
- [ ] T073 [P] Crear `src/test/java/com/ecommerce/sale/presentation/exception/GlobalExceptionHandlerTest.java` — prueba que cada tipo de excepción produce el `ProblemDetail` correcto con `type`, `title`, `status`, `detail`, `correlationId` según RFC 9457

### Pruebas de Contrato (WireMock)

- [ ] T074 Crear `src/test/java/com/ecommerce/sale/contract/SwitchApiContractTest.java` — pruebas con WireMock que simulan el API Switch: respuesta aprobada (AS400, responseCode "00"), respuesta rechazada (CYBERSOURCE, responseCode "51"), timeout (6000ms delay → TimeLimiter dispara), HTTP 500 → Retry se activa, HTTP 503 × 10 → CircuitBreaker se abre
- [ ] T075 [P] Crear `src/test/resources/wiremock/` con mappings JSON para los escenarios del contrato: `switch-authorized.json`, `switch-declined.json`, `switch-timeout.json`, `switch-server-error.json`, `switch-circuit-open.json`

### Prueba de Integración End-to-End

- [ ] T076 Crear `src/test/java/com/ecommerce/sale/integration/SaleFlowIntegrationTest.java` — `@SpringBootTest` con TestContainers MongoDB y WireMock: flujo completo SALE exitoso (JWT mock → validación → sin duplicado → Switch WireMock → persistencia MongoDB → respuesta HTTP 200 AUTHORIZED), flujo con duplicado (segunda solicitud con mismo terminalId+invoice+totalAmount retorna resultado existente), flujo con Switch indisponible (HTTP 503), trazabilidad: `correlationId` presente en respuesta y en MongoDB

### Pruebas de Rendimiento

- [ ] T077 Crear `src/test/java/com/ecommerce/sale/performance/SaleLoadTest.java` — plan de carga con JMeter/Gatling: rampa de 10 a 100 usuarios concurrentes durante 5 minutos, verificar P95 < 3s, P99 < 5s, tasa de error < 1%, throughput sostenido; script debe poder ejecutarse desde CI

### Despliegue Azure

- [ ] T078 [P] Crear `src/main/resources/application-azure.yml` — perfil para Azure App Service: Key Vault URI, Application Insights connection string, MongoDB URI (desde Key Vault), Switch API URL (desde Key Vault), OTel exportador OTLP habilitado, logging nivel INFO
- [ ] T079 [P] Crear `docker/docker-compose.local.yml` — servicios: MongoDB (puerto 27017), WireMock (puerto 8090, mounts `./docker/wiremock/mappings`), Grafana (puerto 3000)
- [ ] T080 [P] Crear `docker/wiremock/mappings/switch-authorized.json`, `switch-declined.json`, `switch-timeout.json` — mappings WireMock para desarrollo local (equivalentes a los de tests de contrato)
- [ ] T081 [P] Crear `README.md` — instrucciones de inicio rápido (referencia a `quickstart.md`), comandos Maven, variables de entorno requeridas, endpoints disponibles, link al Swagger UI

---

## Dependencias por Historia de Usuario

```text
Fase 1 (Setup) → Fase 2 (Fundacional)
                     ↓
              Fase 3 (US1 - MVP) ← PRIMERA historia de usuario
                     ↓
         Fase 4 (US2) ║ Fase 5 (US3)   ← Pueden implementarse en paralelo
                     ↓
              Fase 6 (US4 - Resiliencia)
                     ↓
              Fase Final (Polish, Tests, Azure)
```

**Notas de dependencias**:
- T028 (`AuthorizeTransactionUseCase`) requiere T024 (`SwitchAuthenticationPort`) y T023 (`AuthorizationSwitchPort`)
- T030 (`ProcessSaleUseCase`) requiere T026, T027, T028, T029, T043
- T033 (`MongoTransactionAdapter`) requiere T031, T032
- T036 (`SwitchOAuthAdapter`) requiere T014 (`KeyVaultAdapter`)
- T037 (`SwitchApiAdapter`) requiere T036 y T035
- T042 (`SaleController`) requiere T038, T039, T040, T041, T030
- T045 (actualización de `ProcessSaleUseCase` para duplicados) requiere T043, T044, T046
- T065 (`ProcessSaleUseCaseTest`) requiere T030 completo
- T068 (`MongoTransactionAdapterTest`) requiere T033, T034

---

## Ejemplos de Ejecución Paralela por Historia

### US1 (Fase 3) — Tareas paralelas inmediatas tras T007-T014:
- T015, T016, T017, T018, T019 (enums y value objects del dominio — todos independientes)
- T022, T023, T024, T025 (interfaces de puertos — todos independientes)

### US2 (Fase 4) — Paralelas tras T030:
- T043 (ValidateSaleRequestUseCase), T044 (DuplicateDetectionService), T046 (MongoRepository query), T047 (PanMaskingService)

### US3 (Fase 5) — Paralelas tras T033, T037, T042:
- T049 (logback JSON), T050 (logging en ProcessSaleUseCase), T051 (logging en SwitchApiAdapter), T052 (logging en MongoAdapter), T053 (GrafanaMetricsConfig)

### Fase Final — Tests todos paralelos entre sí:
- T059 a T077 pueden ejecutarse en paralelo una vez que el código de producción está completo

---

## Estrategia de Implementación

### MVP (Solo US1 — Fase 3)
Completar Fases 1+2+3 para un endpoint `POST /api/v1/sales` funcional con:
- Autenticación Entra ID JWT
- Generación de transactionId UUID v4
- Invocación al API Switch (WireMock en local)
- Persistencia en MongoDB
- Respuesta HTTP 200 con resultado de autorización

### Incremento 2 (US2 + US3 — Fases 4+5)
Agregar validación integral, detección de duplicados por reglas de negocio, trazabilidad completa con OpenTelemetry y logging estructurado. Endpoints de consulta disponibles.

### Incremento 3 (US4 — Fase 6 + Fase Final)
Resiliencia completa, pruebas automatizadas completas (ArchUnit, unitarias, integración, contrato, rendimiento), despliegue en Azure App Service.

---

## Validación de Formato

- Total de tareas: **89**
- Tareas identificadas mediante T001–T081 y subtareas complementarias:T007A, T007B, T007C, T008A, T014A, T070A, T070B, T072A. ✅
- Tareas con marker `[P]` (paralelizables): T002–T006, T008–T014, T015–T019, T021–T025, T044, T046, T047, T049–T058, T059–T067, T069–T073, T075, T078–T081
- Tareas con label de historia de usuario: T015–T058 (Fases 3–6) ✅
- Fases Setup y Fundacional: sin label de historia ✅
- Fase Final (Polish): sin label de historia ✅
- Rutas de archivo incluidas en todas las tareas de implementación: ✅
- Alineación con Constitución: 12/12 principios cubiertos ✅
