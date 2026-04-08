package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * Represents a single Active Series managed fund with its associated metrics.
 *
 * <p>Metrics include the annual fee, historical returns across standard periods,
 * a recommended minimum investment timeframe, and a risk indicator on the 1–7 scale.
 */
@Schema(description = "A single managed fund with fee, return, timeframe, and risk metrics")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fund {

    /** Unique machine-readable identifier (e.g. {@code growth}, {@code balanced}). */
    @Schema(description = "Unique fund identifier", example = "growth")
    private String id;

    /** Human-readable display name shown in the micro frontend. */
    @Schema(description = "Display name of the fund", example = "Growth Fund")
    private String name;

    /** Brief description of the fund's investment objective and asset mix. */
    @Schema(description = "Investment objective and asset allocation summary",
            example = "Higher long-term capital growth. Predominantly growth assets.")
    private String description;

    /** Annual fund charge (management fee) expressed as a percentage. */
    @Schema(description = "Annual management fee charged as a percentage of the invested balance")
    private FundFee fee;

    /**
     * Historical percentage returns keyed by period label.
     * Standard periods: {@code 3months}, {@code 6months}, {@code 1year},
     * {@code 3years}, {@code 5years}, {@code 10years}.
     * Periods of 3 years and longer are average annual returns.
     */
    @Schema(description = "Percentage returns keyed by period. Values for 3 years and longer are average annual returns.")
    private Map<ReturnPeriod, Double> returns;

    /** Recommended minimum period to stay invested to ride out volatility. */
    @Schema(description = "Recommended minimum investment timeframe to achieve the fund's objective")
    private InvestmentTimeframe minInvestmentTimeframe;

    /** Risk/return profile on the standard 1 (lowest) to 7 (highest) scale. */
    @Schema(description = "Risk indicator on a 1 (lowest) to 7 (highest) scale")
    private RiskIndicator riskIndicator;
}
