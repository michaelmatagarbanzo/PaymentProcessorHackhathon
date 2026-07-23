# T069 - Variables y secretos operativos para Azure

## Objetivo
Definir la configuración operativa para despliegue en Azure App Service usando Managed Identity y Azure Key Vault, sin secretos en código.

## Convenciones
- Obligatoria: La aplicación no debe iniciar en ambiente Azure sin esta variable.
- Opcional: Tiene valor por defecto en el perfil Azure o depende del nivel de observabilidad deseado.
- Tipo de valor:
  - Plano: valor directo en App Settings.
  - Referencia Key Vault: usar referencia nativa de App Service.

## Formato recomendado para secretos en Azure App Service
Para secretos, usar App Settings con referencia a Key Vault:

@Microsoft.KeyVault(SecretUri=https://kv-sale-prod.vault.azure.net/secrets/NOMBRE-SECRETO/)

## 1) Azure App Service
| Variable | Obligatoria | Tipo | Ejemplo de valor | Descripción |
|---|---|---|---|---|
| SPRING_PROFILES_ACTIVE | Si | Plano | azure | Activa el perfil de despliegue Azure. |
| SERVER_PORT | No | Plano | 8080 | Puerto HTTP del contenedor en App Service. |
| MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE | No | Plano | health,info,metrics,prometheus | Endpoints de Actuator expuestos. |
| MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS | No | Plano | always | Nivel de detalle de health endpoint. |
| MANAGEMENT_ENDPOINT_HEALTH_SHOW_COMPONENTS | No | Plano | always | Componentes mostrados en health endpoint. |
| MANAGEMENT_HEALTH_CIRCUITBREAKERS_ENABLED | No | Plano | true | Publica estado de circuit breakers en health. |
| MANAGEMENT_TRACING_SAMPLING_PROBABILITY | No | Plano | 1.0 | Muestreo de trazas en producción. |

## 2) Managed Identity
| Variable | Obligatoria | Tipo | Ejemplo de valor | Descripción |
|---|---|---|---|---|
| AZURE_CLIENT_ID | No | Plano | 11111111-2222-3333-4444-555555555555 | Requerida solo si se usa User-Assigned Managed Identity. |

Notas operativas:
- Con System-Assigned Managed Identity no se requiere AZURE_CLIENT_ID.
- La identidad administrada debe tener permisos para leer secretos en Key Vault.

## 3) Key Vault
| Variable | Obligatoria | Tipo | Ejemplo de valor | Descripción |
|---|---|---|---|---|
| AZURE_KEYVAULT_URI | Si | Plano | https://kv-sale-prod.vault.azure.net/ | URI del Key Vault usado por la aplicación. |

Permisos mínimos recomendados:
- Secret get
- Secret list

## 4) MongoDB
| Variable | Obligatoria | Tipo | Ejemplo de valor | Descripción |
|---|---|---|---|---|
| SPRING_DATA_MONGODB_URI | Si | Referencia Key Vault | @Microsoft.KeyVault(SecretUri=https://kv-sale-prod.vault.azure.net/secrets/SPRING-DATA-MONGODB-URI/) | Connection string de MongoDB con TLS y credenciales. |

Ejemplo de secreto almacenado en Key Vault:
- mongodb+srv://sale_api_user:StrongPassword@cluster-sale.abcde.mongodb.net/sale_db?retryWrites=true&w=majority

## 5) Entra ID
| Variable | Obligatoria | Tipo | Ejemplo de valor | Descripción |
|---|---|---|---|---|
| SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI | Si | Plano | https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0 | Issuer para validación de JWT. |
| SECURITY_AUDIENCE | Si | Plano | api://sale-api-prod | Audience esperada por la API. |
| SECURITY_TENANT_ID | Si | Plano | aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee | Tenant esperado para validación de claims. |

## 6) Switch (autorizador externo)
| Variable | Obligatoria | Tipo | Ejemplo de valor | Descripción |
|---|---|---|---|---|
| SWITCH_API_BASE_URL | Si | Plano | https://switch.company.com | URL base del servicio switch. |
| SWITCH_OAUTH_TOKEN_ENDPOINT | Si | Plano | https://switch.company.com/oauth/token | Endpoint OAuth client credentials. |
| SWITCH_OAUTH_CLIENT_ID | Si | Referencia Key Vault | @Microsoft.KeyVault(SecretUri=https://kv-sale-prod.vault.azure.net/secrets/SWITCH-OAUTH-CLIENT-ID/) | Client ID para obtener token. |
| SWITCH_OAUTH_CLIENT_SECRET | Si | Referencia Key Vault | @Microsoft.KeyVault(SecretUri=https://kv-sale-prod.vault.azure.net/secrets/SWITCH-OAUTH-CLIENT-SECRET/) | Client secret para OAuth. |
| SWITCH_OAUTH_SCOPE | No | Plano | switch.authorize | Scope solicitado al token endpoint. |
| SWITCH_TIMEOUT_MS | No | Plano | 5000 | Timeout HTTP en milisegundos. |

## 7) Application Insights y OpenTelemetry
| Variable | Obligatoria | Tipo | Ejemplo de valor | Descripción |
|---|---|---|---|---|
| APPLICATIONINSIGHTS_CONNECTION_STRING | No | Referencia Key Vault | @Microsoft.KeyVault(SecretUri=https://kv-sale-prod.vault.azure.net/secrets/APPLICATIONINSIGHTS-CONNECTION-STRING/) | Connection string para exportación a Application Insights. |
| OTEL_EXPORTER_OTLP_ENDPOINT | Si | Plano | https://eastus-0.in.applicationinsights.azure.com/v2/track | Endpoint OTLP/ingest de telemetría. |
| OTEL_EXPORTER_OTLP_HEADERS | No | Referencia Key Vault | @Microsoft.KeyVault(SecretUri=https://kv-sale-prod.vault.azure.net/secrets/OTEL-EXPORTER-OTLP-HEADERS/) | Headers para autenticación del exportador OTLP. |
| OTEL_SERVICE_NAME | No | Plano | sale-api | Nombre de servicio observado. |
| OTEL_RESOURCE_ATTRIBUTES | No | Plano | deployment.environment=azure,service.namespace=ecommerce | Atributos de recurso OTEL. |

## 8) Resilience4j
Todas estas variables son opcionales (con defaults en el perfil Azure), pero recomendadas para ajuste por ambiente:

### Circuit Breaker
- CB_SWITCH_FAILURE_RATE_THRESHOLD: ejemplo 50
- CB_SWITCH_MINIMUM_CALLS: ejemplo 10
- CB_SWITCH_WAIT_OPEN: ejemplo 15s
- CB_SWITCH_HALF_OPEN_CALLS: ejemplo 5
- CB_SWITCH_SLIDING_WINDOW: ejemplo 20
- CB_MONGO_FAILURE_RATE_THRESHOLD: ejemplo 50
- CB_MONGO_MINIMUM_CALLS: ejemplo 10
- CB_MONGO_WAIT_OPEN: ejemplo 15s
- CB_MONGO_HALF_OPEN_CALLS: ejemplo 5
- CB_MONGO_SLIDING_WINDOW: ejemplo 20

### Retry
- RETRY_SWITCH_MAX_ATTEMPTS: ejemplo 3
- RETRY_SWITCH_WAIT: ejemplo 500ms
- RETRY_SWITCH_BACKOFF_MULTIPLIER: ejemplo 2
- RETRY_MONGO_MAX_ATTEMPTS: ejemplo 2
- RETRY_MONGO_WAIT: ejemplo 200ms

### TimeLimiter
- TL_SWITCH_TIMEOUT: ejemplo 5s
- TL_MONGO_TIMEOUT: ejemplo 3s

### Bulkhead
- BH_SWITCH_MAX_CONCURRENT_CALLS: ejemplo 25
- BH_SWITCH_MAX_WAIT: ejemplo 0
- BH_MONGO_MAX_CONCURRENT_CALLS: ejemplo 50
- BH_MONGO_MAX_WAIT: ejemplo 0

### RateLimiter
- RL_SWITCH_LIMIT_FOR_PERIOD: ejemplo 200
- RL_SWITCH_LIMIT_REFRESH_PERIOD: ejemplo 1s
- RL_SWITCH_TIMEOUT_DURATION: ejemplo 0

## Checklist operativo mínimo para salida a producción
1. Definir SPRING_PROFILES_ACTIVE=azure.
2. Habilitar Managed Identity en App Service.
3. Asignar permisos de Secret get/list de Key Vault a la identidad.
4. Configurar AZURE_KEYVAULT_URI.
5. Configurar variables obligatorias de MongoDB, Entra ID y Switch.
6. Configurar al menos un destino de telemetría (Application Insights u OTLP).
7. Verificar endpoint /actuator/health con circuit breakers habilitados.
