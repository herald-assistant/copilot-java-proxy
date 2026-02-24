package com.acme.herald.copilot.api;

import com.acme.herald.copilot.api.dto.OpenAIChatRequest;
import com.acme.herald.copilot.api.dto.OpenAIChatResponse;
import com.acme.herald.copilot.core.CopilotWorkerLauncher;
import com.acme.herald.copilot.core.GithubTokenExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

@RestController
public class ChatCompletionsController {

    private final String defaultModel;
    private final Duration timeout;
    private final CopilotWorkerLauncher launcher;

    public ChatCompletionsController(
            CopilotWorkerLauncher launcher,
            @Value("${herald.copilot.defaultModel:claude-sonnet-4.5}") String defaultModel,
            @Value("${herald.copilot.workerTimeoutMs:120000}") long workerTimeoutMs
    ) {
        this.launcher = launcher;
        this.defaultModel = defaultModel;
        this.timeout = Duration.ofMillis(workerTimeoutMs);
    }

    @PostMapping("/chat/completions")
    public OpenAIChatResponse chatCompletions(HttpServletRequest httpReq, @Valid @RequestBody OpenAIChatRequest req) {
        String token = GithubTokenExtractor.extract(httpReq);
        try {
            GithubTokenExtractor.validateOrThrow(token);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        String model = (req.model == null || req.model.isBlank()) ? defaultModel : req.model;
        String prompt = buildPrompt(req.messages);

        try {
            String answer = launcher.runOnce(token, model, prompt, timeout);
            OpenAIChatResponse resp = new OpenAIChatResponse();
            resp.model = model;

            OpenAIChatResponse.Choice c = new OpenAIChatResponse.Choice();
            c.index = 0;
            c.message = new OpenAIChatResponse.Message(answer);

            resp.choices = List.of(c);
            return resp;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Copilot execution failed: " + safeMsg(e), e);
        }
    }

    private static String buildPrompt(List<OpenAIChatRequest.Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (OpenAIChatRequest.Message m : messages) {
            sb.append("[").append(m.role).append("]\n");
            sb.append(m.content).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static String safeMsg(Exception e) {
        String m = e.getMessage();
        return (m == null) ? e.getClass().getSimpleName() : m;
    }
}