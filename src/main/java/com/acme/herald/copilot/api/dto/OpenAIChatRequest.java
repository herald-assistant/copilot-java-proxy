package com.acme.herald.copilot.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OpenAIChatRequest {

    public String model; // optional
    @NotNull
    public List<Message> messages = new ArrayList<>();

    public Double temperature; // ignored for now
    public Integer max_tokens;  // ignored for now
    public Boolean stream;      // ignored for now (na start false)

    public static class Message {
        @NotBlank
        public String role; // "system" | "user" | "assistant"
        @NotBlank
        public String content;
    }
}