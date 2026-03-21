package com.acme.herald.copilot.core.client;

import com.github.copilot.sdk.SystemMessageMode;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CopilotSessionConfigFactory {

    public SessionConfig createChatOnlyConfig(String model) {
        var config = new SessionConfig()
                .setOnPermissionRequest(CopilotPermissionPolicies.DENY_ALL)
                .setStreaming(false)
                .setAvailableTools(List.of())
                .setSystemMessage(new SystemMessageConfig()
                        .setMode(SystemMessageMode.APPEND)
                        .setContent("""
                                You are running in Herald chat-only mode.
                                Do not rely on or request tools, terminal access, file access, file edits,
                                web access, MCP servers, or any other external actions.
                                Answer only from the prompt and the conversation context provided by the application.
                                If some information is missing, say so clearly instead of inventing it.
                                """));

        if (model != null && !model.isBlank()) {
            config.setModel(model.trim());
        }

        return config;
    }
}