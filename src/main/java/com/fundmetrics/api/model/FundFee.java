package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Annual fund charge (management fee) deducted from the investor's balance.
 *
 * <p>The charge is expressed as a percentage per annum and is deducted continuously
 * from the fund's net asset value rather than billed separately.
 */
@Schema(description = "Annual management fee deducted from the invested balance")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundFee {

    /** The annual charge as a percentage (e.g. {@code 0.85} means 0.85% p.a.). */
    @Schema(description = "Annual fund charge as a percentage of the invested balance", example = "0.85")
    private double annualFundCharge;

    /** Unit of the charge — always {@code %} for percentage. */
    @Schema(description = "Unit of the charge", example = "%")
    private String unit;
}
