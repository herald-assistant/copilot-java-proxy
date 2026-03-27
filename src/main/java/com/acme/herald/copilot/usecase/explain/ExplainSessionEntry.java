package com.acme.herald.copilot.usecase.explain;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.json.Attachment;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class ExplainSessionEntry {

    private final String conversationId;
    private final String tokenFingerprint;
    private final String model;
    private final String attachmentsFingerprint;
    private final List<Attachment> attachments;
    private final Path sessionDirectory;
    private final CopilotClient client;
    private final CopilotSession session;
    private final long ttlMs;
    private final Object mutex = new Object();

    private volatile long lastUsedAtMs;

    ExplainSessionEntry(
            String conversationId,
            String tokenFingerprint,
            String model,
            String attachmentsFingerprint,
            List<Attachment> attachments,
            Path sessionDirectory,
            CopilotClient client,
            CopilotSession session,
            long createdAtMs,
            long ttlMs
    ) {
        this.conversationId = conversationId;
        this.tokenFingerprint = tokenFingerprint;
        this.model = model;
        this.attachmentsFingerprint = attachmentsFingerprint;
        this.attachments = attachments == null ? List.of() : List.copyOf(attachments);
        this.sessionDirectory = sessionDirectory;
        this.client = client;
        this.session = session;
        this.lastUsedAtMs = createdAtMs;
        this.ttlMs = ttlMs;
    }

    String getConversationId() {
        return conversationId;
    }

    String getModel() {
        return model;
    }

    List<Attachment> getAttachments() {
        return attachments;
    }

    CopilotSession getSession() {
        return session;
    }

    Object getMutex() {
        return mutex;
    }

    void touch() {
        this.lastUsedAtMs = System.currentTimeMillis();
    }

    boolean isExpired(long nowMs) {
        return nowMs - lastUsedAtMs > ttlMs;
    }

    boolean matches(
            String rawToken,
            String requestedModel,
            ExplainSessionAttachments.RequestedAttachments requestedAttachments
    ) {
        String incomingFingerprint = ExplainSessionUtils.fingerprint(rawToken);
        if (!Objects.equals(tokenFingerprint, incomingFingerprint)) {
            return false;
        }

        if (requestedModel != null && !requestedModel.isBlank()
                && !Objects.equals(model, requestedModel.trim())) {
            return false;
        }

        if (requestedAttachments == null) {
            return true;
        }

        return requestedAttachments.matchesStoredFingerprint(attachmentsFingerprint);
    }

    void closeQuietly() {
        try {
            session.close();
        } catch (Exception ignored) {
            // ignore
        }

        try {
            client.close();
        } catch (Exception ignored) {
            // ignore
        }

        ExplainSessionAttachments.deleteDirectoryQuietly(sessionDirectory);
    }
}
