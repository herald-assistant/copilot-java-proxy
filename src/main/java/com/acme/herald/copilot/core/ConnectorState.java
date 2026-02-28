package com.acme.herald.copilot.core;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ConnectorState {

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicReference<String> token = new AtomicReference<>(null);

    public boolean isEnabled() {
        return enabled.get();
    }

    public void enableWithToken(String githubPat) {
        token.set(githubPat);
        enabled.set(true);
    }

    public void disableAndClearToken() {
        enabled.set(false);
        token.set(null);
    }

    public String getTokenOrNull() {
        return token.get();
    }
}