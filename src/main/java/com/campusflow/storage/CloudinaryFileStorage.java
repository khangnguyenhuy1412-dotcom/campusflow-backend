package com.campusflow.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Stores images on Cloudinary (served from its CDN). Files arrive already validated and re-encoded
 * by ImageProcessor, so Cloudinary only stores them. Uploads are signed on the server; the API
 * secret never reaches clients.
 */
@Slf4j
public class CloudinaryFileStorage implements FileStorage {

    private static final Pattern SAFE_FOLDER = Pattern.compile("[a-z0-9-]+");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,5}");
    private static final Pattern VERSION_SEGMENT = Pattern.compile("^v\\d+/");

    private final Cloudinary cloudinary;
    private final String rootFolder;
    /** e.g. https://res.cloudinary.com/my-cloud/image/upload/ */
    private final String deliveryPrefix;

    public CloudinaryFileStorage(Cloudinary cloudinary, String cloudName, String rootFolder) {
        this.cloudinary = cloudinary;
        this.rootFolder = rootFolder;
        this.deliveryPrefix = "https://res.cloudinary.com/" + cloudName + "/image/upload/";
    }

    @Override
    public String store(String folder, byte[] content, String extension) {
        if (!SAFE_FOLDER.matcher(folder).matches() || !SAFE_EXTENSION.matcher(extension).matches()) {
            throw new IllegalArgumentException("Unsafe folder or extension");
        }
        YearMonth month = YearMonth.now();
        String publicId = "%s/%s/%d/%02d/%s".formatted(
                rootFolder, folder, month.getYear(), month.getMonthValue(), UUID.randomUUID());
        try {
            Map<?, ?> result = cloudinary.uploader().upload(content, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "format", extension,
                    "overwrite", false));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not upload file to Cloudinary", e);
        }
    }

    @Override
    public void delete(String publicUrl) {
        String publicId = publicIdOf(publicUrl);
        if (publicId == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        } catch (IOException | RuntimeException e) {
            // A leftover file only costs storage; it must not break the user's action
            log.warn("Could not delete Cloudinary file {}", publicId, e);
        }
    }

    /**
     * Turns a delivery URL back into its public id, e.g.
     * ".../image/upload/v1712345/campusflow/items/2026/11/abc.jpg" -> "campusflow/items/2026/11/abc".
     * Returns null for URLs that are not ours, so nothing outside our folder is ever deleted.
     */
    String publicIdOf(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(deliveryPrefix)) {
            return null;
        }
        String path = VERSION_SEGMENT.matcher(publicUrl.substring(deliveryPrefix.length())).replaceFirst("");
        int dot = path.lastIndexOf('.');
        String publicId = dot > path.lastIndexOf('/') ? path.substring(0, dot) : path;
        return publicId.startsWith(rootFolder + "/") && !publicId.contains("..") ? publicId : null;
    }
}
