package com.ecommerce.sale.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI saleOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("API eCommerce SALE - Transacciones Financieras")
                .version("1.1.0")
                .description("Contrato MVP para procesamiento de transacciones SALE"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .schemaRequirement("bearerAuth", new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
    }
}
