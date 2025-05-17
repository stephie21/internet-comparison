package com.check24.internetcomparison.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI internetComparisonOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Internet Comparison API")
                .description("API für den Vergleich von Internetanbietern")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Check24")
                    .email("support@check24.de"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://www.check24.de")))
            .servers(List.of(
                new Server()
                    .url("https://api.check24.de")
                    .description("Produktionsserver"),
                new Server()
                    .url("https://api-staging.check24.de")
                    .description("Staging-Server")
            ));
    }
} 