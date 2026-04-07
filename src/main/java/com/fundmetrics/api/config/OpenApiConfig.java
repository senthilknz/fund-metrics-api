package com.fundmetrics.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI / Swagger metadata shown in the Swagger UI header.
 *
 * <p>SpringDoc picks up this bean automatically and merges it with the endpoint
 * metadata discovered from {@code @Operation} and {@code @Schema} annotations
 * across the codebase.
 *
 * <p>Swagger UI is available at {@code /swagger-ui.html} and the raw spec at
 * {@code /v3/api-docs} when the application is running.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the top-level {@link OpenAPI} metadata object used by Swagger UI.
     *
     * @return an {@link OpenAPI} instance with title, description, version, contact, and license
     */
    @Bean
    public OpenAPI fundMetricsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Active Series Fund Metrics API")
                        .description("Serves versioned managed fund metric data to micro frontends. " +
                                "Config is driven by JSON files — no database required. " +
                                "New quarterly data is activated automatically on its effectiveFrom date.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("xAPI Investments Team")
                                .email("xapi-investments@example.com"))
                        .license(new License()
                                .name("Internal")
                                .url("https://example.com")));
    }
}
