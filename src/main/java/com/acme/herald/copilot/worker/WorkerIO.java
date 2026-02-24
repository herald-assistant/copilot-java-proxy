package com.acme.herald.copilot.worker;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class WorkerIO {

    private WorkerIO() {}

    public static Map<String, Object> readJsonFromStdin(ObjectMapper om) throws Exception {
        InputStream in = System.in;
        byte[] bytes = in.readAllBytes();
        String s = new String(bytes, StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = om.readValue(s, Map.class);
        return m;
    }

    public static void writeJsonToStdout(ObjectMapper om, Map<String, Object> obj) throws Exception {
        byte[] out = om.writeValueAsBytes(obj);
        System.out.write(out);
        System.out.flush();
    }
}