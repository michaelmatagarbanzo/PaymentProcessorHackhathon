# Quickstart: API Financiera de Transacciones SALE

**Feature**: `001-api-transacciones-sale`  
**Fecha**: 2026-07-22  
**Stack**: Java 21 · Spring Boot 3 · MongoDB · Azure

---

## Prerrequisitos

| Herramienta | Versión | Propósito |
|-------------|---------|-----------|
| Java JDK | 21 (LTS) | Runtime de la aplicación |
| Maven | 3.9+ (o Maven Wrapper incluido) | Build y dependencias |
| Docker | 24+ | MongoDB local con TestContainers |
| Docker Compose | 2.x | Ambiente local completo |
| Azure CLI | 2.x | Gestión de secretos en Key Vault (opcional en local) |
| Postman / curl | Cualquiera | Prueba manual de endpoints |

---

## Inicio Rápido (Ambiente Local)

### 1. Clonar y configurar el repositorio

```bash
git clone <repo-url>
cd repo-hackaton-ecommerce
git checkout 001-api-transacciones-sale
```

### 2. Levantar dependencias locales con Docker Compose

```bash
# Levanta MongoDB, WireMock (simulador del API Switch) y Grafana local
docker compose -f docker/docker-compose.local.yml up -d
```

El `docker-compose.local.yml` levantará:

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| MongoDB | 27017 | Base de datos de transacciones |
| WireMock | 8090 | Simulador del API Switch (AS400/Cybersource) |
| Grafana | 3000 | Dashboard de observabilidad (admin/admin) |

### 3. Configurar variables de entorno (sin secretos reales en local)

Crear archivo `.env.local` en la raíz del proyecto (está en `.gitignore`):

```bash
# .env.local — Solo para desarrollo local. NUNCA compartir ni commitear.

# MongoDB
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/sale_db

# API Switch (apunta a WireMock local)
SWITCH_API_BASE_URL=http://localhost:8090
SWITCH_OAUTH_TOKEN_ENDPOINT=http://localhost:8090/oauth/token
SWITCH_OAUTH_CLIENT_ID=local-client
SWITCH_OAUTH_CLIENT_SECRET=local-secret

# Seguridad Entra ID (en local se puede usar un JWT mock con Spring Security test)
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://login.microsoftonline.com/your-tenant-id/v2.0
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES=api://your-client-id

# OpenTelemetry (deshabilitar exportación en local)
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
OTEL_SERVICE_NAME=sale-api-local
```

### 4. Compilar y ejecutar la aplicación

```bash
# Compilar
./mvnw clean compile

# Ejecutar pruebas unitarias
./mvnw test

# Ejecutar la aplicación
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

La aplicación estará disponible en:
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Health**: http://localhost:8080/actuator/health

### 5. Probar el endpoint SALE

#### Con curl

```bash
# Obtener token JWT (modo local — con Spring Security test o mock)
# En ambiente local, puedes usar un token pre-firmado para testing

# Procesar transacción SALE
curl -X POST http://localhost:8080/api/v1/sales \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -H "X-Correlation-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Merchant-Id: MERCHANT-001" \
  -d '{
    "merchantId": "MERCHANT-001",
    "terminalId": "TERM-0001",
    "transactionType": "SALE",
    "totalAmount": 100.00,
    "currency": "USD",
    "paymentInstrument": {
      "tokenizedCardNumber": "tok_visa_4111111111111111",
      "expirationDate": "2027-12",
      "cardBrand": "VISA",
      "cardholderName": "John Doe"
    }
  }'
```

**Respuesta esperada (HTTP 200 — AUTHORIZED)**:
```json
{
  "transactionId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "AUTHORIZED",
  "merchantId": "MERCHANT-001",
  "totalAmount": 100.00,
  "currency": "USD",
  "authorization": {
    "authorizationSource": "AS400",
    "authorizationNumber": "AUTH123456",
    "responseCode": "00",
    "responseDescription": "Aprobado",
    "referenceNumber": "REF20260722001",
    "hostDate": "0722",
    "hostTime": "143052"
  },
  "processingDateTime": "2026-07-22T14:30:52.456Z",
  "createdAt": "2026-07-22T14:30:52.123Z"
}
```

---

## Estructura del Proyecto

```text
repo-hackaton-ecommerce/
├── src/
│   ├── main/java/com/ecommerce/sale/
│   │   ├── domain/           ← Entidades, Value Objects, reglas de negocio
│   │   ├── application/      ← Casos de uso, puertos (interfaces)
│   │   ├── infrastructure/   ← Adaptadores, config, cliente MongoDB, Spring
│   │   └── presentation/     ← Controladores REST, DTOs, handlers de error
│   └── test/java/com/ecommerce/sale/
│       ├── architecture/     ← Pruebas ArchUnit
│       ├── domain/           ← Pruebas unitarias de dominio
│       ├── application/      ← Pruebas unitarias de casos de uso
│       ├── infrastructure/   ← Pruebas de integración (TestContainers)
│       ├── presentation/     ← Pruebas de API (WebMvcTest)
│       ├── contract/         ← Pruebas de contrato (WireMock)
│       ├── integration/      ← Pruebas de flujo completo
│       └── performance/      ← Pruebas de carga (JMeter/Gatling)
├── specs/001-api-transacciones-sale/
│   ├── spec.md
│   ├── plan.md
│   ├── research.md
│   ├── data-model.md
│   ├── quickstart.md         ← Este archivo
│   └── contracts/openapi.yaml
├── docker/
│   └── docker-compose.local.yml
├── pom.xml
└── README.md
```

---

## Ejecutar Pruebas

```bash
# Todas las pruebas
./mvnw verify

