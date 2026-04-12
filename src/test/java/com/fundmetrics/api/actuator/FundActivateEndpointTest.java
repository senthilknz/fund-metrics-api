package com.fundmetrics.api.actuator;

import com.fundmetrics.api.service.FundConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FundActivateEndpointTest {

    private FundConfigService fundConfigService;
    private FundActivateEndpoint endpoint;

    @BeforeEach
    void setUp() {
        fundConfigService = mock(FundConfigService.class);
        endpoint = new FundActivateEndpoint(fundConfigService);
    }

    @Test
    void activate_knownVersion_returnsSuccessTrue() {
        when(fundConfigService.forceActivateVersion("2025.04.01")).thenReturn(true);

        Map<String, Object> result = endpoint.activate("2025.04.01");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("message").toString()).contains("2025.04.01");
        verify(fundConfigService).forceActivateVersion("2025.04.01");
    }

    @Test
    void activate_unknownVersion_returnsSuccessFalse() {
        when(fundConfigService.forceActivateVersion("9999.99.99")).thenReturn(false);

        Map<String, Object> result = endpoint.activate("9999.99.99");

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message").toString()).contains("9999.99.99");
    }
}
