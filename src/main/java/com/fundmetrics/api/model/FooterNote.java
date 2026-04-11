package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * A single independently managed paragraph in the chooser page footer.
 *
 * <p>Storing footer content as a keyed list rather than a single string means
 * each paragraph can be added, removed, or reworded in config without touching
 * any other paragraph, and the frontend can conditionally show or style notes
 * by key without parsing text.
 *
 * <p>Example keys: {@code sustainability}, {@code return}, {@code risk},
 * {@code riaa-certification}.
 */
@Schema(description = "A single keyed disclaimer paragraph shown in the chooser page footer")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FooterNote {

    /**
     * Stable identifier for this note (e.g. {@code "sustainability"}, {@code "risk"}).
     * Used by the frontend to conditionally show/hide or apply specific styling
     * without needing to parse the text content.
     */
    @Schema(description = "Stable identifier for this note", example = "sustainability")
    private String key;

    /** The disclaimer paragraph text shown in the footer. */
    @Schema(description = "Disclaimer paragraph text",
            example = "All our funds are invested in line with our sustainable investment policy.")
    private String text;
}
