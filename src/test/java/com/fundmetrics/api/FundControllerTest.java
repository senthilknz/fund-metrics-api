package com.fundmetrics.api;

import com.fundmetrics.api.controller.FundController;
import com.fundmetrics.api.model.*;
import com.fundmetrics.api.model.ReturnPeriod;
import com.fundmetrics.api.service.FundConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for {@link FundController}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer — {@link FundConfigService}
 * is mocked with {@code @MockBean}. Tests cover all four endpoints including the
 * ETag/conditional-request behaviour on the active-config endpoint.
 */
@WebMvcTest(FundController.class)
class FundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FundConfigService fundConfigService;

    private FundConfig v1Config;
    private FundConfig v2Config;

    @BeforeEach
    void setUp() {
        v1Config = buildConfig("1.0.0", LocalDate.of(2025, 1, 1), Instant.parse("2025-01-01T00:00:00Z"), 12.49);
        v2Config = buildConfig("2.0.0", LocalDate.of(2025, 4, 1), Instant.parse("2025-04-01T00:00:00Z"), 13.11);
    }

    private FundConfig buildConfig(String version, LocalDate effectiveFrom, Instant publishedAt, double growth1yr) {
        FundConfig config = new FundConfig();
        config.setVersion(version);
        config.setEffectiveFrom(effectiveFrom);
        config.setPublishedAt(publishedAt);
        config.setPerformanceAsOf("2024-12-31");
        config.setDataSource("Active Series");
        config.setDisclaimer("Past performance is not a reliable indication of future performance.");

        MetricDescription feeDesc = new MetricDescription();
        feeDesc.setLabel("Annual Fund Charge");
        config.setMetricDescriptions(Map.of("fee", feeDesc));

        Fund growth = new Fund();
        growth.setId("growth");
        growth.setName("Growth Fund");
        growth.setDescription("Higher long-term capital growth.");
        FundFee fee = new FundFee();
        fee.setAnnualFundCharge(0.85);
        fee.setUnit("%");
        growth.setFee(fee);
        growth.setReturns(Map.of(ReturnPeriod.ONE_YEAR, growth1yr, ReturnPeriod.THREE_YEARS, 13.50));
        InvestmentTimeframe timeframe = new InvestmentTimeframe();
        timeframe.setValue(7);
        timeframe.setUnit("years");
        growth.setMinInvestmentTimeframe(timeframe);
        RiskIndicator risk = new RiskIndicator();
        risk.setValue(5);
        risk.setLabel("Medium to High");
        growth.setRiskIndicator(risk);
        config.setFunds(List.of(growth));
        return config;
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/funds — active config + conditional request (ETag) behaviour
    // -------------------------------------------------------------------------

    @Test
    void getActiveConfig_returns200WithBody() throws Exception {
        when(fundConfigService.getActiveConfig()).thenReturn(v2Config);

        mockMvc.perform(get("/api/v1/funds").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2.0.0"))
                .andExpect(jsonPath("$.funds[0].id").value("growth"));
    }

    @Test
    void getActiveConfig_returns200WithNoCacheHeader() throws Exception {
        when(fundConfigService.getActiveConfig()).thenReturn(v2Config);

        mockMvc.perform(get("/api/v1/funds").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    void getActiveConfig_returnsETagHeaderMatchingVersion() throws Exception {
        when(fundConfigService.getActiveConfig()).thenReturn(v2Config);

        mockMvc.perform(get("/api/v1/funds").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2.0.0\""));
    }

    @Test
    void getActiveConfig_returns304WhenETagMatches() throws Exception {
        when(fundConfigService.getActiveConfig()).thenReturn(v2Config);

        // Client sends the ETag it received on the previous request.
        mockMvc.perform(get("/api/v1/funds")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-None-Match", "\"2.0.0\""))
                .andExpect(status().isNotModified());
    }

    @Test
    void getActiveConfig_returns200WhenETagDiffers() throws Exception {
        when(fundConfigService.getActiveConfig()).thenReturn(v2Config);

        // Client holds the old v1 ETag — new config has activated, expect full 200.
        mockMvc.perform(get("/api/v1/funds")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-None-Match", "\"1.0.0\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2.0.0"));
    }

    @Test
    void getActiveConfig_returns503WhenNoConfigAvailable() throws Exception {
        when(fundConfigService.getActiveConfig()).thenReturn(null);

        mockMvc.perform(get("/api/v1/funds").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/funds/history
    // -------------------------------------------------------------------------

    @Test
    void getHistory_returnsAllVersions() throws Exception {
        when(fundConfigService.getHistory()).thenReturn(List.of(v1Config, v2Config));

        mockMvc.perform(get("/api/v1/funds/history").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].version").value("1.0.0"))
                .andExpect(jsonPath("$[1].version").value("2.0.0"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/funds/preview
    // -------------------------------------------------------------------------

    @Test
    void preview_returnsV1ForDateBeforeApril2025() throws Exception {
        when(fundConfigService.resolveConfigForDate(LocalDate.of(2025, 3, 31))).thenReturn(v1Config);

        mockMvc.perform(get("/api/v1/funds/preview")
                        .param("date", "2025-03-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void preview_returnsV2ForDateOnOrAfterApril2025() throws Exception {
        when(fundConfigService.resolveConfigForDate(LocalDate.of(2025, 4, 1))).thenReturn(v2Config);

        mockMvc.perform(get("/api/v1/funds/preview")
                        .param("date", "2025-04-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2.0.0"));
    }

    @Test
    void preview_returns400ForInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/funds/preview")
                        .param("date", "not-a-date")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preview_returns404WhenNoConfigForDate() throws Exception {
        when(fundConfigService.resolveConfigForDate(any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/funds/preview")
                        .param("date", "2020-01-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/funds/activate
    // -------------------------------------------------------------------------

    @Test
    void forceActivate_returns200OnSuccess() throws Exception {
        when(fundConfigService.forceActivateVersion("1.0.0")).thenReturn(true);

        mockMvc.perform(post("/api/v1/funds/activate")
                        .param("version", "1.0.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("1.0.0")));
    }

    @Test
    void forceActivate_returns404ForUnknownVersion() throws Exception {
        when(fundConfigService.forceActivateVersion("99.0.0")).thenReturn(false);

        mockMvc.perform(post("/api/v1/funds/activate")
                        .param("version", "99.0.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
