package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum representing the standard return period labels used in fund config files.
 *
 * <p>Each constant carries the JSON key used in config files (e.g. {@code "5years"}),
 * a numeric {@code periodValue}, and a {@code periodUnit} string so consumers never
 * need to parse the key string themselves.
 *
 * <p>Jackson uses {@link #getKey()} for serialisation and {@link #fromKey(String)}
 * for deserialisation — both as map keys and as list elements — meaning the config
 * JSON files require no structural changes.
 */
@Schema(description = "Standard return period label", example = "5years",
        allowableValues = {"3months", "6months", "1year", "3years", "5years", "10years"})
public enum ReturnPeriod {

    THREE_MONTHS("3months",  3,  "months"),
    SIX_MONTHS  ("6months",  6,  "months"),
    ONE_YEAR    ("1year",    1,  "years"),
    THREE_YEARS ("3years",   3,  "years"),
    FIVE_YEARS  ("5years",   5,  "years"),
    TEN_YEARS   ("10years",  10, "years");

    private final String key;
    private final int periodValue;
    private final String periodUnit;

    ReturnPeriod(String key, int periodValue, String periodUnit) {
        this.key = key;
        this.periodValue = periodValue;
        this.periodUnit = periodUnit;
    }

    /** JSON key used in config files and API responses (e.g. {@code "5years"}). */
    @JsonValue
    public String getKey() {
        return key;
    }

    /** Numeric length of the period (e.g. {@code 5}). */
    public int getPeriodValue() {
        return periodValue;
    }

    /** Unit of the period (e.g. {@code "years"}, {@code "months"}). */
    public String getPeriodUnit() {
        return periodUnit;
    }

    /**
     * Deserialises a period from its JSON key string.
     *
     * @param key the string key from JSON (e.g. {@code "5years"})
     * @return the matching enum constant
     * @throws IllegalArgumentException if the key is not recognised
     */
    @JsonCreator
    public static ReturnPeriod fromKey(String key) {
        for (ReturnPeriod period : values()) {
            if (period.key.equals(key)) {
                return period;
            }
        }
        throw new IllegalArgumentException("Unknown return period: " + key);
    }
}
