package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Recommended minimum period an investor should remain in the fund to
 * achieve the intended investment outcome and absorb short-term volatility.
 *
 * <p>This is a recommendation, not a lock-in — investors can withdraw at any time.
 */
@Schema(description = "Recommended minimum investment timeframe. Not a lock-in period — investors can exit at any time.")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvestmentTimeframe {

    /** Numeric length of the recommended timeframe (e.g. {@code 7}). */
    @Schema(description = "Numeric length of the recommended timeframe", example = "7")
    private int value;

    /** Unit of the timeframe — always {@code years}. */
    @Schema(description = "Unit of the timeframe", example = "years")
    private String unit;
}
