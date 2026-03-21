package com.acme.herald.copilot.usecase.explain;

import com.acme.herald.copilot.core.client.CopilotClientFactory;
import com.acme.herald.copilot.core.client.CopilotSessionConfigFactory;
import com.acme.herald.copilot.core.error.CopilotExceptionMapper;
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class ExplainSessionRegistry {

    private static final int CLIENT_START_TIMEOUT_SECONDS = 30;
    private static final long SESSION_TTL_MS = 30L * 60L * 1000L;

    private final CopilotClientFactory clientFactory;
    private final CopilotSessionConfigFactory sessionConfigFactory;
    private final Map<String, ExplainSessionEntry> sessions = new ConcurrentHashMap<>();

    public ExplainSessionRegistry(
            CopilotClientFactory clientFactory,
            CopilotSessionConfigFactory sessionConfigFactory
    ) {
        this.clientFactory = clientFactory;
        this.sessionConfigFactory = sessionConfigFactory;
    }

    public ExplainSessionHandle getOrCreate(String conversationId, String token, String requestedModel) {
        evictExpiredSessions();

        ExplainSessionEntry existing = sessions.get(conversationId);
        if (existing != null && existing.matches(token, requestedModel) && !existing.isExpired(now())) {
            existing.touch();
            return new ExplainSessionHandle(existing, false);
        }

        synchronized (this) {
            evictExpiredSessions();

            ExplainSessionEntry current = sessions.get(conversationId);
            if (current != null && current.matches(token, requestedModel) && !current.isExpired(now())) {
                current.touch();
                return new ExplainSessionHandle(current, false);
            }

            if (current != null) {
                sessions.remove(conversationId);
                current.closeQuietly();
            }

            ExplainSessionEntry created = createEntry(conversationId, token, requestedModel);
            sessions.put(conversationId, created);
            return new ExplainSessionHandle(created, true);
        }
    }

    public void close(String conversationId) {
        ExplainSessionEntry removed = sessions.remove(conversationId);
        if (removed != null) {
            removed.closeQuietly();
        }
    }

    public void closeIfBroken(String conversationId, ExplainSessionEntry expectedEntry) {
        synchronized (this) {
            ExplainSessionEntry current = sessions.get(conversationId);
            if (current == expectedEntry) {
                sessions.remove(conversationId);
                current.closeQuietly();
            }
        }
    }

    private ExplainSessionEntry createEntry(String conversationId, String token, String requestedModel) {
        String normalizedModel = normalizeModel(requestedModel);

        CopilotClient client = null;
        try {
            client = clientFactory.create(token);
            client.start().get(CLIENT_START_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            CopilotSession session = client.createSession(
                    sessionConfigFactory.createChatOnlyConfig(normalizedModel)
            ).get();

            return new ExplainSessionEntry(
                    conversationId,
                    ExplainSessionUtils.fingerprint(token),
                    normalizedModel,
                    client,
                    session,
                    now(),
                    SESSION_TTL_MS
            );
        } catch (Exception e) {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ignored) {
                    // ignore close failure
                }
            }
            throw CopilotExceptionMapper.mapExecutionFailure(ExplainSessionRegistry.class, e);
        }
    }

    private void evictExpiredSessions() {
        long now = now();

        for (Map.Entry<String, ExplainSessionEntry> item : sessions.entrySet()) {
            ExplainSessionEntry entry = item.getValue();
            if (!entry.isExpired(now)) {
                continue;
            }

            if (sessions.remove(item.getKey(), entry)) {
                entry.closeQuietly();
            }
        }
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        return model.trim();
    }

    private long now() {
        return System.currentTimeMillis();
    }
}