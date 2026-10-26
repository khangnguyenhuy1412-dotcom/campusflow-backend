package com.campusflow.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusflow.common.exception.ApiException;
import com.campusflow.storage.ImageProcessor.ProcessedImageSet;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor();

    private static MockMultipartFile image(String format, int width, int height, String fileName, String contentType)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
        return new MockMultipartFile("files", fileName, contentType, out.toByteArray());
    }

    private static BufferedImage read(byte[] content) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(content));
    }

    @Test
    void largeJpegIsShrunkAndThumbnailCreated() throws IOException {
        ProcessedImageSet result = processor.process(image("jpg", 3000, 2000, "a.jpg", "image/jpeg"), 1600, 400);

        BufferedImage full = read(result.full().content());
        BufferedImage thumb = read(result.thumbnail().content());
        assertThat(result.full().extension()).isEqualTo("jpg");
        assertThat(full.getWidth()).isEqualTo(1600);
        assertThat(full.getHeight()).isEqualTo(1067);
        assertThat(Math.max(thumb.getWidth(), thumb.getHeight())).isEqualTo(400);
    }

    @Test
    void smallImageIsNotEnlarged() throws IOException {
        ProcessedImageSet result = processor.process(image("png", 300, 200, "a.png", "image/png"), 1600, 400);

        assertThat(result.full().extension()).isEqualTo("png");
        assertThat(read(result.full().content()).getWidth()).isEqualTo(300);
    }

    @Test
    void formatIsDetectedFromContentNotFromNameOrContentType() throws IOException {
        // A real PNG sent with a misleading name and content type is still handled as PNG
        ProcessedImageSet result = processor.process(image("png", 100, 100, "evil.exe", "application/x-msdownload"), 1600, 400);

        assertThat(result.full().extension()).isEqualTo("png");
    }

    @Test
    void nonImageIsRejectedEvenIfItClaimsToBeJpeg() {
        MockMultipartFile fake = new MockMultipartFile("files", "photo.jpg", "image/jpeg",
                "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> processor.process(fake, 1600, 400))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("JPEG or PNG");
    }

    @Test
    void unsupportedFormatIsRejected() throws IOException {
        assertThatThrownBy(() -> processor.process(image("gif", 10, 10, "a.gif", "image/gif"), 1600, 400))
                .hasMessageContaining("JPEG or PNG");
    }

    @Test
    void emptyFileIsRejected() {
        MockMultipartFile empty = new MockMultipartFile("files", "a.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> processor.process(empty, 1600, 400)).hasMessageContaining("empty");
    }
}
