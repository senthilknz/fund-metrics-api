package com.fundmetrics.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fundmetrics.api.model.FundConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FundConfigService} edge cases that require mocked infrastructure.
 *
 * <p>Complements {@link com.fundmetrics.api.FundConfigServiceTest} which tests
 * the service against the real classpath config files.
 */
class FundConfigServiceEdgeCaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // -------------------------------------------------------------------------
    // loadAllConfigs() — error handling
    // -------------------------------------------------------------------------

    @Test
    void loadAllConfigs_skipsUnparsableResources_andLoadsRemainingOnes() throws Exception {
        Resource badResource = new ByteArrayResource("not-valid-json".getBytes()) {
            @Override public String getFilename() { return "bad-config.json"; }
        };

        String validJson = """
                {
                  "version": "1.0.0",
                  "effectiveFrom": "2025-01-01",
                  "publishedAt": "2025-01-01T00:00:00Z",
                  "performanceAsOf": "2024-12-31",
                  "dataSource": "Active Series",
                  "disclaimer": "disclaimer",
                  "funds": []
                }
                """;
        Resource goodResource = new ByteArrayResource(validJson.getBytes()) {
            @Override public String getFilename() { return "funds-config-v1.json"; }
        };

        ResourcePatternResolver mockResolver = mock(ResourcePatternResolver.class);
        when(mockResolver.getResources(anyString()))
                .thenReturn(new Resource[]{badResource, goodResource});

        FundConfigService service = new FundConfigService(mockResolver, objectMapper);
        service.init();

        // Bad resource is skipped; only the valid one is loaded
        assertThat(service.getHistory()).hasSize(1);
        assertThat(service.getHistory().get(0).getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void loadAllConfigs_emptyResourceList_resultsInEmptyHistory() throws Exception {
        ResourcePatternResolver mockResolver = mock(ResourcePatternResolver.class);
        when(mockResolver.getResources(anyString())).thenReturn(new Resource[]{});

        FundConfigService service = new FundConfigService(mockResolver, objectMapper);
        service.init();

        assertThat(service.getHistory()).isEmpty();
        assertThat(service.getActiveConfig()).isNull();
    }

    // -------------------------------------------------------------------------
    // refreshActiveConfig() — no effective config
    // -------------------------------------------------------------------------

    @Test
    void refreshActiveConfig_noEffectiveConfig_activeConfigRemainsNull() throws Exception {
        ResourcePatternResolver mockResolver = mock(ResourcePatternResolver.class);
        when(mockResolver.getResources(anyString())).thenReturn(new Resource[]{});

        FundConfigService service = new FundConfigService(mockResolver, objectMapper);
        service.init();

        // No configs loaded, so no active config
        assertThat(service.getActiveConfig()).isNull();
    }

    @Test
    void refreshActiveConfig_onlyFutureConfigs_activeConfigIsNull() throws Exception {
        ResourcePatternResolver mockResolver = mock(ResourcePatternResolver.class);
        when(mockResolver.getResources(anyString())).thenReturn(new Resource[]{});

        FundConfigService service = new FundConfigService(mockResolver, objectMapper);
        service.init();

        // Inject a config with a far-future effectiveFrom
        FundConfig futureConfig = new FundConfig();
        futureConfig.setVersion("99.0.0");
        futureConfig.setEffectiveFrom(LocalDate.of(2099, 1, 1));
        ReflectionTestUtils.setField(service, "configHistory", List.of(futureConfig));

        service.refreshActiveConfig();

        assertThat(service.getActiveConfig()).isNull();
    }

    // -------------------------------------------------------------------------
    // resolveConfigForDate() — edge cases
    // -------------------------------------------------------------------------

    @Test
    void resolveConfigForDate_exactlyOnEffectiveDate_returnsConfig() throws Exception {
        ResourcePatternResolver mockResolver = mock(ResourcePatternResolver.class);
        when(mockResolver.getResources(anyString())).thenReturn(new Resource[]{});

        FundConfigService service = new FundConfigService(mockResolver, objectMapper);
        service.init();

        FundConfig config = new FundConfig();
        config.setVersion("1.0.0");
        config.setEffectiveFrom(LocalDate.of(2025, 1, 1));
        ReflectionTestUtils.setField(service, "configHistory", List.of(config));

        FundConfig result = service.resolveConfigForDate(LocalDate.of(2025, 1, 1));
        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void resolveConfigForDate_dayBeforeEffectiveDate_returnsNull() throws Exception {
        ResourcePatternResolver mockResolver = mock(ResourcePatternResolver.class);
        when(mockResolver.getResources(anyString())).thenReturn(new Resource[]{});

        FundConfigService service = new FundConfigService(mockResolver, objectMapper);
        service.init();

        FundConfig config = new FundConfig();
        config.setVersion("1.0.0");
        config.setEffectiveFrom(LocalDate.of(2025, 1, 1));
        ReflectionTestUtils.setField(service, "configHistory", List.of(config));

        FundConfig result = service.resolveConfigForDate(LocalDate.of(2024, 12, 31));
        assertThat(result).isNull();
    }
}
