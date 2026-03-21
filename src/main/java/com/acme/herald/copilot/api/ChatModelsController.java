package com.acme.herald.copilot.api;

import com.acme.herald.copilot.usecase.models.CopilotModelsService;
import com.github.copilot.sdk.json.ModelInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChatModelsController {

    private final CopilotModelsService modelsService;

    public ChatModelsController(CopilotModelsService modelsService) {
        this.modelsService = modelsService;
    }

    @GetMapping("/models")
    public List<ModelInfo> getModels(HttpServletRequest httpReq) {
        return modelsService.getModels(httpReq);
    }
}