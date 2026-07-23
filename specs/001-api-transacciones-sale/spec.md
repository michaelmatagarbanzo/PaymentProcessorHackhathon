# Especificación de Feature: API Financiera de Transacciones SALE

**Rama de Feature**: `001-api-transacciones-sale`  
**Creada**: 2026-07-22  
**Estado**: Borrador  
**Entrada**: Especificación detallada de API eCommerce para procesamiento de transacciones SALE

---

## Escenarios de Usuario y Pruebas *(obligatorio)*

### Historia de Usuario 1 — Procesamiento de Autorización SALE (Prioridad: P1)

Un comercio afiliado requiere enviar una solicitud de pago (SALE) para que sea autorizada por un proveedor financiero externo. El sistema deberá validar la solicitud, enviarla a un servicio transaccional especializado, recibir la autorización y retornar el resultado al comercio en tiempo real.

**Por qué esta prioridad**: Es la funcionalidad central del sistema. Sin ella, no hay valor comercial posible. Todo comercio requiere poder procesar transacciones de venta.

**Prueba Independiente**: Puede completarse probando únicamente:
1. Un comercio autenticado envía una solicitud SALE válida con monto, moneda, información de pago tokenizado.
2. Entonces genera un transactionId único en formato UUID v4 de 36 caracteres antes de iniciar el procesamiento de la transacción.
2. El sistema valida la solicitud, la envía al servicio transaccional.
3. El comercio recibe una respuesta HTTP 200 con el resultado de autorización (aprobado/rechazado).
4. La transacción queda registrada en MongoDB con transactionId único.

**Escenarios de Aceptación**:

1. **Dado** un comercio autenticado con credenciales válidas de Entra ID, **Cuando** envía una solicitud SALE con transactionId único (UUID v4), monto $100 USD, información de pago tokenizado y correlationId, **Entonces** el sistema retorna HTTP 200 con authorizationNumber, responseCode, authorizationSource y status AUTHORIZED o DECLINED en menos de 3 segundos (P95).

2. **Dado** un servicio transaccional disponible, **Cuando** el sistema recibe una respuesta de autorización, **Entonces** registra la respuesta completa en MongoDB incluyendo authorizationSource (AS400 o CYBERSOURCE), authorizationNumber, responseCode, hostDate, hostTime.

3. **Dado** un servicio transaccional unavailable (timeout), **Cuando** se envía una solicitud SALE, **Entonces** el sistema retorna HTTP 503 con motivo de error descriptivo (CircuitBreaker abierto, timeout, etc.) tras agotar reintentos configurados (máx 3).

---

### Historia de Usuario 2 — Validación Integral de Solicitudes (Prioridad: P2)

El sistema debe garantizar que toda solicitud SALE sea validada antes de iniciar su procesamiento, verificando la presencia de campos obligatorios, la integridad de la información y el cumplimiento de las reglas de negocio aplicables.

**Por qué esta prioridad**: La validación temprana previene errores operativos, evita el procesamiento de información inválida y reduce el riesgo de fallos durante la autorización financiera.

**Prueba Independiente**: Puede completarse sin que P1 esté completamente implementado:

1. Un comercio envía una solicitud SALE válida.
2. El sistema valida todos los campos obligatorios y reglas de negocio.
3. El sistema genera un transactionId único en formato UUID v4 de 36 caracteres.
4. La solicitud válida continúa con el flujo de procesamiento.
5. Las solicitudes inválidas son rechazadas antes de invocar el servicio transaccional.
6. Se registra el resultado de la validación para fines de auditoría y trazabilidad.

**Escenarios de Aceptación**:

1. **Dado** una solicitud SALE sin `terminalId`, **Cuando** se recibe la solicitud, **Entonces** el sistema retorna HTTP 400 indicando que el campo es obligatorio.

2. **Dado** una solicitud SALE con `totalAmount` igual o menor a cero, **Cuando** se ejecuta la validación, **Entonces** el sistema retorna HTTP 400 indicando que el monto debe ser mayor a cero.

3. **Dado** una solicitud SALE con `transactionType` diferente de `SALE`, **Cuando** se valida la solicitud, **Entonces** el sistema retorna HTTP 400 indicando que el tipo de transacción no es soportado.

4. **Dado** una solicitud SALE con formato inválido en `expirationDate`, **Cuando** se ejecuta la validación, **Entonces** el sistema retorna HTTP 400 indicando que el formato de la fecha de expiración es inválido.

5. **Dado** una solicitud SALE válida, **Cuando** finaliza exitosamente la validación, **Entonces** el sistema genera un `transactionId` único en formato UUID v4 de 36 caracteres antes de iniciar el procesamiento transaccional.

