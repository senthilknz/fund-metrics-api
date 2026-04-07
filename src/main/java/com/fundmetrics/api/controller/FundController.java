package com.fundmetrics.api.controller;

import com.fundmetrics.api.model.FundConfig;
import com.fundmetrics.api.model.chooser.FundChooserResponse;
import com.fundmetrics.api.service.FundConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing fund metric endpoints under {@code /api/v1/funds}.
 *
 * <p>All data is driven by versioned JSON config files on the classpath — no database required.
 * The active version is determined at startup and refreshed nightly by {@link FundConfigService}.
 *
 * <h2>Caching strategy for {@code GET /api/v1/funds}</h2>
 * <p>The active-config endpoint uses HTTP conditional request semantics so the micro frontend
 * always calls the same stable URL and always receives the correct latest data:
 * <ul>
 *   <li>{@code Cache-Control: no-cache} — the browser/CDN must revalidate on every request
 *       rather than serving a locally cached copy silently.</li>
 *   <li>{@code ETag} — a quoted version string (e.g. {@code "2.0.0"}) derived from the active
 *       config. If the ETag matches the client's {@code If-None-Match} header, the server returns
 *       {@code 304 Not Modified} with no body — cheap network round-trip, zero bandwidth.</li>
 *   <li>{@code Last-Modified} — the {@code publishedAt} timestamp of the active config.
 *       Used alongside {@code If-Modified-Since} for HTTP/1.0 compatibility.</li>
 * </ul>
 * <p>Result: the micro frontend fires one stable URL on every render. If the fund manager has
 * not deployed a new quarterly config, the response is a near-instant 304. When a new config
 * activates (e.g. on 1 April), the next request gets a full 200 with fresh data automatically —
 * no URL change, no frontend code change required.
 */
@Tag(name = "Fund Metrics", description = "Active Series fund metric data — fees, returns, risk indicators, and investment timeframes")
@RestController
@RequestMapping("/api/v1/funds")
public class FundController {

    private final FundConfigService fundConfigService;

    /**
     * Constructs the controller with the required service dependency.
     *
     * @param fundConfigService service that owns the versioned config lifecycle
     */
    public FundController(FundConfigService fundConfigService) {
        this.fundConfigService = fundConfigService;
    }

