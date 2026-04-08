package com.fundmetrics.api;

import com.fundmetrics.api.model.ReturnPeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReturnPeriodTest {

    // -------------------------------------------------------------------------
    // fromKey() — happy path for every defined key
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "fromKey(\"{0}\") returns {1}")
    @CsvSource({
            "3months,  THREE_MONTHS",
            "6months,  SIX_MONTHS",
            "1year,    ONE_YEAR",
            "3years,   THREE_YEARS",
            "5years,   FIVE_YEARS",
            "10years,  TEN_YEARS"
    })
    void fromKey_returnsCorrectConstant(String key, String expectedName) {
        assertThat(ReturnPeriod.fromKey(key).name()).isEqualTo(expectedName);
    }

    @Test
    void fromKey_throwsIllegalArgumentForUnknownKey() {
        assertThatThrownBy(() -> ReturnPeriod.fromKey("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown return period: unknown");
    }

    @Test
    void fromKey_throwsIllegalArgumentForEmptyKey() {
        assertThatThrownBy(() -> ReturnPeriod.fromKey(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // getKey() — round-trip with fromKey
    // -------------------------------------------------------------------------

    @Test
    void fromKey_isInverseOfGetKey_forAllConstants() {
        for (ReturnPeriod period : ReturnPeriod.values()) {
            assertThat(ReturnPeriod.fromKey(period.getKey())).isEqualTo(period);
        }
    }

    @ParameterizedTest(name = "{0}.getKey() == \"{1}\"")
    @CsvSource({
            "THREE_MONTHS, 3months",
            "SIX_MONTHS,   6months",
            "ONE_YEAR,     1year",
            "THREE_YEARS,  3years",
            "FIVE_YEARS,   5years",
            "TEN_YEARS,    10years"
    })
    void getKey_returnsExpectedString(String constantName, String expectedKey) {
        assertThat(ReturnPeriod.valueOf(constantName).getKey()).isEqualTo(expectedKey);
    }

    // -------------------------------------------------------------------------
    // getPeriodValue()
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0}.getPeriodValue() == {1}")
    @CsvSource({
            "THREE_MONTHS, 3",
            "SIX_MONTHS,   6",
            "ONE_YEAR,     1",
            "THREE_YEARS,  3",
            "FIVE_YEARS,   5",
            "TEN_YEARS,    10"
    })
    void getPeriodValue_returnsCorrectValue(String constantName, int expectedValue) {
        assertThat(ReturnPeriod.valueOf(constantName).getPeriodValue()).isEqualTo(expectedValue);
    }

    // -------------------------------------------------------------------------
    // getPeriodUnit()
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0}.getPeriodUnit() == \"{1}\"")
    @CsvSource({
            "THREE_MONTHS, months",
            "SIX_MONTHS,   months",
            "ONE_YEAR,     years",
            "THREE_YEARS,  years",
            "FIVE_YEARS,   years",
            "TEN_YEARS,    years"
    })
    void getPeriodUnit_returnsCorrectUnit(String constantName, String expectedUnit) {
        assertThat(ReturnPeriod.valueOf(constantName).getPeriodUnit()).isEqualTo(expectedUnit);
    }

    // -------------------------------------------------------------------------
    // Completeness
    // -------------------------------------------------------------------------

    @Test
    void exactlySixConstantsDefined() {
        assertThat(ReturnPeriod.values()).hasSize(6);
    }

    @Test
    void allConstantsHaveNonNullKey() {
        for (ReturnPeriod period : ReturnPeriod.values()) {
            assertThat(period.getKey()).isNotNull().isNotEmpty();
        }
    }

    @Test
    void allConstantsHavePositivePeriodValue() {
        for (ReturnPeriod period : ReturnPeriod.values()) {
            assertThat(period.getPeriodValue()).isPositive();
        }
    }

    @Test
    void allConstantsHaveNonNullPeriodUnit() {
        for (ReturnPeriod period : ReturnPeriod.values()) {
            assertThat(period.getPeriodUnit()).isNotNull().isNotEmpty();
        }
    }
}