6. **Dado** una solicitud SALE con información inconsistente o que incumple reglas de negocio definidas, **Cuando** se ejecuta la validación, **Entonces** la solicitud es rechazada antes de invocar el servicio transaccional.

---

### Historia de Usuario 3 — Trazabilidad Completa y Auditoría (Prioridad: P2)

El sistema debe registrar y conservar la información necesaria para que cualquier transacción procesada pueda ser auditada, conciliada e investigada posteriormente mediante los mecanismos corporativos autorizados.

**Por qué esta prioridad**: La auditoría y trazabilidad son requisitos regulatorios. Auditoría interna, entes reguladores y comercios deben poder rastrear transacciones para investigar incidentes, conciliación y cumplimiento normativo.

**Prueba Independiente**: Puede completarse sin depender de P1:
1. El sistema procesa 10 transacciones SALE durante un período.
2. Cada transacción es almacenada con la información requerida para auditoría.
3. Las trazas registran el flujo completo de procesamiento.
4. Los logs estructurados permiten correlacionar todos los eventos asociados.
5. Es posible identificar transactionId, correlationId, autorizador utilizado, resultado y timestamps de procesamiento.

**Escenarios de Aceptación**:

1. **Dado** una transacción procesada con transactionId T1, **Cuando** se inspecciona la información persistida de la transacción, **Entonces** el documento contiene todos los campos de auditoría: transactionId, correlationId, merchantId, createdAt, updatedAt, processingDateTime, authorizationSource, authorizationNumber, responseCode, responseDescription, referenceNumber, hostDate, hostTime, status.

2. **Dado** OpenTelemetry configurado, **Cuando** se procesa una transacción SALE, **Entonces** se exportan trazas completas mostrando: ingreso en controller → validación en application → invocación a servicio transaccional → persistencia en MongoDB → respuesta HTTP.

3. **Dado** un correlationId único para una solicitud SALE, **Cuando** se revisan los logs estructurados, **Entonces** todos los eventos asociados (request, validación, invocación externa, persistencia, response) incluyen ese correlationId.


---

### Historia de Usuario 4 — Disponibilidad y Resiliencia (Prioridad: P3)

El sistema debe continuar operando de forma controlada ante fallos temporales o interrupciones parciales de dependencias externas (servicio transaccional, MongoDB), manteniendo la estabilidad y la trazabilidad de eventos.

**Por qué esta prioridad**: La resiliencia mejora la experiencia del comercio y reduce pérdida de ingresos, pero es menos crítica que el flujo principal si los mecanismos de retry y circuit breaker están configurados.

**Prueba Independiente**: Puede completarse mediante testing de resiliencia:
1. Servicio transaccional retorna 503 (unavailable).
2. El sistema reinventa con backoff exponencial (1s, 2s, 4s).
3. Tras 3 reintentos fallidos, retorna HTTP 503 al comercio.
4. El CircuitBreaker se abre (estado OPEN).
5. Solicitudes subsecuentes retornan HTTP 503 inmediatamente sin invocar el servicio.
6. El CircuitBreaker se cierra automáticamente tras waitDurationInOpenState (≥ 10 segundos).

**Escenarios de Aceptación**:

1. **Dado** que el servicio transaccional está timeout (respuesta > 5s), **Cuando** se recibe una solicitud SALE, **Entonces** el sistema reintenta automáticamente hasta 3 veces con backoff exponencial y jitter.

2. **Dado** que el servicio transaccional ha fallado 10 veces consecutivas, **Cuando** se recibe una nueva solicitud SALE, **Entonces** el CircuitBreaker está en estado OPEN y retorna HTTP 503 sin intentar invocar el servicio.

3. **Dado** que el CircuitBreaker está OPEN, **Cuando** han transcurrido más de 10 segundos, **Entonces** el CircuitBreaker pasa a HALF_OPEN e intenta invocar nuevamente el servicio.

4. **Dado** que MongoDB está temporalmente unavailable, **Cuando** se intenta persistir una transacción, **Entonces** el sistema reinventa la persistencia con backoff y registra el evento en logs estructurados.

---

### Casos Límite

- ¿Qué ocurre si un comercio envía transactionId duplicado pero con monto diferente? → Retornar HTTP 409 indicando conflicto de datos.
- ¿Cómo maneja el sistema un token JWT expirado? → Retornar HTTP 401 Unauthorized indicando token inválido o expirado.
- ¿Qué sucede si el servicio transaccional retorna un responseCode no reconocido? → Registrar el código en MongoDB, mapear a estado UNKNOWN, alertar en observabilidad.
- ¿Cómo se comporta el sistema con volumen muy alto (10,000 TPS)? → El sistema deberá mantener los niveles de servicio definidos y degradar de forma controlada en caso de alcanzar límites operativos.
- ¿Qué pasa si la base de datos MongoDB excede capacidad de escritura? → CircuitBreaker en MongoDB se abre; comercios reciben HTTP 503; eventos se registran en logs para reintento posterior.

