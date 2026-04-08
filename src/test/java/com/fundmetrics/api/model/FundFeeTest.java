package com.fundmetrics.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundFeeTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        FundFee fee = new FundFee();
        fee.setAnnualFundCharge(0.85);
        fee.setUnit("%");

        assertThat(fee.getAnnualFundCharge()).isEqualTo(0.85);
        assertThat(fee.getUnit()).isEqualTo("%");
    }

    @Test
    void equals_sameFee_areEqual() {
        FundFee a = new FundFee();
        a.setAnnualFundCharge(0.85);
        a.setUnit("%");

        FundFee b = new FundFee();
        b.setAnnualFundCharge(0.85);
        b.setUnit("%");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentCharge_areNotEqual() {
        FundFee a = new FundFee();
        a.setAnnualFundCharge(0.85);
        FundFee b = new FundFee();
        b.setAnnualFundCharge(0.70);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_equalObjects_haveSameHashCode() {
        FundFee a = new FundFee();
        a.setAnnualFundCharge(0.85);
        a.setUnit("%");
        FundFee b = new FundFee();
        b.setAnnualFundCharge(0.85);
        b.setUnit("%");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_containsFieldValues() {
        FundFee fee = new FundFee();
        fee.setAnnualFundCharge(0.85);
        fee.setUnit("%");
        assertThat(fee.toString()).contains("0.85").contains("%");
    }
}
