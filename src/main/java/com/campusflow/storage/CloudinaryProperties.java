package com.campusflow.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Used when {@code app.storage.type=cloudinary}.
 *
 * @param url    connection string from the Cloudinary dashboard: cloudinary://API_KEY:API_SECRET@CLOUD_NAME
 * @param folder top-level folder for all CampusFlow uploads, e.g. "campusflow"
 */
@ConfigurationProperties(prefix = "app.storage.cloudinary")
public record CloudinaryProperties(String url, String folder) {
}
