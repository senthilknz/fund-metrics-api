package com.fundmetrics.api.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FundTest {

    private Fund fund;

    @BeforeEach
    void setUp() {
        FundFee fee = new FundFee();
        fee.setAnnualFundCharge(0.85);
        fee.setUnit("%");

        InvestmentTimeframe timeframe = new InvestmentTimeframe();
        timeframe.setValue(10);
        timeframe.setUnit("years");

        RiskIndicator risk = new RiskIndicator();
        risk.setValue(4);
        risk.setLabel("Medium");

        FundReturns returns = new FundReturns();
        returns.setValues(Map.of(ReturnPeriod.FIVE_YEARS, 6.33, ReturnPeriod.ONE_YEAR, 13.11));

        fund = new Fund();
        fund.setId("growth");
        fund.setName("Growth Fund");
        fund.setDescription("Higher long-term capital growth.");
        fund.setFee(fee);
        fund.setReturns(returns);
        fund.setMinInvestmentTimeframe(timeframe);
        fund.setRiskIndicator(risk);
    }

    @Test
    void gettersReturnCorrectValues() {
        assertThat(fund.getId()).isEqualTo("growth");
        assertThat(fund.getName()).isEqualTo("Growth Fund");
        assertThat(fund.getDescription()).isEqualTo("Higher long-term capital growth.");
        assertThat(fund.getFee().getAnnualFundCharge()).isEqualTo(0.85);
        assertThat(fund.getReturns().getValues().get(ReturnPeriod.FIVE_YEARS)).isEqualTo(6.33);
        assertThat(fund.getReturns().getValues().get(ReturnPeriod.ONE_YEAR)).isEqualTo(13.11);
        assertThat(fund.getMinInvestmentTimeframe().getValue()).isEqualTo(10);
        assertThat(fund.getRiskIndicator().getValue()).isEqualTo(4);
    }

    @Test
    void equals_sameFund_areEqual() {
        FundFee fee = new FundFee();
        fee.setAnnualFundCharge(0.85);
        fee.setUnit("%");
        InvestmentTimeframe tf = new InvestmentTimeframe();
        tf.setValue(10);
        tf.setUnit("years");
        RiskIndicator risk = new RiskIndicator();
        risk.setValue(4);
        risk.setLabel("Medium");

        Fund other = new Fund();
        other.setId("growth");
        other.setName("Growth Fund");
        other.setDescription("Higher long-term capital growth.");
        other.setFee(fee);
        FundReturns otherReturns = new FundReturns();
        otherReturns.setValues(Map.of(ReturnPeriod.FIVE_YEARS, 6.33, ReturnPeriod.ONE_YEAR, 13.11));
        other.setReturns(otherReturns);
        other.setMinInvestmentTimeframe(tf);
        other.setRiskIndicator(risk);

        assertThat(fund).isEqualTo(other);
    }

    @Test
    void equals_differentId_areNotEqual() {
        Fund other = new Fund();
        other.setId("balanced");
        assertThat(fund).isNotEqualTo(other);
    }

    @Test
    void hashCode_equalFunds_haveSameHashCode() {
        Fund other = new Fund();
        other.setId("growth");
        other.setName("Growth Fund");
        other.setDescription("Higher long-term capital growth.");
        other.setFee(fund.getFee());
        other.setReturns(fund.getReturns());
        other.setMinInvestmentTimeframe(fund.getMinInvestmentTimeframe());
        other.setRiskIndicator(fund.getRiskIndicator());
        assertThat(fund.hashCode()).isEqualTo(other.hashCode());
    }

    @Test
    void toString_containsFundId() {
        assertThat(fund.toString()).contains("growth");
    }
}
