package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import com.fundmetrics.api.model.ReturnPeriod;

// Note: ReturnPeriod is imported for the periods field — the enum's @JsonValue/@JsonCreator
// handles serialisation/deserialisation of period strings transparently.

/**
 * Display metadata for a fund metric type (fee, returns, riskIndicator, etc.).
 *
 * <p>Each entry covers everything needed to render a metric card in the chooser UI
 * and serve the full API response — one section, no duplication.
 *
 * <ul>
 *   <li>{@code label} — card heading (e.g. {@code "Fee"}).</li>
 *   <li>{@code description} — card sub-text. For {@code fee} this may contain a
 *       {@code {cents}} placeholder replaced at runtime per fund.</li>
 *   <li>{@code unit} — value suffix (e.g. {@code "%"}, {@code "years"}).</li>
 *   <li>{@code chooserPeriod} — {@code returns} only: which period to show on the card.</li>
 *   <li>{@code scaleMin} / {@code scaleMax} — {@code riskIndicator} only: gauge bounds.</li>
 *   <li>{@code scale} / {@code periods} — full-API-only supplementary metadata.</li>
 * </ul>
 */
@Schema(description = "Display metadata for a fund metric — label, description, unit, and optional chooser-specific config")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricDescription {

    /** Card heading label (e.g. {@code "Fee"}, {@code "Return"}). */
    @Schema(description = "Card heading label", example = "Fee")
    private String label;

    /**
     * Card sub-text shown beneath the value.
     * For the {@code fee} metric, may contain a {@code {cents}} placeholder replaced
     * at runtime with each fund's computed cents-per-$100 value.
     */
    @Schema(description = "Card sub-text; fee supports {cents} placeholder",
            example = "{cents}c per $100 of your balance per year")
    private String description;

    /** Unit suffix appended to the displayed value (e.g. {@code "%"}, {@code "years"}). */
    @Schema(description = "Unit suffix for the displayed value", example = "%")
    private String unit;

    /**
     * Human-readable scale description for indicator-type metrics.
     * Only present for {@code riskIndicator} (e.g. {@code "1 (lowest) to 7 (highest)"}).
     */
    @Schema(description = "Scale description — riskIndicator only",
            example = "1 (lowest) to 7 (highest)", nullable = true)
    private String scale;

    /**
     * Ordered list of all available return period keys.
     * Only present for the {@code returns} metric.
     */
    @Schema(description = "Ordered return periods — returns metric only", nullable = true)
    private List<ReturnPeriod> periods;

    /**
     * Tooltip body text shown when the user taps the ⓘ icon on the chooser card.
     * Only present for metrics that have an info icon (currently {@code returns}
     * and {@code riskIndicator}).
     */
    @Schema(description = "Tooltip body text shown on ⓘ icon tap — returns and riskIndicator only",
            example = "Average annual return is over five years after deducting annual fund charges but before tax.",
            nullable = true)
    private String tooltip;

    /**
     * Optional hyperlink appended to the tooltip text.
     * Only present for the {@code riskIndicator} metric (links to the PDS).
     */
    @Schema(description = "Optional link appended to the tooltip — riskIndicator only", nullable = true)
    private TooltipLink tooltipLink;

    /**
     * Which return period to display on the chooser card.
     * Only present for the {@code returns} metric (e.g. {@code "5years"}).
     */
    @Schema(description = "Return period shown on the chooser card — returns metric only",
            example = "5years", nullable = true)
    private ReturnPeriod chooserPeriod;

    /**
     * Lower bound of the risk scale used by gauge/bar rendering components.
     * Only meaningful for the {@code riskIndicator} metric (typically {@code 1}).
     */
    @Schema(description = "Lower bound of the risk scale — riskIndicator only", example = "1")
    private int scaleMin;

    /**
     * Upper bound of the risk scale used by gauge/bar rendering components.
     * Only meaningful for the {@code riskIndicator} metric (typically {@code 7}).
     */
    @Schema(description = "Upper bound of the risk scale — riskIndicator only", example = "7")
    private int scaleMax;
}
