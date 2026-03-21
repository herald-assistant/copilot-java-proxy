package com.acme.herald.copilot.core.chat;

import com.acme.herald.copilot.api.dto.OpenAIChatRequest;
import com.acme.herald.copilot.api.dto.OpenAIChatResponse;

import java.util.List;

public final class OpenAiMessagesMapper {

    private OpenAiMessagesMapper() {
    }

    public static String flattenConversationForOneShotPrompt(List<OpenAIChatRequest.Message> messages) {
        StringBuilder sb = new StringBuilder();

        for (OpenAIChatRequest.Message message : messages) {
            String role = normalizeRole(message.role);

            if ("system".equals(role)) {
                sb.append("System: ");
            } else if ("assistant".equals(role)) {
                sb.append("Assistant: ");
            } else {
                sb.append("User: ");
            }

            sb.append(message.content).append("\n\n");
        }

        return sb.toString().trim();
    }

    public static String extractLatestUserPromptOrFallback(List<OpenAIChatRequest.Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            OpenAIChatRequest.Message message = messages.get(i);
            String role = normalizeRole(message.role);
            if (!"user".equals(role)) {
                continue;
            }

            if (message.content != null && !message.content.isBlank()) {
                return message.content.trim();
            }
        }

        return flattenConversationForOneShotPrompt(messages);
    }

    public static OpenAIChatResponse toChatResponse(String model, String content) {
        var response = new OpenAIChatResponse();
        response.model = model;

        var choice = new OpenAIChatResponse.Choice();
        choice.index = 0;
        choice.message = new OpenAIChatResponse.Message(content);

        response.choices = List.of(choice);
        return response;
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "user";
        }
        return role.trim().toLowerCase();
    }
}