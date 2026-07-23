# Especificacion de Feature: API Financiera de Transacciones SALE

**Rama de Feature**: `001-api-transacciones-sale`  
**Creada**: 2026-07-22  
**Estado**: Borrador  
**Entrada**: Especificacion detallada de API eCommerce para procesamiento de transacciones SALE

---

## Escenarios de Usuario y Pruebas *(obligatorio)*

### Historia de Usuario 1 - Procesamiento de Autorizacion SALE (Prioridad: P1)

Un comercio afiliado envia una solicitud SALE y recibe una respuesta estandarizada de autorizacion financiera.

**Por que esta prioridad**: Es el flujo principal del MVP y entrega valor inmediato al comercio.

**Prueba Independiente**:
1. El comercio envia `POST /api/v1/sales` con JWT valido y payload valido.
2. La API valida la solicitud, genera `transactionId` (UUID v4) y delega al API Switch.
3. La API persiste la transaccion y retorna HTTP 200 con `AUTHORIZED` o `DECLINED`.

**Escenarios de Aceptacion**:

1. **Dado** un comercio autenticado, **Cuando** envia una solicitud SALE valida, **Entonces** la API retorna HTTP 200 con `transactionId`, `authorizationSource`, `responseCode` y `status`.
2. **Dado** un API Switch disponible, **Cuando** retorna autorizacion, **Entonces** la API persiste la respuesta completa en MongoDB.
3. **Dado** un fallo temporal del API Switch, **Cuando** se procesa la solicitud, **Entonces** la API aplica resiliencia y retorna HTTP 503 en degradacion controlada.

---

### Historia de Usuario 2 - Validacion Integral y Deteccion de Duplicados (Prioridad: P2)

La API valida todos los campos de entrada y evita reprocesar operaciones duplicadas mediante reglas de negocio.

**Por que esta prioridad**: Evita reprocesos, errores operativos y resultados inconsistentes.

**Prueba Independiente**:
1. La API rechaza solicitudes invalidas con HTTP 400.
2. Para la misma combinacion de negocio (`terminalId`, `invoice`, `totalAmount`, `accountNumber`, `transactionType`) la API retorna el resultado ya existente sin invocar nuevamente el API Switch.

**Escenarios de Aceptacion**:

1. **Dado** una solicitud sin `terminalId`, **Cuando** se valida, **Entonces** retorna HTTP 400.
2. **Dado** una solicitud con `totalAmount` menor o igual a 0, **Cuando** se valida, **Entonces** retorna HTTP 400.
3. **Dado** una solicitud con `transactionType` distinto de `SALE`, **Cuando** se valida, **Entonces** retorna HTTP 400.
4. **Dado** una solicitud con formato invalido en `expirationDate` (YYMM), **Cuando** se valida, **Entonces** retorna HTTP 400.
5. **Dado** una operacion previamente procesada con misma clave de negocio, **Cuando** llega un duplicado, **Entonces** la API retorna el resultado existente sin reprocesar.

---

### Historia de Usuario 3 - Trazabilidad Operativa de Extremo a Extremo (Prioridad: P2)

La API debe permitir rastrear cada operacion con `transactionId` y `correlationId` en logs y trazas.

**Por que esta prioridad**: La trazabilidad operativa es obligatoria para soporte, monitoreo e investigacion de incidentes.

**Prueba Independiente**:
1. Cada solicitud genera o propaga `X-Correlation-Id`.
2. Todos los eventos del flujo incluyen `transactionId` y `correlationId`.
3. OpenTelemetry exporta trazas con visibilidad de pasos clave del procesamiento.

**Escenarios de Aceptacion**:

1. **Dado** una transaccion procesada, **Cuando** se revisan logs, **Entonces** todos los eventos incluyen `correlationId` y `transactionId`.
2. **Dado** OpenTelemetry habilitado, **Cuando** se procesa una transaccion, **Entonces** la traza contiene validacion, autorizacion y persistencia.

---

### Historia de Usuario 4 - Disponibilidad y Resiliencia (Prioridad: P3)

La API opera de forma controlada ante fallos parciales de dependencias externas.

**Por que esta prioridad**: Reduce indisponibilidad total y mejora continuidad de servicio.

**Prueba Independiente**:
1. Ante fallos repetidos del API Switch, se abre Circuit Breaker.
2. Ante timeout, se aplican reintentos con backoff exponencial.
3. En estado OPEN, la API responde degradado sin bloquear recursos.

**Escenarios de Aceptacion**:

