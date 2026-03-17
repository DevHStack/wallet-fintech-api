package com.hiral.wallet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet Finance API")
                        .description("A comprehensive wallet API for managing accounts, deposits, withdrawals, and transfers with advanced features like idempotency and audit logging")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Hiral Shivade")
                                .url("https://github.com/DevHStack/wallet-fintech-api")))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Bearer token for authentication. Required for all endpoints.")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
