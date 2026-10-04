package co.com.fcv.training.citas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** HU-004 / RF-20: published OpenAPI contract. Every route requires the JWT access token except /auth/**. */
@Configuration
class OpenApiConfig {
    @Bean OpenAPI citasOpenApi() {
        return new OpenAPI()
                .info(new Info().title("FCV Citas API").version("v1")
                        .description("REST/JSON contract consumed directly by citas-web and by the n8n workflows. "
                                + "POST /api/v1/auth/login, /refresh and /logout also require the header X-Requested-With: XMLHttpRequest."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
