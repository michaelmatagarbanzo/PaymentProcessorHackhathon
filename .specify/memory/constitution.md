<!--
INFORME DE SINCRONIZACIÓN
==========================
Cambio de versión: [PLANTILLA] → 1.0.0 (ratificación inicial)

Principios modificados:
  - N/A (constitución nueva)

Secciones añadidas:
  - Principios Fundamentales (I al XII)
  - Stack Tecnológico
  - Compuertas de Calidad y Controles de Seguridad
  - Gobernanza

Plantillas que requieren actualización:
  - .specify/templates/plan-template.md ✅ alineada
  - .specify/templates/spec-template.md ✅ alineada
  - .specify/templates/tasks-template.md ✅ alineada

TODOs pendientes:
  - Ninguno. Todos los marcadores de posición resueltos.
-->

# Constitución del Proyecto — API eCommerce SALE

**Versión:** 1.0.0  
**Estado:** Activa  
**Aplicabilidad:** Obligatoria para todo el repositorio y sus componentes  
**Ámbito:** Especificaciones, análisis, diseño, desarrollo, pruebas, observabilidad, seguridad, despliegue y mantenimiento

> **Dominio:** Transacciones Financieras – Operaciones SALE  
> **Plataforma:** Java 21 · Spring Boot 3 · MongoDB · Azure (Entra ID · Key Vault · Managed Identity)
> **Infraestructura:** Azure App Service como plataforma estándar para todos los componentes desplegables.
> **Autorizadores:** AS400 · Cybersource  
> **Justificación:** Cada transacción procesada por este sistema involucra dinero real. La corrección, seguridad, trazabilidad y resiliencia son innegociables. Esta constitución rige todas las decisiones de diseño, desarrollo y operación del servicio.

---

# Propósito

Esta Constitución define los principios fundamentales, normas técnicas y criterios verificables que rigen el ciclo de vida completo de la API eCommerce SALE.

Toda especificación, arquitectura, diseño técnico, historia de usuario, tarea, prueba, código fuente, pipeline y despliegue deberá cumplir obligatoriamente con los lineamientos establecidos en este documento.

Debido a que la plataforma procesa transacciones financieras, todas las decisiones deberán priorizar:

- Seguridad
- Resiliencia
- Mantenibilidad
- Auditabilidad
- Trazabilidad
- Integridad transaccional

En caso de conflicto entre una decisión técnica y esta Constitución, prevalece la Constitución.

---

# Principio I — Arquitectura Limpia (NO NEGOCIABLE)

## Declaración

El sistema deberá implementarse siguiendo los principios de Clean Architecture.

## Reglas

- El dominio representa el núcleo del negocio.
- El dominio no conoce frameworks.
- El dominio no conoce bases de datos.
- El dominio no conoce servicios externos.
- Las dependencias fluyen hacia el dominio.
- Se utilizará el patrón Ports and Adapters.
- Toda integración externa deberá implementarse mediante adaptadores.
- Ninguna capa podrá acceder directamente a una capa más interna sin respetar las dependencias definidas.

## Estructura mínima esperada

```text
src/
├── domain/
├── application/
├── infrastructure/
└── presentation/
```

## Permitido

- Anotaciones Spring Boot únicamente en Infrastructure y Presentation.
- DTOs y mappers de traducción entre capas.
- Eventos de dominio publicados mediante interfaces.

## Prohibido

- `@Repository` dentro de Domain.
- `@Service` dentro de Domain.
- Dependencias circulares.
- Llamadas directas desde Presentation hacia Infrastructure.

## Criterios Verificables

- ArchUnit valida dependencias arquitectónicas.
- Domain no importa Infrastructure.
- Application no importa Infrastructure.
- Cada caso de uso contiene un único punto de entrada (`execute`).

---

# Principio II — SOLID y Clean Code (NO NEGOCIABLE)

## Declaración

Todo código de producción deberá cumplir principios SOLID y estándares de Clean Code.

## Reglas

