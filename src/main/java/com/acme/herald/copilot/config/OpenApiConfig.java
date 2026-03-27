package com.acme.herald.copilot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI heraldOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Herald Copilot Connector API")
                        .version("0.1.0")
                        .description("""
                                HTTP API do lokalnego connectora GitHub Copilot.
                                Spec jest przygotowana pod generowanie klientów frontendowych z /v3/api-docs.
                                Większość endpointów może użyć tokena przekazanego nagłówkiem albo tokena
                                zapamiętanego wcześniej w desktopowym UI connectora.
                                """)
                        .contact(new Contact()
                                .name("Herald Connector")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("GitHub PAT")
                                .description("Opcjonalny nagłówek Authorization: Bearer <token>."))
                        .addSecuritySchemes("githubTokenHeader", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-GitHub-Token")
                                .description("Alternatywny nagłówek X-GitHub-Token z GitHub PAT.")));
    }
}
