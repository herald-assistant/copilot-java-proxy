package com.acme.herald.copilot.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CopilotWorkerLauncher {

    private final ObjectMapper om;
    private final Duration timeout;

    public CopilotWorkerLauncher(ObjectMapper om, Duration timeout) {
        this.om = om;
        this.timeout = timeout;
    }

    public String runOnce(String githubToken, String model, String prompt) throws Exception {
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path"); // w dockerze: /app/app.jar

        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "-Dloader.main=com.acme.herald.copilot.worker.CopilotWorkerMain",
                "org.springframework.boot.loader.launch.PropertiesLauncher"
        );

        pb.environment().put("COPILOT_GITHUB_TOKEN", githubToken);

        Process p = pb.start();

        try (OutputStream os = p.getOutputStream()) {
            byte[] in = om.writeValueAsBytes(Map.of(
                    "model", model,
                    "prompt", prompt
            ));
            os.write(in);
            os.flush();
        }

        boolean finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("Copilot worker timed out after " + timeout.toMillis() + "ms");
        }

        String stdout = readAll(p.getInputStream());
        String stderr = readAll(p.getErrorStream());

        if (p.exitValue() != 0) {
            System.err.println("Copilot worker stderr:\n" + stderr);
            throw new RuntimeException("Copilot worker failed. exit=" + p.exitValue() + ", stderr=" + truncate(stderr, 4000));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> out = om.readValue(stdout, Map.class);
        Object c = out.get("content");
        return c == null ? "" : String.valueOf(c);
    }

    private static String readAll(InputStream is) throws IOException {
        try (is) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }
}