package com.campusflow.storage;

/**
 * Stores uploaded files. Callers only see public URLs, so the local implementation
 * can later be replaced by cloud storage (S3, MinIO, Cloudinary) without changing them.
 */
public interface FileStorage {

    /**
     * Saves the content under a new random file name.
     *
     * @param folder    logical folder such as "items" or "avatars"
     * @param extension file extension without the dot, e.g. "jpg"
     * @return the public URL of the stored file
     */
    String store(String folder, byte[] content, String extension);

    /** Deletes a file previously returned by {@link #store}. Unknown URLs are ignored. */
    void delete(String publicUrl);
}
