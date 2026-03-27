package com.acme.herald.copilot.usecase.explain;

import com.acme.herald.copilot.api.dto.ExplainChatRequest;
import com.github.copilot.sdk.json.Attachment;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class ExplainSessionAttachments {

    private static final String SESSIONS_DIRECTORY_NAME = "herald-explain-sessions";

    private ExplainSessionAttachments() {
    }

    static RequestedAttachments fromRequest(
            List<String> files,
            List<ExplainChatRequest.InlineFile> inlineFiles
    ) {
        List<Attachment> pathAttachments = normalizePathAttachments(files);
        List<InlineAttachment> normalizedInlineFiles = normalizeInlineFiles(inlineFiles);
        boolean explicitAttachmentsRequested = !pathAttachments.isEmpty() || !normalizedInlineFiles.isEmpty();

        String fingerprint = explicitAttachmentsRequested
                ? buildFingerprint(pathAttachments, normalizedInlineFiles)
                : null;

        return new RequestedAttachments(
                pathAttachments,
                normalizedInlineFiles,
                explicitAttachmentsRequested,
                fingerprint
        );
    }

    static void deleteDirectoryQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // ignore cleanup failure
                }
            });
        } catch (IOException ignored) {
            // ignore cleanup failure
        }
    }

    private static List<Attachment> normalizePathAttachments(List<String> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<Attachment> attachments = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            String rawPath = files.get(i);
            if (rawPath == null || rawPath.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Attached file path at index " + i + " must not be blank."
                );
            }

            Path resolvedPath = resolveExistingFile(rawPath.trim(), i);
            String attachmentPath = resolvedPath.toString();
            boolean alreadyPresent = attachments.stream()
                    .anyMatch(attachment -> attachment.path().equals(attachmentPath));
            if (alreadyPresent) {
                continue;
            }

            attachments.add(new Attachment("file", attachmentPath, displayNameFor(resolvedPath)));
        }

        attachments.sort(Comparator.comparing(Attachment::path));
        return List.copyOf(attachments);
    }

    private static List<InlineAttachment> normalizeInlineFiles(List<ExplainChatRequest.InlineFile> inlineFiles) {
        if (inlineFiles == null || inlineFiles.isEmpty()) {
            return List.of();
        }

        List<InlineAttachment> normalized = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        for (int i = 0; i < inlineFiles.size(); i++) {
            ExplainChatRequest.InlineFile inlineFile = inlineFiles.get(i);
            if (inlineFile == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Inline file at index " + i + " must not be null."
                );
            }

            String relativePath = normalizeInlineFileName(inlineFile.name, i);
            if (!usedNames.add(relativePath)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate inline file name at index " + i + ": " + relativePath
                );
            }

            String content = inlineFile.content;
            if (content == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Inline file content at index " + i + " must not be null."
                );
            }

            normalized.add(new InlineAttachment(relativePath, content));
        }

        normalized.sort(Comparator.comparing(InlineAttachment::relativePath));
        return List.copyOf(normalized);
    }

    private static String normalizeInlineFileName(String rawName, int index) {
        if (rawName == null || rawName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inline file name at index " + index + " must not be blank."
            );
        }

        try {
            Path rawPath = Paths.get(rawName.trim()).normalize();
            if (rawPath.isAbsolute()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Inline file name at index " + index + " must be a relative path: " + rawName
                );
            }

            String normalizedText = rawPath.toString();
            if (normalizedText.isBlank()
                    || normalizedText.equals(".")
                    || normalizedText.startsWith("..")
                    || rawPath.getFileName() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Inline file name at index " + index + " escapes the session directory: " + rawName
                );
            }

            return normalizedText;
        } catch (InvalidPathException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid inline file name at index " + index + ": " + rawName,
                    e
            );
        }
    }

    private static Path resolveExistingFile(String rawPath, int index) {
        try {
            Path path = Paths.get(rawPath);
            Path normalizedPath = (path.isAbsolute() ? path : path.toAbsolutePath())
                    .normalize()
                    .toRealPath();

            if (!Files.isRegularFile(normalizedPath)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Attached path at index " + index + " is not a regular file: " + normalizedPath
                );
            }

            if (!Files.isReadable(normalizedPath)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Attached file at index " + index + " is not readable: " + normalizedPath
                );
            }

            return normalizedPath;
        } catch (InvalidPathException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid attached file path at index " + index + ": " + rawPath,
                    e
            );
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attached file at index " + index + " does not exist or is not accessible: " + rawPath,
                    e
            );
        }
    }

    private static String buildFingerprint(List<Attachment> pathAttachments, List<InlineAttachment> inlineFiles) {
        StringBuilder payload = new StringBuilder();
        for (Attachment attachment : pathAttachments) {
            payload.append("path:")
                    .append(attachment.path())
                    .append('|')
                    .append(attachment.displayName())
                    .append('\n');
        }
        for (InlineAttachment inlineFile : inlineFiles) {
            payload.append("inline:")
                    .append(inlineFile.relativePath())
                    .append('|')
                    .append(ExplainSessionUtils.fingerprint(inlineFile.content()))
                    .append('\n');
        }
        return ExplainSessionUtils.fingerprint(payload.toString());
    }

    private static String displayNameFor(Path path) {
        return path.getFileName() != null ? path.getFileName().toString() : path.toString();
    }

    private static Path sessionStorageRoot() {
        return Paths.get(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize()
                .resolve(SESSIONS_DIRECTORY_NAME);
    }

    private static Path sessionDirectory(String conversationId) {
        String normalizedConversationId = conversationId == null ? "session" : conversationId.trim();
        String baseName = normalizedConversationId.replaceAll("[^A-Za-z0-9._-]", "_");
        if (baseName.isBlank()) {
            baseName = "session";
        }
        if (baseName.length() > 48) {
            baseName = baseName.substring(0, 48);
        }

        String hash = ExplainSessionUtils.fingerprint(normalizedConversationId)
                .replace("/", "_")
                .replace("+", "-")
                .replace("=", "");

        return sessionStorageRoot().resolve(baseName + "-" + hash.substring(0, Math.min(hash.length(), 12)));
    }

    record RequestedAttachments(
            List<Attachment> pathAttachments,
            List<InlineAttachment> inlineFiles,
            boolean explicitAttachmentsRequested,
            String fingerprint
    ) {
        RequestedAttachments {
            pathAttachments = pathAttachments == null ? List.of() : List.copyOf(pathAttachments);
            inlineFiles = inlineFiles == null ? List.of() : List.copyOf(inlineFiles);
        }

        boolean matchesStoredFingerprint(String storedFingerprint) {
            if (!explicitAttachmentsRequested) {
                return true;
            }
            return Objects.equals(fingerprint, storedFingerprint);
        }

        MaterializedAttachments materializeForSession(String conversationId) {
            Path sessionDirectory = null;
            try {
                List<Attachment> attachments = new ArrayList<>(pathAttachments);
                if (!inlineFiles.isEmpty()) {
                    Files.createDirectories(sessionStorageRoot());
                    sessionDirectory = sessionDirectory(conversationId);
                    deleteDirectoryQuietly(sessionDirectory);
                    Files.createDirectories(sessionDirectory);

                    for (InlineAttachment inlineFile : inlineFiles) {
                        Path target = sessionDirectory.resolve(inlineFile.relativePath()).normalize();
                        if (!target.startsWith(sessionDirectory)) {
                            throw new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "Inline file escapes the session directory: " + inlineFile.relativePath()
                            );
                        }

                        Path parent = target.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }

                        Files.writeString(
                                target,
                                inlineFile.content(),
                                StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING,
                                StandardOpenOption.WRITE
                        );

                        attachments.add(new Attachment("file", target.toAbsolutePath().normalize().toString(),
                                displayNameFor(target)));
                    }
                }

                attachments.sort(Comparator.comparing(Attachment::path));
                return new MaterializedAttachments(List.copyOf(attachments), sessionDirectory, fingerprint);
            } catch (IOException e) {
                deleteDirectoryQuietly(sessionDirectory);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to persist inline files for explain session.",
                        e
                );
            }
        }
    }

    record MaterializedAttachments(List<Attachment> attachments, Path sessionDirectory, String fingerprint) {
        MaterializedAttachments {
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
        }
    }

    private record InlineAttachment(String relativePath, String content) {
    }
}
