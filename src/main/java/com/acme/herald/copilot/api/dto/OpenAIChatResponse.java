package com.acme.herald.copilot.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OpenAIChatResponse {

    public String id = "chatcmpl_" + UUID.randomUUID();
    public String object = "chat.completion";
    public long created = Instant.now().getEpochSecond();
    public String model;

    public List<Choice> choices;

    public static class Choice {
        public int index;
        public Message message;
        public String finish_reason = "stop";
    }

    public static class Message {
        public String role = "assistant";
        public String content;

        public Message(String content) {
            this.content = content;
        }
    }
}