package com.acme.herald.copilot.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "ExplainChatRequest", description = "Request dla stateful `/chat/explain/completions`.")
public class ExplainChatRequest {

    @NotBlank
    @Schema(description = "Stabilny identyfikator sesji explain.", example = "analysis-42")
    public String conversationId;

    @Schema(description = "Opcjonalny model przypisany do sesji explain.", example = "gpt-5", nullable = true)
    public String model; // optional

    /**
     * Opcjonalna lista plików, które mają zostać przypięte do sesji explain.
     * Przy kolejnych żądaniach można pominąć to pole, a backend użyje listy
     * zapisanej już w sesji.
     */
    @ArraySchema(schema = @Schema(type = "string", example = "C:\\work\\repo\\src\\main\\java\\App.java"))
    @Schema(description = "Opcjonalna lista istniejących plików na dysku do przypięcia do sesji.")
    public List<@NotBlank String> files = new ArrayList<>();

    /**
     * Opcjonalna lista plików przesyłanych inline jako name + content. Backend
     * zapisuje je tymczasowo na dysku w katalogu sesji i dołącza do Copilota jak
     * zwykłe pliki.
     */
    @Valid
    @ArraySchema(schema = @Schema(implementation = InlineFile.class))
    @Schema(description = "Opcjonalna lista plików przekazywanych inline jako name + content.")
    public List<@Valid InlineFile> inlineFiles = new ArrayList<>();

    @NotNull
    @Valid
    @ArraySchema(schema = @Schema(implementation = OpenAIChatRequest.Message.class))
    @Schema(description = "Wiadomości przekazywane do sesji explain.")
    public List<OpenAIChatRequest.Message> messages = new ArrayList<>();

    /**
     * Gdy true, backend zamknie starą sesję o tym conversationId i utworzy nową.
     * Przydaje się np. po zmianie system prompta lub pełnym restarcie rozmowy w UI.
     */
    @Schema(description = "Gdy `true`, backend zamknie starą sesję i utworzy nową.", example = "false", nullable = true)
    public Boolean reset;

    @Schema(name = "ExplainInlineFile", description = "Plik przekazywany inline jako nazwa i treść.")
    public static class InlineFile {
        @NotBlank
        @Schema(description = "Relatywna nazwa pliku w katalogu sesji.", example = "docs/confluence-summary.md")
        public String name;

        @NotNull
        @Schema(description = "Treść pliku zapisywana tymczasowo na dysku w UTF-8.", example = "# Podsumowanie\n\nTreść wygenerowana z Confluence.")
        public String content;
    }
}
