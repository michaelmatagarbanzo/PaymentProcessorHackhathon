package com.ecommerce.sale.infrastructure.exception;

public final class ExternalDependencyUnavailableException extends RuntimeException {

    private final String dependency;

    public ExternalDependencyUnavailableException(String dependency, Throwable cause) {
        super("Dependencia externa no disponible: " + dependency, cause);
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}
