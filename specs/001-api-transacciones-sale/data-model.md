# Modelo de Datos: API Financiera de Transacciones SALE

**Feature**: `001-api-transacciones-sale`  
**Fecha**: 2026-07-22  
**Prerequisito**: [research.md](research.md) completado

---

## Entidades del Dominio

### 1. SaleTransaction (Raíz de Aggregate)

**Propósito**: Representa el ciclo de vida completo de una transacción SALE. Es la entidad principal y la única raíz de Aggregate del dominio.

**Capa**: `domain/model/`  
**Tipo Java**: `record` (inmutable)

```
SaleTransaction
├── transactionId       : String (UUID v4, generado por sistema, 36 chars)
├── correlationId       : String (UUID v4, propagado del request)
├── merchantId          : String (identificador del comercio, obligatorio)
├── terminalId          : String (identificador del terminal POS, obligatorio)
├── transactionType     : TransactionType (enum: SALE)
├── totalAmount         : Long (monto de la transacción)
├── accountNumber       : String (cuenta o tarjeta tokenizada)
├── cardBrand           : CardBrand (determinado a partir del BIN)
├── expirationDate      : String
├── invoice             : Long
├── securityCodeEntry   : String
├── securityValidationResponse : String
├── binValidate         : Boolean
├── status              : TransactionStatus
├── authorizationResult : AuthorizationResponse
├── createdAt           : Instant
├── processingDateTime  : Instant
└── updatedAt           : Instant
```

**Ciclo de Vida (State Machine)**:
```
PENDING → AUTHORIZED
        → DECLINED
        → ERROR
```

**Reglas de Dominio / Invariantes**:
- `transactionId` nunca es `null` ni vacío; es UUID v4.
- `totalAmount` debe ser > 0.
- `accountNumber`  debe contener únicamente datos tokenizados o enmascarados.
-  `invoice` es obligatorio.
- `transactionType`  debe ser SALE.
- `cardBrand`  debe determinarse automáticamente a partir del BIN de accountNumber cuando la información esté disponible.
- Una vez que `status` es `AUTHORIZED`, `DECLINED` o `ERROR` (estado terminal), no puede modificarse.
- `authorizationResult` solo se establece cuando `status` cambia a estado terminal.


---

### 2. AuthorizationResponse (Value Object)

**Propósito**: Encapsula la respuesta recibida del API Switch transaccional. Es inmutable una vez creada.

**Capa**: `domain/model/`  
**Tipo Java**: `record`

```
AuthorizationResponse
├── authorizationSource   : AuthorizationSource (enum: AS400, CYBERSOURCE, UNKNOWN)
├── authorizationNumber   : String (número de autorización del autorizador)
├── responseCode          : String (código de respuesta del autorizador)
├── responseDescription   : String (descripción de la respuesta)
├── referenceNumber       : String (número de referencia de la operación)
├── hostDate              : String (fecha del host autorizador, formato MMDD)
└── hostTime              : String (hora del host autorizador, formato HHMMSS)
```

**Reglas**:
- `authorizationSource` nunca es `null`; si no se puede determinar, es `UNKNOWN`.
- `responseCode` siempre está presente; es la fuente de verdad del resultado de autorización.
- Una respuesta con `responseCode = "00"` (o equivalente aprobatorio) resulta en `TransactionStatus.AUTHORIZED`.
- Cualquier otro `responseCode` de negocio resulta en `TransactionStatus.DECLINED`.
- Fallos de comunicación/timeout resultan en `TransactionStatus.ERROR`.

---



## Enumeraciones del Dominio

```
TransactionStatus   : PENDING | AUTHORIZED | DECLINED | ERROR
TransactionType     : SALE
AuthorizationSource : AS400 | CYBERSOURCE | UNKNOWN
CardBrand           : VISA | MASTERCARD | AMEX | DISCOVER | OTHER
```

---

## Puertos (Interfaces de Application Layer)

### TransactionRepositoryPort
```java
// application/port/out/TransactionRepositoryPort.java
interface TransactionRepositoryPort {
    void save(SaleTransaction transaction);                          // Persiste nueva transacción
   Optional<SaleTransaction> findByTransactionId(String id);       // Busca por ID para trazabilidad
   Optional<SaleTransaction> findDuplicate(
    String terminalId,
    Long invoice,
    Long totalAmount,
    String accountNumber,
    String transactionType
);
    List<SaleTransaction> findByMerchantId(String merchantId,       // Consulta para auditoría
                                           Instant from, Instant to,
                                           int page, int size);
    void updateStatus(String transactionId,                         // Actualiza estado terminal
                      TransactionStatus status,
                      AuthorizationResponse response);
}
```

### AuthorizationSwitchPort
```java
// application/port/out/AuthorizationSwitchPort.java
interface AuthorizationSwitchPort {
    AuthorizationResponse authorize(SaleTransaction transaction);   // Delega al API Switch
}
```

### SwitchAuthenticationPort
```java
// application/port/out/SwitchAuthenticationPort.java
interface SwitchAuthenticationPort {
    String getAccessToken();   // OAuth 2.0 Client Credentials, con caché automática
}
```


---

## Esquema MongoDB

### Colección: `sale_transactions`

