package com.acme.herald.copilot.api;

import com.acme.herald.copilot.api.dto.OpenAIChatRequest;
import com.acme.herald.copilot.api.dto.OpenAIChatResponse;
import com.acme.herald.copilot.core.ConnectorState;
import com.acme.herald.copilot.core.GithubTokenExtractor;
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
public class ChatCompletionsController {

    private final ConnectorState state;

    public ChatCompletionsController(ConnectorState state) {
        this.state = state;
    }

    @PostMapping("/chat/completions")
    public OpenAIChatResponse chatCompletions(HttpServletRequest httpReq, @Valid @RequestBody OpenAIChatRequest req) {

        if (!state.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Connector jest OFF. Włącz go w UI.");
        }

        String token = GithubTokenExtractor.extract(httpReq);

        if (token == null || token.isBlank()) {
            token = state.getTokenOrNull();
        }

        try {
            GithubTokenExtractor.validateOrThrow(token);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        String model = req.model;
        String prompt = buildPrompt(req.messages);

        var copilotClientOptions = new CopilotClientOptions();
        copilotClientOptions.setGithubToken(token);
        copilotClientOptions.setCliPath("copilot"); // albo z properties
        copilotClientOptions.setLogLevel("info");

        try (var client = new CopilotClient(copilotClientOptions)) {
            client.start().get(30, TimeUnit.SECONDS);

            var session = client.createSession(new SessionConfig().setModel(model)).get();

            var messageOptions = new MessageOptions();
            messageOptions.setPrompt(prompt);

            int waitTimeoutMs = 300_000;
            CompletableFuture<AssistantMessageEvent> fut = session.sendAndWait(messageOptions, waitTimeoutMs);
            AssistantMessageEvent ev = fut.get(waitTimeoutMs + 5_000, TimeUnit.MILLISECONDS);

            String content = (ev.getData() == null) ? "" : String.valueOf(ev.getData().content());

            var resp = new OpenAIChatResponse();
            resp.model = model;

            var c = new OpenAIChatResponse.Choice();
            c.index = 0;
            c.message = new OpenAIChatResponse.Message(content);
            resp.choices = List.of(c);

            return resp;

        } catch (Exception e) {
            LoggerFactory.getLogger(ChatCompletionsController.class).info(e.getMessage(), e);
            throw new RuntimeException("Copilot execution failed: " + safeMsg(e), e);
        }
    }

    private static String buildPrompt(List<OpenAIChatRequest.Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (OpenAIChatRequest.Message m : messages) {
            String role = (m.role == null) ? "user" : m.role.trim().toLowerCase();
            if ("system".equals(role)) sb.append("System: ");
            else if ("assistant".equals(role)) sb.append("Assistant: ");
            else sb.append("User: ");
            sb.append(m.content).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static String safeMsg(Exception e) {
        String m = e.getMessage();
        return (m == null) ? e.getClass().getSimpleName() : m;
    }
}