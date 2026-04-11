package com.fundmetrics.api;

import com.fundmetrics.api.model.FundConfig;
import com.fundmetrics.api.model.MetricDescription;
import com.fundmetrics.api.model.ReturnPeriod;
import com.fundmetrics.api.model.chooser.FundChooserItem;
import com.fundmetrics.api.model.chooser.FundChooserResponse;
import com.fundmetrics.api.service.FundConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FundConfigServiceTest {

    @Autowired
    private FundConfigService fundConfigService;

    @BeforeEach
    void ensureV2IsActive() {
        fundConfigService.forceActivateVersion("2.0.0");
    }

    @Test
    void bothConfigFilesLoad() {
        List<FundConfig> history = fundConfigService.getHistory();
        assertThat(history).hasSize(2);
        assertThat(history).extracting(FundConfig::getVersion)
                .containsExactlyInAnyOrder("1.0.0", "2.0.0");
    }

    @Test
    void historyIsSortedByEffectiveFromAscending() {
        List<FundConfig> history = fundConfigService.getHistory();
        assertThat(history.get(0).getVersion()).isEqualTo("1.0.0");
        assertThat(history.get(1).getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void v1IsActiveBeforeApril2025() {
        FundConfig config = fundConfigService.resolveConfigForDate(LocalDate.of(2025, 3, 31));
        assertThat(config).isNotNull();
        assertThat(config.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void v2IsActiveOnApril2025() {
        FundConfig config = fundConfigService.resolveConfigForDate(LocalDate.of(2025, 4, 1));
        assertThat(config).isNotNull();
        assertThat(config.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void v2IsActiveAfterApril2025() {
        FundConfig config = fundConfigService.resolveConfigForDate(LocalDate.of(2025, 12, 31));
        assertThat(config).isNotNull();
        assertThat(config.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void noConfigBeforeV1EffectiveDate() {
        FundConfig config = fundConfigService.resolveConfigForDate(LocalDate.of(2024, 12, 31));
        assertThat(config).isNull();
    }

    @Test
    void forceActivateVersionSwitchesToV1() {
        boolean result = fundConfigService.forceActivateVersion("1.0.0");
        assertThat(result).isTrue();
        assertThat(fundConfigService.getActiveConfig().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void forceActivateVersionSwitchesToV2() {
        boolean result = fundConfigService.forceActivateVersion("2.0.0");
        assertThat(result).isTrue();
        assertThat(fundConfigService.getActiveConfig().getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void forceActivateUnknownVersionReturnsFalse() {
        boolean result = fundConfigService.forceActivateVersion("99.0.0");
        assertThat(result).isFalse();
    }

    @Test
    void eachFundHasExpectedFields() {
        List<FundConfig> history = fundConfigService.getHistory();
        history.forEach(config ->
                config.getFunds().forEach(fund -> {
                    assertThat(fund.getId()).isNotBlank();
                    assertThat(fund.getName()).isNotBlank();
                    assertThat(fund.getFee()).isNotNull();
                    assertThat(fund.getReturns()).isNotEmpty();
                    assertThat(fund.getMinInvestmentTimeframe()).isNotNull();
                    assertThat(fund.getRiskIndicator()).isNotNull();
                })
        );
    }

    // -------------------------------------------------------------------------
    // toChooserResponse()
    // -------------------------------------------------------------------------

    @Test
    void toChooserResponse_returnsNullWhenActiveConfigIsNull() {
        ReflectionTestUtils.setField(fundConfigService, "activeConfig", null);
        assertThat(fundConfigService.toChooserResponse()).isNull();
    }

    @Test
    void toChooserResponse_returnsNonNull() {
        assertThat(fundConfigService.toChooserResponse()).isNotNull();
    }

    @Test
    void toChooserResponse_hasDisclaimer() {
        FundChooserResponse response = fundConfigService.toChooserResponse();
        assertThat(response.getDisclaimer())
                .contains("Past performance is not a reliable indication of future performance.");
    }

    @Test
    void toChooserResponse_hasFooterNotes() {
        FundChooserResponse response = fundConfigService.toChooserResponse();
        assertThat(response.getFooterNotes()).isNotEmpty();
        assertThat(response.getFooterNotes()).extracting("key")
                .contains("sustainability", "return", "riaa-certification");
    }

    @Test
    void toChooserResponse_v2HasRiskFooterNote() {
        FundChooserResponse response = fundConfigService.toChooserResponse();
        assertThat(response.getFooterNotes()).extracting("key").contains("risk");
    }

    @Test
    void toChooserResponse_hasPerformanceAsOf() {
        FundChooserResponse response = fundConfigService.toChooserResponse();
        assertThat(response.getPerformanceAsOf()).isEqualTo("2025-03-31");
    }

    @Test
    void toChooserResponse_returnsAllFourFunds() {
        FundChooserResponse response = fundConfigService.toChooserResponse();
        assertThat(response.getFunds()).hasSize(4);
    }

    @Test
    void toChooserResponse_fundsAreInExpectedOrder() {
        List<FundChooserItem> funds = fundConfigService.toChooserResponse().getFunds();
        assertThat(funds).extracting(FundChooserItem::getId)
                .containsExactly("growth", "balanced", "moderate", "conservative");
    }

    @Test
    void toChooserResponse_feeValueAndUnitAreCorrect() {
        List<FundChooserItem> funds = fundConfigService.toChooserResponse().getFunds();
        assertThat(funds.get(0).getFee().getValue()).isEqualTo(0.85);
        assertThat(funds.get(1).getFee().getValue()).isEqualTo(0.80);
        assertThat(funds.get(2).getFee().getValue()).isEqualTo(0.70);
        assertThat(funds.get(3).getFee().getValue()).isEqualTo(0.70);
        funds.forEach(f -> assertThat(f.getFee().getUnit()).isEqualTo("%"));
    }

    @Test
    void toChooserResponse_feeCentsDescriptionComputedCorrectly() {
        List<FundChooserItem> funds = fundConfigService.toChooserResponse().getFunds();
        assertThat(funds.get(0).getFee().getDescription()).isEqualTo("85c per $100 of your balance per year");
        assertThat(funds.get(1).getFee().getDescription()).isEqualTo("80c per $100 of your balance per year");
        assertThat(funds.get(2).getFee().getDescription()).isEqualTo("70c per $100 of your balance per year");
        assertThat(funds.get(3).getFee().getDescription()).isEqualTo("70c per $100 of your balance per year");
    }

    @Test
    void toChooserResponse_feeLabelIsCorrect() {
        fundConfigService.toChooserResponse().getFunds()
                .forEach(f -> assertThat(f.getFee().getLabel()).isEqualTo("Fee"));
    }

    @Test
    void toChooserResponse_estimatedReturnUsesFiveYearValue() {
        List<FundChooserItem> funds = fundConfigService.toChooserResponse().getFunds();
        assertThat(funds.get(0).getEstimatedReturn().getValue()).isEqualTo(6.33);
        assertThat(funds.get(1).getEstimatedReturn().getValue()).isEqualTo(5.04);
        assertThat(funds.get(2).getEstimatedReturn().getValue()).isEqualTo(3.81);
        assertThat(funds.get(3).getEstimatedReturn().getValue()).isEqualTo(2.60);
    }

    @Test
    void toChooserResponse_estimatedReturnPeriodIsCorrect() {
        fundConfigService.toChooserResponse().getFunds().forEach(f -> {
            assertThat(f.getEstimatedReturn().getPeriodValue()).isEqualTo(5);
            assertThat(f.getEstimatedReturn().getPeriodUnit()).isEqualTo("years");
            assertThat(f.getEstimatedReturn().getUnit()).isEqualTo("%");
        });
    }

    @Test
    void toChooserResponse_estimatedReturnLabelsAreCorrect() {
        fundConfigService.toChooserResponse().getFunds().forEach(f -> {
            assertThat(f.getEstimatedReturn().getLabel()).isEqualTo("Return");
            assertThat(f.getEstimatedReturn().getDescription())
                    .isEqualTo("Estimated average annual return over 5 years");
        });
    }

    @Test
    void toChooserResponse_timeframeValuesAreCorrect() {
        List<FundChooserItem> funds = fundConfigService.toChooserResponse().getFunds();
        assertThat(funds.get(0).getMinInvestmentTimeframe().getValue()).isEqualTo(10);
        assertThat(funds.get(1).getMinInvestmentTimeframe().getValue()).isEqualTo(7);
        assertThat(funds.get(2).getMinInvestmentTimeframe().getValue()).isEqualTo(5);
        assertThat(funds.get(3).getMinInvestmentTimeframe().getValue()).isEqualTo(3);
    }

    @Test
    void toChooserResponse_timeframeLabelsAreCorrect() {
        fundConfigService.toChooserResponse().getFunds().forEach(f -> {
            assertThat(f.getMinInvestmentTimeframe().getUnit()).isEqualTo("years");
            assertThat(f.getMinInvestmentTimeframe().getLabel()).isEqualTo("Time");
            assertThat(f.getMinInvestmentTimeframe().getDescription())
                    .isEqualTo("Recommended min. investment time");
        });
    }

    @Test
    void toChooserResponse_riskIndicatorValuesAreCorrect() {
        List<FundChooserItem> funds = fundConfigService.toChooserResponse().getFunds();
        assertThat(funds.get(0).getRiskIndicator().getValue()).isEqualTo(4);
        assertThat(funds.get(1).getRiskIndicator().getValue()).isEqualTo(4);
        assertThat(funds.get(2).getRiskIndicator().getValue()).isEqualTo(4);
        assertThat(funds.get(3).getRiskIndicator().getValue()).isEqualTo(3);
    }

    @Test
    void toChooserResponse_riskScaleBoundsAreCorrect() {
        fundConfigService.toChooserResponse().getFunds().forEach(f -> {
            assertThat(f.getRiskIndicator().getScaleMin()).isEqualTo(1);
            assertThat(f.getRiskIndicator().getScaleMax()).isEqualTo(7);
        });
    }

    @Test
    void toChooserResponse_riskLabelsAreCorrect() {
        fundConfigService.toChooserResponse().getFunds().forEach(f -> {
            assertThat(f.getRiskIndicator().getLabel()).isEqualTo("Risk");
            assertThat(f.getRiskIndicator().getDescription()).isEqualTo("How much the fund goes up and down");
        });
    }

    @Test
    void toChooserResponse_missingFiveYearReturnDefaultsToZero() {
        // Build a minimal config with a fund that has no 5-year return
        FundConfig sparseConfig = new FundConfig();
        sparseConfig.setVersion("test");
        sparseConfig.setPerformanceAsOf("2025-03-31");
        sparseConfig.setDisclaimer("disclaimer");
        com.fundmetrics.api.model.Fund fund = new com.fundmetrics.api.model.Fund();
        fund.setId("test");
        fund.setName("Test Fund");
        com.fundmetrics.api.model.FundFee fee = new com.fundmetrics.api.model.FundFee();
        fee.setAnnualFundCharge(0.50);
        fee.setUnit("%");
        fund.setFee(fee);
        fund.setReturns(java.util.Map.of(ReturnPeriod.ONE_YEAR, 5.0)); // no 5-year entry
        com.fundmetrics.api.model.InvestmentTimeframe tf = new com.fundmetrics.api.model.InvestmentTimeframe();
        tf.setValue(3);
        tf.setUnit("years");
        fund.setMinInvestmentTimeframe(tf);
        com.fundmetrics.api.model.RiskIndicator risk = new com.fundmetrics.api.model.RiskIndicator();
        risk.setValue(2);
        risk.setLabel("Low");
        fund.setRiskIndicator(risk);
        sparseConfig.setFunds(List.of(fund));

        MetricDescription feeDesc = new MetricDescription();
        feeDesc.setLabel("Fee");
        feeDesc.setDescription("{cents}c per $100 of your balance per year");

        MetricDescription returnsDesc = new MetricDescription();
        returnsDesc.setUnit("%");
        returnsDesc.setLabel("Return");
        returnsDesc.setDescription("Estimated average annual return over 5 years");
        returnsDesc.setChooserPeriod(ReturnPeriod.FIVE_YEARS);

        MetricDescription timeframeDesc = new MetricDescription();
        timeframeDesc.setLabel("Time");
        timeframeDesc.setDescription("Recommended min. investment time");

        MetricDescription riskDesc = new MetricDescription();
        riskDesc.setLabel("Risk");
        riskDesc.setDescription("How much the fund goes up and down");
        riskDesc.setScaleMin(1);
        riskDesc.setScaleMax(7);

        sparseConfig.setMetricDescriptions(java.util.Map.of(
                "fee", feeDesc,
                "returns", returnsDesc,
                "minInvestmentTimeframe", timeframeDesc,
                "riskIndicator", riskDesc));

        ReflectionTestUtils.setField(fundConfigService, "activeConfig", sparseConfig);
        FundChooserResponse response = fundConfigService.toChooserResponse();
        assertThat(response.getFunds().get(0).getEstimatedReturn().getValue()).isEqualTo(0.0);
    }

    // -------------------------------------------------------------------------

    @Test
    void returnsValuesAreDifferentBetweenVersions() {
        FundConfig v1 = fundConfigService.getHistory().get(0);
        FundConfig v2 = fundConfigService.getHistory().get(1);
        double v1Growth1yr = v1.getFunds().stream()
                .filter(f -> "growth".equals(f.getId())).findFirst().orElseThrow()
                .getReturns().get(ReturnPeriod.ONE_YEAR);
        double v2Growth1yr = v2.getFunds().stream()
                .filter(f -> "growth".equals(f.getId())).findFirst().orElseThrow()
                .getReturns().get(ReturnPeriod.ONE_YEAR);
        assertThat(v1Growth1yr).isNotEqualTo(v2Growth1yr);
    }
}
