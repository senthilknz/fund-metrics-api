package com.fundmetrics.api.model.chooser;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * A single fund entry shaped for the fund chooser card UI.
 *
 * <p>Each metric embeds its own {@code label} and {@code description} so the frontend
 * can render column headings and tooltip copy without referencing a separate lookup map.
 * Only the data needed for the chooser page is included — full return history is omitted.
 */
@Schema(description = "Fund data shaped for the chooser card — each metric includes its own display strings")
@Value
@Builder
public class FundChooserItem {

    @Schema(description = "Unique fund identifier", example = "growth")
    String id;

    @Schema(description = "Display name of the fund", example = "Growth Fund")
    String name;

    @Schema(description = "Annual management fee metric")
    FeeDisplay fee;

    @Schema(description = "Estimated return metric (5-year average annual)")
    ReturnDisplay estimatedReturn;

    @Schema(description = "Recommended minimum investment timeframe metric")
    TimeframeDisplay minInvestmentTimeframe;

    @Schema(description = "Risk indicator metric on the 1–7 scale")
    RiskDisplay riskIndicator;

    // -------------------------------------------------------------------------
    // Nested display types — value + display strings travel together
    // -------------------------------------------------------------------------

    @Schema(description = "Annual fee with co-located display strings")
    @Value
    @Builder
    public static class FeeDisplay {
        @Schema(description = "Annual fund charge as a percentage", example = "0.85")
        double value;

        @Schema(description = "Unit suffix", example = "%")
        String unit;

        @Schema(description = "Column heading label", example = "Fee")
        String label;

        @Schema(description = "Human-readable cost description shown beneath the value",
                example = "85c per $100 of your balance per year")
        String description;
    }

    @Schema(description = "5-year average annual return with co-located display strings")
    @Value
    @Builder
    public static class ReturnDisplay {
        @Schema(description = "5-year average annual return as a percentage", example = "6.33")
        double value;

        @Schema(description = "Unit suffix", example = "%")
        String unit;

        @Schema(description = "Numeric length of the return period", example = "5")
        int periodValue;

        @Schema(description = "Unit of the return period", example = "years")
        String periodUnit;

        @Schema(description = "Column heading label", example = "Return")
        String label;

        @Schema(description = "Tooltip / sub-label describing the return figure",
                example = "Estimated average annual return over 5 years")
        String description;
    }

    @Schema(description = "Recommended minimum investment timeframe with co-located display strings")
    @Value
    @Builder
    public static class TimeframeDisplay {
        @Schema(description = "Numeric length of the recommended timeframe", example = "10")
        int value;

        @Schema(description = "Unit of the timeframe", example = "years")
        String unit;

        @Schema(description = "Column heading label", example = "Time")
        String label;

        @Schema(description = "Tooltip / sub-label describing the timeframe",
                example = "Recommended min. investment time")
        String description;
    }

    @Schema(description = "Risk/return indicator with co-located display strings and scale bounds for gauge rendering")
    @Value
    @Builder
    public static class RiskDisplay {
        @Schema(description = "Numeric risk score", example = "4", minimum = "1", maximum = "7")
        int value;

        @Schema(description = "Lower bound of the risk scale — always 1", example = "1")
        int scaleMin;

        @Schema(description = "Upper bound of the risk scale — always 7", example = "7")
        int scaleMax;

        @Schema(description = "Column heading label", example = "Risk")
        String label;

        @Schema(description = "Tooltip / sub-label describing what the indicator means",
                example = "How much the fund goes up and down")
        String description;
    }
}
