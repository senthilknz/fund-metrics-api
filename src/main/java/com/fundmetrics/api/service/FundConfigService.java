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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages the lifecycle of versioned fund config files embedded in the application JAR.
 *
 * <h2>How versioning works</h2>
 * <p>All {@code funds-config-*.json} files under {@code src/main/resources/fund-configs/}
 * are packaged into the JAR at build time and loaded on startup. Each file carries an
 * {@code effectiveFrom} date-time (NZT). The active config is always the latest version
 * whose {@code effectiveFrom} is not after now (NZT).
 *
 * <h2>Activation mechanism — two complementary paths</h2>
 * <ol>
 *   <li><strong>Precise one-time scheduling</strong> — on startup (and after each
 *       activation), {@link #scheduleNextActivation()} finds the earliest future config
 *       and schedules a single {@link TaskScheduler} task to fire at its exact
 *       {@code effectiveFrom} instant. Zero unnecessary firings between quarterly updates.</li>
 *   <li><strong>Daily midnight safety net</strong> — a midnight cron re-evaluates the
 *       active config and reschedules the next activation. Ensures correctness after an
 *       app restart, a clock correction, or any edge case that cancels the one-time task.</li>
 * </ol>
 *
 * <h2>Quarterly update operational workflow</h2>
 * <ol>
 *   <li>Add {@code funds-config-2025.07.01.json} with
 *       {@code "effectiveFrom": "2025-07-01T00:00:00"} (midnight) or a specific time,
 *       e.g. {@code "2025-07-01T09:00:00"} (9 am), and the new return figures.</li>
 *   <li>Commit and push — the pipeline builds and deploys the new JAR.
 *       This can be done weeks in advance.</li>
 *   <li>The app runs normally, still serving the previous version.</li>
 *   <li>At the specified NZT date-time, the one-time task fires and activates the new
 *       version — no deployment, no manual intervention required.</li>
 * </ol>
 *
 * <h2>Emergency override</h2>
 * <p>{@link #forceActivateVersion(String)} overrides the active config in memory without
 * redeployment. Resets on the next restart or midnight safety-net tick.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundConfigService {

    private static final String CONFIG_PATTERN = "classpath:fund-configs/funds-config-*.json";
    static final ZoneId NZT = ZoneId.of("Pacific/Auckland");

    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;

    /** Full history of all loaded configs, sorted ascending by {@code effectiveFrom}. */
    private List<FundConfig> configHistory = new ArrayList<>();

    /**
     * The currently live config. Declared {@code volatile} so the scheduler thread
     * and request threads always see a consistent value without synchronisation overhead.
     */
    private volatile FundConfig activeConfig;

    /**
     * The pending one-time activation task, kept so it can be cancelled and replaced
     * when {@link #scheduleNextActivation()} is called again (e.g. after a safety-net tick).
     */
    private volatile ScheduledFuture<?> pendingActivation;

    /**
     * Initialises the service on application startup: loads all embedded config files,
     * resolves the initial active config, then schedules the next future activation.
     *
     * @throws IOException if the classpath resource scan fails at the I/O level
     */
    @PostConstruct
    public void init() throws IOException {
        loadAllConfigs();
        refreshActiveConfig();
        scheduleNextActivation();
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
     * <p>Called from three places:
     * <ul>
     *   <li>At startup via {@link #init()}</li>
     *   <li>By the precise one-time {@link TaskScheduler} task at each config's
     *       {@code effectiveFrom} instant</li>
     *   <li>By the daily midnight safety-net cron</li>
     * </ul>
     */
    public void refreshActiveConfig() {
        LocalDateTime now = LocalDateTime.now(NZT);
        FundConfig resolved = configHistory.stream()
                .filter(c -> !c.getEffectiveFrom().isAfter(now))
                .reduce((first, second) -> second)
                .orElse(null);

        if (resolved != null) {
            this.activeConfig = resolved;
            log.info("Active fund config set: version={} effectiveFrom={}", resolved.getVersion(), resolved.getEffectiveFrom());
        } else {
            log.warn("No fund config is effective as of now ({})", now);
        }
    }

    /**
     * Daily midnight safety net. Re-evaluates the active config and reschedules the
     * next precise activation task.
     *
     * <p>This is <em>not</em> the primary activation path — {@link #scheduleNextActivation()}
     * handles that with a single precisely-timed task. The daily cron exists to recover
     * from edge cases: app restart after a missed activation, JVM clock correction, or
     * cancellation of the one-time task.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Pacific/Auckland")
    public void dailySafetyCheck() {
        log.debug("Daily safety check: re-evaluating active config");
        refreshActiveConfig();
        scheduleNextActivation();
    }

    /**
     * Schedules a single one-time task to fire at the {@code effectiveFrom} instant of
     * the next future config version. Cancels any previously pending task first to
     * prevent duplicate firings.
     *
     * <p>If no future config exists (all versions are already effective), this is a no-op.
     * When the task fires, it activates the new config and immediately chains to schedule
     * the one after that — so the activation of v3 automatically arms the task for v4.
     */
    void scheduleNextActivation() {
        if (pendingActivation != null && !pendingActivation.isDone()) {
            pendingActivation.cancel(false);
        }

        LocalDateTime now = LocalDateTime.now(NZT);
        configHistory.stream()
                .filter(c -> c.getEffectiveFrom().isAfter(now))
                .min(Comparator.comparing(FundConfig::getEffectiveFrom))
                .ifPresent(next -> {
                    Instant when = next.getEffectiveFrom().atZone(NZT).toInstant();
                    pendingActivation = taskScheduler.schedule(() -> {
                        log.info("Precise activation trigger: version={}", next.getVersion());
                        refreshActiveConfig();
                        scheduleNextActivation();
                    }, when);
                    log.info("Scheduled precise activation: version={} at {} NZT",
                            next.getVersion(), next.getEffectiveFrom());
                });
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
     * or daily safety-net tick.
     *
     * @param version the CalVer version string to activate (e.g. {@code "2025.04.01"})
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
