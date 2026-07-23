package com.ecommerce.sale.infrastructure.security;

import java.util.Set;

public class AuthorizationConfig {

    public static final String SCOPE_SALES_WRITE = "SCOPE_sales.write";
    public static final String ROLE_SALE_PROCESSOR = "ROLE_sale.processor";

    private final Set<String> allowedAuthorities = Set.of(SCOPE_SALES_WRITE, ROLE_SALE_PROCESSOR);

    public boolean isAuthorized(Set<String> authorities) {
        return authorities.stream().anyMatch(allowedAuthorities::contains);
    }
}
