package com.campusflow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Async (background emails) and @Scheduled (nightly housekeeping); both run on
 * Spring Boot's auto-configured executors.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
