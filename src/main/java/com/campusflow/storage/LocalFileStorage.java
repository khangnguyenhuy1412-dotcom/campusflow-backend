package com.campusflow.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Writes files under {@code app.storage.local-root}, in folders like {@code items/2026/10/<uuid>.jpg}. */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private static final Pattern SAFE_FOLDER = Pattern.compile("[a-z0-9-]+");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,5}");

    private final Path root;
    private final String publicUrlPrefix;

    public LocalFileStorage(StorageProperties properties) {
        this.root = Path.of(properties.localRoot()).toAbsolutePath().normalize();
        this.publicUrlPrefix = properties.publicUrlPrefix();
    }

    @Override
    public String store(String folder, byte[] content, String extension) {
        if (!SAFE_FOLDER.matcher(folder).matches() || !SAFE_EXTENSION.matcher(extension).matches()) {
            throw new IllegalArgumentException("Unsafe folder or extension");
        }
        YearMonth month = YearMonth.now();
        String relative = "%s/%d/%02d/%s.%s".formatted(
                folder, month.getYear(), month.getMonthValue(), UUID.randomUUID(), extension);
        Path target = root.resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file", e);
        }
        return publicUrlPrefix + "/" + relative;
    }

    @Override
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(publicUrlPrefix + "/")) {
            return;
        }
        Path target = root.resolve(publicUrl.substring(publicUrlPrefix.length() + 1)).normalize();
        // Never delete anything outside the storage folder, whatever the URL contains
        if (!target.startsWith(root)) {
            log.warn("Refused to delete file outside storage root: {}", publicUrl);
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Could not delete file {}", target, e);
        }
    }

    public Path root() {
        return root;
    }
}