# Solo unitarias (rápido, sin Docker)
./mvnw test -P unit-tests

# Solo integración (requiere Docker para TestContainers)
./mvnw test -P integration-tests

# ArchUnit (reglas de arquitectura)
./mvnw test -Dtest=ArchitectureRulesTest

# Con reporte de cobertura JaCoCo
./mvnw verify jacoco:report
# Reporte en: target/site/jacoco/index.html

# Con SonarQube local (requiere SonarQube en Docker)
./mvnw sonar:sonar \
  -Dsonar.projectKey=sale-api \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<token>
```

---

## Configuración de Resilience4j (Local)

En `src/main/resources/application-local.yml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      switchApi:
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        slidingWindowSize: 10
  retry:
    instances:
      switchApi:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
  timelimiter:
    instances:
      switchApi:
        timeoutDuration: 5s
  bulkhead:
    instances:
      switchApi:
        maxConcurrentCalls: 25
```

---

## Simular Escenarios con WireMock

WireMock simula el API Switch en ambiente local. Configurar respuestas en `docker/wiremock/mappings/`:

**Respuesta aprobada (AS400)**:
```json
{
  "request": {
    "method": "POST",
    "url": "/api/switch/authorize"
  },
  "response": {
    "status": 200,
    "jsonBody": {
      "authorizationSource": "AS400",
      "authorizationNumber": "AUTH123456",
      "responseCode": "00",
      "responseDescription": "Aprobado",
      "referenceNumber": "REF20260722001",
      "hostDate": "0722",
      "hostTime": "143052"
    }
  }
}
```

**Simular timeout (para probar CircuitBreaker)**:
```json
{
  "request": {
    "method": "POST",
    "url": "/api/switch/authorize"
  },
  "response": {
    "fixedDelayMilliseconds": 6000,
    "status": 200
  }
}
```

---

## Verificar MongoDB

```bash
# Conectar al MongoDB local
docker exec -it mongo-local mongosh sale_db

# Ver transacciones procesadas
db.sale_transactions.find().limit(5).pretty()

# Verificar índices configurados
db.sale_transactions.getIndexes()

# Verificar TTL index
db.sale_transactions.getIndexes().filter(i => i.expireAfterSeconds)
```

---

## Verificar Logs Estructurados

```bash
# Ver logs de la aplicación con jq para formato legible
./mvnw spring-boot:run | jq '.'

# Filtrar por correlationId
./mvnw spring-boot:run | jq 'select(.correlationId == "550e8400-e29b-41d4-a716-446655440000")'

# Filtrar eventos de error
./mvnw spring-boot:run | jq 'select(.level == "ERROR")'
```

---

## Despliegue en Azure App Service

> **Prerrequisito**: Managed Identity configurado, Key Vault con secretos cargados, Application Insights provisionado.

```bash
# Build del artefacto
./mvnw clean package -DskipTests

# Desplegar en Azure App Service (vía Azure CLI)
az webapp deploy \
  --name <app-service-name> \
  --resource-group <resource-group> \
  --src-path target/sale-api.jar \
  --type jar

# Verificar estado
az webapp show --name <app-service-name> --resource-group <resource-group> \
  --query "state"

# Ver logs en tiempo real
az webapp log tail --name <app-service-name> --resource-group <resource-group>
```

### Variables de Entorno en App Service

Configurar en Azure Portal o Azure CLI (sin secretos en código):

```bash
az webapp config appsettings set \
  --name <app-service-name> \
  --resource-group <resource-group> \
  --settings \
    SWITCH_API_BASE_URL="https://api-switch.internal" \
    SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI="https://login.microsoftonline.com/<tenant>/v2.0" \
    SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES="api://<client-id>" \
    AZURE_KEYVAULT_URI="https://<vault-name>.vault.azure.net" \
    OTEL_SERVICE_NAME="sale-api"
    # Los secretos se leen automáticamente de Key Vault vía Managed Identity
```

---

## Troubleshooting Común

| Síntoma | Causa Probable | Solución |
|---------|----------------|----------|
| HTTP 401 en todos los requests | Token JWT inválido o no configurado | Verificar `issuer-uri` y `audiences` en config |
| CircuitBreaker siempre OPEN | WireMock no responde o timeout muy bajo | Verificar que WireMock está corriendo en puerto 8090 |
| MongoDB connection refused | Docker Compose no iniciado | `docker compose -f docker/docker-compose.local.yml up -d` |
| ArchUnit test falla | Importación incorrecta entre capas | Revisar paquetes de la clase importada |
| Cobertura < 80% | Falta prueba unitaria | Revisar reporte JaCoCo en `target/site/jacoco/` |
| SonarQube no conecta | Sonar no disponible | `docker run -p 9000:9000 sonarqube:lts-community` |

---

## Referencias

- [Especificación del Feature](spec.md)
- [Plan de Implementación](plan.md)
- [Investigación Técnica](research.md)
- [Modelo de Datos](data-model.md)
- [Contrato OpenAPI](contracts/openapi.yaml)
- [Constitución del Proyecto](../../.specify/memory/constitution.md)
