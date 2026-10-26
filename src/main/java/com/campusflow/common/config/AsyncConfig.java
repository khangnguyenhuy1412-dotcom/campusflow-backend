package com.campusflow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables @Async; methods run on Spring Boot's auto-configured task executor. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
