package com.acme.herald.copilot.core.auth;

import com.acme.herald.copilot.core.ConnectorState;
import com.acme.herald.copilot.core.GithubTokenExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CopilotTokenResolver {

    private final ConnectorState state;

    public CopilotTokenResolver(ConnectorState state) {
        this.state = state;
    }

    public String resolveOrThrow(HttpServletRequest request) {
        ensureConnectorEnabled();

        String token = GithubTokenExtractor.extract(request);
        if (token == null || token.isBlank()) {
            token = state.getTokenOrNull();
        }

        try {
            GithubTokenExtractor.validateOrThrow(token);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        return token;
    }

    private void ensureConnectorEnabled() {
        if (!state.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Connector jest OFF. Włącz go w UI."
            );
        }
    }
}