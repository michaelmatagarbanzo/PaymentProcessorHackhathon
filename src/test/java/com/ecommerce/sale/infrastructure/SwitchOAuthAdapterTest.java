package com.ecommerce.sale.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecommerce.sale.infrastructure.adapter.security.KeyVaultAdapter;
import com.ecommerce.sale.infrastructure.adapter.switch_api.SwitchOAuthAdapter;
import com.ecommerce.sale.infrastructure.config.SwitchProperties;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SwitchOAuthAdapterTest {

    @Mock
    private KeyVaultAdapter keyVaultAdapter;

    @Test
    void shouldMapOAuthTransportFailureToExternalDependencyException() {
        SwitchProperties properties = new SwitchProperties();
        properties.getOauth().setTokenEndpoint("http://127.0.0.1:1/oauth/token");
        properties.getOauth().setClientId("local-client");
        properties.getOauth().setClientSecret("local-secret");
        properties.getOauth().setScope("switch.authorize");

        SwitchOAuthAdapter adapter = new SwitchOAuthAdapter(properties, keyVaultAdapter);

        ExternalDependencyUnavailableException ex = assertThrows(
            ExternalDependencyUnavailableException.class,
            adapter::getAccessToken
        );

        assertEquals("switch", ex.getDependency());
    }
}
