# Investigación: API Financiera de Transacciones SALE

**Feature**: `001-api-transacciones-sale`  
**Fecha**: 2026-07-22  
**Propósito**: Resolver todas las decisiones técnicas necesarias antes de iniciar el diseño de Fase 1

---

## 1. Arquitectura: Clean Architecture con Spring Boot 3 y Java 21

### Decisión
Implementar Clean Architecture con cuatro capas (`domain`, `application`, `infrastructure`, `presentation`) usando el patrón Ports and Adapters. Los casos de uso se implementan como clases con un único método público `execute()`.

### Rationale
- Spring Boot 3 con Java 21 permite implementar Clean Architecture sin frameworks de terceros adicionales.
- Java 21 `record` es ideal para Value Objects (inmutables, `equals/hashCode` automáticos).
- Java 21 `sealed interfaces` para modelar resultados de casos de uso (éxito/fallo sin excepciones de control de flujo).
- Spring Boot 3 anota solo en `infrastructure` y `presentation`; `domain` y `application` son POJO puros.
- ArchUnit verifica las reglas de dependencia en cada ejecución de CI.

### Alternativas Consideradas
- **Hexagonal Architecture explícita**: Esencialmente equivalente a Ports and Adapters — elegido como sinónimo.
- **DDD completo con Aggregates**: Complejidad adicional innecesaria para el alcance de esta API. Un solo Aggregate (`SaleTransaction`) es suficiente.
- **Arquitectura en capas clásica (N-Tier)**: Rechazado por acoplamiento entre capas y violación de Principio I de la Constitución.

### Hallazgos Clave
- `@SpringBootApplication` solo en clase principal dentro de `infrastructure` o raíz.
- Inyección de dependencias de Application → Infrastructure vía interfaces: Spring IoC resuelve la inversión sin anotaciones en capas internas.
- Lombok `@Value` y `@Builder` en DTOs de `infrastructure` y `presentation`; `record` en `domain`.
- MapStruct para mappers entre capas sin código boilerplate.

---

## 2. Resilience4j 2.x: Configuración para Dependencias Externas

### Decisión
Aplicar los cuatro patrones de Resilience4j en cada adaptador de dependencia externa:

| Adaptador | CircuitBreaker | Retry | Bulkhead | TimeLimiter |
|-----------|----------------|-------|----------|-------------|
| `SwitchApiAdapter` (API Switch) | ✅ | ✅ (máx 3, backoff exp) | ✅ (pool separado) | ✅ (5s) |
| `MongoTransactionAdapter` | ✅ | ✅ (máx 2, backoff exp) | ✅ (pool separado) | ✅ (3s) |

### Rationale
- CircuitBreaker evita cascading failures cuando el API Switch o MongoDB tienen fallos sostenidos.
- Retry con backoff exponencial y jitter maneja fallos transitorios sin sobrecargar el servicio destino.
- Bulkhead (SemaphoreBulkhead o ThreadPoolBulkhead) aísla el pool de hilos por dependencia.
- TimeLimiter garantiza que el SLA de P95 < 3s no se vea comprometido por dependencias lentas.

### Configuración de Referencia
```yaml
resilience4j:
  circuitbreaker:
    instances:
      switchApi:
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 5
        slidingWindowSize: 10
      mongoDb:
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  retry:
    instances:
      switchApi:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        enableRandomizedWait: true
      mongoDb:
        maxAttempts: 2
        waitDuration: 200ms
  timelimiter:
    instances:
      switchApi:
        timeoutDuration: 5s
      mongoDb:
        timeoutDuration: 3s
  bulkhead:
    instances:
      switchApi:
        maxConcurrentCalls: 25
      mongoDb:
        maxConcurrentCalls: 50
```

### Alternativas Consideradas
- **Hystrix (Netflix)**: Obsoleto, en modo mantenimiento. Rechazado.
- **Solo Retry sin CircuitBreaker**: Insuficiente — sin CircuitBreaker, reintentos siguen fallando y deterioran el servicio.

---