- Single Responsibility Principle.
- Open/Closed Principle.
- Liskov Substitution Principle.
- Interface Segregation Principle.
- Dependency Inversion Principle.
- Métodos ≤ 20 líneas recomendadas.
- Clases ≤ 200 líneas recomendadas.
- Sin números mágicos.
- Sin cadenas hardcodeadas.

## Permitido

- Java Records.
- Lombok para DTOs inmutables.
- Patrones de diseño cuando agreguen valor.

## Prohibido

- Clases Dios.
- Retornos `null`.
- Código duplicado.
- Utilitarios gigantes.

## Criterios Verificables

- SonarQube Rating A.
- Complejidad ciclomática ≤ 10.
- Duplicación < 3%.
- 0 Code Smells críticos.

---

# Principio III — Seguridad Empresarial (NO NEGOCIABLE)

## Declaración

Toda operación financiera deberá ejecutarse bajo un modelo Zero Trust.

## Reglas

### Autenticación

- Todos los endpoints deberán requerir JWT emitido por Microsoft Entra ID.
- Validar:
  - issuer
  - audience
  - expiration
  - signature

### Gestión de Identidades

- Utilizar Managed Identity.
- No se permiten credenciales almacenadas.

### Gestión de Secretos

- Utilizar exclusivamente Azure Key Vault.
- Los secretos nunca deberán persistirse en código.

### Protección de Datos

- Enmascarar PAN.
- Enmascarar PII.
- Nunca registrar CVV.

## Permitido

- OAuth2.
- OpenID Connect.
- Spring Security.

## Prohibido

- Secrets hardcodeados.
- HTTP sin TLS.
- JWT completos en logs.
- Números de tarjeta en logs.

## Criterios Verificables

- 100% secretos en Key Vault.
- 100% APIs protegidas con Entra ID.
- 0 secretos detectados por escáner.

---

# Principio IV — Validación Integral de Transacciones (NO NEGOCIABLE)

## Declaración

Toda transacción SALE deberá ser validada antes de iniciar cualquier proceso de autorización.

## Reglas

Validar obligatoriamente:

- transactionId
- merchantId
- amount
- currency
- timestamp
- correlationId
- información de pago tokenizada

Validar además:

- Formato
- Campos obligatorios
- Integridad de datos
- Duplicidad
- Reglas de negocio

## Permitido

- Bean Validation.
- Validaciones desacopladas.
- Problem Details (RFC 9457).

## Prohibido

- Procesar solicitudes inválidas.
- Omitir validaciones de negocio.

## Criterios Verificables

- 100% solicitudes validadas.
- Casos de prueba para escenarios límite.

---

# Principio V — Identidad Transaccional e Idempotencia (NO NEGOCIABLE)

## Declaración

Toda transacción deberá poseer una identidad única e irrepetible.

## Reglas

- transactionId obligatorio.
- UUID v4 obligatorio.
- Idempotencia obligatoria.
- Solicitudes duplicadas retornan el mismo resultado.

## Permitido

- Persistencia de claves de idempotencia.
- Respuesta cacheada para reintentos.

## Prohibido

- Cobros duplicados.
- Reprocesar transacciones finalizadas.

## Criterios Verificables

- Índice único sobre transactionId.
- Cero transacciones duplicadas.

---

# Principio VI — Persistencia y Retención de Datos (NO NEGOCIABLE)

## Declaración

MongoDB es el sistema oficial de registro de transacciones.

## Reglas

Toda transacción deberá almacenar:

- transactionId
- correlationId
- merchantId
- amount
- currency
- authorizationSource
- authorizationNumber
- responseCode
- responseDescription
- hostDate
- hostTime
- processingDateTime
- status

### Retención

- Período mínimo: 18 meses.
- TTL: 548 días.

## Permitido

- MongoDB Replica Sets.
- MongoDB Atlas.
- Azure Cosmos DB API MongoDB.

## Prohibido

- Eliminaciones prematuras.
- Datos sensibles sin enmascarar.
- MongoDB sin TLS.

## Criterios Verificables

