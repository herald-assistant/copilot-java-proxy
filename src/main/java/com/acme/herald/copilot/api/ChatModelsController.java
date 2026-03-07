package com.acme.herald.copilot.api;

import com.acme.herald.copilot.core.ConnectorState;
import com.acme.herald.copilot.core.GithubTokenExtractor;
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.ModelInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class ChatModelsController {

    private final ConnectorState state;

    public ChatModelsController(ConnectorState state) {
        this.state = state;
    }

    @GetMapping("/models")
    public List<ModelInfo> getModels(HttpServletRequest httpReq) {

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

        var copilotClientOptions = new CopilotClientOptions();
        copilotClientOptions.setGithubToken(token);
        copilotClientOptions.setCliPath("copilot");
        copilotClientOptions.setLogLevel("info");

        try (var client = new CopilotClient(copilotClientOptions)) {
            client.start().get(30, TimeUnit.SECONDS);

            return client.listModels().get(30, TimeUnit.SECONDS);

        } catch (Exception e) {
            LoggerFactory.getLogger(ChatModelsController.class).info(e.getMessage(), e);
            throw new RuntimeException("Copilot execution failed: " + safeMsg(e), e);
        }
    }

    private static String safeMsg(Exception e) {
        String m = e.getMessage();
        return (m == null) ? e.getClass().getSimpleName() : m;
    }
}