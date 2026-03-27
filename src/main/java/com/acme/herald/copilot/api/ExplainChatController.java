package com.acme.herald.copilot.api;

import com.acme.herald.copilot.api.dto.ExplainChatRequest;
import com.acme.herald.copilot.api.dto.OpenAIChatResponse;
import com.acme.herald.copilot.usecase.explain.ExplainConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Explain Chat", description = "Stateful endpoint do rozmów explain z utrzymaniem sesji po conversationId.")
public class ExplainChatController {

    private final ExplainConversationService explainConversationService;

    public ExplainChatController(ExplainConversationService explainConversationService) {
        this.explainConversationService = explainConversationService;
    }

    @PostMapping(
            value = "/chat/explain/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Stateful explain completion",
            description = """
                    Wysyła wiadomość do sesji explain utrzymywanej po `conversationId`.
                    Backend może przechowywać model, listę plików i inline files przez czas życia sesji.
                    """,
            security = {
                    @SecurityRequirement(name = "bearerAuth"),
                    @SecurityRequirement(name = "githubTokenHeader")
            }
    )
    @Parameters({
            @Parameter(
                    name = "Authorization",
                    in = ParameterIn.HEADER,
                    description = "Opcjonalny nagłówek `Bearer <GitHub PAT>`.",
                    required = false,
                    schema = @Schema(type = "string")
            ),
            @Parameter(
                    name = "X-GitHub-Token",
                    in = ParameterIn.HEADER,
                    description = "Opcjonalny alternatywny nagłówek z GitHub PAT.",
                    required = false,
                    schema = @Schema(type = "string")
            )
    })
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Odpowiedź asystenta dla sesji explain.",
                    content = @Content(schema = @Schema(implementation = OpenAIChatResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Niepoprawne body żądania lub błędne ścieżki plików.",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Brak poprawnego tokena GitHub.",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Connector jest wyłączony w UI.",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public OpenAIChatResponse explainCompletions(
            HttpServletRequest httpReq,
            @Valid @RequestBody ExplainChatRequest req
    ) {
        return explainConversationService.execute(httpReq, req);
    }

    @DeleteMapping("/chat/explain/sessions/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Zamknij sesję explain",
            description = "Usuwa sesję explain i czyści pliki tymczasowe zapisane dla tej sesji."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesja została zamknięta albo nie istniała."),
            @ApiResponse(responseCode = "400", description = "Niepoprawny conversationId.")
    })
    public void closeExplainSession(@PathVariable String conversationId) {
        explainConversationService.close(conversationId);
    }
}
