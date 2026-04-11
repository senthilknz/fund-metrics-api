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
 *
 * <h2>Chooser card fields</h2>
 * <p>The {@code chooser*} fields drive the fund chooser UI card for this metric.
 * Co-locating them here means one section of config covers both the full API response
 * and the chooser card — no duplication, no separate section to maintain.
 *
 * <ul>
 *   <li>{@code chooserLabel} — short heading on the card (e.g. {@code "Fee"}).</li>
 *   <li>{@code chooserDescription} — sub-text on the card. For the {@code fee} metric this
 *       may contain a {@code {cents}} placeholder replaced at runtime per fund.</li>
 *   <li>{@code chooserPeriod} — {@code returns} only: which period value to display
 *       (e.g. {@code "5years"}).</li>
 *   <li>{@code scaleMin} / {@code scaleMax} — {@code riskIndicator} only: bounds used by
 *       gauge/bar rendering components.</li>
 * </ul>
 */
@Schema(description = "Display metadata for a fund metric type — covers both the full API response and the chooser card UI")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricDescription {

    /** Full display label for the metric (e.g. {@code "Annual Fund Charge"}). */
    @Schema(description = "Full display label for the metric", example = "Annual Fund Charge")
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

    // -------------------------------------------------------------------------
    // Chooser card display fields
    // -------------------------------------------------------------------------

    /** Short heading shown at the top of the chooser card (e.g. {@code "Fee"}). */
    @Schema(description = "Short card heading for the chooser UI", example = "Fee", nullable = true)
    private String chooserLabel;

    /**
     * Sub-text shown beneath the value on the chooser card.
     * For the {@code fee} metric, may contain a {@code {cents}} placeholder that is
     * replaced at runtime with the computed cents-per-$100 value for each fund.
     */
    @Schema(description = "Sub-text for the chooser card; fee supports {cents} placeholder",
            example = "{cents}c per $100 of your balance per year", nullable = true)
    private String chooserDescription;

    /**
     * Which return period to display on the chooser card.
     * Only present for the {@code returns} metric (e.g. {@code "5years"}).
     */
    @Schema(description = "Return period shown on the chooser card — only for the returns metric",
            nullable = true)
    private ReturnPeriod chooserPeriod;

    /**
     * Lower bound of the risk scale for gauge rendering.
     * Only meaningful for the {@code riskIndicator} metric (typically {@code 1}).
     */
    @Schema(description = "Lower bound of the risk scale — only for the riskIndicator metric",
            example = "1", nullable = true)
    private int scaleMin;

    /**
     * Upper bound of the risk scale for gauge rendering.
     * Only meaningful for the {@code riskIndicator} metric (typically {@code 7}).
     */
    @Schema(description = "Upper bound of the risk scale — only for the riskIndicator metric",
            example = "7", nullable = true)
    private int scaleMax;
}
