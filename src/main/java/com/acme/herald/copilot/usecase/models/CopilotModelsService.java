package com.acme.herald.copilot.usecase.models;

import com.acme.herald.copilot.core.auth.CopilotTokenResolver;
import com.acme.herald.copilot.core.client.CopilotClientFactory;
import com.acme.herald.copilot.core.error.CopilotExceptionMapper;
import com.github.copilot.sdk.json.ModelInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CopilotModelsService {

    private static final int CLIENT_START_TIMEOUT_SECONDS = 30;
    private static final int LIST_MODELS_TIMEOUT_SECONDS = 30;

    private final CopilotTokenResolver tokenResolver;
    private final CopilotClientFactory clientFactory;

    public CopilotModelsService(
            CopilotTokenResolver tokenResolver,
            CopilotClientFactory clientFactory
    ) {
        this.tokenResolver = tokenResolver;
        this.clientFactory = clientFactory;
    }

    public List<ModelInfo> getModels(HttpServletRequest httpRequest) {
        String token = tokenResolver.resolveOrThrow(httpRequest);

        try (var client = clientFactory.create(token)) {
            client.start().get(CLIENT_START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return client.listModels().get(LIST_MODELS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw CopilotExceptionMapper.mapExecutionFailure(CopilotModelsService.class, e);
        }
    }
}