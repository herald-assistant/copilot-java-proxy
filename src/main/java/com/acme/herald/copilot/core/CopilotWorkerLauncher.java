package com.acme.herald.copilot.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class CopilotWorkerLauncher {

    private final ObjectMapper om;
    private final ServletContext servletContext;

    public CopilotWorkerLauncher(ObjectMapper om, ServletContext servletContext) {
        this.om = om;
        this.servletContext = servletContext;
    }

    public String runOnce(String githubToken, String model, String prompt, Duration timeout) throws Exception {
        ProcessBuilder pb = buildProcess(githubToken);

        Process p = pb.start();

        try (OutputStream os = p.getOutputStream()) {
            byte[] in = om.writeValueAsBytes(Map.of("model", model, "prompt", prompt));
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

    private ProcessBuilder buildProcess(String githubToken) throws Exception {
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

        // Heurystyka: jeśli mamy servletContext i realPath WEB-INF/classes istnieje, jesteśmy w exploded WAR
        String classesPath = servletContext.getRealPath("/WEB-INF/classes");
        String libPath = servletContext.getRealPath("/WEB-INF/lib");

        if (classesPath != null && Files.exists(Path.of(classesPath))) {
            // WAR / Tomcat mode
            String cp = buildWarClasspath(classesPath, libPath);
            ProcessBuilder pb = new ProcessBuilder(
                    javaBin,
                    "-cp", cp,
                    "com.acme.herald.copilot.worker.CopilotWorkerMain"
            );
            pb.environment().put("COPILOT_GITHUB_TOKEN", githubToken);
            return pb;
        }

        // Fallback: JAR mode (jak u Ciebie w dockerze)
        String classpath = System.getProperty("java.class.path");
        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "-Dloader.main=com.acme.herald.copilot.worker.CopilotWorkerMain",
                "org.springframework.boot.loader.launch.PropertiesLauncher"
        );
        pb.environment().put("COPILOT_GITHUB_TOKEN", githubToken);
        return pb;
    }

    private String buildWarClasspath(String classesPath, String libPath) throws IOException {
        Path classes = Path.of(classesPath);

        String jars = "";
        if (libPath != null && Files.exists(Path.of(libPath))) {
            jars = Files.list(Path.of(libPath))
                    .filter(p -> p.toString().endsWith(".jar"))
                    .map(Path::toString)
                    .collect(Collectors.joining(File.pathSeparator));
        }

        if (jars.isBlank()) {
            return classes.toString();
        }
        return classes + File.pathSeparator + jars;
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