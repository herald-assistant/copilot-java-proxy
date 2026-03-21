package com.acme.herald.copilot.api;

import com.acme.herald.copilot.api.dto.ExplainChatRequest;
import com.acme.herald.copilot.api.dto.OpenAIChatResponse;
import com.acme.herald.copilot.usecase.explain.ExplainConversationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExplainChatController {

    private final ExplainConversationService explainConversationService;

    public ExplainChatController(ExplainConversationService explainConversationService) {
        this.explainConversationService = explainConversationService;
    }

    @PostMapping("/chat/explain/completions")
    public OpenAIChatResponse explainCompletions(
            HttpServletRequest httpReq,
            @Valid @RequestBody ExplainChatRequest req
    ) {
        return explainConversationService.execute(httpReq, req);
    }

    @DeleteMapping("/chat/explain/sessions/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeExplainSession(@PathVariable String conversationId) {
        explainConversationService.close(conversationId);
    }
}