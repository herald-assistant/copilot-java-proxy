package com.acme.herald.copilot.api;

import com.acme.herald.copilot.api.dto.ChatModelResponse;
import com.acme.herald.copilot.usecase.models.CopilotModelsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Models", description = "Lista modeli dostępnych w bieżącym kontekście GitHub Copilot.")
public class ChatModelsController {

    private final CopilotModelsService modelsService;

    public ChatModelsController(CopilotModelsService modelsService) {
        this.modelsService = modelsService;
    }

    @GetMapping(value = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Pobierz listę modeli",
            description = """
                    Zwraca modele dostępne dla aktualnego tokena GitHub Copilot.
                    Token może być przekazany nagłówkiem albo wcześniej zapamiętany w UI connectora.
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
                    description = "Lista modeli dostępnych dla aktualnego tokena.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatModelResponse.class)))
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
    public List<ChatModelResponse> getModels(HttpServletRequest httpReq) {
        return modelsService.getModels(httpReq);
    }
}