1. **Dado** timeout del API Switch (> 5s), **Cuando** se procesa una solicitud, **Entonces** se aplican reintentos configurados y luego degradacion controlada.
2. **Dado** fallos consecutivos, **Cuando** el Circuit Breaker esta OPEN, **Entonces** la API responde 503 sin invocacion externa.

---

### Casos Limite

- Duplicado por clave de negocio con misma informacion: retornar respuesta existente.
- JWT expirado o invalido: retornar HTTP 401.
- API Switch con `responseCode` no reconocido: persistir codigo, mapear estado operativo y alertar en observabilidad.
- Saturacion temporal de MongoDB: degradacion controlada y eventos trazables.

---

## Requisitos *(obligatorio)*

### Requisitos Funcionales

- **RF-001**: La API DEBE exponer `POST /api/v1/sales` para procesar transacciones SALE de comercios autenticados y autorizados.
- **RF-002**: La solicitud DEBE validar como minimo: `merchantId`, `terminalId`, `transactionType`, `totalAmount`, `accountNumber`, `expirationDate`, `invoice`, `securityCodeEntry`, `securityValidationResponse`, `binValidate`, `authenticationInformation`, `tokenizationInformation`, `processingInformation`.
- **RF-003**: La API DEBE generar `transactionId` automaticamente (UUID v4, 36 caracteres) antes de iniciar el procesamiento.
- **RF-004**: La deteccion de duplicados DEBE realizarse usando la combinacion: `terminalId`, `invoice`, `totalAmount`, `accountNumber`, `transactionType`.
- **RF-005**: El `transactionId` DEBE usarse solo para identificacion y trazabilidad de la transaccion, no como clave de duplicados.
- **RF-006**: `totalAmount` DEBE modelarse como `Long` en unidad monetaria minima (centavos). Ejemplo: 56.33 USD = 5633.
- **RF-007**: La API DEBE delegar la autorizacion financiera al API Switch, sin logica de seleccion de autorizador dentro de la API SALE.
- **RF-008**: Toda transaccion DEBE persistirse en MongoDB con retencion de 548 dias (TTL) e indice unico por `transactionId`.
- **RF-009**: La API DEBE propagar `X-Correlation-Id` y registrar trazabilidad con OpenTelemetry, logs estructurados y Application Insights.
- **RF-010**: La API DEBE implementar resiliencia con Circuit Breaker, Retry, Bulkhead, TimeLimiter y RateLimiter para dependencias externas.
- **RF-011**: Los errores DEBEN responder con formato RFC 9457.

### Entidades Clave *(datos involucrados)*

- **SaleTransaction**: `transactionId`, `correlationId`, `merchantId`, `terminalId`, `transactionType`, `totalAmount` (Long en centavos), `accountNumber` (tokenizado/enmascarado), `expirationDate`, `invoice`, `securityValidationResponse`, `binValidate`, `status`, `authorizationResult`, `createdAt`, `processingDateTime`, `updatedAt`.
- **AuthorizationResponse**: `authorizationSource`, `authorizationNumber`, `responseCode`, `responseDescription`, `referenceNumber`, `hostDate`, `hostTime`.

**Fuera de alcance MVP**:
- Entidad dedicada `TransactionAudit`.
- Puerto `AuditRepositoryPort`.
- Caso de uso `RecordAuditUseCase`.

La auditoria del MVP se cubre mediante `CorrelationId`, OpenTelemetry, logs estructurados y Application Insights.

---

## Criterios de Exito *(obligatorio)*

### Resultados Medibles

- **CE-001**: Latencia P95 < 3 segundos.
- **CE-002**: Latencia P99 < 5 segundos.
- **CE-003**: Disponibilidad mensual >= 99.9%.
- **CE-004**: Tasa de error < 1%.
- **CE-005**: Cero reprocesos para operaciones duplicadas detectadas por la clave de negocio definida.
- **CE-006**: 100% de transacciones persisten en MongoDB dentro de la ventana de retencion.
- **CE-007**: 100% de operaciones trazables por `transactionId` y `correlationId`.
- **CE-008**: 100% de endpoints del MVP protegidos con JWT Entra ID.
- **CE-009**: OpenAPI y Swagger consistentes con el payload real del MVP.

---

## Supuestos

- Comercios autenticados mediante Microsoft Entra ID.
- Credenciales del API Switch gestionadas en Azure Key Vault via Managed Identity.
- Datos de cuenta llegan tokenizados o enmascarados; no se permite PAN en claro.
- El API Switch expone contrato REST vigente para autorizaciones.
- Azure Monitor y Application Insights estan disponibles para observabilidad.

---

**Version**: 1.1.0  
**Estado**: Borrador alineado para MVP  
**Fecha de Actualizacion**: 2026-07-22
