# API Financiera SALE - Integration Guide

## 1. Propósito

Esta guía documenta cómo consumir la API Financiera SALE y cómo interpretar su comportamiento operativo.

Incluye:

- Ambientes y URLs
- Autenticación y autorización
- Headers requeridos
- Contrato de request/response
- Errores RFC9457
- CorrelationId
- Ejemplos Postman y Curl
- Trazabilidad y observabilidad
- Variables de configuración operativas

## 2. Endpoint principal

**Método**: `POST`  
**Path**: `/api/v1/sales`  
**Descripción**: Procesa una transacción financiera tipo SALE.

## 3. Ambientes

| Ambiente | Base URL API SALE |
|---|---|
| Local | `http://localhost:8080` |
| Dev | `TBD` |
| QA | `TBD` |
| Prod | `TBD` |

Notas:

- Las URLs de Dev/QA/Prod aún no están definidas por infraestructura.
- Se deben publicar como variables de configuración de despliegue por ambiente.
- Ejemplo de convención para equipos consumidores: `SALE_API_BASE_URL_DEV`, `SALE_API_BASE_URL_QA`, `SALE_API_BASE_URL_PROD`.

## 4. Autenticación

### 4.1 Local

- No requiere token.
- Se puede usar Swagger, Postman o Curl sin header `Authorization`.

### 4.2 Dev / QA / Prod

- Autenticación con Microsoft Entra ID.
- Esquema: OAuth2 + JWT Bearer.
- Header requerido:

```http
Authorization: Bearer <token>
```

Parámetros esperados (placeholders):

- Tenant: `<tenant-id>`
- Client/Application ID (audience): `<client-id>`
- Scope: `<scope>`

Validaciones esperadas del JWT:

- `iss` válido para `<tenant-id>`
- `aud` esperado para `<client-id>`
- vigencia (`exp`) no expirada
- firma JWT válida

## 5. Headers requeridos

| Header | Requerido | Descripción |
|---|---|---|
| `Content-Type: application/json` | Sí | Tipo de contenido del request |
| `Accept: application/json` | Recomendado | Tipo de respuesta esperado |
| `Authorization: Bearer <token>` | Dev/QA/Prod | Token OAuth2 JWT de Entra ID |
| `X-Correlation-Id` | Recomendado | Id de trazabilidad extremo a extremo |

Comportamiento de correlación:

- Si el cliente envía `X-Correlation-Id`, el sistema lo reutiliza.
- Si el cliente no lo envía, el sistema lo genera automáticamente.
- La API responde siempre `X-Correlation-Id`.

## 6. Request

### 6.1 JSON del endpoint `POST /api/v1/sales`

```json
{
  "terminalId": "TERM-0001",
  "transactionType": "SALE",
  "accountNumber": "4111111111111111",
  "expirationDate": "2805",
  "invoice": 14611279,
  "totalAmount": 5633,
  "securityCodeEntry": "123",
  "securityValidationResponse": "M",
  "binValidate": true,
  "authenticationInformation": {
    "eci": "05",
    "cavv": "AAABBBCCC111",
    "xid": "XID-123",
    "enrollmentStatus": "Y"
  },
  "processingInformation": {
    "errorCentinel": "0",
    "statusReason": "NONE"
  },
  "tokenizationInformation": {
    "wallet": "APPLE_PAY",
    "device": "IOS",
    "paymentIndicator": "3DS",
    "cryptogramEci": "05",
    "cryptogram": "CRYPT-001"
  }
}
```

### 6.2 Definición de atributos (negocio vs contrato)

| Atributo de negocio | Campo JSON actual | Tipo | Requerido | Notas |
|---|---|---|---|---|
| terminalId | `terminalId` | string | Sí | Identificador de terminal |
| transactionType | `transactionType` | string | Sí | Valor esperado: `SALE` |
| accountNumber | `accountNumber` | string | Sí | PAN/token; se enmascara en logs |
| expirationDate | `expirationDate` | string | Sí | Formato `YYMM` |
| invoiceNumber | `invoice` | number | Sí | Corresponde a invoiceNumber de negocio |
| amount | `totalAmount` | number | Sí | En unidad monetaria mínima (ej. 56.33 -> 5633) |
| currency | N/A (no expuesto en contrato actual) | string | No | Puede manejarse por configuración de canal/comercio |
| authenticationInformation | `authenticationInformation` | object | No | Datos 3DS/autenticación |
| processingInformation | `processingInformation` | object | No | Señales de procesamiento |
| tokenizationInformation | `tokenizationInformation` | object | No | Datos de wallet/tokenización |

## 7. Response exitoso

### 7.1 HTTP 200 OK

```json
{
  "transactionId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "AUTHORIZED",
  "authorization": {
    "authorizationSource": "AS400",
    "authorizationNumber": "AUTH123456",
    "responseCode": "00",
    "responseDescription": "Aprobado",
    "referenceNumber": "REF20260722001",
    "hostDate": "0722",
    "hostTime": "143052"
  },
  "processingDateTime": "2026-07-23T19:30:52.456Z"
}
```

Notas:

- `AUTHORIZED` equivale funcionalmente al concepto de `APPROVED`.
- `transactionId` es generado por la API.

