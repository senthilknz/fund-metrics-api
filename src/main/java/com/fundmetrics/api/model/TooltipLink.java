package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * An optional hyperlink rendered at the end of a tooltip.
 *
 * <p>Used where a tooltip needs to direct the user to an external document
 * (e.g. the Product Disclosure Statement for the risk indicator explanation).
 */
@Schema(description = "Hyperlink appended to a tooltip — text and destination URL")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TooltipLink {

    /** Visible link text displayed to the user. */
    @Schema(description = "Visible link text", example = "Westpac Active Series Product Disclosure Statement")
    private String text;

    /** Destination URL the link points to. */
    @Schema(description = "Destination URL", example = "https://www.westpac.co.nz/pds")
    private String url;
}
