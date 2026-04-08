package com.fundmetrics.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OpenApiConfigTest {

    @Autowired
    private OpenAPI openAPI;

    @Test
    void openApiBean_isLoaded() {
        assertThat(openAPI).isNotNull();
    }

    @Test
    void openApiBean_hasCorrectTitle() {
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Active Series Fund Metrics API");
    }

    @Test
    void openApiBean_hasCorrectVersion() {
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void openApiBean_hasDescription() {
        assertThat(openAPI.getInfo().getDescription()).isNotBlank();
    }

    @Test
    void openApiBean_hasContactName() {
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("xAPI Investments Team");
    }

    @Test
    void openApiBean_hasContactEmail() {
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("xapi-investments@example.com");
    }

    @Test
    void openApiBean_hasLicenseName() {
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("Internal");
    }

    @Test
    void openApiBean_hasLicenseUrl() {
        assertThat(openAPI.getInfo().getLicense().getUrl()).isNotBlank();
    }
}
