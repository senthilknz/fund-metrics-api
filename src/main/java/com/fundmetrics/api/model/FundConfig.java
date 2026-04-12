package com.fundmetrics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Top-level wrapper representing a single versioned fund config file.
 *
 * <p>Each config file under {@code src/main/resources/fund-configs/} maps to one instance.
 * The {@link #effectiveFrom} date determines when this version becomes the active config.
 */
@Schema(description = "Versioned fund configuration snapshot, including metadata and the full list of funds")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundConfig {

    /** CalVer version of this config file (e.g. {@code 2025.04.01}). */
    @Schema(description = "CalVer version of this config (YYYY.MM.DD)", example = "2025.04.01")
    private String version;

    /**
     * The date and time (NZT) from which this config becomes the active version.
     *
     * <p>The scheduler re-evaluates every minute — a config with
     * {@code effectiveFrom: "2025-07-01T09:00:00"} activates within one minute
     * of 9:00 am NZT on 1 July 2025, with no deployment or manual step required.
     * Use midnight ({@code T00:00:00}) when time-of-day precision is not needed.
     *
     * <p>All values are interpreted as NZT (Pacific/Auckland).
     */
    @Schema(description = "NZT date-time from which this config becomes active — scheduler checks every minute",
            example = "2025-04-01T00:00:00")
    private LocalDateTime effectiveFrom;

    /** ISO-8601 timestamp of when this config file was published. */
    @Schema(description = "ISO-8601 timestamp of when this config was published", example = "2025-04-01T00:00:00Z")
    private Instant publishedAt;

    /** The last date for which performance figures in this config are valid. */
    @Schema(description = "Date up to which fund performance figures are calculated", example = "2025-03-31")
    private String performanceAsOf;

    /** Human-readable label for the originating data source. */
    @Schema(description = "Name of the data source for fund metrics", example = "Active Series")
    private String dataSource;

    /** Short legal disclaimer shown inline with fund performance data. */
    @Schema(description = "Short legal disclaimer shown with fund performance data",
            example = "Past performance is not a reliable indication of future performance.")
    private String disclaimer;

    /**
     * Ordered list of independently managed disclaimer paragraphs shown in the chooser
     * page footer. Each note has a stable {@code key} so the frontend can conditionally
     * show/hide or style individual paragraphs without parsing text, and individual
     * paragraphs can be added or removed per config version without touching others.
     */
    @Schema(description = "Ordered list of keyed disclaimer paragraphs for the chooser page footer")
    private List<FooterNote> footerNotes;

    /**
     * Display metadata for each metric type (fee, returns, riskIndicator, etc.),
     * keyed by metric name. Each entry covers both the full API response (label,
     * description, unit) and the chooser card UI (chooserLabel, chooserDescription,
     * chooserPeriod, scaleMin, scaleMax) — one section, no duplication.
     */
    @Schema(description = "Display metadata for each metric — covers both the full API response and the chooser card UI")
    private Map<String, MetricDescription> metricDescriptions;

    /** The ordered list of funds included in this config. */
    @Schema(description = "List of funds with their metrics for this config version")
    private List<Fund> funds;
}