- Índice TTL configurado.
- Índice único para transactionId.
- Datos disponibles durante 18 meses.

---

# Principio VII — Integración con Servicio Transaccional y Trazabilidad de Autorización (NO NEGOCIABLE)

## Declaración

La API eCommerce SALE no realizará directamente procesos de autorización financiera.

Toda autorización deberá ser delegada a un servicio transaccional externo especializado responsable de procesar la solicitud, determinar la estrategia de enrutamiento y ejecutar la autorización financiera correspondiente.

## Reglas

La integración deberá realizarse mediante APIs seguras y desacopladas.

La API eCommerce SALE será responsable únicamente de:

- Validar la solicitud.
- Gestionar la identidad transaccional.
- Aplicar controles de idempotencia.
- Invocar al servicio transaccional.
- Registrar la respuesta recibida.
- Mantener la trazabilidad de la autorización.

El servicio transaccional será responsable de:

- Procesar la autorización financiera.
- Determinar la ruta de autorización.
- Gestionar mecanismos de failover.
- Comunicarse con los autorizadores finales.
- Retornar el resultado de autorización.

Como mínimo deberán existir capacidades para autorizar mediante:

- AS400
- Cybersource

La lógica de selección del autorizador no deberá existir dentro de la API eCommerce SALE.

## Trazabilidad del Origen de Autorización

Toda respuesta de autorización deberá indicar explícitamente el origen de procesamiento de la transacción.

Como mínimo deberán soportarse los siguientes valores:

```json
{
  "authorizationSource": "AS400"
}
```

```json
{
  "authorizationSource": "CYBERSOURCE"
}
```

## Información Obligatoria de Autorización

Toda respuesta recibida del servicio transaccional deberá registrar como mínimo:

- authorizationSource
- authorizationNumber
- responseCode
- responseDescription
- referenceNumber
- hostDate
- hostTime
- processingDateTime

## Permitido

- Integración mediante APIs REST seguras.
- Adaptadores especializados para el servicio transaccional.
- Failover administrado por el servicio transaccional.
- Registro del origen de autorización.
- Estrategias de resiliencia para la comunicación con el servicio transaccional.

## Prohibido

- Implementar lógica de autorización financiera dentro de la API eCommerce SALE.
- Invocar directamente AS400 desde la lógica de negocio.
- Invocar directamente Cybersource desde la lógica de negocio.
- Duplicar lógica de enrutamiento existente en el servicio transaccional.
- Perder trazabilidad del origen de autorización.
- Procesar transacciones sin registrar el autorizador utilizado.

## Criterios Verificables

- El 100% de las solicitudes de autorización son enviadas al servicio transaccional.
- El 100% de las transacciones almacenan authorizationSource.
- El 100% de las transacciones almacenan los datos de autorización requeridos.
- La API eCommerce SALE no contiene lógica de selección de autorizadores.
- Es posible identificar para cualquier transacción:
  - Quién la procesó.
  - Cuándo fue procesada.
  - Qué respuesta fue recibida.
  - Qué autorizador fue utilizado.
  
# Principio VIII — Resiliencia Operativa (NO NEGOCIABLE)

## Declaración

La plataforma deberá tolerar fallas parciales sin comprometer la estabilidad del servicio.

## Reglas

Resilience4j será obligatorio para:

- Circuit Breaker
- Retry
- Bulkhead
- Rate Limiter
- Time Limiter

## Permitido

- Fallback controlado.
- Reintentos exponenciales.
- Degradación controlada.

## Prohibido

- Reintentos infinitos.
- Integraciones sin protección.

## Criterios Verificables

- 100% dependencias protegidas.
- Dashboards operativos.
- Configuraciones documentadas.

---

# Principio IX — Observabilidad, Calidad y Testing (NO NEGOCIABLE)

## Declaración

Toda transacción deberá ser observable y todo cambio deberá estar respaldado por pruebas automatizadas.

## Reglas de Observabilidad

Implementar:

Implementar:

- OpenTelemetry
- Azure Application Insights
- Azure Monitor
- CorrelationId
- Distributed Tracing
- Logs Estructurados
- Grafana

Todo flujo deberá propagar:

```text
X-Correlation-Id
```

### Métricas Operativas

Los dashboards de Grafana deberán mostrar como mínimo:

- Latencia P50
- Latencia P95
- Latencia P99
- Disponibilidad
- Tasa de errores
- Throughput transaccional
- Estado de Circuit Breakers
- Tiempos de respuesta de autorizadores

#### Logs Obligatorios

- timestamp
- level
- correlationId
- transactionId
- traceId
- spanId

## Reglas de Calidad

Implementar:

- Unit Testing
- Integration Testing
- Contract Testing
- End-to-End Testing

Cobertura mínima:

- Domain ≥ 90%
- Application ≥ 85%
- Global ≥ 80%


## Seguridad Aplicativa

Cumplimiento obligatorio:

- OWASP Top 10
- SonarQube
- Dependency Check
- SAST
- DAST

## Permitido

- TestContainers
- WireMock
- Pact
- Spring Cloud Contract

## Prohibido

- Merge con pruebas fallidas.
- Vulnerabilidades críticas.
- Reducción de cobertura.

## Criterios Verificables

- Sonar Quality Gate aprobado.
- Cobertura ≥ 80%.
- OWASP sin vulnerabilidades críticas.
- Trazabilidad completa de extremo a extremo.

---

# Principio X — Acuerdos de Nivel de Servicio (SLA) y Rendimiento (NO NEGOCIABLE)

## Declaración

La plataforma deberá garantizar niveles de desempeño compatibles con sistemas financieros transaccionales, proporcionando tiempos de respuesta consistentes, predecibles y monitoreables para las operaciones SALE.

## Reglas

- El tiempo de respuesta de una transacción SALE deberá ser inferior a **3 segundos** para el percentil 95 (P95) bajo condiciones normales de operación.
- La medición del tiempo de respuesta deberá realizarse desde la recepción completa de la solicitud hasta la entrega de la respuesta al consumidor.
- El tiempo consumido por servicios externos, autorizadores financieros y componentes internos formará parte de la medición total del SLA.
- Todas las dependencias externas deberán contar con configuraciones de timeout alineadas con el cumplimiento del SLA.
- Los procesos síncronos no deberán introducir latencias innecesarias que comprometan los objetivos de servicio.
- Cualquier degradación del servicio que impacte los objetivos definidos deberá generar alertas automáticas y mecanismos de escalación operativa.

## Objetivos de Servicio (SLO)

| Métrica | Objetivo |
|----------|----------|
| Latencia P95 | < 3 segundos |
| Latencia P99 | < 5 segundos |
| Disponibilidad mensual | ≥ 99.9% |
| Tasa de error | < 1% |

## Permitido

- Escalamiento horizontal.
- Balanceo de carga.
- Caché para datos de configuración.
- Optimización de consultas.
- Estrategias de resiliencia y recuperación automática.
- Ajustes de capacidad orientados al cumplimiento del SLA.

## Prohibido

- Dependencias externas sin timeout configurado.
- Operaciones bloqueantes innecesarias dentro del flujo transaccional.
- Liberaciones que degraden el rendimiento por debajo de los objetivos establecidos.
- Procesos síncronos que comprometan el tiempo máximo de respuesta definido.

## Criterios Verificables

- OpenTelemetry deberá registrar la latencia de extremo a extremo de todas las transacciones.
- Grafana deberá mostrar métricas P50, P95, P99, disponibilidad y tasa de errores.
- Se deberán configurar alertas automáticas cuando el P95 supere los 3 segundos durante más de 5 minutos consecutivos.
- Las pruebas de rendimiento deberán validar el cumplimiento del SLA antes de cada liberación a producción.
- Los reportes operativos deberán evidenciar el cumplimiento mensual de los indicadores definidos.

## Impacto en las Liberaciones

Ninguna versión podrá ser promovida a producción si:

- El P95 excede los 3 segundos.
- La disponibilidad esperada es inferior al 99.9%.
- Las pruebas de carga evidencian degradación significativa del servicio.
- Existen incidentes abiertos relacionados con rendimiento crítico no mitigados.

---

# Principio XI — Documentación y Contratos API (NO NEGOCIABLE)

## Declaración

Toda funcionalidad expuesta por la API eCommerce SALE deberá estar documentada mediante OpenAPI 3.x y publicada mediante Swagger UI para garantizar gobernanza, trazabilidad, consistencia contractual e integración segura con consumidores internos y externos.

## Reglas

- Toda API REST deberá contar con una especificación OpenAPI 3.x actualizada.
- La especificación deberá generarse automáticamente desde el código fuente cuando sea posible.
- Toda modificación de request o response deberá reflejarse en el contrato OpenAPI.
- La documentación deberá incluir:
  - Descripción funcional.
  - Parámetros de entrada.
  - Headers requeridos.
  - Esquemas de request.
  - Esquemas de response.
  - Ejemplos funcionales.
  - Códigos de error.
  - Requisitos de autenticación.
- Todos los endpoints deberán documentar los mecanismos de seguridad de Microsoft Entra ID.
- Todos los endpoints deberán documentar el uso de CorrelationId.
- Los cambios incompatibles deberán gestionarse mediante versionado de API.

## Seguridad Documentada

La especificación OpenAPI deberá incluir:

- OAuth 2.0.
- OpenID Connect.
- JWT Bearer Authentication.
- Scopes requeridos.
- Roles requeridos.
- X-Correlation-Id.

## Ejemplo de Seguridad

```yaml
security:
  - bearerAuth: []

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

## Ejemplo de CorrelationId

```yaml
parameters:
  - name: X-Correlation-Id
    in: header
    required: true
    schema:
      type: string
      format: uuid