---

## Requisitos *(obligatorio)*

### Requisitos Funcionales

- **RF-001**: El sistema DEBE recibir solicitudes de autorización SALE desde comercios comercios autenticados y autorizados y retornar respuestas estructuradas con resultado de autorización (AUTHORIZED, DECLINED, ERROR) en menos de 3 segundos (P95).

- **RF-002**: Toda solicitud SALE DEBE ser validada antes de iniciar su procesamiento. Como mínimo el sistema deberá validar:

- transactionType obligatorio y con valor SALE.
- terminalId obligatorio.
- entryMode obligatorio.
- totalAmount obligatorio y mayor o igual a cero.
- accountNumber obligatorio.
- expirationDate obligatoria y con formato válido.
- invoice obligatoria.
- securityCodeEntry obligatorio.
- securityValidationResponse obligatorio.
- securityCode obligatorio.
- binValidate no obligatorio.
- Integridad y consistencia de la información recibida.
- Cumplimiento de las reglas de negocio aplicables.

Adicionalmente:

- transactionId deberá generarse como identificador único de la transacción.
- transactionId deberá tener formato UUID v4.
- transactionId deberá contener exactamente 36 caracteres.
- transactionId no podrá reutilizarse para una transacción distinta.
- Las solicitudes que incumplan cualquiera de las validaciones deberán ser rechazadas antes de invocar el servicio transaccional.

Las solicitudes que incumplan cualquiera de estas validaciones deberán ser rechazadas antes de invocar el servicio transaccional.

- **RF-003**: El sistema DEBE generar automáticamente un transactionId único para cada transacción recibida antes de iniciar su procesamiento. El transactionId deberá tener formato UUID v4, una longitud de 36 caracteres y ser irrepetible durante todo el período de retención de la información. El transactionId deberá utilizarse como identificador único de la operación para fines de trazabilidad, auditoría, monitoreo y correlación de eventos asociados a la transacción.

- **RF-004**: El sistema DEBE enviar solicitudes de autorización a un servicio transaccional especializado, recibir respuestas de autorización financiera e identificar explícitamente el autorizador utilizado (AS400 o CYBERSOURCE).

- **RF-005**: Toda transacción DEBE ser registrada en MongoDB con información completa: transactionId (índice único), correlationId, merchantId, amount, currency, authorizationSource, authorizationNumber, responseCode, responseDescription, timestamps (createdAt, processingDateTime, hostTime, hostDate), status (PENDING, AUTHORIZED, DECLINED, ERROR).

- **RF-006**: El sistema DEBE retener datos de transacciones durante mínimo 18 meses (548 días), con eliminación automática mediante TTL index en MongoDB.

- **RF-007**: El sistema DEBE proporcionar trazabilidad de extremo a extremo mediante CorrelationId, OpenTelemetry, logs estructurados en JSON, permitiendo rastrear flujo completo: ingreso → validación → autorización → persistencia → respuesta.

- **RF-008**: El sistema DEBE conservar toda la información necesaria para soportar procesos de auditoría, conciliación financiera, monitoreo operativo e investigación de incidentes durante el período de retención definido.

- **RF-009**: El sistema DEBE continuar operando de forma controlada ante fallos temporales de dependencias externas, evitando la degradación total del servicio y preservando la trazabilidad de las operaciones afectadas.

- **RF-010**: El sistema DEBE permitir únicamente el acceso de consumidores autenticados y autorizados para procesar transacciones financieras.

- **RF-011**: Las solicitudes que no puedan ser autenticadas o autorizadas deberán ser rechazadas antes de iniciar cualquier procesamiento transaccional.

- **RF-012**: El sistema DEBE generar métricas operativas y de negocio que permitan monitorear el comportamiento de las transacciones SALE y visualizar indicadores de desempeño, disponibilidad, errores y tiempos de respuesta mediante las herramientas corporativas de observabilidad.


### Entidades Clave *(datos involucrados)*

- **Transacción SALE**: Representa una solicitud de autorización de pago enviada por un comercio. Contiene: transactionId (UUID v4 único), correlationId (UUID v4), merchantId, customerId (enmascarado), amount (BigDecimal), currency (ISO 4217), paymentInstrument (tokenizado), authorizationSource (AS400 | CYBERSOURCE), authorizationNumber, responseCode, responseDescription, status (PENDING | AUTHORIZED | DECLINED | ERROR), createdAt, processingDateTime, hostDate, hostTime, referenceNumber.

