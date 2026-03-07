package com.acme.herald.copilot.api;

import com.acme.herald.copilot.core.ConnectorState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @Autowired
    private ConnectorState state;

    @GetMapping("/healthz")
    public ResponseEntity<String> health() {
        if (state.isEnabled()) {
            return ResponseEntity.ok("ok");
        }

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("not_started");
    }
}