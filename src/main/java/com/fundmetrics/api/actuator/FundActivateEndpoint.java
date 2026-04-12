package com.fundmetrics.api.actuator;

import com.fundmetrics.api.service.FundConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spring Boot Actuator endpoint for emergency in-memory config version override.
 *
 * <p>Accessible only on the management port (default 8081), which is firewalled
 * to internal/ops access. The public API port (8080) has no activation capability.
 *
 * <h2>Usage</h2>
 * <pre>
 * POST http://&lt;host&gt;:8081/actuator/fund-activate
 * Content-Type: application/json
 * { "version": "2025.04.01" }
 * </pre>
 *
 * <h2>When to use</h2>
 * <p>Use only when bad fund data has activated and a redeployment cannot happen fast
 * enough. This override holds the specified version in memory until the next application
 * restart or midnight scheduler tick — it is a stopgap, not a permanent fix.
 *
 * <h2>Follow-up required</h2>
 * <p>After using this endpoint, immediately raise a CR and deploy a corrected config
 * file (fix-forward) or remove the bad file (redeploy) before the override resets.
 */
@Slf4j
@Component
@Endpoint(id = "fund-activate")
@RequiredArgsConstructor
public class FundActivateEndpoint {

    private final FundConfigService fundConfigService;

    /**
     * Force-activates a specific config version in memory, bypassing the normal
     * date-based resolution.
     *
     * @param version the CalVer version string to activate (e.g. {@code "2025.04.01"})
     * @return a result map with {@code success} flag and a human-readable {@code message}
     */
    @WriteOperation
    public Map<String, Object> activate(String version) {
        boolean success = fundConfigService.forceActivateVersion(version);
        String message = success
                ? "Activated version: " + version
                : "Version not found: " + version;
        log.warn("fund-activate actuator invoked: version={} success={}", version, success);
        return Map.of("success", success, "message", message);
    }
}
