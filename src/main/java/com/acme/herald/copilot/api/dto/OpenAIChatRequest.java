package com.acme.herald.copilot.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "OpenAIChatRequest", description = "Request dla stateless `/chat/completions`.")
public class OpenAIChatRequest {

    @Schema(description = "Opcjonalny model do użycia w danym wywołaniu.", example = "gpt-5", nullable = true)
    public String model; // optional

    @NotNull
    @Valid
    @ArraySchema(schema = @Schema(implementation = Message.class))
    @Schema(description = "Pełna lista wiadomości przekazywana do modelu.")
    public List<Message> messages = new ArrayList<>();

    @Schema(description = "Pole zachowane dla kompatybilności z OpenAI-like API. Obecnie ignorowane.", example = "0.2", nullable = true)
    public Double temperature; // ignored for now

    @Schema(description = "Pole zachowane dla kompatybilności z OpenAI-like API. Obecnie ignorowane.", example = "1024", nullable = true)
    public Integer max_tokens;  // ignored for now

    @Schema(description = "Pole zachowane dla kompatybilności z OpenAI-like API. Obecnie ignorowane.", example = "false", nullable = true)
    public Boolean stream;      // ignored for now (na start false)

    @Schema(name = "OpenAIChatMessage", description = "Pojedyncza wiadomość rozmowy.")
    public static class Message {
        @NotBlank
        @Schema(description = "Rola nadawcy wiadomości.", allowableValues = {"system", "user", "assistant"}, example = "user")
        public String role; // "system" | "user" | "assistant"

        @NotBlank
        @Schema(description = "Treść wiadomości.", example = "Wyjaśnij mi działanie tego serwisu.")
        public String content;
    }
}
