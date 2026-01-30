package org.bnemu.bnftp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves BNFTP file requests to file data from a local directory.
 */
public class BnftpFileProvider {
    private static final Logger logger = LoggerFactory.getLogger(BnftpFileProvider.class);

    private final Path filesDirectory;

    public BnftpFileProvider(Path filesDirectory) {
        this.filesDirectory = filesDirectory;
        if (!Files.isDirectory(filesDirectory)) {
            logger.warn("BNFTP files directory does not exist: {}", filesDirectory.toAbsolutePath());
        } else {
            logger.info("BNFTP serving files from: {}", filesDirectory.toAbsolutePath());
        }
    }

    /**
     * Returns the file contents for the given filename, or null if not found.
     * Sanitizes the filename to prevent path traversal.
     */
    public byte[] getFile(String filename) {
        String sanitized = sanitizeFilename(filename);
        if (sanitized.isEmpty()) {
            logger.warn("BNFTP: Rejected empty/invalid filename: '{}'", filename);
            return null;
        }

        Path filePath = filesDirectory.resolve(sanitized);

        // Verify the resolved path is still within the files directory
        if (!filePath.normalize().startsWith(filesDirectory.normalize())) {
            logger.warn("BNFTP: Path traversal attempt blocked: '{}'", filename);
            return null;
        }

        if (!Files.isRegularFile(filePath)) {
            logger.warn("BNFTP: File not found: '{}'", sanitized);
            return null;
        }

        try {
            byte[] data = Files.readAllBytes(filePath);
            logger.info("BNFTP: Serving file '{}' ({} bytes)", sanitized, data.length);
            return data;
        } catch (IOException e) {
            logger.error("BNFTP: Failed to read file '{}': {}", sanitized, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the last-modified time of a file as a Windows FILETIME value
     * (100-nanosecond intervals since January 1, 1601), or 0 if unavailable.
     */
    public long getFiletime(String filename) {
        String sanitized = sanitizeFilename(filename);
        if (sanitized.isEmpty()) return 0;

        Path filePath = filesDirectory.resolve(sanitized);
        if (!filePath.normalize().startsWith(filesDirectory.normalize())) return 0;
        if (!Files.isRegularFile(filePath)) return 0;

        try {
            long lastModifiedMillis = Files.getLastModifiedTime(filePath).toMillis();
            // Convert Unix millis to Windows FILETIME:
            // FILETIME epoch is Jan 1, 1601. Unix epoch is Jan 1, 1970.
            // Difference is 11644473600 seconds.
            return (lastModifiedMillis + 11644473600000L) * 10000L;
        } catch (IOException e) {
            return 0;
        }
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null) return "";
        // Strip any directory components
        String name = filename.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        return name.trim();
    }
}