## 3. OAuth 2.0 Client Credentials para Autenticación con API Switch

### Decisión
Implementar `SwitchOAuthAdapter` que obtiene tokens mediante `grant_type=client_credentials`, los cachea en memoria mientras sean válidos (con margen de 30 segundos antes de expiración) y los renueva automáticamente de forma transparente para `SwitchApiAdapter`.

### Rationale
- `client_credentials` es el flujo estándar para comunicación service-to-service sin usuario.
- La caché en memoria evita una llamada de autenticación en cada solicitud de autorización, reduciendo latencia.
- Las credenciales (client_id, client_secret, token_endpoint) se almacenan en Azure Key Vault y se leen vía Managed Identity al inicio o bajo demanda.
- El diseño permite reemplazar la implementación de `SwitchAuthenticationPort` sin afectar `SwitchApiAdapter` (Principio de Inversión de Dependencias).

### Implementación de Referencia
```java
// SwitchAuthenticationPort (Application layer)
public interface SwitchAuthenticationPort {
    String getAccessToken();
}

// SwitchOAuthAdapter (Infrastructure layer)
@Component
public class SwitchOAuthAdapter implements SwitchAuthenticationPort {
    private volatile TokenCache tokenCache = TokenCache.empty();
    
    @Override
    public String getAccessToken() {
        if (tokenCache.isExpired()) {
            tokenCache = refreshToken();
        }
        return tokenCache.accessToken();
    }
}
```

### Alternativas Consideradas
- **Spring Security OAuth2 Client**: Adecuado, pero agrega complejidad de configuración para el caso service-to-service. `SwitchOAuthAdapter` es más transparente y testeable.
- **API Key estática**: Menos segura, no soporta rotación automática. Rechazado.

---

## 4. OpenTelemetry con Azure Application Insights

### Decisión
Usar el agente Java de OpenTelemetry (opentelemetry-javaagent) con el exportador de Azure Application Insights. Propagar `X-Correlation-Id` manualmente mediante `MDC` (Logback) y propagación de contexto en cliente HTTP.

### Rationale
- OpenTelemetry Java agent instrumenta automáticamente Spring Boot, MongoDB driver, HTTP clients y Resilience4j sin código adicional.
- Azure Application Insights acepta trazas OTLP directamente — no se requiere collector intermediario en el despliegue básico.
- El `X-Correlation-Id` se extrae en un filtro de Servlet, se almacena en `MDC` y en el contexto de OpenTelemetry como atributo de span.
- Grafana se conecta a Azure Monitor como datasource para dashboards operativos.

### Campos Obligatorios en Logs
```json
{
  "timestamp": "2026-07-22T10:30:00.123Z",
  "level": "INFO",
  "service": "sale-api",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "transactionId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "message": "Transaction authorized successfully"
}
```

### Alternativas Consideradas
- **Micrometer Tracing**: Compatible con Spring Boot 3; sin embargo, OpenTelemetry ofrece mayor interoperabilidad con Azure Monitor y es el estándar corporativo.
- **Sleuth (Spring Cloud Sleuth)**: Obsoleto en Spring Boot 3. Rechazado.

---

## 5. MongoDB: TTL Index, Unique Index y Transacciones ACID

### Decisión
- **Índice único**: `{ transactionId: 1 }` con `unique: true`. Rechaza duplicados a nivel de base de datos.
- **TTL Index**: `{ createdAt: 1 }` con `expireAfterSeconds: 47347200` (548 días). Eliminación automática post-retención.
- **Transacciones multi-documento**: Usar sesiones MongoDB para actualizar `SaleTransactionDocument` y `AuditDocument` atómicamente.
- **TLS**: Obligatorio en todos los entornos (Atlas y Cosmos DB incluyen TLS por defecto).

### Rationale
- El índice único en `transactionId` garantiza idempotencia a nivel de base de datos como segunda línea de defensa (la primera es la validación en Application layer).
- TTL Index garantiza cumplimiento de la retención de 18 meses sin procesos batch adicionales.
- MongoDB 6.x+ soporta transacciones ACID multi-documento en replica sets (Atlas y Cosmos DB lo soportan).

