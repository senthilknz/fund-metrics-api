package com.fundmetrics.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundmetrics.api.model.FundConfig;
import com.fundmetrics.api.model.MetricDescription;
import com.fundmetrics.api.model.ReturnPeriod;
import com.fundmetrics.api.model.chooser.FundChooserItem;
import com.fundmetrics.api.model.chooser.FundChooserItem.FeeDisplay;
import com.fundmetrics.api.model.chooser.FundChooserItem.ReturnDisplay;
import com.fundmetrics.api.model.chooser.FundChooserItem.RiskDisplay;
import com.fundmetrics.api.model.chooser.FundChooserItem.TimeframeDisplay;
import com.fundmetrics.api.model.chooser.FundChooserResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manages the lifecycle of versioned fund config files embedded in the application JAR.
 *
 * <h2>How versioning works</h2>
 * <p>All {@code funds-config-*.json} files under {@code src/main/resources/fund-configs/}
 * are packaged into the JAR at build time and loaded on startup. Each file carries an
 * {@code effectiveFrom} date-time (NZT). The active config is always the latest version
 * whose {@code effectiveFrom} is not after now (NZT), resolved at startup and re-evaluated
 * every minute by the scheduler.
 *
 * <h2>Quarterly update operational workflow</h2>
 * <ol>
 *   <li>Add {@code funds-config-2025.07.01.json} with
 *       {@code "effectiveFrom": "2025-07-01T00:00:00"} (or a specific time, e.g.
 *       {@code "2025-07-01T09:00:00"}) and the new return figures.</li>
 *   <li>Commit and push — the pipeline builds and deploys the new JAR.
 *       This can be done weeks in advance of the activation date.</li>
 *   <li>The app runs normally, still serving the previous version.</li>
 *   <li>At the specified NZT date-time, the minute scheduler automatically activates
 *       the new version — no further deployment, no manual intervention required.</li>
 * </ol>
 *
 * <h2>Emergency override</h2>
 * <p>{@link #forceActivateVersion(String)} overrides the active config in memory without
 * redeployment. Resets on the next restart or scheduler tick.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundConfigService {

    private static final String CONFIG_PATTERN = "classpath:fund-configs/funds-config-*.json";
    static final ZoneId NZT = ZoneId.of("Pacific/Auckland");

    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper objectMapper;

    /** Full history of all loaded configs, sorted ascending by {@code effectiveFrom}. */
    private List<FundConfig> configHistory = new ArrayList<>();

    /**
     * The currently live config. Declared {@code volatile} so the midnight scheduler
     * and the request thread always see a consistent value without synchronisation overhead.
     */
    private volatile FundConfig activeConfig;

    /**
     * Initialises the service on application startup: loads all embedded config files
     * then resolves the initial active config based on today's date.
     *
     * @throws IOException if the classpath resource scan fails at the I/O level
     */
    @PostConstruct
    public void init() throws IOException {
        loadAllConfigs();
        refreshActiveConfig();
    }

    /**
     * Scans the classpath for all files matching {@value #CONFIG_PATTERN}, parses each
     * into a {@link FundConfig}, and stores them sorted by {@code effectiveFrom}.
     *
     * <p>Files that fail to parse are logged and skipped — they do not abort startup.
     * Since configs are embedded in the JAR, the set of files is fixed at build time;
     * this method only runs once at startup.
     *
     * @throws IOException if the resource pattern resolver cannot scan the classpath
     */
    private void loadAllConfigs() throws IOException {
        Resource[] resources = resourcePatternResolver.getResources(CONFIG_PATTERN);
        List<FundConfig> loaded = new ArrayList<>();
        for (Resource resource : resources) {
            try {
                FundConfig config = objectMapper.readValue(resource.getInputStream(), FundConfig.class);
                loaded.add(config);
                log.info("Loaded fund config version={} effectiveFrom={}", config.getVersion(), config.getEffectiveFrom());
            } catch (IOException e) {
                log.error("Failed to parse fund config file: {} — skipping", resource.getFilename(), e);
            }
        }
        loaded.sort(Comparator.comparing(FundConfig::getEffectiveFrom));
        this.configHistory = loaded;
        log.info("Fund config load complete: {} file(s) found", configHistory.size());
    }

    /**
     * Re-evaluates which config version should be active based on the current NZT date-time.
     * Selects the latest config whose {@code effectiveFrom} is not after now (NZT).
     *
     * <p>Runs every minute so a version deployed in advance (with a future
     * {@code effectiveFrom}) activates within one minute of the specified NZT date-time,
     * with no further deployment or manual intervention required.
     */
    @Scheduled(cron = "0 * * * * *", zone = "Pacific/Auckland")
    public void refreshActiveConfig() {
        LocalDateTime now = LocalDateTime.now(NZT);
        FundConfig resolved = configHistory.stream()
                .filter(c -> !c.getEffectiveFrom().isAfter(now))
                .reduce((first, second) -> second)
                .orElse(null);

        if (resolved != null) {
            this.activeConfig = resolved;
            log.info("Active fund config refreshed: version={} effectiveFrom={}", resolved.getVersion(), resolved.getEffectiveFrom());
        } else {
            log.warn("No fund config is effective as of now ({})", now);
        }
    }

    /**
     * Returns the currently active fund config.
     *
     * @return the active {@link FundConfig}, or {@code null} if none has become effective yet
     */
    public FundConfig getActiveConfig() {
        return activeConfig;
    }

    /**
     * Returns an unmodifiable view of all loaded config versions, ordered ascending
     * by {@code effectiveFrom} date.
     *
     * @return all loaded configs — never {@code null}, may be empty
     */
    public List<FundConfig> getHistory() {
        return Collections.unmodifiableList(configHistory);
    }

    /**
     * Resolves the config that would be active on the given date without modifying
     * the live active config. Safe to call for preview/QA purposes.
     *
     * <p>The date is interpreted as the full calendar day in NZT: any config whose
     * {@code effectiveFrom} falls on or before 23:59:59 of the given date is eligible.
     * For example, a config with {@code effectiveFrom: "2025-07-01T09:00:00"} is correctly
     * returned when previewing {@code 2025-07-01}.
     *
     * @param date the hypothetical date to evaluate (interpreted as NZT end-of-day)
     * @return the {@link FundConfig} effective on that date, or {@code null} if none applies
     */
    public FundConfig resolveConfigForDate(LocalDate date) {
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return configHistory.stream()
                .filter(c -> !c.getEffectiveFrom().isAfter(endOfDay))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
     * Maps the active fund config into a chooser-page-ready response.
     *
     * <p>Each metric's label and description travel alongside its value so the
     * micro frontend can render every card without cross-referencing the separate
     * {@code metricDescriptions} map or hardcoding display strings client-side.
     * Only the 5-year return is included — all other periods are omitted.
     *
     * @return chooser response, or {@code null} if no config is currently active
     */
    public FundChooserResponse toChooserResponse() {
        FundConfig config = activeConfig;
        if (config == null) {
            return null;
        }

        var md = config.getMetricDescriptions();
        MetricDescription feeDesc = md.get("fee");
        MetricDescription returnsDesc = md.get("returns");
        MetricDescription timeframeDesc = md.get("minInvestmentTimeframe");
        MetricDescription riskDesc = md.get("riskIndicator");
        ReturnPeriod returnPeriod = returnsDesc.getChooserPeriod();

        var items = config.getFunds().stream().map(fund -> {
            double feeValue = fund.getFee().getAnnualFundCharge();
            long centsPerHundred = Math.round(feeValue * 100);
            String feeDescription = feeDesc.getDescription()
                    .replace("{cents}", String.valueOf(centsPerHundred));

            return FundChooserItem.builder()
                    .id(fund.getId())
                    .name(fund.getName())
                    .fee(FeeDisplay.builder()
                            .value(feeValue)
                            .unit(fund.getFee().getUnit())
                            .label(feeDesc.getLabel())
                            .description(feeDescription)
                            .build())
                    .estimatedReturn(ReturnDisplay.builder()
                            .value(fund.getReturns().getValues().getOrDefault(returnPeriod, 0.0))
                            .unit(returnsDesc.getUnit())
                            .periodValue(returnPeriod.getPeriodValue())
                            .periodUnit(returnPeriod.getPeriodUnit())
                            .label(returnsDesc.getLabel())
                            .description(returnsDesc.getDescription())
                            .tooltip(fund.getReturns().getTooltip())
                            .tooltipLink(fund.getReturns().getTooltipLink())
                            .build())
                    .minInvestmentTimeframe(TimeframeDisplay.builder()
                            .value(fund.getMinInvestmentTimeframe().getValue())
                            .unit(fund.getMinInvestmentTimeframe().getUnit())
                            .label(timeframeDesc.getLabel())
                            .description(timeframeDesc.getDescription())
                            .build())
                    .riskIndicator(RiskDisplay.builder()
                            .value(fund.getRiskIndicator().getValue())
                            .scaleMin(riskDesc.getScaleMin())
                            .scaleMax(riskDesc.getScaleMax())
                            .label(riskDesc.getLabel())
                            .description(riskDesc.getDescription())
                            .tooltip(fund.getRiskIndicator().getTooltip())
                            .tooltipLink(fund.getRiskIndicator().getTooltipLink())
                            .build())
                    .build();
        }).toList();

        return FundChooserResponse.builder()
                .disclaimer(config.getDisclaimer())
                .footerNotes(config.getFooterNotes())
                .performanceAsOf(config.getPerformanceAsOf())
                .funds(items)
                .build();
    }

    /**
     * Force-activates a specific config version in memory, bypassing date resolution.
     * Intended for emergency ops rollback without redeployment.
     *
     * <p><strong>Warning:</strong> this override resets on the next application restart
     * or midnight scheduler run.
     *
     * @param version the version string to activate (e.g. {@code "1.0.0"})
     * @return {@code true} if the version was found and activated, {@code false} otherwise
     */
    public boolean forceActivateVersion(String version) {
        return configHistory.stream()
                .filter(c -> c.getVersion().equals(version))
                .findFirst()
                .map(config -> {
                    this.activeConfig = config;
                    log.info("Force-activated fund config version={}", version);
                    return true;
                })
                .orElse(false);
    }
}
