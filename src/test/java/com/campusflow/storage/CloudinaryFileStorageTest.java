package com.campusflow.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CloudinaryFileStorageTest {

    private final Cloudinary cloudinary = mock(Cloudinary.class);
    private final Uploader uploader = mock(Uploader.class);
    private CloudinaryFileStorage storage;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);
        storage = new CloudinaryFileStorage(cloudinary, "demo-cloud", "campusflow");
    }

    @Test
    @SuppressWarnings("unchecked")
    void storeUploadsUnderOurFolderWithRandomNameAndReturnsHttpsUrl() throws IOException {
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url",
                "https://res.cloudinary.com/demo-cloud/image/upload/v1/campusflow/items/2026/11/x.jpg"));

        String url = storage.store("items", new byte[] {1, 2}, "jpg");

        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), options.capture());
        assertThat((String) options.getValue().get("public_id"))
                .matches("campusflow/items/\\d{4}/\\d{2}/[0-9a-f-]{36}");
        assertThat(options.getValue()).containsEntry("overwrite", false).containsEntry("format", "jpg");
        assertThat(url).startsWith("https://");
    }

    @Test
    void unsafeFolderIsRefused() {
        assertThatThrownBy(() -> storage.store("../x", new byte[0], "jpg")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicIdIsDerivedFromDeliveryUrl() {
        assertThat(storage.publicIdOf(
                "https://res.cloudinary.com/demo-cloud/image/upload/v1712345/campusflow/items/2026/11/abc.jpg"))
                .isEqualTo("campusflow/items/2026/11/abc");
    }

    @Test
    void foreignUrlsAreNeverDeleted() throws IOException {
        storage.delete("https://res.cloudinary.com/other-cloud/image/upload/v1/campusflow/items/a.jpg");
        storage.delete("https://res.cloudinary.com/demo-cloud/image/upload/v1/someone-else/a.jpg");
        storage.delete("/uploads/items/2026/11/a.jpg");

        verify(uploader, never()).destroy(any(), anyMap());
    }

    @Test
    void deleteDestroysOurFile() throws IOException {
        storage.delete("https://res.cloudinary.com/demo-cloud/image/upload/v9/campusflow/avatars/2026/11/me.png");

        verify(uploader).destroy(eq("campusflow/avatars/2026/11/me"), anyMap());
    }
}
