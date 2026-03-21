package com.acme.herald.copilot.core.client;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.CopilotClientOptions;
import org.springframework.stereotype.Component;

@Component
public class CopilotClientFactory {

    public CopilotClient create(String githubToken) {
        return new CopilotClient(buildOptions(githubToken));
    }

    public CopilotClientOptions buildOptions(String githubToken) {
        var options = new CopilotClientOptions();
        options.setGitHubToken(githubToken);
        options.setUseLoggedInUser(false);
        options.setCliPath("copilot");
        options.setLogLevel("info");
        options.setAutoStart(false);
        options.setAutoRestart(true);
        return options;
    }
}