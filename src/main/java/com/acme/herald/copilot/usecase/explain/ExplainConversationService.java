package com.acme.herald.copilot.usecase.explain;

import com.acme.herald.copilot.api.dto.ExplainChatRequest;
import com.acme.herald.copilot.api.dto.OpenAIChatResponse;
import com.acme.herald.copilot.core.auth.CopilotTokenResolver;
import com.acme.herald.copilot.core.chat.OpenAiMessagesMapper;
import com.acme.herald.copilot.core.error.CopilotExceptionMapper;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ExplainConversationService {

    private static final int WAIT_TIMEOUT_MS = 300_000;
    private static final int WAIT_RESULT_EXTRA_TIMEOUT_MS = 5_000;

    private final CopilotTokenResolver tokenResolver;
    private final ExplainSessionRegistry explainSessionRegistry;

    public ExplainConversationService(
            CopilotTokenResolver tokenResolver,
            ExplainSessionRegistry explainSessionRegistry
    ) {
        this.tokenResolver = tokenResolver;
        this.explainSessionRegistry = explainSessionRegistry;
    }

    public OpenAIChatResponse execute(HttpServletRequest httpRequest, ExplainChatRequest request) {
        String token = tokenResolver.resolveOrThrow(httpRequest);
        ExplainSessionAttachments.RequestedAttachments requestedAttachments =
                ExplainSessionAttachments.fromRequest(request.files, request.inlineFiles);

        boolean resetRequested = Boolean.TRUE.equals(request.reset);
        if (resetRequested) {
            explainSessionRegistry.close(request.conversationId);
        }

        ExplainSessionHandle handle = explainSessionRegistry.getOrCreate(
                request.conversationId,
                token,
                request.model,
                requestedAttachments
        );

        ExplainSessionEntry entry = handle.entry();

        String prompt = handle.created()
                ? OpenAiMessagesMapper.flattenConversationForOneShotPrompt(request.messages)
                : OpenAiMessagesMapper.extractLatestUserPromptOrFallback(request.messages);

        try {
            synchronized (entry.getMutex()) {
                entry.touch();

                var messageOptions = new MessageOptions();
                messageOptions.setPrompt(prompt);
                if (!entry.getAttachments().isEmpty()) {
                    messageOptions.setAttachments(entry.getAttachments());
                }

                CompletableFuture<AssistantMessageEvent> future =
                        entry.getSession().sendAndWait(messageOptions, WAIT_TIMEOUT_MS);

                AssistantMessageEvent event =
                        future.get(WAIT_TIMEOUT_MS + WAIT_RESULT_EXTRA_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                String content = extractContent(event);
                String responseModel = request.model != null && !request.model.isBlank()
                        ? request.model
                        : entry.getModel();

                return OpenAiMessagesMapper.toChatResponse(responseModel, content);
            }
        } catch (Exception e) {
            explainSessionRegistry.closeIfBroken(entry.getConversationId(), entry);
            throw CopilotExceptionMapper.mapExecutionFailure(ExplainConversationService.class, e);
        }
    }

    public void close(String conversationId) {
        explainSessionRegistry.close(conversationId);
    }

    private String extractContent(AssistantMessageEvent event) {
        if (event == null || event.getData() == null || event.getData().content() == null) {
            return "";
        }
        return String.valueOf(event.getData().content());
    }
}