```

## Permitido

- OpenAPI 3.x.
- Swagger UI.
- SpringDoc OpenAPI.
- Generación automática de contratos.
- Portales corporativos de APIs.

## Prohibido

- Endpoints sin documentación.
- Contratos desactualizados.
- DTOs expuestos sin documentación.
- Cambios de API sin actualizar Swagger.
- Despliegues con diferencias entre implementación y documentación.

## Criterios Verificables

- 100% de endpoints documentados en Swagger UI.
- 100% de requests y responses documentados.
- OpenAPI generado exitosamente durante CI/CD.
- Validación automática del contrato en cada Pull Request.
- Sin diferencias entre el contrato OpenAPI y la implementación desplegada.

---
# Principio XII — Plataforma Cloud Corporativa (NO NEGOCIABLE)

## Declaración

Toda solución desarrollada para la API eCommerce SALE deberá ejecutarse sobre servicios administrados de Microsoft Azure, priorizando simplicidad operativa, seguridad, escalabilidad, disponibilidad, observabilidad y eficiencia en costos.

## Reglas

- Todos los componentes desplegables deberán ejecutarse sobre Azure App Service.
- Los ambientes de Desarrollo, QA, UAT y Producción deberán utilizar Azure App Service como plataforma oficial de ejecución.
- La configuración de infraestructura deberá gestionarse mediante Infrastructure as Code.
- Todas las aplicaciones deberán integrarse con Azure Monitor y Azure Application Insights.
- Todas las aplicaciones deberán exponer telemetría mediante OpenTelemetry.
- La autenticación entre servicios deberá utilizar Managed Identity siempre que sea posible.
- Todos los secretos deberán obtenerse desde Azure Key Vault.
- Los despliegues deberán realizarse exclusivamente mediante pipelines automatizados.
- La configuración de ambientes deberá externalizarse y administrarse mediante configuraciones seguras.
- Todo recurso de Azure deberá cumplir los estándares corporativos de seguridad y gobierno.

## Permitido

- Azure App Service.
- Azure Key Vault.
- Azure Monitor.
- Azure Application Insights.
- Azure Front Door.
- Azure API Management.
- Azure Service Bus.
- Azure Storage.
- Managed Identity.
- Infrastructure as Code.

## Prohibido

- Máquinas virtuales administradas manualmente.
- Secretos almacenados localmente.
- Configuración sensible dentro del código fuente.
- Despliegues manuales en Producción.
- Infraestructura fuera de Azure sin aprobación formal de Arquitectura.
- Credenciales compartidas entre aplicaciones.

## Criterios Verificables

- El 100% de las aplicaciones se ejecutan sobre Azure App Service.
- El 100% de los secretos provienen de Azure Key Vault.
- El 100% de las aplicaciones tienen Azure Application Insights configurado.
- El 100% de los despliegues se realizan mediante pipelines automatizados.
- El 100% de los servicios utilizan observabilidad corporativa de Azure.
---

# Stack Tecnológico

| Área | Tecnología |
|--------|------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.x |
| Hosting | Azure App Service |
| Seguridad | Spring Security 6 |
| Persistencia | MongoDB |
| Identidad | Microsoft Entra ID |
| Secretos | Azure Key Vault |
| Autenticación Servicio a Servicio | Managed Identity |
| Resiliencia | Resilience4j |
| Observabilidad | OpenTelemetry |
| Monitoreo | Grafana |
| Monitoreo Cloud | Azure Monitor |
| Telemetría | Azure Application Insights |
| Testing | JUnit 5, Mockito, TestContainers |
| Calidad | SonarQube |
| Seguridad Aplicativa | OWASP Dependency Check |
| Build | Maven |
| Documentación API | OpenAPI 3.x + Swagger UI (SpringDoc OpenAPI) |

---

# Compuertas de Calidad y Seguridad

Todo Pull Request deberá aprobar obligatoriamente:

1. Compilación exitosa.
2. Pruebas unitarias.
3. Pruebas de integración.
4. SonarQube Quality Gate.
5. OWASP Dependency Check.
6. Escaneo de secretos.
7. Validaciones ArchUnit.
8. Pruebas de contrato.
9. Validación OpenAPI / Swagger.
   
    - Generación exitosa del contrato OpenAPI.
    - Sin errores de validación del esquema.
    - 100% de endpoints documentados.
    - Swagger UI accesible en ambientes autorizados.
    - Cambios de contratos revisados y aprobados antes del merge.
10. Validación de SLA
   
    - Las pruebas de rendimiento deberán validar un tiempo de respuesta P95 menor a 3 segundos.
    - Los resultados deberán documentarse antes de cada liberación a producción.
    - No podrá aprobarse una liberación que incumpla el SLA definido.
11. Validación de Plataforma Azure

    - El despliegue deberá realizarse en Azure App Service.
    - Managed Identity deberá encontrarse habilitada.
    - Azure Key Vault deberá ser utilizado para la gestión de secretos.
    - Azure Monitor deberá encontrarse configurado.
    - Azure Application Insights deberá encontrarse configurado.
    - OpenTelemetry deberá reportar trazas correctamente.

Ningún despliegue a producción podrá ejecutarse si alguna compuerta falla.

---

# Gobernanza

Esta constitución reemplaza acuerdos verbales, convenciones no documentadas y lineamientos previos.

## Proceso de Enmienda

1. Crear Pull Request.
2. Justificar el cambio.
3. Obtener dos aprobaciones técnicas.
4. Obtener aprobación de Seguridad si aplica.
5. Actualizar documentación dependiente.
6. Publicar nueva versión.

## Revisión de Cumplimiento

- Frecuencia: Trimestral.
- Responsable: Arquitectura y Seguridad.
- Incumplimientos: deuda técnica prioridad P1.

---

# Principio Rector

**Toda decisión de arquitectura, diseño, implementación, operación o soporte deberá priorizar la seguridad, la integridad financiera, la trazabilidad, la resiliencia y el cumplimiento regulatorio por encima de cualquier consideración funcional, técnica o de negocio.**

---

**Versión:** 1.0.0  
**Ratificada:** 2026-07-22  
**Última Enmienda:** 2026-07-22