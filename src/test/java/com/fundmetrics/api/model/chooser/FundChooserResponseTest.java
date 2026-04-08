package com.fundmetrics.api.model.chooser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FundChooserResponseTest {

    private FundChooserResponse buildResponse() {
        return FundChooserResponse.builder()
                .disclaimer("Past performance is not a reliable indication of future performance.")
                .performanceAsOf("2025-03-31")
                .funds(List.of())
                .build();
    }

    @Test
    void builder_setsAllFields() {
        FundChooserResponse response = buildResponse();
        assertThat(response.getDisclaimer())
                .isEqualTo("Past performance is not a reliable indication of future performance.");
        assertThat(response.getPerformanceAsOf()).isEqualTo("2025-03-31");
        assertThat(response.getFunds()).isEmpty();
    }

    @Test
    void builder_withFunds_setsCorrectly() {
        FundChooserItem item = FundChooserItem.builder()
                .id("growth").name("Growth Fund")
                .fee(FundChooserItem.FeeDisplay.builder()
                        .value(0.85).unit("%").label("Fee")
                        .description("85c per $100 of your balance per year").build())
                .estimatedReturn(FundChooserItem.ReturnDisplay.builder()
                        .value(6.33).unit("%").periodValue(5).periodUnit("years")
                        .label("Return").description("Estimated average annual return over 5 years").build())
                .minInvestmentTimeframe(FundChooserItem.TimeframeDisplay.builder()
                        .value(10).unit("years").label("Time")
                        .description("Recommended min. investment time").build())
                .riskIndicator(FundChooserItem.RiskDisplay.builder()
                        .value(4).scaleMin(1).scaleMax(7).label("Risk")
                        .description("How much the fund goes up and down").build())
                .build();

        FundChooserResponse response = FundChooserResponse.builder()
                .disclaimer("disclaimer")
                .performanceAsOf("2025-03-31")
                .funds(List.of(item))
                .build();

        assertThat(response.getFunds()).hasSize(1);
        assertThat(response.getFunds().get(0).getId()).isEqualTo("growth");
    }

    @Test
    void equals_sameValues_areEqual() {
        FundChooserResponse a = buildResponse();
        FundChooserResponse b = buildResponse();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentDisclaimer_areNotEqual() {
        FundChooserResponse a = buildResponse();
        FundChooserResponse b = FundChooserResponse.builder()
                .disclaimer("different")
                .performanceAsOf("2025-03-31")
                .funds(List.of())
                .build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_equalObjects_haveSameHashCode() {
        assertThat(buildResponse().hashCode()).isEqualTo(buildResponse().hashCode());
    }

    @Test
    void toString_containsPerformanceAsOf() {
        assertThat(buildResponse().toString()).contains("2025-03-31");
    }
}
