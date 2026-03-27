package com.acme.herald.copilot.api;

import com.acme.herald.copilot.core.ConnectorState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Prosty endpoint stanu lokalnego connectora.")
public class HealthController {

    @Autowired
    private ConnectorState state;

    @GetMapping("/healthz")
    @Operation(summary = "Sprawdź stan connectora", description = "Zwraca prosty tekstowy status lokalnej aplikacji.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Connector jest włączony.",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(value = "ok")
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Connector jest wyłączony.",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(value = "not_started")
                    )
            )
    })
    public ResponseEntity<String> health() {
        if (state.isEnabled()) {
            return ResponseEntity.ok("ok");
        }

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("not_started");
    }
}
