package com.acme.herald.copilot.core.client;

import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PermissionRequestResultKind;

import java.util.concurrent.CompletableFuture;

public final class CopilotPermissionPolicies {

    private CopilotPermissionPolicies() {
    }

    public static final PermissionHandler DENY_ALL = (request, invocation) ->
            CompletableFuture.completedFuture(
                    new PermissionRequestResult()
                            .setKind(PermissionRequestResultKind.DENIED_BY_RULES)
            );
}