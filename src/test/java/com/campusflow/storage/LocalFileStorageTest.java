package com.campusflow.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage() {
        return new LocalFileStorage(new StorageProperties(tempDir.resolve("uploads").toString(), "/uploads"));
    }

    @Test
    void storeWritesUnderRandomNameAndDeleteRemovesIt() {
        LocalFileStorage storage = storage();

        String url = storage.store("items", new byte[] {1, 2, 3}, "jpg");

        assertThat(url).matches("/uploads/items/\\d{4}/\\d{2}/[0-9a-f-]{36}\\.jpg");
        Path file = storage.root().resolve(url.substring("/uploads/".length()));
        assertThat(file).exists();

        storage.delete(url);
        assertThat(file).doesNotExist();
    }

    @Test
    void unsafeFolderOrExtensionIsRefused() {
        LocalFileStorage storage = storage();

        assertThatThrownBy(() -> storage.store("../etc", new byte[0], "jpg")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.store("items", new byte[0], "j/sp")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteNeverLeavesTheStorageRoot() throws IOException {
        Path outside = Files.writeString(tempDir.resolve("secret.txt"), "keep me");

        storage().delete("/uploads/../secret.txt");

        assertThat(outside).exists();
    }
}
