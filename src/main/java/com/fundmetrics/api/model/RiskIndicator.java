package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Risk/return profile of a fund on the standard 1–7 scale defined by
 * the Financial Markets Authority (FMA) Product Disclosure Statement framework.
 *
 * <ul>
 *   <li>1–2 = Conservative / Low risk</li>
 *   <li>3–4 = Moderate / Medium risk</li>
 *   <li>5–6 = Growth / Medium-High risk</li>
 *   <li>7 = Aggressive / High risk</li>
 * </ul>
 */
@Schema(description = "Risk/return indicator on the FMA 1–7 scale (1 = lowest risk/return, 7 = highest)")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskIndicator {

    /** Numeric risk score from 1 (lowest) to 7 (highest). */
    @Schema(description = "Numeric risk score", example = "5", minimum = "1", maximum = "7")
    private int value;

    /** Human-readable label corresponding to the numeric score. */
    @Schema(description = "Human-readable risk label", example = "Medium to High")
    private String label;
}
