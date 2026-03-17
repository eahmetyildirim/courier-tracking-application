package com.migros.couriertracking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for the Courier Tracking API.
 * Provides API metadata displayed in the Swagger UI at {@code /swagger-ui.html}.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI courierTrackingOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT-like")
                                .description("Paste the token value from GET /api/testing/example-token.")
                ))
                .info(new Info()
                        .title("Migros Courier Tracking API")
                        .description("""
                                RESTful API for tracking courier geolocations and querying total travel distances.

                                Testing in Swagger:
                                1. Call `GET /api/testing/example-token`
                                2. Copy the `token` field
                                3. Click `Authorize` and paste the token value
                                4. Call `POST /api/courier-locations`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Migros Online")
                                .url("https://www.migros.com.tr")));
    }
}