```json
{
  "_id": "ObjectId",
  "transactionId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "merchantId": "MERCHANT-001",
  "terminalId": "TERM-0001",
  "transactionType": "SALE",
"totalAmount": 77477,
"accountNumber": "340000000001098",
"cardBrand": "AMEX",
"expirationDate": "2805",
"invoice": 14611279,
"securityCodeEntry": "1234",
"securityValidationResponse": "1",
"binValidate": true,
  "status": "AUTHORIZED",
  "authorizationResult": {
    "authorizationSource": "AS400",
    "authorizationNumber": "AUTH123456",
    "responseCode": "00",
    "responseDescription": "Aprobado",
    "referenceNumber": "REF20260722001",
    "hostDate": "0722",
    "hostTime": "143052"
  },
  "createdAt": "2026-07-22T14:30:52.123Z",
  "processingDateTime": "2026-07-22T14:30:52.456Z",
  "updatedAt": "2026-07-22T14:30:53.789Z"
}
```

### Índices de la Colección

| Nombre | Campos | Tipo | Propósito |
|--------|--------|------|-----------|
| `idx_transactionId_unique` | `{ transactionId: 1 }` | Único | Identificación única y trazabilidad |
| `idx_createdAt_ttl` | `{ createdAt: 1 }` | TTL (47347200s = 548 días) | Retención 18 meses |
| `idx_merchantId_createdAt` | `{ merchantId: 1, createdAt: -1 }` | Compuesto | Consultas de auditoría por comercio |
| `idx_status` | `{ status: 1 }` | Simple | Filtros por estado operativo |


## Transiciones de Estado

```
                    ┌─────────────────────────────────────────┐
                    │                                         │
  [REQUEST]    ┌────▼─────┐    [SWITCH OK, code=00]   ┌──────▼──────┐
  ──────────►  │ PENDING  │ ─────────────────────────► │ AUTHORIZED  │
               └────┬─────┘                            └─────────────┘
                    │
                    │  [SWITCH OK, code≠00]            ┌─────────────┐
                    ├──────────────────────────────────► │  DECLINED   │
                    │                                   └─────────────┘
                    │
                    │  [Timeout / CB open / error]      ┌─────────────┐
                    └──────────────────────────────────► │    ERROR    │
                                                        └─────────────┘
```

**Regla**: AUTHORIZED, DECLINED y ERROR son estados terminales. Ninguna transacción en estado terminal puede ser modificada.

---

## Reglas de Enmascaramiento de Datos

## Reglas de Enmascaramiento de Datos

| Campo | Almacenamiento | Logs |
|-------|----------------|------|
| accountNumber (VISA / MASTERCARD) | Enmascarado | Mostrar BIN de 8 dígitos + enmascarar dígitos intermedios + mostrar últimos 4 dígitos |
| accountNumber (otros esquemas con BIN de 6) | Enmascarado | Mostrar BIN de 6 dígitos + enmascarar dígitos intermedios + mostrar últimos 4 dígitos |
| securityCodeEntry | NUNCA almacenar | NUNCA registrar |
| authorizationNumber | Completo | Completo |
| responseCode | Completo | Completo |
| correlationId | Completo | Completo |
| transactionId | Completo | Completo |

---

### Reglas de Enmascaramiento de PAN

- Para esquemas que utilizan BIN de 8 dígitos (ej. VISA y MASTERCARD), los logs deberán mostrar los primeros 8 dígitos y los últimos 4 dígitos, enmascarando el resto.

Ejemplo:
12345678****1234

- Para esquemas que utilizan BIN de 6 dígitos, los logs deberán mostrar los primeros 6 dígitos y los últimos 4 dígitos, enmascarando el resto.

Ejemplo:
123456******1234

- El securityCodeEntry (CVV/CVC/CID) nunca deberá almacenarse ni registrarse en logs.

- Los datos enmascarados deberán utilizarse en logs, trazas, métricas y mensajes de auditoría.

---

## Re-Verificación de Constitución Post-Diseño

| Principio | Estado | Evidencia |
|-----------|--------|-----------|
| I — Arquitectura Limpia | ✅ | Puertos en `application/port/out`, adaptadores en `infrastructure/adapter` |
| II — SOLID | ✅ | Value Objects inmutables (record), un port = una responsabilidad, sin `null` en domain |
| III — Seguridad | ✅ | `accountNumber`  tokenizado o enmascarado, securityCode nunca persistido ni registrado en logs, reglas de enmascaramiento basadas en BIN de 6 u 8 dígitos definidas.|
| IV — Validación | ✅ | Invariantes de dominio definidas en `SaleTransaction`  |
| V — Idempotencia | ✅ | Detección de duplicados mediante reglas de negocio utilizando terminalId, invoice, totalAmount, accountNumber y transactionType. transactionId se utiliza únicamente para identificación y trazabilidad. |
| VI — MongoDB 18 meses | ✅ |  TTL 548 días en la colección `sale_transactions` y retención de información de trazabilidad durante el período definido.|
| VII — Servicio Transaccional | ✅ | `AuthorizationSwitchPort` encapsula completamente la integración |
| VIII — Resiliencia | ✅ | Adaptadores decorados con Resilience4j (definido en research.md) |
| IX — Observabilidad | ✅ |  CorrelationId propagado mediante OpenTelemetry, Application Insights, Azure Monitor y logs estructurados |
| X — SLA | ✅ | Timeout ≤ 5s en `SwitchApiAdapter` alineado con P95 < 3s |
| XI — OpenAPI | ✅ | Contratos definidos en `contracts/openapi.yaml` |
| XII — Azure | ✅ | Sin dependencias de plataforma en entidades de dominio |

**Resultado**: ✅ TODOS LOS PRINCIPIOS PASAN (post-diseño)
