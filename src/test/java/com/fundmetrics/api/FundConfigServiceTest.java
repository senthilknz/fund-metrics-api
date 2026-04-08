package com.fundmetrics.api;

import com.fundmetrics.api.model.FundConfig;
import com.fundmetrics.api.model.ReturnPeriod;
import com.fundmetrics.api.service.FundConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FundConfigServiceTest {

    @Autowired
    private FundConfigService fundConfigService;

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
