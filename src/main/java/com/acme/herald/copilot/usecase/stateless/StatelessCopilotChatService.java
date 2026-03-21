package com.acme.herald.copilot.usecase.stateless;

import com.acme.herald.copilot.api.dto.OpenAIChatRequest;
import com.acme.herald.copilot.api.dto.OpenAIChatResponse;
import com.acme.herald.copilot.core.auth.CopilotTokenResolver;
import com.acme.herald.copilot.core.chat.OpenAiMessagesMapper;
import com.acme.herald.copilot.core.client.CopilotClientFactory;
import com.acme.herald.copilot.core.client.CopilotSessionConfigFactory;
import com.acme.herald.copilot.core.error.CopilotExceptionMapper;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class StatelessCopilotChatService {

    private static final int CLIENT_START_TIMEOUT_SECONDS = 30;
    private static final int WAIT_TIMEOUT_MS = 300_000;
    private static final int WAIT_RESULT_EXTRA_TIMEOUT_MS = 5_000;

    private final CopilotTokenResolver tokenResolver;
    private final CopilotClientFactory clientFactory;
    private final CopilotSessionConfigFactory sessionConfigFactory;

    public StatelessCopilotChatService(
            CopilotTokenResolver tokenResolver,
            CopilotClientFactory clientFactory,
            CopilotSessionConfigFactory sessionConfigFactory
    ) {
        this.tokenResolver = tokenResolver;
        this.clientFactory = clientFactory;
        this.sessionConfigFactory = sessionConfigFactory;
    }

    public OpenAIChatResponse execute(HttpServletRequest httpRequest, OpenAIChatRequest request) {
        String token = tokenResolver.resolveOrThrow(httpRequest);
        String model = request.model;
        String prompt = OpenAiMessagesMapper.flattenConversationForOneShotPrompt(request.messages);

        try (var client = clientFactory.create(token)) {
            client.start().get(CLIENT_START_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var session = client.createSession(
                    sessionConfigFactory.createChatOnlyConfig(model)
            ).get();

            var messageOptions = new MessageOptions();
            messageOptions.setPrompt(prompt);

            CompletableFuture<AssistantMessageEvent> future =
                    session.sendAndWait(messageOptions, WAIT_TIMEOUT_MS);

            AssistantMessageEvent event =
                    future.get(WAIT_TIMEOUT_MS + WAIT_RESULT_EXTRA_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            String content = extractContent(event);

            return OpenAiMessagesMapper.toChatResponse(model, content);

        } catch (Exception e) {
            throw CopilotExceptionMapper.mapExecutionFailure(StatelessCopilotChatService.class, e);
        }
    }

    private String extractContent(AssistantMessageEvent event) {
        if (event == null || event.getData() == null || event.getData().content() == null) {
            return "";
        }
        return String.valueOf(event.getData().content());
    }
}