package com.example.chat.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.chat.exception.ValidationException;

import jakarta.servlet.http.Part;

@Service
public class FileStorageService {

    /**
     * Allowed content types -> the extension the file is stored under.
     *
     * The stored extension always comes from this table, never from the
     * client-supplied file name. Otherwise someone could upload "evil.html"
     * with a Content-Type of image/png, and /uploads/ would later serve it as
     * text/html from our own origin.
     */
    private static final Map<String, String> ALLOWED_TYPES = new LinkedHashMap<>();

    static {
        // images
        ALLOWED_TYPES.put("image/jpeg", ".jpg");
        ALLOWED_TYPES.put("image/png", ".png");
        ALLOWED_TYPES.put("image/gif", ".gif");
        ALLOWED_TYPES.put("image/webp", ".webp");
        // video
        ALLOWED_TYPES.put("video/mp4", ".mp4");
        ALLOWED_TYPES.put("video/webm", ".webm");
        ALLOWED_TYPES.put("video/quicktime", ".mov");
        // audio (mp3 and friends, plus what browsers produce when recording voice messages)
        ALLOWED_TYPES.put("audio/mpeg", ".mp3");
        ALLOWED_TYPES.put("audio/mp3", ".mp3");
        ALLOWED_TYPES.put("audio/wav", ".wav");
        ALLOWED_TYPES.put("audio/x-wav", ".wav");
        ALLOWED_TYPES.put("audio/wave", ".wav");
        ALLOWED_TYPES.put("audio/ogg", ".ogg");
        ALLOWED_TYPES.put("audio/webm", ".webm");   // Chrome / Edge / Firefox voice recordings
        ALLOWED_TYPES.put("audio/mp4", ".m4a");     // Safari voice recordings
        ALLOWED_TYPES.put("audio/x-m4a", ".m4a");
        ALLOWED_TYPES.put("audio/aac", ".aac");
        // documents
        ALLOWED_TYPES.put("application/pdf", ".pdf");
        ALLOWED_TYPES.put("text/plain", ".txt");
        ALLOWED_TYPES.put("text/csv", ".csv");
        ALLOWED_TYPES.put("application/msword", ".doc");
        ALLOWED_TYPES.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
        ALLOWED_TYPES.put("application/vnd.ms-excel", ".xls");
        ALLOWED_TYPES.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");
        ALLOWED_TYPES.put("application/vnd.ms-powerpoint", ".ppt");
        ALLOWED_TYPES.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx");
    }

    /** Some browsers/OSes send no type (or a generic one) for less common files, so fall back to the extension. */
    private static final Map<String, String> TYPE_BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"), Map.entry("gif", "image/gif"), Map.entry("webp", "image/webp"),
            Map.entry("mp4", "video/mp4"), Map.entry("webm", "video/webm"), Map.entry("mov", "video/quicktime"),
            Map.entry("mp3", "audio/mpeg"), Map.entry("wav", "audio/wav"), Map.entry("ogg", "audio/ogg"),
            Map.entry("m4a", "audio/mp4"), Map.entry("aac", "audio/aac"),
            Map.entry("pdf", "application/pdf"), Map.entry("txt", "text/plain"), Map.entry("csv", "text/csv"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
    );

    private final long maxBytes;
    private final Path root;

    public FileStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${app.upload.max-bytes:52428800}") long maxBytes) {

        this.maxBytes = maxBytes;
        this.root = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not create upload directory: " + root,
                    e
            );
        }
    }

    public Map<String, Object> store(Part file) {

        if (file == null || file.getSize() == 0) {
            throw new ValidationException(
                    Map.of("file", "No file was uploaded")
            );
        }

        if (file.getSize() > maxBytes) {
            throw new ValidationException(
                    Map.of("file", "File must be smaller than " + (maxBytes / (1024 * 1024)) + " MB")
            );
        }

        String originalName = StringUtils.cleanPath(
                file.getSubmittedFileName() != null
                        ? file.getSubmittedFileName()
                        : "file"
        );

        String contentType = resolveContentType(file.getContentType(), originalName);

        if (contentType == null) {
            String sent = file.getContentType();
            throw new ValidationException(
                    Map.of("file", "Unsupported file type" + (sent != null && !sent.isBlank() ? ": " + sent : ""))
            );
        }

        String storedName = UUID.randomUUID() + ALLOWED_TYPES.get(contentType);

        try {
            Path target = root
                    .resolve(storedName)
                    .normalize();

            if (!target.startsWith(root)) {
                throw new ValidationException(
                        Map.of("file", "Invalid file name")
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(
                        inputStream,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to store file",
                    e
            );
        }

        return Map.of(
                "url", "/uploads/" + storedName,
                "type", contentType,
                "category", mediaCategory(contentType),
                "fileName", originalName,
                "size", file.getSize()
        );
    }

    /**
     * Normalises the client-sent content type ("audio/webm;codecs=opus" -> "audio/webm")
     * and returns it if allowed, otherwise tries the file extension. Returns null when the
     * file is not an allowed type.
     */
    private String resolveContentType(String sent, String fileName) {
        if (sent != null) {
            String base = sent.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (ALLOWED_TYPES.containsKey(base)) {
                return base;
            }
            boolean generic = base.isEmpty() || base.equals("application/octet-stream");
            if (!generic) {
                return null;
            }
        }
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            return TYPE_BY_EXTENSION.get(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
        }
        return null;
    }

    private String mediaCategory(String contentType) {
        if (contentType.startsWith("image/")) {
            return "IMAGE";
        }

        if (contentType.startsWith("video/")) {
            return "VIDEO";
        }

        if (contentType.startsWith("audio/")) {
            return "AUDIO";
        }

        return "FILE";
    }

    public List<String> allowedTypes() {
        return List.copyOf(ALLOWED_TYPES.keySet());
    }
}
