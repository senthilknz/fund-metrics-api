package com.fundmetrics.api.model.chooser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundChooserItemTest {

    // -------------------------------------------------------------------------
    // FeeDisplay
    // -------------------------------------------------------------------------

    @Test
    void feeDisplay_builder_setsAllFields() {
        FundChooserItem.FeeDisplay fee = FundChooserItem.FeeDisplay.builder()
                .value(0.85).unit("%").label("Fee")
                .description("85c per $100 of your balance per year")
                .build();

        assertThat(fee.getValue()).isEqualTo(0.85);
        assertThat(fee.getUnit()).isEqualTo("%");
        assertThat(fee.getLabel()).isEqualTo("Fee");
        assertThat(fee.getDescription()).isEqualTo("85c per $100 of your balance per year");
    }

    @Test
    void feeDisplay_equals_sameValues_areEqual() {
        FundChooserItem.FeeDisplay a = FundChooserItem.FeeDisplay.builder()
                .value(0.85).unit("%").label("Fee").description("desc").build();
        FundChooserItem.FeeDisplay b = FundChooserItem.FeeDisplay.builder()
                .value(0.85).unit("%").label("Fee").description("desc").build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void feeDisplay_equals_differentValue_areNotEqual() {
        FundChooserItem.FeeDisplay a = FundChooserItem.FeeDisplay.builder()
                .value(0.85).unit("%").label("Fee").description("desc").build();
        FundChooserItem.FeeDisplay b = FundChooserItem.FeeDisplay.builder()
                .value(0.70).unit("%").label("Fee").description("desc").build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void feeDisplay_hashCode_equalObjects_match() {
        FundChooserItem.FeeDisplay a = FundChooserItem.FeeDisplay.builder()
                .value(0.85).unit("%").label("Fee").description("desc").build();
        FundChooserItem.FeeDisplay b = FundChooserItem.FeeDisplay.builder()
                .value(0.85).unit("%").label("Fee").description("desc").build();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void feeDisplay_toString_containsValue() {
        FundChooserItem.FeeDisplay fee = FundChooserItem.FeeDisplay.builder()
                .value(0.85).unit("%").label("Fee").description("desc").build();
        assertThat(fee.toString()).contains("0.85");
    }

    // -------------------------------------------------------------------------
    // ReturnDisplay
    // -------------------------------------------------------------------------

    @Test
    void returnDisplay_builder_setsAllFields() {
        FundChooserItem.ReturnDisplay ret = FundChooserItem.ReturnDisplay.builder()
                .value(6.33).unit("%").periodValue(5).periodUnit("years")
                .label("Return").description("Estimated average annual return over 5 years")
                .build();

        assertThat(ret.getValue()).isEqualTo(6.33);
        assertThat(ret.getUnit()).isEqualTo("%");
        assertThat(ret.getPeriodValue()).isEqualTo(5);
        assertThat(ret.getPeriodUnit()).isEqualTo("years");
        assertThat(ret.getLabel()).isEqualTo("Return");
        assertThat(ret.getDescription()).isEqualTo("Estimated average annual return over 5 years");
    }

    @Test
    void returnDisplay_equals_sameValues_areEqual() {
        FundChooserItem.ReturnDisplay a = FundChooserItem.ReturnDisplay.builder()
                .value(6.33).unit("%").periodValue(5).periodUnit("years")
                .label("Return").description("desc").build();
        FundChooserItem.ReturnDisplay b = FundChooserItem.ReturnDisplay.builder()
                .value(6.33).unit("%").periodValue(5).periodUnit("years")
                .label("Return").description("desc").build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void returnDisplay_equals_differentPeriodValue_areNotEqual() {
        FundChooserItem.ReturnDisplay a = FundChooserItem.ReturnDisplay.builder()
                .value(6.33).unit("%").periodValue(5).periodUnit("years")
                .label("Return").description("desc").build();
        FundChooserItem.ReturnDisplay b = FundChooserItem.ReturnDisplay.builder()
                .value(6.33).unit("%").periodValue(3).periodUnit("years")
                .label("Return").description("desc").build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void returnDisplay_hashCode_equalObjects_match() {
        FundChooserItem.ReturnDisplay a = FundChooserItem.ReturnDisplay.builder()
                .value(6.33).unit("%").periodValue(5).periodUnit("years")
                .label("Return").description("desc").build();
        FundChooserItem.ReturnDisplay b = FundChooserItem.ReturnDisplay.builder()
                .value(6.33).unit("%").periodValue(5).periodUnit("years")
                .label("Return").description("desc").build();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void returnDisplay_toString_containsValue() {
        FundChooserItem.ReturnDisplay ret = FundChooserItem.ReturnDisplay.builder()
                .value(6.33).unit("%").periodValue(5).periodUnit("years")
                .label("Return").description("desc").build();
        assertThat(ret.toString()).contains("6.33");
    }

    // -------------------------------------------------------------------------
    // TimeframeDisplay
    // -------------------------------------------------------------------------

    @Test
    void timeframeDisplay_builder_setsAllFields() {
        FundChooserItem.TimeframeDisplay tf = FundChooserItem.TimeframeDisplay.builder()
                .value(10).unit("years").label("Time")
                .description("Recommended min. investment time")
                .build();

        assertThat(tf.getValue()).isEqualTo(10);
        assertThat(tf.getUnit()).isEqualTo("years");
        assertThat(tf.getLabel()).isEqualTo("Time");
        assertThat(tf.getDescription()).isEqualTo("Recommended min. investment time");
    }

    @Test
    void timeframeDisplay_equals_sameValues_areEqual() {
        FundChooserItem.TimeframeDisplay a = FundChooserItem.TimeframeDisplay.builder()
                .value(10).unit("years").label("Time").description("desc").build();
        FundChooserItem.TimeframeDisplay b = FundChooserItem.TimeframeDisplay.builder()
                .value(10).unit("years").label("Time").description("desc").build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void timeframeDisplay_equals_differentValue_areNotEqual() {
        FundChooserItem.TimeframeDisplay a = FundChooserItem.TimeframeDisplay.builder()
                .value(10).unit("years").label("Time").description("desc").build();
        FundChooserItem.TimeframeDisplay b = FundChooserItem.TimeframeDisplay.builder()
                .value(7).unit("years").label("Time").description("desc").build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void timeframeDisplay_hashCode_equalObjects_match() {
        FundChooserItem.TimeframeDisplay a = FundChooserItem.TimeframeDisplay.builder()
                .value(10).unit("years").label("Time").description("desc").build();
        FundChooserItem.TimeframeDisplay b = FundChooserItem.TimeframeDisplay.builder()
                .value(10).unit("years").label("Time").description("desc").build();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void timeframeDisplay_toString_containsValue() {
        FundChooserItem.TimeframeDisplay tf = FundChooserItem.TimeframeDisplay.builder()
                .value(10).unit("years").label("Time").description("desc").build();
        assertThat(tf.toString()).contains("10");
    }

    // -------------------------------------------------------------------------
    // RiskDisplay
    // -------------------------------------------------------------------------

    @Test
    void riskDisplay_builder_setsAllFields() {
        FundChooserItem.RiskDisplay risk = FundChooserItem.RiskDisplay.builder()
                .value(4).scaleMin(1).scaleMax(7).label("Risk")
                .description("How much the fund goes up and down")
                .build();

        assertThat(risk.getValue()).isEqualTo(4);
        assertThat(risk.getScaleMin()).isEqualTo(1);
        assertThat(risk.getScaleMax()).isEqualTo(7);
        assertThat(risk.getLabel()).isEqualTo("Risk");
        assertThat(risk.getDescription()).isEqualTo("How much the fund goes up and down");
    }

    @Test
    void riskDisplay_equals_sameValues_areEqual() {
        FundChooserItem.RiskDisplay a = FundChooserItem.RiskDisplay.builder()
                .value(4).scaleMin(1).scaleMax(7).label("Risk").description("desc").build();
        FundChooserItem.RiskDisplay b = FundChooserItem.RiskDisplay.builder()
                .value(4).scaleMin(1).scaleMax(7).label("Risk").description("desc").build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void riskDisplay_equals_differentValue_areNotEqual() {
        FundChooserItem.RiskDisplay a = FundChooserItem.RiskDisplay.builder()
                .value(4).scaleMin(1).scaleMax(7).label("Risk").description("desc").build();
        FundChooserItem.RiskDisplay b = FundChooserItem.RiskDisplay.builder()
                .value(5).scaleMin(1).scaleMax(7).label("Risk").description("desc").build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void riskDisplay_hashCode_equalObjects_match() {
        FundChooserItem.RiskDisplay a = FundChooserItem.RiskDisplay.builder()
                .value(4).scaleMin(1).scaleMax(7).label("Risk").description("desc").build();
        FundChooserItem.RiskDisplay b = FundChooserItem.RiskDisplay.builder()
                .value(4).scaleMin(1).scaleMax(7).label("Risk").description("desc").build();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void riskDisplay_toString_containsValue() {
        FundChooserItem.RiskDisplay risk = FundChooserItem.RiskDisplay.builder()
                .value(4).scaleMin(1).scaleMax(7).label("Risk").description("desc").build();
        assertThat(risk.toString()).contains("4");
    }

    // -------------------------------------------------------------------------
    // FundChooserItem (top-level)
    // -------------------------------------------------------------------------

    private FundChooserItem buildItem() {
        return FundChooserItem.builder()
                .id("growth").name("Growth Fund")
                .fee(FundChooserItem.FeeDisplay.builder()
                        .value(0.85).unit("%").label("Fee").description("desc").build())
                .estimatedReturn(FundChooserItem.ReturnDisplay.builder()
                        .value(6.33).unit("%").periodValue(5).periodUnit("years")
                        .label("Return").description("desc").build())
                .minInvestmentTimeframe(FundChooserItem.TimeframeDisplay.builder()
                        .value(10).unit("years").label("Time").description("desc").build())
                .riskIndicator(FundChooserItem.RiskDisplay.builder()
                        .value(4).scaleMin(1).scaleMax(7).label("Risk").description("desc").build())
                .build();
    }

    @Test
    void fundChooserItem_builder_setsAllFields() {
        FundChooserItem item = buildItem();
        assertThat(item.getId()).isEqualTo("growth");
        assertThat(item.getName()).isEqualTo("Growth Fund");
        assertThat(item.getFee()).isNotNull();
        assertThat(item.getEstimatedReturn()).isNotNull();
        assertThat(item.getMinInvestmentTimeframe()).isNotNull();
        assertThat(item.getRiskIndicator()).isNotNull();
    }

    @Test
    void fundChooserItem_equals_sameValues_areEqual() {
        assertThat(buildItem()).isEqualTo(buildItem());
    }

    @Test
    void fundChooserItem_equals_differentId_areNotEqual() {
        FundChooserItem a = buildItem();
        FundChooserItem b = FundChooserItem.builder()
                .id("balanced").name("Growth Fund")
                .fee(a.getFee()).estimatedReturn(a.getEstimatedReturn())
                .minInvestmentTimeframe(a.getMinInvestmentTimeframe())
                .riskIndicator(a.getRiskIndicator())
                .build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void fundChooserItem_hashCode_equalObjects_match() {
        assertThat(buildItem().hashCode()).isEqualTo(buildItem().hashCode());
    }

    @Test
    void fundChooserItem_toString_containsId() {
        assertThat(buildItem().toString()).contains("growth");
    }
}
