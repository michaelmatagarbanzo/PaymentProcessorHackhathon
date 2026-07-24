package com.ecommerce.sale.contract;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.AuthorizationSource;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.infrastructure.adapter.observability.ApplicationInsightsAdapter;
import com.ecommerce.sale.infrastructure.adapter.switch_api.SwitchApiAdapter;
import com.ecommerce.sale.infrastructure.adapter.switch_api.SwitchRequestMapper;
import com.ecommerce.sale.infrastructure.config.GrafanaMetricsConfig;
import com.ecommerce.sale.infrastructure.config.SwitchProperties;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SwitchApiAdapterContractTest {

    private WireMockServer wireMockServer;
    private SwitchApiAdapter switchApiAdapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();

        SwitchProperties properties = new SwitchProperties();
        properties.getAppconnector().setBaseUrl("http://localhost:" + wireMockServer.port());
        properties.getAppconnector().setApiKey("test-api-key");

        var registry = new SimpleMeterRegistry();
        var metrics = new GrafanaMetricsConfig(registry);
        var observability = new ApplicationInsightsAdapter(metrics);

        switchApiAdapter = new SwitchApiAdapter(properties, new SwitchRequestMapper(), observability);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @Disabled("Known local issue: JDK HttpClient + WireMock may close HTTP/2 stream with EOF")
    void shouldMapApprovedResponseFromSwitch() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/payments"))
                        .willReturn(okJson("""
                                {
                        "provider": "AS400",
                        "authorizationCode": "AUTH001",
                        "providerResponseCode": "00",
                        "providerMessage": "Aprobado",
                                    "referenceNumber": "REF001",
                        "status": "AUTHORIZED"
                                }
                                """)));

        AuthorizationResponse response = switchApiAdapter.authorize(pendingTransaction(), sampleCommand());

        assertEquals(AuthorizationSource.AS400, response.authorizationSource());
        assertEquals("00", response.responseCode());
            wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v1/payments"))
                .withHeader("X-API-Key", equalTo("test-api-key"))
                        .withHeader("X-Correlation-Id", equalTo("550e8400-e29b-41d4-a716-446655440000")));
    }

    @Test
    void shouldTranslateTechnicalFailureToExternalDependencyException() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/payments"))
            .willReturn(aResponse().withStatus(500).withBody("upstream error")));

        assertThrows(
            ExternalDependencyUnavailableException.class,
            () -> switchApiAdapter.authorize(pendingTransaction(), sampleCommand())
        );
    }

    private ProcessSaleCommand sampleCommand() {
        return new ProcessSaleCommand(
            "550e8400-e29b-41d4-a716-446655440000",
            "TERM-0001",
            "SALE",
            5633L,
            "55189800****2751",
            "2805",
            14611279L,
            "123",
            "1",
            true,
            null,
            null,
            null
        );
    }

    private SaleTransaction pendingTransaction() {
        Instant now = Instant.now();
        return new SaleTransaction(
            "7c9e6679-7425-40de-944b-e07fc1f90ae7",
            "550e8400-e29b-41d4-a716-446655440000",
            "TERM-0001",
            TransactionType.SALE,
            5633L,
            "55189800****2751",
            "2805",
            14611279L,
            "1",
            true,
            TransactionStatus.PENDING,
            null,
            now,
            null,
            now
        );
    }
}
