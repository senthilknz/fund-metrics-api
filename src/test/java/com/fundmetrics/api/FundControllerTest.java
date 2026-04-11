package com.fundmetrics.api;

import com.fundmetrics.api.controller.FundController;
import com.fundmetrics.api.model.*;
import com.fundmetrics.api.model.chooser.FundChooserItem;
import com.fundmetrics.api.model.chooser.FundChooserResponse;
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
        v2Config = buildConfig("2025.04.01", LocalDate.of(2025, 4, 1), Instant.parse("2025-04-01T00:00:00Z"), 13.11);
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
                .andExpect(jsonPath("$.version").value("2025.04.01"))
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
                .andExpect(header().string("ETag", "\"2025.04.01\""));
    }

    @Test
    void getActiveConfig_returns304WhenETagMatches() throws Exception {
        when(fundConfigService.getActiveConfig()).thenReturn(v2Config);

        // Client sends the ETag it received on the previous request.
        mockMvc.perform(get("/api/v1/funds")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-None-Match", "\"2025.04.01\""))
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
                .andExpect(jsonPath("$.version").value("2025.04.01"));
    }

    @Test
    void getActiveConfig_returns503WhenNoConfigAvailable() throws Exception {
        when(fundConfigService.getActiveConfig()).thenReturn(null);

        mockMvc.perform(get("/api/v1/funds").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/funds/chooser
    // -------------------------------------------------------------------------

    private FundChooserResponse buildChooserResponse() {
        return FundChooserResponse.builder()
                .disclaimer("Past performance is not a reliable indication of future performance.")
                .performanceAsOf("2025-03-31")
                .funds(List.of(
                        FundChooserItem.builder()
                                .id("growth").name("Growth Fund")
                                .fee(FundChooserItem.FeeDisplay.builder()
                                        .value(0.85).unit("%").label("Fee")
                                        .description("85c per $100 of your balance per year").build())
                                .estimatedReturn(FundChooserItem.ReturnDisplay.builder()
                                        .value(6.33).unit("%").periodValue(5).periodUnit("years")
                                        .label("Return")
                                        .description("Estimated average annual return over 5 years").build())
                                .minInvestmentTimeframe(FundChooserItem.TimeframeDisplay.builder()
                                        .value(10).unit("years").label("Time")
                                        .description("Recommended min. investment time").build())
                                .riskIndicator(FundChooserItem.RiskDisplay.builder()
                                        .value(4).scaleMin(1).scaleMax(7).label("Risk")
                                        .description("How much the fund goes up and down").build())
                                .build()
                ))
                .build();
    }

    @Test
    void getChooser_returns200WithFundData() throws Exception {
        when(fundConfigService.toChooserResponse()).thenReturn(buildChooserResponse());

        mockMvc.perform(get("/api/v1/funds/chooser").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disclaimer").isNotEmpty())
                .andExpect(jsonPath("$.performanceAsOf").value("2025-03-31"))
                .andExpect(jsonPath("$.funds", hasSize(1)))
                .andExpect(jsonPath("$.funds[0].id").value("growth"))
                .andExpect(jsonPath("$.funds[0].name").value("Growth Fund"))
                .andExpect(jsonPath("$.funds[0].fee.value").value(0.85))
                .andExpect(jsonPath("$.funds[0].fee.unit").value("%"))
                .andExpect(jsonPath("$.funds[0].fee.label").value("Fee"))
                .andExpect(jsonPath("$.funds[0].fee.description").value("85c per $100 of your balance per year"))
                .andExpect(jsonPath("$.funds[0].estimatedReturn.value").value(6.33))
                .andExpect(jsonPath("$.funds[0].estimatedReturn.unit").value("%"))
                .andExpect(jsonPath("$.funds[0].estimatedReturn.periodValue").value(5))
                .andExpect(jsonPath("$.funds[0].estimatedReturn.periodUnit").value("years"))
                .andExpect(jsonPath("$.funds[0].estimatedReturn.label").value("Return"))
                .andExpect(jsonPath("$.funds[0].estimatedReturn.description")
                        .value("Estimated average annual return over 5 years"))
                .andExpect(jsonPath("$.funds[0].minInvestmentTimeframe.value").value(10))
                .andExpect(jsonPath("$.funds[0].minInvestmentTimeframe.unit").value("years"))
                .andExpect(jsonPath("$.funds[0].minInvestmentTimeframe.label").value("Time"))
                .andExpect(jsonPath("$.funds[0].minInvestmentTimeframe.description")
                        .value("Recommended min. investment time"))
                .andExpect(jsonPath("$.funds[0].riskIndicator.value").value(4))
                .andExpect(jsonPath("$.funds[0].riskIndicator.scaleMin").value(1))
                .andExpect(jsonPath("$.funds[0].riskIndicator.scaleMax").value(7))
                .andExpect(jsonPath("$.funds[0].riskIndicator.label").value("Risk"))
                .andExpect(jsonPath("$.funds[0].riskIndicator.description")
                        .value("How much the fund goes up and down"));
    }

    @Test
    void getChooser_returns503WhenNoActiveConfig() throws Exception {
        when(fundConfigService.toChooserResponse()).thenReturn(null);

        mockMvc.perform(get("/api/v1/funds/chooser").accept(MediaType.APPLICATION_JSON))
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
                .andExpect(jsonPath("$[1].version").value("2025.04.01"));
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
                .andExpect(jsonPath("$.version").value("2025.04.01"));
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
