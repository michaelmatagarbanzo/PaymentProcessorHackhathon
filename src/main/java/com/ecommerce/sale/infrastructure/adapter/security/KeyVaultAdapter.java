package com.ecommerce.sale.infrastructure.adapter.security;

import com.azure.security.keyvault.secrets.SecretClient;
import org.springframework.stereotype.Component;

@Component
public class KeyVaultAdapter {

    private final SecretClient secretClient;

    public KeyVaultAdapter(SecretClient secretClient) {
        this.secretClient = secretClient;
    }

    public String getSecretValue(String secretName) {
        return secretClient.getSecret(secretName).getValue();
    }
}
