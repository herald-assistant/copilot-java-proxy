package com.acme.herald.copilot.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(name = "OpenAIChatResponse", description = "OpenAI-like odpowiedź z jedną wiadomością asystenta.")
public class OpenAIChatResponse {

    @Schema(description = "Id odpowiedzi generowany po stronie backendu.", example = "chatcmpl_123456", accessMode = Schema.AccessMode.READ_ONLY)
    public String id = "chatcmpl_" + UUID.randomUUID();

    @Schema(description = "Typ obiektu odpowiedzi.", example = "chat.completion", accessMode = Schema.AccessMode.READ_ONLY)
    public String object = "chat.completion";

    @Schema(description = "Czas utworzenia odpowiedzi w sekundach Unix epoch.", example = "1743072000", accessMode = Schema.AccessMode.READ_ONLY)
    public long created = Instant.now().getEpochSecond();

    @Schema(description = "Model zwrócony w odpowiedzi.", example = "gpt-5", nullable = true)
    public String model;

    @ArraySchema(schema = @Schema(implementation = Choice.class))
    @Schema(description = "Lista wyborów odpowiedzi. Aktualnie zawsze jeden element.")
    public List<Choice> choices;

    @Schema(name = "OpenAIChatChoice", description = "Pojedynczy wybór odpowiedzi.")
    public static class Choice {
        @Schema(description = "Indeks wyboru.", example = "0")
        public int index;

        @Schema(description = "Wiadomość asystenta.")
        public Message message;

        @Schema(description = "Powód zakończenia generacji.", example = "stop")
        public String finish_reason = "stop";
    }

    @Schema(name = "OpenAIChatAssistantMessage", description = "Wiadomość asystenta zwrócona w odpowiedzi.")
    public static class Message {
        @Schema(description = "Rola nadawcy wiadomości.", example = "assistant")
        public String role = "assistant";

        @Schema(description = "Treść wygenerowanej odpowiedzi.")
        public String content;

        public Message(String content) {
            this.content = content;
        }
    }
}
