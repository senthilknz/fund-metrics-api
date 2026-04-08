package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import com.fundmetrics.api.model.ReturnPeriod;

// Note: ReturnPeriod is imported for the periods field — the enum's @JsonValue/@JsonCreator
// handles serialisation/deserialisation of period strings transparently.

/**
 * Human-readable metadata for a metric type (fee, returns, riskIndicator, etc.).
 *
 * <p>Consumed by the micro frontend to render dynamic labels, tooltips, and unit suffixes
 * without hardcoding display strings in the UI. Fields {@code scale} and {@code periods}
 * are optional and only present for applicable metric types.
 */
@Schema(description = "Display metadata for a fund metric type, used by the micro frontend for labels and tooltips")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricDescription {

    /** Short display label shown as the column/field heading (e.g. {@code Annual Fund Charge}). */
    @Schema(description = "Short display label for the metric", example = "Annual Fund Charge")
    private String label;

    /** Longer explanatory text used for tooltips or help text in the UI. */
    @Schema(description = "Explanatory text for use in tooltips or help sections",
            example = "Annual % fee deducted from invested balance.")
    private String description;

    /** Unit suffix appended to displayed values (e.g. {@code %}, {@code years}). */
    @Schema(description = "Unit suffix for displayed values", example = "% per annum")
    private String unit;

    /**
     * Describes the scale for indicator-type metrics.
     * Only present for {@code riskIndicator} (e.g. {@code "1 (lowest) to 7 (highest)"}).
     */
    @Schema(description = "Scale description — only present for indicator-type metrics",
            example = "1 (lowest) to 7 (highest)", nullable = true)
    private String scale;

    /**
     * Ordered list of return period labels for the {@code returns} metric.
     * Only present for the returns metric type (e.g. {@code ["3months","1year","5years"]}).
     */
    @Schema(description = "Ordered return periods — only present for the returns metric",
            nullable = true)
    private List<ReturnPeriod> periods;
}
