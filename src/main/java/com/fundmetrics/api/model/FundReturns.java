package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * Historical return figures for a fund, bundled with fund-specific tooltip copy.
 *
 * <p>Keeping the values and their explanatory text together avoids detached sibling
 * fields on the parent {@link Fund} and makes each fund self-describing — a fund with
 * no five-year track record simply provides different tooltip copy without any
 * structural change.
 */
@Schema(description = "Return values keyed by period, with fund-specific tooltip copy for the returns card")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundReturns {

    /**
     * Percentage returns keyed by period label.
     * Standard periods: {@code 3months}, {@code 6months}, {@code 1year},
     * {@code 3years}, {@code 5years}, {@code 10years}.
     * Values for periods of 3 years or longer are average annual returns.
     */
    @Schema(description = "Percentage returns keyed by period. Values for 3 years and longer are average annual returns.")
    private Map<ReturnPeriod, Double> values;

    /**
     * Fund-specific tooltip body shown on the ⓘ icon of the returns card.
     * Allows each fund to carry distinct copy — e.g. a newly-launched fund without
     * a full five-year track record needs different text to established funds.
     */
    @Schema(description = "Fund-specific tooltip body for the returns card", nullable = true)
    private String tooltip;

    /**
     * Optional link appended to the returns tooltip.
     * Most funds leave this null; used when the copy needs to direct investors
     * to a specific page (e.g. a comparison tool for newer funds).
     */
    @Schema(description = "Fund-specific tooltip link for the returns card", nullable = true)
    private TooltipLink tooltipLink;
}
