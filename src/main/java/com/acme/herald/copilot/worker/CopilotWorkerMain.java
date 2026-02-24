package com.acme.herald.copilot.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionIdleEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CopilotWorkerMain {

    public static void main(String[] args) throws Exception {
        ObjectMapper om = new ObjectMapper();
        Map<String, Object> in = WorkerIO.readJsonFromStdin(om);

        String model = String.valueOf(in.getOrDefault("model", "claude-sonnet-4.5"));
        String prompt = String.valueOf(in.getOrDefault("prompt", ""));

        // Izolacja per request (configDir) – przydaje się też do rozdzielenia workspace
        Path cfg = Files.createTempDirectory("copilot-config-");

        StringBuilder content = new StringBuilder();
        CompletableFuture<Void> done = new CompletableFuture<>();

        try (var client = new CopilotClient()) {
            client.start().get();

            var session = client.createSession(
                    new SessionConfig()
                            .setModel(model)
                            .setConfigDir(cfg.toString())
            ).get();

            session.on(AssistantMessageEvent.class, msg -> {
                // w praktyce eventy mogą przychodzić porcjami
                content.append(msg.getData().content());
            });

            session.on(SessionIdleEvent.class, idle -> done.complete(null));

            // send + wait for idle
            session.send(new MessageOptions().setPrompt(prompt)).get();
            done.get();
        } finally {
            // best-effort cleanup
            try { deleteRecursively(cfg); } catch (Exception ignored) {}
        }

        WorkerIO.writeJsonToStdout(om, Map.of("content", content.toString()));
    }

    private static void deleteRecursively(Path dir) throws Exception {
        if (dir == null) return;
        if (!Files.exists(dir)) return;
        try (var s = Files.walk(dir)) {
            s.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            });
        }
    }
}