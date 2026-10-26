package com.campusflow.storage;

import com.campusflow.common.exception.ApiException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploaded images and re-encodes them. The content is checked by decoding it,
 * never by trusting the file name or Content-Type sent by the client. Re-encoding also
 * removes metadata such as the GPS location phones store in photos.
 */
@Component
public class ImageProcessor {

    /** Allowed formats (ImageIO format name -> file extension). */
    private static final Map<String, String> ALLOWED_FORMATS = Map.of("jpeg", "jpg", "png", "png");
    /** Rejects "decompression bombs": small files that expand to huge images in memory. */
    private static final long MAX_PIXELS = 40_000_000L;
    private static final double JPEG_QUALITY = 0.85;

    public record ProcessedImage(byte[] content, String extension) {
    }

    public record ProcessedImageSet(ProcessedImage full, ProcessedImage thumbnail) {
    }

    private record ImageInfo(String extension, int width, int height) {
    }

    /** Produces a full-size version (at most {@code maxSide} px) and a thumbnail (at most {@code thumbSide} px). */
    public ProcessedImageSet process(MultipartFile file, int maxSide, int thumbSide) {
        byte[] original = readBytes(file);
        ImageInfo info = inspect(original);
        return new ProcessedImageSet(
                resize(original, info, maxSide),
                resize(original, info, thumbSide));
    }

    /** Produces a single version of at most {@code maxSide} px, e.g. for avatars. */
    public ProcessedImage processSingle(MultipartFile file, int maxSide) {
        byte[] original = readBytes(file);
        return resize(original, inspect(original), maxSide);
    }

    private static byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Image file is empty");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file");
        }
    }

    /** Reads only the header to find the real format and size, without decoding the pixels. */
    private static ImageInfo inspect(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = input == null ? null : ImageIO.getImageReaders(input);
            if (readers == null || !readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                String extension = ALLOWED_FORMATS.get(reader.getFormatName().toLowerCase(Locale.ROOT));
                if (extension == null) {
                    throw invalidImage();
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;
                if (pixels <= 0 || pixels > MAX_PIXELS) {
                    throw ApiException.badRequest("Image dimensions are too large");
                }
                return new ImageInfo(extension, width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw invalidImage();
        }
    }

    private static ProcessedImage resize(byte[] original, ImageInfo info, int maxSide) {
        String extension = info.extension();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Thumbnails.Builder<?> builder = Thumbnails.of(new ByteArrayInputStream(original));
            // Only shrink: small images keep their size instead of being blown up
            if (Math.max(info.width(), info.height()) > maxSide) {
                builder.size(maxSide, maxSide).keepAspectRatio(true);
            } else {
                builder.scale(1.0);
            }
            builder.useExifOrientation(true).outputFormat(extension);
            if ("jpg".equals(extension)) {
                builder.outputQuality(JPEG_QUALITY);
            }
            builder.toOutputStream(output);
            return new ProcessedImage(output.toByteArray(), extension);
        } catch (IOException | IllegalArgumentException e) {
            throw invalidImage();
        }
    }

    private static ApiException invalidImage() {
        return ApiException.badRequest("Only valid JPEG or PNG images are allowed");
    }
}
