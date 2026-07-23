# API Financiera de Transacciones SALE

Backend REST para procesamiento de transacciones SALE en eCommerce, construido con Java 21, Spring Boot 3 y arquitectura limpia.

## Arquitectura
El proyecto usa Clean Architecture con separación estricta por capas:

- domain: entidades, value objects y reglas de negocio puras
- application: casos de uso y puertos
- infrastructure: adaptadores, configuración Spring, MongoDB, seguridad y observabilidad
- presentation: controladores REST, DTOs y manejo de errores

Estructura principal:

```text
src/main/java/com/ecommerce/sale/
  domain/
  application/
  infrastructure/
  presentation/
src/test/java/com/ecommerce/sale/
  architecture/
  application/
  domain/
  infrastructure/
  contract/
  performance/
```

## Requisitos

- Java 21
- Maven 3.9+ o Maven Wrapper
- Docker y Docker Compose para entorno local con MongoDB y WireMock

## Ejecución local

1. Levantar dependencias locales:

```bash
docker compose -f docker/docker-compose.local.yml up -d
```

2. Configurar variables de entorno locales mínimas:

```bash
export SPRING_PROFILES_ACTIVE=local
export SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/sale_db
export SWITCH_API_BASE_URL=http://localhost:8089
export SWITCH_OAUTH_TOKEN_ENDPOINT=http://localhost:8089/oauth/token
export SWITCH_OAUTH_CLIENT_ID=local-client
export SWITCH_OAUTH_CLIENT_SECRET=local-secret
```

3. Compilar y ejecutar:

```bash
./mvnw clean compile
./mvnw spring-boot:run
```

4. Verificar endpoints principales:

- Health: http://localhost:8080/actuator/health
- OpenAPI: http://localhost:8080/v3/api-docs
- Swagger UI: http://localhost:8080/swagger-ui.html

## Perfiles Spring

- default: local
- local: desarrollo local, incluye valores para trabajo sin Azure
- azure: configuración productiva en App Service basada en variables de entorno

Archivos:

- src/main/resources/application.yml
- src/main/resources/application-local.yml
- src/main/resources/application-azure.yml

## Docker

Compose local definido en docker/docker-compose.local.yml con:

- MongoDB 7 en puerto 27017
- WireMock en puerto 8089
- Volumen persistente mongodb_data para datos de Mongo

Comandos útiles:

```bash
docker compose -f docker/docker-compose.local.yml up -d
docker compose -f docker/docker-compose.local.yml ps
docker compose -f docker/docker-compose.local.yml logs -f
docker compose -f docker/docker-compose.local.yml down
```

## MongoDB

Uso principal:

- Persistencia de transacciones SALE
- Índice único por transactionId
- Índice TTL para retención operativa
- Búsqueda de duplicados por clave de negocio

Conexión por variable:

- SPRING_DATA_MONGODB_URI

## WireMock

Uso principal:

- Simular integración con Switch en entorno local

Puerto expuesto local:

- 8089

Variables típicas:

- SWITCH_API_BASE_URL=http://localhost:8089
- SWITCH_OAUTH_TOKEN_ENDPOINT=http://localhost:8089/oauth/token

## Swagger

Documentación de API disponible en:

- /swagger-ui.html
- /v3/api-docs

## Pruebas

Ejecutar compilación y pruebas:

```bash
./mvnw clean compile
./mvnw test
./mvnw verify
```

Notas:

- Las pruebas con Testcontainers pueden omitirse si Docker no está disponible.
- Las pruebas de arquitectura se ejecutan dentro del ciclo de pruebas.

## Azure App Service

Despliegue recomendado:

1. Publicar imagen o paquete del servicio.
2. Configurar App Settings con variables de entorno.
3. Definir SPRING_PROFILES_ACTIVE=azure.
4. Habilitar endpoint de health para monitoreo.

Variables operativas documentadas en:

- specs/001-api-transacciones-sale/operational-variables-azure.md

## Key Vault

Buenas prácticas:

- No guardar secretos en repositorio
- Usar referencias de Key Vault en App Settings
- Mantener rotación periódica de secretos

Ejemplo de referencia en App Service:

```text
@Microsoft.KeyVault(SecretUri=https://kv-sale-prod.vault.azure.net/secrets/SWITCH-OAUTH-CLIENT-SECRET/)
```

## Managed Identity

Recomendación:

- Usar System-Assigned Managed Identity en App Service
- Conceder permisos mínimos sobre Key Vault: Secret get y Secret list
- Usar User-Assigned Identity solo cuando se requiera aislamiento por identidad

## Exportación de logs a Application Insights

La aplicación mantiene logs JSON en consola y exporta los eventos de logger (INFO/WARN/ERROR) a Azure Application Insights vía OpenTelemetry cuando existe la variable de entorno `APPLICATIONINSIGHTS_CONNECTION_STRING`.

Variables requeridas:

- APPLICATIONINSIGHTS_CONNECTION_STRING
- SPRING_PROFILES_ACTIVE (recomendado: local o azure)

Ejemplo local:

```bash
export SPRING_PROFILES_ACTIVE=local
export APPLICATIONINSIGHTS_CONNECTION_STRING="InstrumentationKey=...;IngestionEndpoint=https://..."
mvn spring-boot:run
```

Logs de validación esperados en arranque:

- Application Insights exporter enabled
- Application Insights connectivity test

Validación de llegada en Application Insights:

1. Generar logs INFO, WARN y ERROR (por ejemplo, invocando `POST /api/v1/sales` y `GET /actuator/health` en escenarios de dependencia no disponible).
2. Abrir Logs en Application Insights y ejecutar:

```kusto
AppTraces
| order by TimeGenerated desc
| take 50
```

3. Confirmar presencia de campos de contexto en customDimensions cuando estén disponibles:

- correlationId
- transactionId
- traceId
- spanId

Compatibilidad Grafana:

- Configurar Azure Monitor como Data Source en Grafana.
- Consultar `AppTraces` para paneles de logs y correlacionar con trazas/métricas.

## Troubleshooting

1. Error de Java o versión incorrecta
- Validar java -version
- Configurar JAVA_HOME a JDK 21

2. Fallas en pruebas de integración con Mongo
- Verificar Docker activo
- Revisar docker compose ps

3. Error de conexión a MongoDB
- Revisar SPRING_DATA_MONGODB_URI
- Confirmar que Mongo está escuchando en 27017

4. Fallas de autenticación JWT en local
- Verificar perfil activo local
- Revisar issuer y audience configurados

5. Error en integración con Switch
- Verificar SWITCH_API_BASE_URL y token endpoint
- Revisar logs de WireMock en puerto 8089

6. Swagger no responde
- Verificar que la aplicación inició correctamente
- Revisar /actuator/health y logs de arranque

## Referencias

- Contrato OpenAPI: specs/001-api-transacciones-sale/contracts/openapi.yaml
- Plan de implementación: specs/001-api-transacciones-sale/plan.md
- Tareas: specs/001-api-transacciones-sale/tasks.md
- Variables Azure: specs/001-api-transacciones-sale/operational-variables-azure.md
