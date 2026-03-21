package com.acme.herald.copilot.api;

import com.acme.herald.copilot.api.dto.OpenAIChatRequest;
import com.acme.herald.copilot.api.dto.OpenAIChatResponse;
import com.acme.herald.copilot.usecase.stateless.StatelessCopilotChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatCompletionsController {

    private final StatelessCopilotChatService chatService;

    public ChatCompletionsController(StatelessCopilotChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat/completions")
    public OpenAIChatResponse chatCompletions(
            HttpServletRequest httpReq,
            @Valid @RequestBody OpenAIChatRequest req
    ) {
        return chatService.execute(httpReq, req);
    }
}