package com.fundmetrics.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures Cross-Origin Resource Sharing (CORS) for the API.
 *
 * <p>Allows {@code GET} and {@code POST} requests on all {@code /api/**} paths from
 * the known micro frontend origins. Requests from any other origin are rejected by
 * the browser's CORS preflight check.
 *
 * <p>Allowed origins:
 * <ul>
 *   <li>{@code http://localhost:3000} — local micro frontend development server</li>
 *   <li>{@code https://your-microfrontend.example.com} — production micro frontend</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    /**
     * Registers the CORS mappings with Spring MVC.
     *
     * @return a {@link WebMvcConfigurer} that applies the CORS rules globally
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:3000",
                                "https://your-microfrontend.example.com"
                        )
                        .allowedMethods("GET", "POST");
            }
        };
    }
}