- **Respuesta de Autorización**: Información retornada por el servicio transaccional indicando resultado de la autorización. Contiene: authorizationNumber, responseCode, responseDescription, authorizationSource, hostDate, hostTime, referenceNumber, status (aprobado/rechazado/error).

- **Auditoría de Transacción**: Registro de eventos relevantes durante el ciclo de vida de una transacción. Incluye: correlationId, transactionId, timestamp, evento (RECEIVED, VALIDATED, SENT_TO_AUTHORIZER, AUTHORIZATION_RECEIVED, PERSISTED, RESPONSE_SENT), detalles, traceId, spanId.

---

## Criterios de Éxito *(obligatorio)*

### Resultados Medibles

- **CE-001**: Latencia P95 de transacciones SALE ≤ 3 segundos bajo condiciones normales (medido desde recepción de solicitud hasta entrega de respuesta al comercio).

- **CE-002**: Latencia P99 de transacciones SALE ≤ 5 segundos bajo condiciones normales.

- **CE-003**: Disponibilidad mensual del sistema ≥ 99.9% (máximo 43.2 minutos de downtime por mes).

- **CE-004**: Tasa de error < 1% (menos de 1 error por cada 100 transacciones procesadas).

- **CE-005**: Cero transacciones duplicadas procesadas: every transactionId retorna resultado consistente en reintentos dentro de ventana de retención.

- **CE-006**: Cero pérdida de datos de transacciones: 100% de transacciones recibidas se persisten en MongoDB dentro de ventana de retención de 18 meses.

- **CE-007**: Trazabilidad completa de 100% de transacciones: cada transacción procesada es rastreable mediante correlationId desde ingreso hasta respuesta final, visible en logs estructurados y OpenTelemetry.

- **CE-008**: Seguridad: 100% de endpoints protegidos con Entra ID JWT, 0 secretos detectados en código, 0 vulnerabilidades CRÍTICAS en OWASP Dependency Check.

- **CE-009**: Calidad de código: SonarQube Quality Gate A, cobertura de pruebas ≥ 80% en Domain y Application, complejidad ciclomática ≤ 10 por método.

- **CE-010**: Documentación API: 100% de endpoints documentados en OpenAPI 3.x + Swagger UI, sin diferencias entre contrato y implementación.

- **CE-011**: El 100% de las métricas operativas requeridas se encuentran disponibles en Grafana y permiten monitorear latencia, disponibilidad, throughput, tasa de errores y estado de las dependencias críticas.

---

## Supuestos

- **Supuesto 1**: Los comercios ya están registrados en el sistema y cuentan con credenciales válidas de Entra ID. La autenticación de comercios la realiza Microsoft Entra ID, no es responsabilidad de esta API.

- **Supuesto 2**: La información de pago (número de tarjeta, cuenta bancaria) llega tokenizada al API. El API nunca maneja datos de pago sensibles sin tokenizar; esta responsabilidad la tiene el comercio o un servicio PCI-compliant anterior.

- **Supuesto 3**: El servicio transaccional ya existe y expone un endpoint REST documentado para procesamiento de autorizaciones. Esta API se integra con él como consumidor.

- **Supuesto 4**: MongoDB Atlas o Azure Cosmos DB para MongoDB API ya está provisionado y disponible. La API solo consume; no la crea.

- **Supuesto 5**: Azure Key Vault ya contiene secretos de integración (credenciales de servicio transaccional, configuración). Managed Identity está configurado.

- **Supuesto 6**: Azure Monitor y Application Insights ya están provisioned. OpenTelemetry exporta automáticamente a estos servicios.

- **Supuesto 7**: Los SLA definidos (latencia P95 < 3s, disponibilidad ≥ 99.9%) son alcanzables con la arquitectura propuesta (Clean Architecture, Resilience4j, Azure App Service, MongoDB).

- **Supuesto 8**: El volumen de transacciones esperado está dentro del rango de capacidad de Azure App Service (replicating horizontalmente según demanda) y MongoDB Atlas/Cosmos DB.

- **Supuesto 9**: La ventana de retención de 18 meses cumple requisitos regulatorios de auditoría y conciliación para transacciones financieras en la jurisdicción operativa.

- **Supuesto 10**: CorrelationId se propaga por el consumidor (comercio) en cada solicitud o se genera automáticamente por el API. OpenTelemetry lo registra en todas las operaciones subsecuentes.

- **Supuesto 11**: Si el consumidor no envía el encabezado X-Correlation-Id, la API generará automáticamente un CorrelationId y lo propagará durante todo el flujo de procesamiento, incluyendo logs, trazas y llamadas al API Switch.

---

**Versión**: 1.0.0  
**Estado**: Borrador  
**Fecha de Creación**: 2026-07-22