    /**
     * Returns the currently active fund config — the latest version whose
     * {@code effectiveFrom} date is not after today.
     *
     * <p>Uses HTTP conditional request semantics to guarantee the micro frontend always
     * sees the latest data without needing to change its URL:
     * <ol>
     *   <li>Response includes {@code ETag: "version"} and {@code Last-Modified} headers.</li>
     *   <li>Subsequent requests from the same client include {@code If-None-Match} /
     *       {@code If-Modified-Since}.</li>
     *   <li>If the active config has not changed, the server returns {@code 304 Not Modified}
     *       with no body — fast and bandwidth-free.</li>
     *   <li>If a new quarterly config has activated, the server returns {@code 200} with the
     *       full updated payload.</li>
     * </ol>
     *
     * @param webRequest Spring's {@link WebRequest}, used to evaluate conditional request headers
     * @return {@code 200} with the active config, {@code 304 Not Modified} if unchanged,
     *         or {@code 503} if no config has become effective yet
     */
    @Operation(
            summary = "Get active fund config",
            description = "Returns the currently active fund config — the latest version whose effectiveFrom " +
                          "date is not after today. Uses ETag + Last-Modified conditional request semantics: " +
                          "the micro frontend always calls the same URL and receives either a full 200 (new data) " +
                          "or a cheap 304 Not Modified (data unchanged). Cache-Control: no-cache ensures the " +
                          "browser always revalidates rather than silently serving a stale cached response."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active fund config returned",
                    content = @Content(schema = @Schema(implementation = FundConfig.class))),
            @ApiResponse(responseCode = "304", description = "Config unchanged since last request — client cache is still valid",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "No config has become effective yet",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<FundConfig> getActiveConfig(WebRequest webRequest) {
        FundConfig config = fundConfigService.getActiveConfig();
        if (config == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No active fund config available");
        }

        // ETag is the quoted version string — changes precisely when a new config activates.
        String eTag = "\"" + config.getVersion() + "\"";

        // Last-Modified is the publishedAt timestamp of the active config.
        long lastModifiedEpochMs = config.getPublishedAt().toEpochMilli();

        // Spring evaluates If-None-Match / If-Modified-Since and returns 304 automatically
        // if neither the ETag nor the timestamp has changed.
        if (webRequest.checkNotModified(eTag, lastModifiedEpochMs)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok()
                // no-cache: must revalidate on every request — never serve stale data silently.
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate")
                .eTag(eTag)
                .lastModified(config.getPublishedAt())
                .body(config);
    }

    /**
     * Returns a fund chooser response shaped specifically for the fund selection page.
     *
     * <p>Each fund's metrics include co-located labels and descriptions so the micro
     * frontend can render every card without:
     * <ul>
     *   <li>Cross-referencing a separate {@code metricDescriptions} map</li>
     *   <li>Knowing which return period to display (always 5-year here)</li>
     *   <li>Hardcoding display strings client-side</li>
     * </ul>
     *
     * <p>Applies the same HTTP conditional request semantics as the full config endpoint:
     * {@code ETag} + {@code Cache-Control: no-cache} so the frontend always revalidates
     * and gets a cheap {@code 304 Not Modified} when nothing has changed.
     *
     * @param webRequest Spring's {@link WebRequest} for conditional request evaluation
     * @return {@code 200} with chooser payload, {@code 304} if unchanged, or {@code 503} if unavailable
     */
    @Operation(
            summary = "Get fund chooser page data",
            description = "Returns fund metrics shaped for the fund chooser UI — each metric embeds its own " +
                          "label and description so the frontend requires no client-side joins. " +
                          "Only the 5-year return is included. Supports ETag/304 caching."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chooser data returned",
                    content = @Content(schema = @Schema(implementation = FundChooserResponse.class))),
            @ApiResponse(responseCode = "304", description = "Data unchanged — client cache still valid",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "No active fund config available",
                    content = @Content)
    })
    @GetMapping("/chooser")
    public ResponseEntity<FundChooserResponse> getChooser(WebRequest webRequest) {
        FundConfig config = fundConfigService.getActiveConfig();
        if (config == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No active fund config available");
        }

        String eTag = "\"" + config.getVersion() + "\"";
        long lastModifiedEpochMs = config.getPublishedAt().toEpochMilli();

        if (webRequest.checkNotModified(eTag, lastModifiedEpochMs)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        FundChooserResponse body = fundConfigService.toChooserResponse();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate")
                .eTag(eTag)
                .lastModified(config.getPublishedAt())
                .body(body);
    }

    /**
     * Returns all loaded config versions in ascending {@code effectiveFrom} order.
     *
     * <p>Useful for auditing what data was served historically, or for validating
     * that a newly deployed config file was picked up correctly.
     *
     * @return unmodifiable list of all {@link FundConfig} versions
     */
    @Operation(
            summary = "Get all config versions",
            description = "Returns every loaded config version sorted by effectiveFrom ascending. " +
                          "Useful for auditing historical data or confirming a new file was deployed correctly."
    )
    @ApiResponse(responseCode = "200", description = "All config versions returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = FundConfig.class))))
    @GetMapping("/history")
    public List<FundConfig> getHistory() {
        return fundConfigService.getHistory();
    }

    /**
     * Returns the config that would be active on the given date, without changing
     * the live active config. Intended for QA previewing before a scheduled version
     * goes live.
     *
     * @param date the target date in {@code YYYY-MM-DD} format
     * @return the {@link FundConfig} effective on that date, or 404 if none applies
     * @throws ResponseStatusException 400 if the date format is invalid, 404 if no config is effective on that date
     */
    @Operation(
            summary = "Preview config for a given date",
            description = "Returns the fund config that would be active on the supplied date without altering " +
                          "the live config. Use this to QA a future version before its effectiveFrom date arrives."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Config effective on the given date",
                    content = @Content(schema = @Schema(implementation = FundConfig.class))),
            @ApiResponse(responseCode = "400", description = "Date parameter is not in YYYY-MM-DD format",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "No config is effective on the given date",
                    content = @Content)
    })
    @GetMapping("/preview")
    public FundConfig previewForDate(
            @Parameter(description = "Target date in YYYY-MM-DD format", example = "2025-04-01", required = true)
            @RequestParam("date") String date) {
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format. Use YYYY-MM-DD.");
        }
        FundConfig config = fundConfigService.resolveConfigForDate(localDate);
        if (config == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No fund config effective on " + date);
        }
        return config;
    }

    /**
     * Force-activates a specific config version in memory, overriding the normal
     * date-based resolution. Intended for emergency ops rollback without redeployment.
     *
     * <p><strong>Note:</strong> this override is in-memory only and resets on the next
     * application restart or midnight scheduler tick.
     *
     * @param version the version string to activate (e.g. {@code 1.0.0})
     * @return a JSON body with {@code success: true} on success, or 404 if the version is unknown
     */
    @Operation(
            summary = "Force-activate a config version",
            description = "Overrides the active config to the specified version in memory. " +
                          "Intended for emergency rollback without redeployment. " +
                          "Resets on the next restart or midnight scheduler tick."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Version activated successfully"),
            @ApiResponse(responseCode = "404", description = "Version not found in loaded history",
                    content = @Content)
    })
    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> forceActivateVersion(
            @Parameter(description = "Version string to activate", example = "1.0.0", required = true)
            @RequestParam("version") String version) {
        boolean success = fundConfigService.forceActivateVersion(version);
        if (!success) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Version not found: " + version));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Activated version: " + version));
    }
}
