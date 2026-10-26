package com.campusflow.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param localRoot       folder on disk where uploaded files are written
 * @param publicUrlPrefix URL path the files are served under, e.g. "/uploads"
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String localRoot, String publicUrlPrefix) {
}
