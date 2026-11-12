package com.campusflow.storage;

import com.cloudinary.Cloudinary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Active with {@code app.storage.type=cloudinary}; otherwise {@link LocalFileStorage} is used. */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "cloudinary")
public class CloudinaryStorageConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        if (properties.url() == null || !properties.url().startsWith("cloudinary://")) {
            throw new IllegalStateException(
                    "STORAGE_TYPE=cloudinary needs CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME");
        }
        Cloudinary cloudinary = new Cloudinary(properties.url());
        cloudinary.config.secure = true;
        return cloudinary;
    }

    @Bean
    public FileStorage cloudinaryFileStorage(Cloudinary cloudinary, CloudinaryProperties properties) {
        String folder = properties.folder() == null || properties.folder().isBlank() ? "campusflow" : properties.folder();
        return new CloudinaryFileStorage(cloudinary, cloudinary.config.cloudName, folder);
    }
}
