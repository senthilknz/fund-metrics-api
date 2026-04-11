package com.fundmetrics.api.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FundConfigTest {

    private FundConfig config;

    @BeforeEach
    void setUp() {
        config = new FundConfig();
        config.setVersion("2025.04.01");
        config.setEffectiveFrom(LocalDate.of(2025, 4, 1));
        config.setPublishedAt(Instant.parse("2025-04-01T00:00:00Z"));
        config.setPerformanceAsOf("2025-03-31");
        config.setDataSource("Active Series");
        config.setDisclaimer("Past performance is not a reliable indication of future performance.");

        MetricDescription feeDesc = new MetricDescription();
        feeDesc.setLabel("Annual Fund Charge");
        config.setMetricDescriptions(Map.of("fee", feeDesc));

        Fund fund = new Fund();
        fund.setId("growth");
        config.setFunds(List.of(fund));
    }

    @Test
    void gettersReturnCorrectValues() {
        assertThat(config.getVersion()).isEqualTo("2025.04.01");
        assertThat(config.getEffectiveFrom()).isEqualTo(LocalDate.of(2025, 4, 1));
        assertThat(config.getPublishedAt()).isEqualTo(Instant.parse("2025-04-01T00:00:00Z"));
        assertThat(config.getPerformanceAsOf()).isEqualTo("2025-03-31");
        assertThat(config.getDataSource()).isEqualTo("Active Series");
        assertThat(config.getDisclaimer()).contains("Past performance");
        assertThat(config.getMetricDescriptions()).containsKey("fee");
        assertThat(config.getFunds()).hasSize(1);
        assertThat(config.getFunds().get(0).getId()).isEqualTo("growth");
    }

    @Test
    void equals_sameConfig_areEqual() {
        FundConfig other = new FundConfig();
        other.setVersion("2025.04.01");
        other.setEffectiveFrom(LocalDate.of(2025, 4, 1));
        other.setPublishedAt(Instant.parse("2025-04-01T00:00:00Z"));
        other.setPerformanceAsOf("2025-03-31");
        other.setDataSource("Active Series");
        other.setDisclaimer("Past performance is not a reliable indication of future performance.");
        other.setMetricDescriptions(config.getMetricDescriptions());
        other.setFunds(config.getFunds());

        assertThat(config).isEqualTo(other);
    }

    @Test
    void equals_differentVersion_areNotEqual() {
        FundConfig other = new FundConfig();
        other.setVersion("1.0.0");
        assertThat(config).isNotEqualTo(other);
    }

    @Test
    void hashCode_equalObjects_haveSameHashCode() {
        FundConfig other = new FundConfig();
        other.setVersion("2025.04.01");
        other.setEffectiveFrom(LocalDate.of(2025, 4, 1));
        other.setPublishedAt(Instant.parse("2025-04-01T00:00:00Z"));
        other.setPerformanceAsOf("2025-03-31");
        other.setDataSource("Active Series");
        other.setDisclaimer("Past performance is not a reliable indication of future performance.");
        other.setMetricDescriptions(config.getMetricDescriptions());
        other.setFunds(config.getFunds());
        assertThat(config.hashCode()).isEqualTo(other.hashCode());
    }

    @Test
    void toString_containsVersion() {
        assertThat(config.toString()).contains("2025.04.01");
    }
}
