package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Chooser-page display configuration loaded from the fund config file.
 *
 * <p>Drives all labels, units, descriptions, and scale bounds shown on the fund
 * chooser card UI. Keeping these values in the JSON config means copy changes
 * (e.g. renaming "Fee" to "Annual Charge") never require a code change.
 *
 * <p>The {@code fee.description} field supports a {@code {cents}} placeholder
 * which is replaced at runtime with the computed cents-per-$100 value derived
 * from each fund's {@code annualFundCharge}.
 */
@Schema(description = "Chooser-page display configuration — all labels, descriptions, and scale bounds for the fund chooser UI")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChooserDisplay {

    /** Which return period to show on the chooser card (e.g. {@code 5years}). */
    @Schema(description = "Return period displayed on the chooser card", example = "5years")
    private ReturnPeriod returnPeriod;

    /** Display config for the annual fee metric. */
    @Schema(description = "Display config for the fee metric")
    private MetricConfig fee;

    /** Display config for the estimated return metric. */
    @Schema(description = "Display config for the estimated return metric")
    private MetricConfig returns;

    /** Display config for the minimum investment timeframe metric. */
    @Schema(description = "Display config for the timeframe metric")
    private MetricConfig timeframe;

    /** Display config for the risk indicator, including scale bounds. */
    @Schema(description = "Display config for the risk indicator, including scale bounds")
    private RiskConfig risk;

    // -------------------------------------------------------------------------
    // Nested config types
    // -------------------------------------------------------------------------

    /**
     * Label, optional unit, and description for a single chooser metric.
     *
     * <p>For the fee metric, {@code description} may contain a {@code {cents}}
     * placeholder that is replaced at runtime with the computed value.
     */
    @Schema(description = "Display strings for a chooser metric card")
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetricConfig {

        /** Short heading shown at the top of the card (e.g. {@code "Fee"}). */
        @Schema(description = "Short card heading label", example = "Fee")
        private String label;

        /**
         * Unit suffix appended to the displayed value (e.g. {@code "%"}).
         * Optional — when absent the unit is taken from the per-fund data.
         */
        @Schema(description = "Unit suffix for the displayed value", example = "%", nullable = true)
        private String unit;

        /**
         * Sub-label or tooltip copy beneath the value.
         * May contain {@code {cents}} for the fee card, replaced per fund at runtime.
         */
        @Schema(description = "Sub-label or tooltip description; fee supports {cents} placeholder",
                example = "{cents}c per $100 of your balance per year")
        private String description;
    }

    /** Label, description, and numeric scale bounds for the risk indicator card. */
    @Schema(description = "Display strings and scale bounds for the risk indicator")
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RiskConfig {

        /** Short heading shown at the top of the risk card (e.g. {@code "Risk"}). */
        @Schema(description = "Short card heading label", example = "Risk")
        private String label;

        /** Sub-label or tooltip copy describing what the risk score means. */
        @Schema(description = "Sub-label or tooltip description", example = "How much the fund goes up and down")
        private String description;

        /** Lower bound of the risk scale for gauge rendering (typically {@code 1}). */
        @Schema(description = "Lower bound of the risk scale", example = "1")
        private int scaleMin;

        /** Upper bound of the risk scale for gauge rendering (typically {@code 7}). */
        @Schema(description = "Upper bound of the risk scale", example = "7")
        private int scaleMax;
    }
}