### Creación de Índices (al inicio de la aplicación o en migración)
```javascript
db.sale_transactions.createIndex(
  { "transactionId": 1 },
  { unique: true, name: "idx_transactionId_unique" }
);
db.sale_transactions.createIndex(
  { "createdAt": 1 },
  { expireAfterSeconds: 47347200, name: "idx_createdAt_ttl" }
);
db.sale_transactions.createIndex(
  { "merchantId": 1, "createdAt": -1 },
  { name: "idx_merchantId_createdAt" }
);
```

### Alternativas Consideradas
- **PostgreSQL**: Rechazado — MongoDB es el sistema de registro oficial según Principio VI de la Constitución.
- **Soft delete manual**: Rechazado — TTL index es más confiable y no requiere proceso batch.

---

## 6. Spring Security 6 con Microsoft Entra ID JWT Bearer

### Decisión
Usar `spring-security-oauth2-resource-server` con validación JWT delegada al JWKS endpoint de Microsoft Entra ID. Configurar `SecurityFilterChain` para validar `issuer`, `audience`, `expiration` y `signature` en cada request.

### Rationale
- Spring Security 6 tiene soporte nativo para JWT Bearer en Resource Server.
- Microsoft Entra ID expone endpoint JWKS estándar (`/.well-known/openid-configuration`) para validación de firmas.
- `@PreAuthorize` protege casos de uso sensibles a nivel de método.
- Los endpoints son stateless (sin sesión HTTP); JWT es la única fuente de autenticación.

### Configuración de Referencia
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/{tenant-id}/v2.0
          audiences: api://{client-id}
```

### Alternativas Consideradas
- **Validación manual de JWT**: Más complejidad, más riesgo de errores de seguridad. Rechazado.
- **API Key**: Inadecuada para autenticación empresarial. No cumple Principio III de la Constitución. Rechazado.

---

## 7. Generación de transactionId UUID v4

### Decisión
El `transactionId` es generado automáticamente por el sistema mediante `GenerateTransactionIdUseCase` antes de iniciar el procesamiento. El comercio NO envía `transactionId` en la solicitud; el sistema lo genera y lo retorna en la respuesta.

### Rationale
- Centralizar la generación en el sistema elimina dependencia del comercio para generarlo correctamente.
- UUID v4 de 36 caracteres garantiza unicidad global sin coordinación.
- El índice único en MongoDB es la segunda línea de defensa para garantizar no-duplicidad.
- `UUID.randomUUID().toString()` en Java es criptográficamente seguro (SecureRandom).

### Alternativas Consideradas
- **transactionId enviado por el comercio**: Requiere validar formato UUID v4; mayor superficie de error. La Constitución indica que puede ser generado por el sistema.
- **ULID / NanoID**: Alternativas válidas para ordenamiento, pero UUID v4 es estándar más ampliamente adoptado.

---

## Resumen de Decisiones

| Área | Decisión | Alternativas Rechazadas |
|------|----------|------------------------|
| Arquitectura | Clean Architecture + Ports and Adapters | DDD completo, N-Tier clásico |
| Resiliencia | Resilience4j 2.x (CB + Retry + Bulkhead + TL) | Hystrix (obsoleto), Solo Retry |
| Auth Switch | OAuth 2.0 Client Credentials + caché en memoria | API Key estática, Spring Security OAuth2 Client |
| Observabilidad | OpenTelemetry + Azure Application Insights | Micrometer Tracing, Sleuth |
| Persistencia | MongoDB TTL + Unique Index + Transacciones ACID | PostgreSQL, Soft delete batch |
| Seguridad API | Spring Security 6 + Entra ID JWT JWKS | JWT manual, API Key |
| transactionId | UUID v4 generado por sistema | Enviado por comercio, ULID |

**Estado**: Todas las decisiones resueltas — sin `NEEDS CLARIFICATION` pendientes. Listo para Fase 1.
