package com.fundmetrics.api.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricDescriptionTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        MetricDescription desc = new MetricDescription();
        desc.setLabel("Fee");
        desc.setDescription("{cents}c per $100 of your balance per year");
        desc.setUnit("%");
        desc.setScale("1 (lowest) to 7 (highest)");
        desc.setPeriods(List.of(ReturnPeriod.THREE_MONTHS, ReturnPeriod.FIVE_YEARS));
        desc.setChooserPeriod(ReturnPeriod.FIVE_YEARS);
        desc.setScaleMin(1);
        desc.setScaleMax(7);

        assertThat(desc.getLabel()).isEqualTo("Fee");
        assertThat(desc.getDescription()).isEqualTo("{cents}c per $100 of your balance per year");
        assertThat(desc.getUnit()).isEqualTo("%");
        assertThat(desc.getScale()).isEqualTo("1 (lowest) to 7 (highest)");
        assertThat(desc.getPeriods()).containsExactly(ReturnPeriod.THREE_MONTHS, ReturnPeriod.FIVE_YEARS);
        assertThat(desc.getChooserPeriod()).isEqualTo(ReturnPeriod.FIVE_YEARS);
        assertThat(desc.getScaleMin()).isEqualTo(1);
        assertThat(desc.getScaleMax()).isEqualTo(7);
        // tooltip and tooltipLink are per-fund fields (on Fund / RiskIndicator), not on MetricDescription
    }

    @Test
    void optionalFields_areNullByDefault() {
        MetricDescription desc = new MetricDescription();
        assertThat(desc.getScale()).isNull();
        assertThat(desc.getPeriods()).isNull();
        assertThat(desc.getChooserPeriod()).isNull();
    }

    @Test
    void equals_sameValues_areEqual() {
        MetricDescription a = new MetricDescription();
        a.setLabel("Fee");
        a.setDescription("desc");

        MetricDescription b = new MetricDescription();
        b.setLabel("Fee");
        b.setDescription("desc");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentLabel_areNotEqual() {
        MetricDescription a = new MetricDescription();
        a.setLabel("Fee");
        MetricDescription b = new MetricDescription();
        b.setLabel("Return");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_equalObjects_haveSameHashCode() {
        MetricDescription a = new MetricDescription();
        a.setLabel("Fee");
        MetricDescription b = new MetricDescription();
        b.setLabel("Fee");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_containsLabel() {
        MetricDescription desc = new MetricDescription();
        desc.setLabel("Annual Fund Charge");
        assertThat(desc.toString()).contains("Annual Fund Charge");
    }
}
