package com.fundmetrics.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Active Series Fund Metrics API.
 *
 * <p>{@code @SpringBootApplication} enables component scanning, auto-configuration,
 * and property source loading for the {@code com.fundmetrics.api} package.
 *
 * <p>{@code @EnableScheduling} activates the Spring task scheduler required by
 * {@link com.fundmetrics.api.service.FundConfigService}: a precise one-time
 * {@code TaskScheduler} task fires at each future config's {@code effectiveFrom}
 * instant, and a daily midnight {@code @Scheduled} cron acts as a safety net.
 */
@SpringBootApplication
@EnableScheduling
public class FundMetricsApiApplication {

    /**
     * Bootstraps the Spring application context and starts the embedded Tomcat server.
     *
     * @param args optional command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(FundMetricsApiApplication.class, args);
    }
}
