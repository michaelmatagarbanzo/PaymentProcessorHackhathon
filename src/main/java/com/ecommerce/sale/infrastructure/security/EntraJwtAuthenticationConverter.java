package com.ecommerce.sale.infrastructure.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class EntraJwtAuthenticationConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Object scopesClaim = jwt.getClaims().get("scp");

        if (scopesClaim instanceof String scopes) {
            for (String scope : scopes.split(" ")) {
                if (!scope.isBlank()) {
                    authorities.add(
                            new SimpleGrantedAuthority("SCOPE_" + scope));
                }
            }
        }

        Object rolesClaim = jwt.getClaims().get("roles");

        if (rolesClaim instanceof List<?> roles) {
            for (Object role : roles) {
                authorities.add(
                        new SimpleGrantedAuthority("ROLE_" + role));
            }
        }

        return authorities;
    }
}