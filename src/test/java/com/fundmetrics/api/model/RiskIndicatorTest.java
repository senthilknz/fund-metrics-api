package com.fundmetrics.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskIndicatorTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        RiskIndicator risk = new RiskIndicator();
        risk.setValue(4);
        risk.setLabel("Medium");

        assertThat(risk.getValue()).isEqualTo(4);
        assertThat(risk.getLabel()).isEqualTo("Medium");
    }

    @Test
    void equals_sameValues_areEqual() {
        RiskIndicator a = new RiskIndicator();
        a.setValue(4);
        a.setLabel("Medium");

        RiskIndicator b = new RiskIndicator();
        b.setValue(4);
        b.setLabel("Medium");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentValue_areNotEqual() {
        RiskIndicator a = new RiskIndicator();
        a.setValue(4);
        RiskIndicator b = new RiskIndicator();
        b.setValue(5);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_differentLabel_areNotEqual() {
        RiskIndicator a = new RiskIndicator();
        a.setValue(4);
        a.setLabel("Medium");
        RiskIndicator b = new RiskIndicator();
        b.setValue(4);
        b.setLabel("Medium to High");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_equalObjects_haveSameHashCode() {
        RiskIndicator a = new RiskIndicator();
        a.setValue(4);
        a.setLabel("Medium");
        RiskIndicator b = new RiskIndicator();
        b.setValue(4);
        b.setLabel("Medium");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_containsFieldValues() {
        RiskIndicator risk = new RiskIndicator();
        risk.setValue(4);
        risk.setLabel("Medium");
        assertThat(risk.toString()).contains("4").contains("Medium");
    }
}
