package com.fundmetrics.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentTimeframeTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        InvestmentTimeframe tf = new InvestmentTimeframe();
        tf.setValue(10);
        tf.setUnit("years");

        assertThat(tf.getValue()).isEqualTo(10);
        assertThat(tf.getUnit()).isEqualTo("years");
    }

    @Test
    void equals_sameValues_areEqual() {
        InvestmentTimeframe a = new InvestmentTimeframe();
        a.setValue(10);
        a.setUnit("years");

        InvestmentTimeframe b = new InvestmentTimeframe();
        b.setValue(10);
        b.setUnit("years");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentValue_areNotEqual() {
        InvestmentTimeframe a = new InvestmentTimeframe();
        a.setValue(10);
        InvestmentTimeframe b = new InvestmentTimeframe();
        b.setValue(7);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_equalObjects_haveSameHashCode() {
        InvestmentTimeframe a = new InvestmentTimeframe();
        a.setValue(10);
        a.setUnit("years");
        InvestmentTimeframe b = new InvestmentTimeframe();
        b.setValue(10);
        b.setUnit("years");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_containsFieldValues() {
        InvestmentTimeframe tf = new InvestmentTimeframe();
        tf.setValue(10);
        tf.setUnit("years");
        assertThat(tf.toString()).contains("10").contains("years");
    }
}
