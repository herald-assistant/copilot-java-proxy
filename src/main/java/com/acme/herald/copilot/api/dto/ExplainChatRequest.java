package com.acme.herald.copilot.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ExplainChatRequest {

    @NotBlank
    public String conversationId;

    public String model; // optional

    @NotNull
    public List<OpenAIChatRequest.Message> messages = new ArrayList<>();

    /**
     * Gdy true, backend zamknie starą sesję o tym conversationId i utworzy nową.
     * Przydaje się np. po zmianie system prompta lub pełnym restarcie rozmowy w UI.
     */
    public Boolean reset;
}