package com.ecommerce.sale.presentation.exception;

import com.ecommerce.sale.domain.exception.AuthorizationSwitchException;
import com.ecommerce.sale.domain.exception.DuplicateTransactionException;
import com.ecommerce.sale.domain.exception.InvalidSaleRequestException;
import com.ecommerce.sale.domain.exception.SaleDomainException;
import com.ecommerce.sale.domain.exception.SwitchAuthenticationException;
import com.ecommerce.sale.domain.exception.TransactionPersistenceException;
import com.ecommerce.sale.infrastructure.adapter.switch_api.SwitchDiagnosticsContext;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.concurrent.TimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI VALIDATION_ERROR_TYPE = URI.create("/errors/validation-error");
    private static final URI DUPLICATE_TRANSACTION_TYPE = URI.create("/errors/duplicate-transaction");
    private static final URI DATABASE_UNAVAILABLE_TYPE = URI.create("/errors/database-unavailable");
    private static final URI SWITCH_UNAVAILABLE_TYPE = URI.create("/errors/switch-unavailable");
    private static final URI UNAUTHORIZED_TYPE = URI.create("/errors/unauthorized");
    private static final URI ACCESS_DENIED_TYPE = URI.create("/errors/access-denied");
    private static final URI INTERNAL_SERVER_ERROR_TYPE = URI.create("/errors/internal-server-error");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logSaleError(request, "ValidationException", String.valueOf(HttpStatus.BAD_REQUEST.value()),
            "La solicitud no cumple las validaciones requeridas", ex);
        ProblemDetail problemDetail = baseProblem(
            HttpStatus.BAD_REQUEST,
            VALIDATION_ERROR_TYPE,
            "Solicitud inválida",
            "La solicitud no cumple las validaciones requeridas",
            request
        );
        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldError)
            .collect(Collectors.toList());
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler({InvalidSaleRequestException.class, IllegalArgumentException.class, IllegalStateException.class})
    ProblemDetail handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        logSaleError(request, "BusinessException", String.valueOf(HttpStatus.BAD_REQUEST.value()), ex.getMessage(), ex);
        return baseProblem(
            HttpStatus.BAD_REQUEST,
            VALIDATION_ERROR_TYPE,
            "Regla de negocio inválida",
            ex.getMessage(),
            request
        );
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    ProblemDetail handleDuplicateTransaction(DuplicateTransactionException ex, HttpServletRequest request) {
        logSaleError(request, "BusinessException", String.valueOf(HttpStatus.CONFLICT.value()), ex.getMessage(), ex);
        return baseProblem(
            HttpStatus.CONFLICT,
            DUPLICATE_TRANSACTION_TYPE,
            "Transacción duplicada",
            "Ya existe una transacción con la misma clave de negocio",
            request
        );
    }

    @ExceptionHandler(ExternalDependencyUnavailableException.class)
    ResponseEntity<ProblemDetail> handleExternalDependencyUnavailable(
        ExternalDependencyUnavailableException ex,
        HttpServletRequest request
    ) {
        logSaleError(request, "ExternalDependencyUnavailableException", String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()),
            ex.getMessage(), ex);
        return externalDependencyUnavailableResponse(ex.getDependency(), request);
    }

    @ExceptionHandler({AuthorizationSwitchException.class, SwitchAuthenticationException.class, TransactionPersistenceException.class})
    ResponseEntity<ProblemDetail> handleServiceUnavailable(SaleDomainException ex, HttpServletRequest request) {
        logSaleError(request, ex.getClass().getSimpleName(), String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()),
            ex.getMessage(), ex);
        if (isExternalDependencyFailure(ex)) {
            return externalDependencyUnavailableResponse(resolveDependency(ex), request);
        }
        ProblemDetail detail = baseProblem(
            HttpStatus.SERVICE_UNAVAILABLE,
            SWITCH_UNAVAILABLE_TYPE,
            "Dependencia no disponible",
            ex.getMessage(),
            request
        );
        enrichWithSwitchDiagnostics(detail);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header(HttpHeaders.RETRY_AFTER, "2")
            .body(detail);
    }

    @ExceptionHandler({CallNotPermittedException.class, TimeoutException.class})
    ResponseEntity<ProblemDetail> handleResilienceFailures(RuntimeException ex, HttpServletRequest request) {
        logSaleError(request, ex.getClass().getSimpleName(), String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()),
            ex.getMessage(), ex);
        return externalDependencyUnavailableResponse("switch", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleUnauthorized(AuthenticationException ex, HttpServletRequest request) {
        logSaleError(request, "AuthenticationException", String.valueOf(HttpStatus.UNAUTHORIZED.value()),
            "La autenticación es requerida para acceder a este recurso", ex);
        return baseProblem(
            HttpStatus.UNAUTHORIZED,
            UNAUTHORIZED_TYPE,
            "No autorizado",
            "La autenticación es requerida para acceder a este recurso",
            request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        logSaleError(request, "AccessDeniedException", String.valueOf(HttpStatus.FORBIDDEN.value()),
            "No tiene permisos suficientes para acceder a este recurso", ex);
        return baseProblem(
            HttpStatus.FORBIDDEN,
            ACCESS_DENIED_TYPE,
            "Acceso denegado",
            "No tiene permisos suficientes para acceder a este recurso",
            request
        );
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        logSaleError(request, ex.getClass().getSimpleName(), String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
            "Ocurrió un error inesperado", ex);
        return baseProblem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR_TYPE,
            "Error interno",
            "Ocurrió un error inesperado",
            request
        );
    }

    private ProblemDetail baseProblem(HttpStatus status, URI type, String title, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(type);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId != null && !correlationId.isBlank()) {
            problemDetail.setProperty("correlationId", correlationId);
        }
        return problemDetail;
    }

    private ResponseEntity<ProblemDetail> externalDependencyUnavailableResponse(String dependency, HttpServletRequest request) {
        URI type = resolveExternalDependencyType(dependency);
        String detailMessage = resolveExternalDependencyDetail(dependency);
        ProblemDetail detail = baseProblem(
            HttpStatus.SERVICE_UNAVAILABLE,
            type,
            "Dependencia externa no disponible",
            detailMessage,
            request
        );
        if (SWITCH_UNAVAILABLE_TYPE.equals(type)) {
            enrichWithSwitchDiagnostics(detail);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header(HttpHeaders.RETRY_AFTER, "30")
            .body(detail);
    }

    private void enrichWithSwitchDiagnostics(ProblemDetail detail) {
        Map<String, Object> ctx = SwitchDiagnosticsContext.pop();
        if (ctx == null) {
            return;
        }
        Object diagnostics = ctx.get("diagnostics");
        if (diagnostics instanceof Map<?, ?>) {
            detail.setProperty("diagnostics", diagnostics);
        }
    }

    private URI resolveExternalDependencyType(String dependency) {
        if (dependency == null) {
            return SWITCH_UNAVAILABLE_TYPE;
        }
        return switch (dependency.toLowerCase()) {
            case "mongodb", "cosmos" -> DATABASE_UNAVAILABLE_TYPE;
            default -> SWITCH_UNAVAILABLE_TYPE;
        };
    }

    private String resolveExternalDependencyDetail(String dependency) {
        if (dependency == null) {
            return "El servicio requerido no está disponible temporalmente";
        }
        return switch (dependency.toLowerCase()) {
            case "mongodb", "cosmos" -> "No fue posible acceder a la base de datos";
            case "switch" -> "El servicio AppConnector no está disponible temporalmente";
            default -> "El servicio requerido no está disponible temporalmente";
        };
    }

    private String resolveDependency(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ExternalDependencyUnavailableException external) {
                return external.getDependency();
            }
            current = current.getCause();
        }
        return "switch";
    }

    private boolean isExternalDependencyFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ExternalDependencyUnavailableException
                || current instanceof CallNotPermittedException
                || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Map<String, Object> toFieldError(FieldError error) {
        return Map.of(
            "field", error.getField(),
            "message", error.getDefaultMessage(),
            "rejectedValue", safeRejectedValue(error.getRejectedValue())
        );
    }

    private Object safeRejectedValue(Object rejectedValue) {
        if (rejectedValue instanceof String text) {
            return switch (text) {
                case String ignored when text.length() > 8 -> "[REDACTED]";
                default -> text;
            };
        }
        return rejectedValue;
    }

    private void logSaleError(HttpServletRequest request, String errorType, String errorCode, String errorMessage, Throwable throwable) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = MDC.get("correlationId");
        }
        String transactionId = request.getHeader("X-Transaction-Id");
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = MDC.get("transactionId");
        }
        LOG.error(
            "event=sale.error correlationId={} transactionId={} errorType={} errorCode={} errorMessage={}",
            safe(correlationId),
            safe(transactionId),
            errorType,
            errorCode,
            errorMessage,
            throwable
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}