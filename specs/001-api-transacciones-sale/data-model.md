# Modelo de Datos: API Financiera de Transacciones SALE

**Feature**: `001-api-transacciones-sale`  
**Fecha**: 2026-07-22  
**Prerequisito**: [research.md](research.md)

---

## Entidades del Dominio

### 1. SaleTransaction (Aggregate Root)

**Proposito**: Representa el ciclo completo de una transaccion SALE.

**Capa**: `domain/model/`  
**Tipo Java**: `record` (inmutable)

```text
SaleTransaction
├── transactionId               : String (UUID v4 generado por API)
├── correlationId               : String (UUID v4 propagado o generado)
├── merchantId                  : String
├── terminalId                  : String
├── transactionType             : TransactionType (SALE)
├── totalAmount                 : Long (unidad monetaria minima, centavos)
├── accountNumber               : String (tokenizado o enmascarado)
├── expirationDate              : String (YYMM)
├── invoice                     : Long
├── securityValidationResponse  : String
├── binValidate                 : Boolean
├── status                      : TransactionStatus
├── authorizationResult         : AuthorizationResponse
├── createdAt                   : Instant
├── processingDateTime          : Instant
└── updatedAt                   : Instant
```

**Regla monetaria**:
- `totalAmount` usa unidad minima (centavos).
- Ejemplo: 56.33 USD = 5633.

**Invariantes**:
- `transactionId` no puede ser `null` ni vacio.
- `transactionType` debe ser `SALE`.
- `totalAmount` debe ser > 0.
- `accountNumber` no puede ser PAN en claro.
- `invoice` es obligatoria.
- Estados terminales (`AUTHORIZED`, `DECLINED`, `ERROR`) no pueden mutar.

**Deteccion de duplicados**:
- Se determina por combinacion exacta de:
  - `terminalId`
  - `invoice`
  - `totalAmount`
  - `accountNumber`
  - `transactionType`

`transactionId` no participa en la deteccion de duplicados.

---

### 2. AuthorizationResponse (Value Object)

**Proposito**: Encapsula la respuesta del API Switch.

**Capa**: `domain/model/`  
**Tipo Java**: `record`

```text
AuthorizationResponse
├── authorizationSource   : AuthorizationSource (AS400 | CYBERSOURCE | UNKNOWN)
├── authorizationNumber   : String
├── responseCode          : String
├── responseDescription   : String
├── referenceNumber       : String
├── hostDate              : String (MMDD)
└── hostTime              : String (HHMMSS)
```

**Reglas**:
- `responseCode` es obligatorio.
- `responseCode = "00"` mapea a `AUTHORIZED`.
- Otros `responseCode` de negocio mapean a `DECLINED`.
- Errores tecnicos/timeout mapean a `ERROR`.

---

## Enumeraciones

```text
TransactionStatus   : PENDING | AUTHORIZED | DECLINED | ERROR
TransactionType     : SALE
AuthorizationSource : AS400 | CYBERSOURCE | UNKNOWN
CardBrand           : VISA | MASTERCARD | AMEX | DISCOVER | OTHER
```

---

## Puertos (Application Layer)

### TransactionRepositoryPort

```java
interface TransactionRepositoryPort {
    void save(SaleTransaction transaction);
    Optional<SaleTransaction> findByTransactionId(String transactionId);
    Optional<SaleTransaction> findDuplicate(
        String terminalId,
        Long invoice,
        Long totalAmount,
        String accountNumber,
        String transactionType
    );
    void updateStatus(
        String transactionId,
        TransactionStatus status,
        AuthorizationResponse response
    );
}
```

### AuthorizationSwitchPort

```java
interface AuthorizationSwitchPort {
    AuthorizationResponse authorize(SaleTransaction transaction);
}
```

### SwitchAuthenticationPort

```java
interface SwitchAuthenticationPort {
    String getAccessToken();
}
```

---

## Esquema MongoDB

### Coleccion `sale_transactions`

```json
{
  "_id": "ObjectId",
  "transactionId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "merchantId": "MERCHANT-001",
  "terminalId": "TERM-0001",
  "transactionType": "SALE",
  "totalAmount": 5633,
  "accountNumber": "55189800****2751",
  "expirationDate": "2805",
  "invoice": 14611279,
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

`securityCodeEntry` no se persiste en MongoDB.

### Indices

| Nombre | Campos | Tipo | Proposito |
|--------|--------|------|-----------|
| `idx_transactionId_unique` | `{ transactionId: 1 }` | Unico | Identificacion/trazabilidad |
| `idx_createdAt_ttl` | `{ createdAt: 1 }` | TTL 47347200s | Retencion 548 dias |
| `idx_duplicate_validation` | `{ terminalId: 1, invoice: 1, totalAmount: 1, accountNumber: 1, transactionType: 1 }` | Compuesto | Deteccion eficiente de duplicados |

---

## Transiciones de Estado

```text
PENDING -> AUTHORIZED
PENDING -> DECLINED
PENDING -> ERROR
```

---

## Proteccion de PAN

| Campo | Almacenamiento | Logs |
|-------|----------------|------|
| accountNumber (VISA/MC BIN 8) | Enmascarado | BIN 8 + mascara + ultimos 4 |
| accountNumber (otros BIN 6) | Enmascarado | BIN 6 + mascara + ultimos 4 |
| securityCodeEntry | NUNCA almacenar | NUNCA registrar |

Ejemplos validos de salida:
- `55189800****2751`
- `123456******1234`

---

## Alcance de Auditoria en MVP

**Fuera de alcance en esta iteracion**:
- `TransactionAudit` como entidad dedicada
- `AuditRepositoryPort`
- `RecordAuditUseCase`

**Se mantiene en MVP**:
- `CorrelationId`
- OpenTelemetry
- Logs estructurados JSON
- Application Insights

---

## Re-Verificacion de Constitucion

| Principio | Estado | Evidencia |
|-----------|--------|-----------|
| I - Arquitectura Limpia | PASA | Puertos en application y adaptadores en infrastructure |
| III - Seguridad | PASA | PAN enmascarado/tokenizado, CVV no persistido |
| V - Idempotencia | PASA | Duplicados por clave de negocio, no por transactionId |
| VI - Persistencia | PASA | TTL 548 dias + indice unico transactionId |
| IX - Observabilidad | PASA | CorrelationId + OTel + logs + App Insights |

**Resultado**: PASA