## 8. Errores RFC9457

Formato base (`application/problem+json`):

```json
{
  "type": "/errors/<error-type>",
  "title": "<título>",
  "status": 400,
  "detail": "<detalle>",
  "instance": "/api/v1/sales"
}
```

### 8.1 400 Validation Error

```json
{
  "type": "/errors/validation-error",
  "title": "Solicitud inválida",
  "status": 400
}
```

### 8.2 401 Unauthorized

```json
{
  "type": "/errors/unauthorized",
  "title": "No autorizado",
  "status": 401
}
```

### 8.3 403 Forbidden

```json
{
  "type": "/errors/access-denied",
  "title": "Acceso denegado",
  "status": 403
}
```

### 8.4 409 Duplicate Transaction

```json
{
  "type": "/errors/duplicate-transaction",
  "title": "Transacción duplicada",
  "status": 409
}
```

### 8.5 503 Database Unavailable

```json
{
  "type": "/errors/database-unavailable",
  "title": "Dependencia externa no disponible",
  "status": 503
}
```

### 8.6 503 Switch Unavailable

```json
{
  "type": "/errors/switch-unavailable",
  "title": "Dependencia externa no disponible",
  "status": 503
}
```

### 8.7 500 Internal Server Error

```json
{
  "type": "/errors/internal-server-error",
  "title": "Error interno",
  "status": 500
}
```

## 9. Ejemplo Postman

### 9.1 Local (sin token)

- Method: `POST`
- URL: `http://localhost:8080/api/v1/sales`
- Headers:
  - `Content-Type: application/json`
  - `X-Correlation-Id: 550e8400-e29b-41d4-a716-446655440000`
- Body: raw JSON (usar el ejemplo de sección 6.1)

### 9.2 Dev/QA/Prod (con token)

- Method: `POST`
- URL: `https://<dev-url>/api/v1/sales` (o `https://<qa-url>`, `https://<prod-url>`)
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
  - `X-Correlation-Id: 550e8400-e29b-41d4-a716-446655440000`

## 10. Ejemplos Curl

### 10.1 Local

```bash
curl -X POST "http://localhost:8080/api/v1/sales" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "terminalId":"TERM-0001",
    "transactionType":"SALE",
    "accountNumber":"4111111111111111",
    "expirationDate":"2805",
    "invoice":14611279,
    "totalAmount":5633,
    "securityCodeEntry":"123",
    "securityValidationResponse":"M",
    "binValidate":true
  }'
```

### 10.2 Dev/QA/Prod

```bash
curl -X POST "https://<dev-url>/api/v1/sales" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "X-Correlation-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "terminalId":"TERM-0001",
    "transactionType":"SALE",
    "accountNumber":"4111111111111111",
    "expirationDate":"2805",
    "invoice":14611279,
    "totalAmount":5633,
    "securityCodeEntry":"123",
    "securityValidationResponse":"M",
    "binValidate":true
  }'
```

## 11. Trazabilidad y correlación

La API registra y propaga trazabilidad en todo el flujo:

- Header de entrada/salida: `X-Correlation-Id`
- Identificador interno de transacción: `transactionId`
- Correlación de trazas: `traceId`, `spanId`

La integración con dependencias externas conserva la correlación para facilitar troubleshooting E2E.

## 12. Observabilidad

La API emite logs estructurados JSON y eventos de negocio/técnicos.

Eventos principales:

- `sale.request.received`
- `sale.process.started`
- `sale.process.completed`
- `sale.response.generated`
- `sale.error`

Campos clave en telemetría:

- `correlationId`
- `transactionId`
- `traceId`
- `spanId`

## 13. Seguridad (PCI-DSS)

Políticas obligatorias de logging y datos sensibles:

- No registrar CVV (`securityCodeEntry` / `securityCode`).
- No registrar PAN completo.
- Aplicar enmascaramiento de PAN (ejemplo): `411111******1111`.

## 14. Variables de configuración operativa

| Variable | Descripción |
|---|---|
| `SWITCH_BASE_URL` | Base URL de AppConnector para integraciones salientes (`/api/v1/payments`) |
| `SWITCH_API_KEY` | API Key enviada en header `X-API-Key` hacia AppConnector |
| `MONGO_URI` | URI de conexión a MongoDB |
| `MONGO_DATABASE` | Nombre de base de datos Mongo |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | Conexión a Azure Application Insights |
| `SPRING_PROFILES_ACTIVE` | Perfil activo de Spring (`local`, `azure`, etc.) |

## 15. Integración saliente a AppConnector (referencia operativa)

Para observabilidad de dependencias, la API SALE invoca internamente AppConnector con:

- Método: `POST`
- Path: `/api/v1/payments`
- Base URL: `SWITCH_BASE_URL`
- Header: `X-API-Key: ${SWITCH_API_KEY}`
- Header de correlación: `X-Correlation-Id`

Esto permite cambiar endpoint por ambiente sin recompilar.

## 16. Estado de definición de URLs no productivas/productivas

- Dev URL: `TBD`
- QA URL: `TBD`
- Prod URL: `TBD`

Estas URLs serán publicadas por infraestructura y consumidas mediante variables/configuración de despliegue por ambiente.
