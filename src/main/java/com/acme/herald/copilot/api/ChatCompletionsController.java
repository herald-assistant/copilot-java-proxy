package com.acme.herald.copilot.api;

import com.acme.herald.copilot.api.dto.OpenAIChatRequest;
import com.acme.herald.copilot.api.dto.OpenAIChatResponse;
import com.acme.herald.copilot.usecase.stateless.StatelessCopilotChatService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Chat", description = "Stateless endpoint do pojedynczych wywołań chat completion.")
public class ChatCompletionsController {

    private final StatelessCopilotChatService chatService;

    public ChatCompletionsController(StatelessCopilotChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(
            value = "/chat/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Stateless chat completion",
            description = """
                    Wysyła pełną listę wiadomości do Copilota bez utrzymywania sesji po stronie backendu.
                    To endpoint odpowiedni dla jednorazowych zapytań albo klientów, którzy sami zarządzają historią rozmowy.
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
                    description = "Odpowiedź asystenta.",
                    content = @Content(schema = @Schema(implementation = OpenAIChatResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Niepoprawne body żądania.",
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
    public OpenAIChatResponse chatCompletions(
            HttpServletRequest httpReq,
            @Valid @RequestBody OpenAIChatRequest req
    ) {
        return chatService.execute(httpReq, req);
    }
}
